package io.github.bchmsl.keel.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeTest {

    private val light = Theme("light-only", "Light only", "#ffffff", supportsDark = false)
    private val dark = Theme("dark-only", "Dark only", "#000000", supportsLight = false)
    private val both = Theme("both", "Both", "#888888")

    // ------------------------------------------------------------- a theme

    @Test
    fun aThemeSupportsBothModesUnlessItSaysOtherwise() {
        assertTrue(both.supportsLight)
        assertTrue(both.supportsDark)
    }

    @Test
    fun anIdMustBeUsableInAllThreePlacesItGoes() {
        // An id becomes an attribute value, part of the CSS selector
        // [data-theme='...'], and a quoted string inside the generated boot script.
        // Anything outside this shape is safe in some of those and not others, and
        // the failures are silent: a palette that never applies, or a script that
        // does not parse.
        listOf("coral", "dark-only", "theme2", "a1-b2-c3").forEach {
            Theme(it, "Fine", "#000000")
        }

        listOf(
            // Blank resolves to the fallback for ever, which looks like the theme
            // simply not working. `Coral` matches no attribute selector, since those
            // are case-sensitive here. `it's` would close the quote in the generated
            // boot script. The hyphen shapes are ruled out to keep ids to one form.
            "",
            "   ",
            "Coral",
            "my theme",
            "it's",
            "-leading",
            "trailing-",
            "double--hyphen",
        ).forEach {
            assertFailsWith<IllegalArgumentException>("'$it' should be refused") {
                Theme(it, "Bad", "#000000")
            }
        }
    }

    @Test
    fun aThemeSupportingNeitherModeIsRefused() {
        assertFailsWith<IllegalArgumentException> {
            Theme("void", "Void", "#000000", supportsLight = false, supportsDark = false)
        }
    }

    // --------------------------------------------------------- a catalogue

    @Test
    fun fromId_resolvesEveryThemeItHolds() {
        val catalog = ThemeCatalog(listOf(light, dark, both), default = both)
        listOf(light, dark, both).forEach { assertEquals(it, catalog.fromId(it.id)) }
    }

    @Test
    fun fromId_fallsBackForAnythingUnrecognised() {
        val catalog = ThemeCatalog(listOf(light, both), default = both)

        // Null is the first visit; the other two are a palette that has since been
        // removed and a value edited by hand. All three are the same answer.
        assertEquals(both, catalog.fromId(null))
        assertEquals(both, catalog.fromId("dark-only"))
        assertEquals(both, catalog.fromId(""))
    }

    @Test
    fun anEmptyCatalogueIsRefused() {
        assertFailsWith<IllegalArgumentException> {
            ThemeCatalog(emptyList(), default = both)
        }
    }

    @Test
    fun aDefaultOutsideTheCatalogueIsRefused() {
        // Otherwise every unrecognised id resolves to a palette with no CSS block.
        assertFailsWith<IllegalArgumentException> {
            ThemeCatalog(listOf(light, dark), default = both)
        }
    }

    @Test
    fun duplicateIdsAreRefused() {
        val clash = Theme("both", "A second Both", "#111111")

        val error = assertFailsWith<IllegalArgumentException> {
            ThemeCatalog(listOf(both, clash), default = both)
        }
        assertTrue("both" in error.message.orEmpty(), "the message should name the clash")
    }

    @Test
    fun hasChoice_isFalseForASinglePaletteApp() {
        assertFalse(ThemeCatalog(listOf(dark), default = dark).hasChoice)
        assertTrue(ThemeCatalog(listOf(dark, both), default = dark).hasChoice)
    }

    // -------------------------------------------------------- a colour mode

    @Test
    fun colorMode_resolvesStoredIdsAndFallsBackToSystem() {
        ColorMode.entries.forEach { assertEquals(it, ColorMode.fromId(it.id)) }
        assertEquals(ColorMode.System, ColorMode.fromId(null))
        assertEquals(ColorMode.System, ColorMode.fromId("sepia"))
        assertEquals(ColorMode.System, ColorMode.Default)
    }

    // ------------------------------------------------- what to offer

    @Test
    fun aThemeWithBothModesOffersAllThree() {
        assertEquals(
            listOf(ColorMode.Light, ColorMode.Dark, ColorMode.System),
            both.availableColorModes,
        )
    }

    @Test
    fun aSingleModeThemeOffersOnlyThatMode() {
        // One entry means there is no choice, so a picker built from this shows
        // nothing rather than one useless option.
        assertEquals(listOf(ColorMode.Light), light.availableColorModes)
        assertEquals(listOf(ColorMode.Dark), dark.availableColorModes)
    }

    // ------------------------------------------------------------ resolving

    @Test
    fun anExplicitChoiceIgnoresTheDevice() {
        assertFalse(both.resolvesToDark(ColorMode.Light, systemPrefersDark = true))
        assertTrue(both.resolvesToDark(ColorMode.Dark, systemPrefersDark = false))
    }

    @Test
    fun systemFollowsTheDevice() {
        assertTrue(both.resolvesToDark(ColorMode.System, systemPrefersDark = true))
        assertFalse(both.resolvesToDark(ColorMode.System, systemPrefersDark = false))
    }

    @Test
    fun aSingleModeThemeWinsOverTheStoredPreference() {
        // Someone who chose Light under one theme and then picked a dark-only theme
        // must not be shown an unpainted page. Their choice is kept, not applied.
        ColorMode.entries.forEach { mode ->
            assertTrue(dark.resolvesToDark(mode, systemPrefersDark = false), "dark-only, $mode")
            assertTrue(dark.resolvesToDark(mode, systemPrefersDark = true), "dark-only, $mode")
            assertFalse(light.resolvesToDark(mode, systemPrefersDark = true), "light-only, $mode")
            assertFalse(light.resolvesToDark(mode, systemPrefersDark = false), "light-only, $mode")
        }
    }
}

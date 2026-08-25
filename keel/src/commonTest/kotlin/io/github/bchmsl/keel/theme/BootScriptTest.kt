package io.github.bchmsl.keel.theme

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The shape of the generated script. That it *decides* the same thing the Kotlin
 * decides is `BootScriptExecutionTest`, which runs it.
 */
class BootScriptTest {

    private val dark = Theme("cinema", "Cinema", "#e1352f", supportsLight = false)
    private val light = Theme("paper", "Paper", "#ffffff", supportsDark = false)

    @Test
    fun itReadsTheKeysItWasGiven() {
        val script = bootScript(
            KeelThemes.Standard,
            StorageKeys(theme = "palette", colorMode = "appearance"),
        )

        assertTrue("localStorage.getItem('palette')" in script)
        assertTrue("localStorage.getItem('appearance')" in script)
    }

    @Test
    fun itFallsBackToTheCataloguesDefault() {
        assertTrue("|| 'coral'" in bootScript(KeelThemes.Standard))
        assertTrue(
            "|| 'cinema'" in bootScript(ThemeCatalog(listOf(dark), default = dark)),
        )
    }

    @Test
    fun aCatalogueOfBothModeThemesForcesNothing() {
        // The common case. An empty lookup means every id falls through to the
        // stored preference, which is all the script used to do.
        assertTrue("var forced = {};" in bootScript(KeelThemes.Standard))
    }

    @Test
    fun aSingleModeThemeIsForcedToItsOneMode() {
        assertTrue("'cinema': true" in bootScript(ThemeCatalog(listOf(dark), default = dark)))
        assertTrue("'paper': false" in bootScript(ThemeCatalog(listOf(light), default = light)))
    }

    @Test
    fun onlySingleModeThemesAreNamed() {
        val mixed = ThemeCatalog(KeelThemes.All + dark, default = KeelThemes.Coral)
        val script = bootScript(mixed)

        assertTrue("'cinema': true" in script)
        KeelThemes.All.forEach {
            assertFalse("'${it.id}':" in script, "${it.id} has both modes and needs no entry")
        }
    }

    @Test
    fun theLookupCannotBorrowAnAnswerFromObjectPrototype() {
        // `'toString' in forced` is true for any plain object, so a bare `in` would
        // read a function as a boolean and paint dark for a palette called toString.
        // Theme's id rule already forbids that id, but the script is what a consumer
        // pastes into a page, and it should not depend on that.
        assertTrue(
            "Object.prototype.hasOwnProperty.call(forced, id)" in bootScript(KeelThemes.Standard),
        )
    }
}

package io.github.bchmsl.keel.theme

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs the generated script and holds its answer against [Theme.resolvesToDark].
 *
 * This is the test the bug it guards deserved. The script and the Kotlin decide the
 * same thing in two languages, and nothing but executing both can show they agree -
 * a structural assertion on the text would have passed just as happily on the
 * version that ignored single-mode themes entirely.
 *
 * Node has no `document`, `localStorage` or `matchMedia`, so the script runs against
 * stubs that record what it did. On Node rather than in `commonTest` for the obvious
 * reason: only a Javascript host can run Javascript.
 */
class BootScriptExecutionTest {

    private val cinema = Theme("cinema", "Cinema", "#e1352f", supportsLight = false)
    private val paper = Theme("paper", "Paper", "#ffffff", supportsDark = false)
    private val coral = KeelThemes.Coral

    private val catalog = ThemeCatalog(listOf(coral, cinema, paper), default = coral)

    @Test
    fun theScriptAgreesWithTheKotlinForEveryCombination() {
        val combinations = catalog.themes.flatMap { theme ->
            ColorMode.entries.flatMap { mode ->
                listOf(true, false).map { prefersDark -> Triple(theme, mode, prefersDark) }
            }
        }

        combinations.forEach { (theme, mode, prefersDark) ->
            val fromScript = runBootScript(
                script = bootScript(catalog),
                storedTheme = theme.id,
                storedMode = mode.id,
                prefersDark = prefersDark,
            )

            assertEquals(
                theme.resolvesToDark(mode, prefersDark),
                fromScript.dark,
                "${theme.id} / ${mode.id} / prefersDark=$prefersDark",
            )
            assertEquals(theme.id, fromScript.dataTheme, "the palette it selected")
        }
    }

    @Test
    fun anEmptyStoreGetsTheCataloguesDefault() {
        // A first visit. Nothing stored, so the script has to invent the same answer
        // the controller would.
        val result = runBootScript(bootScript(catalog), null, null, prefersDark = true)

        assertEquals(coral.id, result.dataTheme)
        assertEquals(coral.resolvesToDark(ColorMode.Default, true), result.dark)
    }

    @Test
    fun anUnknownStoredThemeIsLeftAloneRatherThanForced() {
        // The script cannot resolve an unknown id the way `ThemeCatalog.fromId` does,
        // and must not pretend to: it sets the id it found and falls through to the
        // stored preference. The controller corrects the palette a moment later.
        val result = runBootScript(bootScript(catalog), "sepia", "dark", prefersDark = false)

        assertEquals("sepia", result.dataTheme)
        assertEquals(true, result.dark)
    }

    @Test
    fun storageThatThrowsIsSurvived() {
        // Safari's private mode does not return null from localStorage, it throws.
        // Without the try/catch the page would fail to paint because it could not
        // look up a colour.
        val result =
            runBootScript(bootScript(catalog), null, null, prefersDark = false, throwing = true)

        assertEquals(null, result.dataTheme, "nothing should have been set")
        assertEquals(false, result.dark)
    }
}

/** What the script did to its stub document. */
private class BootResult(val dataTheme: String?, val dark: Boolean)

/**
 * Executes [script] against stubbed browser globals and reports the outcome.
 *
 * The stubs are the smallest surface the script touches: one attribute, one class
 * list, one storage and one media query. Building them by hand rather than reaching
 * for a DOM implementation keeps the test's subject the script rather than jsdom.
 */
private fun runBootScript(
    script: String,
    storedTheme: String?,
    storedMode: String?,
    prefersDark: Boolean,
    throwing: Boolean = false,
): BootResult {
    val harness = js("{}")
    harness.script = script
    harness.storedTheme = storedTheme
    harness.storedMode = storedMode
    harness.prefersDark = prefersDark
    harness.throwing = throwing

    val raw = js(
        """
        (function (h) {
          var attributes = {};
          var classes = {};
          var document = {
            documentElement: {
              setAttribute: function (name, value) { attributes[name] = value; },
              classList: {
                toggle: function (name, on) { classes[name] = !!on; }
              }
            }
          };
          var localStorage = {
            getItem: function (key) {
              if (h.throwing) throw new Error('access denied');
              if (key === 'themeId') return h.storedTheme;
              if (key === 'colorMode') return h.storedMode;
              return null;
            }
          };
          var window = {
            matchMedia: function () { return { matches: h.prefersDark }; }
          };
          // Indirect eval would not see these locals, so a Function with them as
          // parameters is what puts the stubs in the script's scope.
          new Function('document', 'localStorage', 'window', h.script)(
            document, localStorage, window
          );
          return { dataTheme: attributes['data-theme'], dark: !!classes['dark'] };
        })
        """,
    )(harness)

    return BootResult(raw.dataTheme as String?, raw.dark as Boolean)
}

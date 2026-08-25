package io.github.bchmsl.keel.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeelThemesTest {

    @Test
    fun theStandardCatalogueHoldsEveryShippedPalette() {
        assertEquals(KeelThemes.All, KeelThemes.Standard.themes)
    }

    @Test
    fun coralIsTheDefault() {
        // It is also the palette declared on bare `:root` in tokens.css. Changing
        // this without moving that block would make the frame the boot script paints
        // disagree with every frame after it.
        assertEquals(KeelThemes.Coral, KeelThemes.Standard.default)
        assertEquals("coral", KeelThemes.Standard.default.id)
    }

    @Test
    fun everyShippedPaletteHasBothModes() {
        // A dark-only palette is legal, but none of the six is one, and the stylesheet
        // ships a `.dark` block for each. This is what would catch the two disagreeing.
        KeelThemes.All.forEach { theme ->
            assertTrue(theme.supportsLight, "${theme.id} should have a light form")
            assertTrue(theme.supportsDark, "${theme.id} should have a dark form")
        }
    }

    @Test
    fun everySwatchIsASixDigitHex() {
        // The picker paints these directly. A malformed one is not an error anywhere,
        // it is simply an unpainted swatch.
        val sixDigitHex = Regex("^#[0-9a-f]{6}$")
        KeelThemes.All.forEach { theme ->
            assertTrue(
                sixDigitHex.matches(theme.accentHex),
                "${theme.id} has accentHex '${theme.accentHex}'",
            )
        }
    }

    @Test
    fun everyPaletteHasALabelAndTheyAreAllDistinct() {
        val labels = KeelThemes.All.map { it.label }
        assertTrue(labels.none { it.isBlank() }, "a palette with no label cannot be offered")
        assertEquals(labels.size, labels.distinct().size, "two palettes share a label")
    }
}

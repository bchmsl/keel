package io.github.bchmsl.keel.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwatchesTest {

    @Test
    fun theDefaultIsTheFirstSwatchOffered() {
        // So the picker opens with its first entry already selected rather than with
        // a selection the user cannot see.
        assertEquals(Swatches.All.first(), Swatches.Default)
    }

    @Test
    fun everySwatchIsADistinctSixDigitHex() {
        val sixDigitHex = Regex("^#[0-9a-f]{6}$")
        Swatches.All.forEach { assertTrue(sixDigitHex.matches(it), "swatch '$it'") }

        // Two identical swatches are two labels the user cannot tell apart, which is
        // the one thing a fixed palette exists to prevent.
        assertEquals(Swatches.All.size, Swatches.All.distinct().size)
    }

    @Test
    fun aShadeIsAppendedAsAlphaLeavingTheStoredDigitsIntact() {
        assertEquals("#6366f120", swatchBackground("#6366f1", SwatchShade.Pill))
        assertEquals("#6366f115", swatchBackground("#6366f1", SwatchShade.Faint))
    }

    @Test
    fun everyShadeIsATwoDigitHexAlpha() {
        // Eight-digit hex only parses if the alpha is exactly two digits; one digit
        // silently makes a four-digit shorthand out of a three-digit colour.
        val twoDigitHex = Regex("^[0-9a-f]{2}$")
        SwatchShade.entries.forEach {
            assertTrue(twoDigitHex.matches(it.hexAlpha), "${it.name} is '${it.hexAlpha}'")
        }
    }

    @Test
    fun theShadesRunFromFaintestToStrongest() {
        // The names are only meaningful if the order matches. A shade list that had
        // drifted out of order would read correctly and paint wrongly.
        val alphas = SwatchShade.entries.map { it.hexAlpha.toInt(radix = 16) }
        assertEquals(alphas.sorted(), alphas)
    }

    @Test
    fun aSwatchIsPassedThroughRatherThanValidated() {
        // Deliberate: substituting a colour the user did not pick would hide the data
        // problem instead of showing it.
        assertEquals("not-a-colour20", swatchBackground("not-a-colour", SwatchShade.Pill))
    }
}

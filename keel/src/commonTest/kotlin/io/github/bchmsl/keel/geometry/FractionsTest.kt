package io.github.bchmsl.keel.geometry

import kotlin.test.Test
import kotlin.test.assertEquals

class FractionsTest {

    @Test
    fun mapsTheRangeOntoPercent() {
        assertEquals(0.0, percentOf(0.0))
        assertEquals(50.0, percentOf(0.5))
        assertEquals(100.0, percentOf(1.0))
    }

    @Test
    fun clampsOutsideTheRange() {
        // A player seeking past the end reports a position slightly beyond its own
        // duration, so this is a real input rather than a hypothetical one.
        assertEquals(0.0, percentOf(-0.4))
        assertEquals(100.0, percentOf(1.2))
    }

    @Test
    fun nanBecomesZeroRatherThanAnInvalidWidth() {
        // The case this function exists for: position / duration while duration is
        // still zero. `width: NaN%` is dropped by CSS, leaving the previous width in
        // place, so the failure looks like a bar stuck at the last episode's spot.
        assertEquals(0.0, percentOf(Double.NaN))
    }

    @Test
    fun infinitiesClampRatherThanPropagate() {
        // A non-zero position over a zero duration is an infinity, and `Infinity%`
        // is as invalid to CSS as `NaN%`. Unlike NaN these do clamp, which is why
        // only NaN is handled ahead of the clamp.
        assertEquals(100.0, percentOf(Double.POSITIVE_INFINITY))
        assertEquals(0.0, percentOf(Double.NEGATIVE_INFINITY))
    }
}

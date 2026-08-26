package io.github.bchmsl.keel.geometry

/**
 * A `0.0..1.0` fraction as a percentage, clamped to that range.
 *
 * Internal: the components are the API, not the arithmetic. Shared by the two that
 * set a width from a fraction, so both clamp the same way, and pure so the awkward
 * inputs can be tested rather than argued about.
 *
 * The clamp is not defensive tidiness. A media element reports a duration of zero
 * until its metadata arrives, so a caller dividing position by duration hands this
 * `NaN` on the first frames of every episode. `width: NaN%` is invalid, and an
 * invalid value is *dropped* rather than applied - measured, not assumed - which
 * leaves whatever width was there before. A bar frozen at the previous episode's
 * position is a great deal harder to explain than one sitting at zero.
 *
 * `NaN` is handled before the clamp because it has to be: `coerceIn` compares, and
 * every comparison against `NaN` is false, so a bare clamp returns it unchanged.
 * The infinities do clamp, to 100 and 0, which is why they are not special-cased.
 */
internal fun percentOf(fraction: Double): Double =
    if (fraction.isNaN()) 0.0 else fraction.coerceIn(0.0, 1.0) * PERCENT

private const val PERCENT = 100.0

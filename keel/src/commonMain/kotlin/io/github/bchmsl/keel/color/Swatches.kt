package io.github.bchmsl.keel.color

/**
 * The fixed colours an app offers for things the user labels themselves.
 *
 * This is the one place a colour deliberately escapes the theme. A tag or a category
 * the user coloured has to keep that colour when they switch palette, because they
 * chose it to tell two of their own things apart - and a theme change must not make
 * them the same colour.
 *
 * Fixed swatches rather than a colour picker, for two reasons that both matter: every
 * one of these stays legible against all twelve light and dark backgrounds, and no
 * two of them are hard to tell apart at the size of a pill.
 */
public object Swatches {

    /** The swatches, in the order a picker shows them. */
    public val All: List<String> = listOf(
        "#6366f1",
        "#ec4899",
        "#f59e0b",
        "#10b981",
        "#3b82f6",
        "#8b5cf6",
        "#ef4444",
        "#14b8a6",
        "#f97316",
        "#64748b",
    )

    /** What a newly created label starts on. */
    public val Default: String = All.first()
}

/**
 * How strongly a swatch is used behind text.
 *
 * Four levels, named. The alpha is a two-digit hex suffix rather than a separate
 * opacity, so the stored six digits pass through untouched and a wrong colour on
 * screen can be traced straight back to what was stored.
 *
 * Naming them is what keeps the numbers out of the markup, and makes it obvious when
 * a fifth level is being invented rather than an existing one reused.
 */
public enum class SwatchShade(public val hexAlpha: String) {
    /** Unselected: a filter chip that is off, or a label not yet applied. */
    Faint("15"),

    /** Crowded: a pill inside a dense row, competing with the text beside it. */
    Inline("18"),

    /** Roomy: a pill with space around it, in an expanded row or a dialog. */
    Pill("20"),

    /** Selected: the chip whose filter is on. */
    Selected("30"),
}

/**
 * A swatch at a given strength, as a CSS colour.
 *
 * Eight-digit hex rather than `rgba(...)`: the six digits that were stored stay
 * visible in the output, which is what makes the result debuggable by reading it.
 *
 * [hex] is passed through rather than validated. It arrives from storage, and a
 * value that is not a colour is a data problem to see in the browser's own styling
 * warnings, not one to swallow here by substituting a colour the user did not pick.
 */
public fun swatchBackground(hex: String, shade: SwatchShade): String = hex + shade.hexAlpha

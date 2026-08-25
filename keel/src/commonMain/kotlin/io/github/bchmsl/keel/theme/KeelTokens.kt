package io.github.bchmsl.keel.theme

/**
 * The token contract, as data.
 *
 * The same list is used three ways, which is the point of it existing at all: the
 * gallery paints every token from it, `TokenContractTest` holds it against the
 * stylesheet in both directions, and it is the documentation of what an app may rely
 * on. A token added to the CSS but not here fails the test; one added here but not to
 * the CSS fails it too.
 *
 * Only the colours are listed. Shape, motion, type and shadow are single values with
 * no per-theme form, so there is nothing for a palette to get wrong about them and
 * nothing for a gallery to enumerate.
 */
public object KeelTokens {

    /**
     * The nine colours each palette declares.
     *
     * A palette that misses one inherits whichever palette was declared last, so the
     * page still paints and exactly one colour is quietly wrong.
     */
    public val PerPalette: List<String> = listOf(
        "--background",
        "--foreground",
        "--card",
        "--primary",
        "--secondary",
        "--muted",
        "--muted-foreground",
        "--accent",
        "--border",
    )

    /**
     * Colours that are the same whatever palette is active.
     *
     * States rather than accents: what "this deletes something" looks like should not
     * change with an app's identity. `--secondary-foreground` is here because it has a
     * global default, which two palettes with a pale secondary override in their own
     * blocks.
     */
    public val Global: List<String> = listOf(
        "--primary-foreground",
        "--secondary-foreground",
        "--destructive",
        "--destructive-foreground",
        "--success",
        "--success-foreground",
    )

    /**
     * Colours derived from another token rather than declared per palette.
     *
     * Derived so a mismatch is impossible rather than merely unlikely: an input border
     * that has drifted from the panel border is the kind of thing nobody notices for
     * a year.
     */
    public val Derived: List<String> = listOf(
        "--input",
        "--ring",
        "--popover",
        "--card-foreground",
        "--popover-foreground",
        "--accent-foreground",
    )

    /** Every colour token, in the order the gallery shows them. */
    public val AllColors: List<String> = PerPalette + Global + Derived
}

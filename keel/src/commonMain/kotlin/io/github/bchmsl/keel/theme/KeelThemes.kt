package io.github.bchmsl.keel.theme

/**
 * The palettes this library ships a stylesheet for.
 *
 * Each one has a matching block in `keel/palettes.css`, keyed by its [Theme.id]. The
 * two halves are a pair: a [Theme] with no CSS block paints the fallback palette
 * while claiming to be something else, and a CSS block with no [Theme] can never be
 * selected. `ThemeCssTest` holds the two lists against each other so neither half
 * can be added alone.
 *
 * An app is not limited to these. Adding a palette means a [Theme] of your own and a
 * `[data-theme='...']` block in your own stylesheet; see ARCHITECTURE.md. That is the
 * route a dark-only palette takes, and why [Standard] is one catalogue rather than
 * the only one.
 */
public object KeelThemes {

    public val Coral: Theme = Theme("coral", "Coral", "#f43f5e")
    public val Ocean: Theme = Theme("ocean", "Ocean", "#0ea5e9")
    public val Forest: Theme = Theme("forest", "Forest", "#22c55e")
    public val Lavender: Theme = Theme("lavender", "Lavender", "#a78bfa")
    public val Ember: Theme = Theme("ember", "Ember", "#f97316")
    public val Slate: Theme = Theme("slate", "Slate", "#64748b")

    /** Every palette with a block in `keel/palettes.css`, in picker order. */
    public val All: List<Theme> = listOf(Coral, Ocean, Forest, Lavender, Ember, Slate)

    /**
     * The six palettes, defaulting to Coral.
     *
     * Coral is also the palette `keel/palettes.css` declares on bare `:root`, so an
     * unknown or missing `data-theme` paints a complete palette rather than leaving
     * the page unstyled. Changing this default without moving that block would make
     * the first frame after a reload disagree with every frame after it.
     */
    public val Standard: ThemeCatalog = ThemeCatalog(All, default = Coral)
}

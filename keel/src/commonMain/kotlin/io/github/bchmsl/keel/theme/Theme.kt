package io.github.bchmsl.keel.theme

/**
 * One palette.
 *
 * A theme is data rather than an enum entry, because the set of palettes belongs to
 * the app and not to this library. Dayboard offers six; Dakalebi offers one. An enum
 * here would force the second consumer to carry five palettes it will never paint,
 * and would make adding a palette a release of this library rather than a line in
 * the app that wants it.
 *
 * [id] is the stored value. It is written to `localStorage`, saved on the user's
 * account, and set as `data-theme` on the document element, where it selects a block
 * in the stylesheet. Changing an id orphans every stored preference, so ids are part
 * of the data format and not a display detail.
 *
 * [accentHex] is the swatch a theme picker paints. It is a plain hex rather than a
 * token because the picker draws every swatch at once, while only one theme's
 * variables are live - so five of the six cannot come from CSS.
 *
 * [supportsLight] and [supportsDark] are what let one design system serve an app
 * that wants both and an app that wants exactly one. A video app framing its own
 * content has a real reason to refuse a light mode, and refusing it here is better
 * than shipping a light palette nobody may select.
 */
public data class Theme(
    val id: String,
    val label: String,
    val accentHex: String,
    val supportsLight: Boolean = true,
    val supportsDark: Boolean = true,
) {
    init {
        require(THEME_ID.matches(id)) {
            "'$id' cannot be a theme id. An id is used three ways and each one " +
                "constrains it: as an attribute value, inside the CSS selector " +
                "[data-theme='...'], and interpolated into the boot script. " +
                "Lower-case letters, digits and single hyphens keep all three safe."
        }
        require(supportsLight || supportsDark) {
            "theme '$id' supports neither light nor dark, so it can never be painted"
        }
    }
}

/**
 * What a [Theme.id] may be.
 *
 * File-private rather than a companion object, which for one constant would be an
 * extra class carrying nothing.
 */
private val THEME_ID = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

/**
 * The palettes an app offers, and which one it falls back to.
 *
 * [default] is not merely the first entry: it is what an unrecognised stored id
 * resolves to, which is the case that actually happens - a first visit, a value
 * left behind by a palette that has since been removed, or a browser whose storage
 * was edited by hand.
 */
public class ThemeCatalog(public val themes: List<Theme>, public val default: Theme) {
    init {
        require(themes.isNotEmpty()) { "a catalogue with no themes cannot paint anything" }
        require(default in themes) { "the default theme '${default.id}' is not in the catalogue" }

        val duplicates = themes.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "theme ids must be unique, because a stored id resolves to exactly one " +
                "palette; duplicated: ${duplicates.sorted()}"
        }
    }

    /**
     * Resolves a stored id, falling back to [default] for anything unrecognised -
     * including null, which is what a first visit reads out of storage.
     */
    public fun fromId(id: String?): Theme = themes.firstOrNull { it.id == id } ?: default

    /** True when there is a choice to offer. A single-palette app has no picker. */
    public val hasChoice: Boolean get() = themes.size > 1
}

/**
 * Whether the user wants light styling, dark styling, or whatever the device asks
 * for. [System] is the default, so a first visit matches the operating system
 * instead of announcing a preference the user never expressed.
 */
public enum class ColorMode(public val id: String, public val label: String) {
    Light("light", "Light"),
    Dark("dark", "Dark"),
    System("system", "System"),
    ;

    public companion object {
        public val Default: ColorMode = System

        /** Resolves a stored mode, falling back to [Default]. See [ThemeCatalog.fromId]. */
        public fun fromId(id: String?): ColorMode = entries.firstOrNull { it.id == id } ?: Default
    }
}

/**
 * The modes worth offering for this theme, in the order a picker shows them.
 *
 * A single entry means there is no choice: the theme paints one way, and a picker
 * with one option is worse than none. Callers check `size > 1` rather than being
 * handed an empty list, so the result always names what will actually be used.
 */
public val Theme.availableColorModes: List<ColorMode>
    get() = when {
        supportsLight && supportsDark -> listOf(ColorMode.Light, ColorMode.Dark, ColorMode.System)
        supportsLight -> listOf(ColorMode.Light)
        else -> listOf(ColorMode.Dark)
    }

/**
 * Whether dark styling applies right now.
 *
 * [systemPrefersDark] is what `prefers-color-scheme` currently reports; it only
 * matters under [ColorMode.System], because making an explicit choice is precisely
 * the act of overriding the device.
 *
 * A theme that supports only one mode wins over the stored preference rather than
 * being overridden by it. That is deliberate: a user who picked Light under one
 * theme and then switched to a dark-only theme must not be shown an unpainted page,
 * and their Light preference is kept for when they switch back.
 */
public fun Theme.resolvesToDark(mode: ColorMode, systemPrefersDark: Boolean): Boolean = when {
    !supportsLight -> true
    !supportsDark -> false
    else -> when (mode) {
        ColorMode.Light -> false
        ColorMode.Dark -> true
        ColorMode.System -> systemPrefersDark
    }
}

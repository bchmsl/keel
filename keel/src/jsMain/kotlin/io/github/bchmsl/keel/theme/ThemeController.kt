package io.github.bchmsl.keel.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Storage

/**
 * Owns the active palette: the stored preference, what the device asks for, and the
 * two attributes on `<html>` that the stylesheet selects palettes with.
 *
 * Deliberately the only place in the library that touches the outside world -
 * storage, media queries, the document element. Everything it decides is decided by
 * the pure functions in `Theme.kt`, so the decisions are testable without a browser
 * and this class is only wiring.
 *
 * Storage is per-device and is read first, on purpose: a reload paints the right
 * palette without waiting for the network. An app that also keeps the preference on
 * an account applies that copy afterwards, through [setTheme] and [setColorMode],
 * and it wins where the two disagree.
 *
 * [keys] default to the names Dayboard already stores under, so an existing browser
 * keeps its theme through the migration rather than reverting to the default on
 * first load. An app with no history to preserve can name them anything.
 */
public class ThemeController(
    public val catalog: ThemeCatalog,
    private val keys: StorageKeys = StorageKeys(),
) {

    /** The two names read and written, held together so they cannot drift apart. */
    public data class StorageKeys(
        val theme: String = "themeId",
        val colorMode: String = "colorMode",
    )

    private val darkQuery = window.matchMedia(DARK_MEDIA_QUERY)

    public var theme: Theme by mutableStateOf(catalog.fromId(read(keys.theme)))
        private set

    public var colorMode: ColorMode by mutableStateOf(ColorMode.fromId(read(keys.colorMode)))
        private set

    private var systemPrefersDark: Boolean by mutableStateOf(darkQuery.matches)

    /** Whether dark styling applies, taking both the theme and the device into account. */
    public val isDark: Boolean get() = theme.resolvesToDark(colorMode, systemPrefersDark)

    /**
     * The modes worth offering right now. One entry means there is no choice to make;
     * see [availableColorModes].
     */
    public val availableColorModes: List<ColorMode> get() = theme.availableColorModes

    /**
     * Applies the stored preference and begins following the device.
     *
     * Call once, before the first composition. Without the listener, someone on
     * [ColorMode.System] who changed their operating system's appearance would keep
     * the old palette until they reloaded the page.
     */
    public fun start() {
        darkQuery.addEventListener("change", {
            systemPrefersDark = darkQuery.matches
            applyToDocument()
        })
        applyToDocument()
    }

    public fun setTheme(value: Theme) {
        if (value == theme) return
        theme = value
        write(keys.theme, value.id)
        applyToDocument()
    }

    public fun setColorMode(value: ColorMode) {
        if (value == colorMode) return
        colorMode = value
        write(keys.colorMode, value.id)
        applyToDocument()
    }

    /**
     * The palette is chosen entirely in CSS; this only states which one.
     *
     * `data-theme` picks the palette block and the `dark` class picks the mode, so no
     * colour is ever computed in Kotlin. That is what lets an app add a palette
     * without this library being rebuilt.
     */
    private fun applyToDocument() {
        val root = document.documentElement ?: return
        root.setAttribute("data-theme", theme.id)
        root.classList.toggle("dark", isDark)
    }

    /**
     * Reading and writing are both guarded.
     *
     * `localStorage` is not merely empty in a locked-down browser: touching it throws
     * outright in Safari's private mode and under a blocked-cookies policy. An
     * unreadable preference is a first visit, and an unwritable one is a preference
     * that lasts as long as the tab - both far better than a page that fails to
     * paint because it could not look up a colour.
     */
    private fun read(key: String): String? = runCatching { storage()?.getItem(key) }.getOrNull()

    private fun write(key: String, value: String) {
        runCatching { storage()?.setItem(key, value) }
    }

    private fun storage(): Storage? = window.localStorage

    public companion object {
        private const val DARK_MEDIA_QUERY = "(prefers-color-scheme: dark)"

        /**
         * The script to inline in `<head>`, before the bundle, so a reload paints the
         * stored palette on the first frame instead of flashing the default one.
         *
         * It has to be inline and it has to be Javascript: by the time this class
         * exists, the browser has already painted. Anyone in dark mode would see one
         * white frame per reload otherwise.
         *
         * Exposed as a string so the names in the page and the names in
         * [StorageKeys] have one source. An app that inlines the snippet by hand
         * instead should keep `KeelBootScriptTest` in mind, which is what catches the
         * two drifting apart.
         */
        public fun bootScript(keys: StorageKeys = StorageKeys(), defaultTheme: String): String =
            """
            (function () {
              try {
                var root = document.documentElement;
                var mode = localStorage.getItem('${keys.colorMode}') || 'system';
                root.setAttribute(
                  'data-theme',
                  localStorage.getItem('${keys.theme}') || '$defaultTheme'
                );
                root.classList.toggle(
                  'dark',
                  mode === 'dark' ||
                    (mode === 'system' &&
                      window.matchMedia('(prefers-color-scheme: dark)').matches)
                );
              } catch (error) {
                /* Private browsing can make localStorage throw; the defaults are fine. */
              }
            })();
            """.trimIndent()
    }
}

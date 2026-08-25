package io.github.bchmsl.keel.theme

/**
 * The two storage names read and written, held together so they cannot drift apart.
 *
 * The defaults are the names Dayboard already stores under, so an existing browser
 * keeps its palette through a migration rather than reverting on first load. An app
 * with no history to preserve can name them anything.
 */
public data class StorageKeys(val theme: String = "themeId", val colorMode: String = "colorMode")

/**
 * The script to inline in `<head>`, before the bundle, so a reload paints the
 * stored palette on the first frame instead of flashing the default one.
 *
 * It has to be inline and it has to be Javascript: by the time this class
 * exists the browser has already painted, so anyone in dark mode would see one
 * white frame per reload.
 *
 * Generated from the catalogue rather than from a bare id, because it has to
 * reach the same answer [Theme.resolvesToDark] does, and that answer depends
 * on the theme. A theme supporting one mode overrides the stored preference,
 * so a script that read only the preference would disagree with the very next
 * frame - a dark-only app on a light device would paint light and be corrected,
 * on every reload, for ever, because nothing ever rewrites the stored mode.
 *
 * Single-mode themes are therefore emitted as a lookup of forced values.
 * `hasOwnProperty` rather than `in`, so an id like `toString` cannot borrow an
 * answer from `Object.prototype`.
 */
public fun bootScript(catalog: ThemeCatalog, keys: StorageKeys = StorageKeys()): String {
    // Only the themes that override the stored preference need naming. For the
    // common case - every theme supporting both modes - this is `{}` and the lookup
    // below always falls through to the preference, exactly as it used to.
    //
    // The braces are built here rather than written into the template, so the empty
    // case is `{}` and not `{  }`.
    val forced = catalog.themes
        .filterNot { it.supportsLight && it.supportsDark }
        .joinToString(separator = ", ", prefix = "{", postfix = "}") {
            " '${it.id}': ${!it.supportsLight} "
        }

    return """
    (function () {
      try {
        var root = document.documentElement;
        var forced = $forced;
        var id = localStorage.getItem('${keys.theme}') || '${catalog.default.id}';
        var mode = localStorage.getItem('${keys.colorMode}') || 'system';
        root.setAttribute('data-theme', id);
        root.classList.toggle(
          'dark',
          Object.prototype.hasOwnProperty.call(forced, id)
            ? forced[id]
            : mode === 'dark' ||
                (mode === 'system' &&
                  window.matchMedia('(prefers-color-scheme: dark)').matches)
        );
      } catch (error) {
        /* Private browsing can make localStorage throw; the defaults are fine. */
      }
    })();
    """.trimIndent()
}

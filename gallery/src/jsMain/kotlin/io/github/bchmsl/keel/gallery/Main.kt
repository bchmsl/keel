package io.github.bchmsl.keel.gallery

import io.github.bchmsl.keel.theme.KeelThemes
import io.github.bchmsl.keel.theme.ThemeController
import org.jetbrains.compose.web.renderComposable

/**
 * Entry point.
 *
 * The gallery has one dependency and it is the library, which is the point: anything
 * that fails to build or fails to paint here is a fault in keel rather than in the
 * page showing it off.
 */
fun main() {
    val theme = ThemeController(catalog = KeelThemes.Standard)

    // Before the first composition, so the palette is settled before anything paints.
    theme.start()

    renderComposable(rootElementId = "root") {
        Gallery(theme)
    }
}

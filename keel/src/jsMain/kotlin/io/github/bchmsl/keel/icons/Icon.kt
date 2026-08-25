package io.github.bchmsl.keel.icons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import io.github.bchmsl.keel.dom.classNames
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Span

/**
 * Draws a lucide icon at [size] pixels square, in the current text colour.
 *
 * The markup is written straight into the span rather than built from a Compose SVG
 * tree, for a reason worth knowing: an SVG child has to be created in the SVG
 * namespace, and Compose HTML's element builders create HTML ones. `<svg><circle>`
 * built that way parses without complaint and draws nothing at all.
 *
 * Since the icons are compile-time constants generated from the lucide repository,
 * there is no untrusted content here and so nothing to escape.
 *
 * The `key(icon)` is what makes a play/pause button actually change. The effect that
 * writes the markup runs when the element is created and never again, so swapping
 * the icon on an existing span would leave the old drawing in place while the
 * button's `aria-label` updated - announcing one thing and drawing another. Keying
 * discards the span instead.
 *
 * Hidden from assistive technology by design: every icon sits next to a label or
 * inside a control that carries its own accessible name. An icon that needs to be
 * announced is a sign the control around it is missing one.
 *
 * A null [size] writes no inline dimensions and lets CSS decide, which is not a
 * nicety: an interface that scales itself by setting one root font size - a
 * television UI viewed from a sofa is the usual reason - has every length in `rem`,
 * and an inline `px` written at the call site is the one thing that cannot
 * participate. Sizing such an icon from a rule beside its context keeps it in the
 * same canvas as everything around it.
 */
@Composable
public fun Icon(icon: LucideIcon, size: Int? = DEFAULT_ICON_SIZE, className: String? = null) {
    key(icon) {
        Span({
            classNames("icon", className)
            size?.let {
                style {
                    width(it.px)
                    height(it.px)
                }
            }
            ref { element ->
                element.innerHTML = icon.svg()
                onDispose { }
            }
        })
    }
}

/**
 * Wraps an icon's body in the frame every lucide icon shares.
 *
 * `stroke="currentColor"` is the whole reason an icon inherits its colour from the
 * text around it, so a button's hover rule recolours its icon without naming it.
 */
private fun LucideIcon.svg(): String =
    """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" """ +
        """stroke="currentColor" stroke-width="2" stroke-linecap="round" """ +
        """stroke-linejoin="round" aria-hidden="true" focusable="false">$body</svg>"""

/** Matches the type size the interface is set at, which is what icons sit beside. */
private const val DEFAULT_ICON_SIZE = 16

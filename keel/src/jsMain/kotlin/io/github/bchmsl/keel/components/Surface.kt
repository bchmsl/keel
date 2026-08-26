package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

/**
 * How much room a [Surface] leaves between its border and its content.
 *
 * [None] is for a surface whose content reaches the border - a poster, a chart, a
 * list whose rows have their own padding. Adding padding from outside is easy;
 * removing keel's would mean overriding a keel selector, so the option lives here.
 */
public enum class SurfacePadding(internal val className: String?) {
    None(null),
    Small("surface--pad-sm"),
    Default("surface--pad"),
    Large("surface--pad-lg"),
}

/**
 * A panel: a border, a radius, and the card colour. Nothing else.
 *
 * This is the plain box that [Card] turns into a titled one. It exists separately
 * because the untitled shape is the more common of the two - a login panel, a stat
 * tile, a next-up card - and every app was hand-writing its own box for it. Four of
 * them across two apps, agreeing on none of the four values.
 *
 * There is no header, no title and no actions, so a caller that wants any of those
 * wants [Card]. What this adds over a bare `Div` is only the box, which is exactly
 * the part that has to match everywhere.
 *
 * [elevated] adds a shadow, which is what separates a surface that sits above the
 * page from one flush with it. It defaults to off because the flush form is the one
 * that composes: a shadow inside another shadowed panel reads as a mistake. [Card]
 * passes it, being the top-level panel.
 *
 * [clipped] cuts the content to the radius, which a surface holding a poster or a
 * chart that reaches its own border needs - square corners inside a rounded box make
 * the radius decorative. It is off by default and not simply implied by
 * [SurfacePadding.None], because clipping also removes anything meant to leave the
 * box: a focus ring is drawn outside its control, so a clipped [Card] would lose the
 * ring on the button in its header. Only the caller knows which kind it has.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun Surface(
    padding: SurfacePadding = SurfacePadding.Default,
    elevated: Boolean = false,
    clipped: Boolean = false,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    Div({
        classNames(
            "surface",
            padding.className,
            "surface--elevated".takeIf { elevated },
            "surface--clip".takeIf { clipped },
        )
        attrs?.invoke(this)
    }) {
        content()
    }
}

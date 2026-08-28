package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.drawerClasses
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

/** Which side the drawer is anchored to, and therefore which side it enters from. */
public enum class DrawerEdge(internal val className: String) {
    Left("drawer--left"),
    Right("drawer--right"),
}

/**
 * Who scrolls: the drawer, or the content inside it.
 *
 * One decision with three consequences, which is why it is one name rather than three
 * flags. A drawer that scrolls as a single column wants keel's padding, keel's gap
 * between blocks, and its own scrollbar. A drawer with a header that must stay put
 * wants none of the three: the header and the scrolling region below it carry their
 * own padding, sit flush against each other so the divider between them reaches both
 * edges, and only the lower one scrolls.
 */
public enum class DrawerLayout(internal val className: String?) {
    /**
     * One padded column, scrolling as a whole.
     *
     * Right for a sheet that is a list of blocks - a menu, a summary, a set of links.
     */
    Scrolling(null),

    /**
     * The box and nothing else: no padding, no gap, no scrollbar of its own.
     *
     * For a sheet the caller divides itself, which in practice means one with a fixed
     * header. A long settings list is the case: losing the way out of it while
     * scrolling is unkind, so the header stays and the body below it scrolls. Padding
     * on the drawer would then inset that header and stop its lower border reaching
     * the edges, and a scrollbar on the drawer would be a second one beside the
     * body's.
     */
    Framed("drawer--framed"),
}

/**
 * A panel anchored to one edge, with a scrim behind it.
 *
 * A drawer rather than a [Dialog] when the content is a long list of controls rather
 * than one decision: everything in it applies as it is changed, so there is nothing
 * to confirm and nothing to cancel, and it does not need to sit in the middle of the
 * screen demanding an answer. Both apps had one, and they were mirror images of each
 * other - same flex column, same slide, opposite edges.
 *
 * [ariaLabel] names it, and is required for the same reason [Dialog]'s title is: a
 * modal surface that reaches a screen reader unnamed is a failure invisible to
 * whoever wrote it. It is `aria-label` here rather than `aria-labelledby`, because a
 * drawer has no hidden heading to point at - unlike a dialog, its visible header is
 * part of the caller's content and keel does not build it.
 *
 * Focus moves in on open and returns to whatever held it on close, the same as
 * [Dialog], and Tab is likewise not cycled - see that function for why.
 *
 * Dismissing works two ways: the scrim and Escape. There is no close button, because
 * where it goes depends on the header the caller draws.
 *
 * [layout] decides whether keel pads and scrolls the sheet or hands both to the
 * caller; see [DrawerLayout].
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun Drawer(
    onDismiss: () -> Unit,
    ariaLabel: String,
    edge: DrawerEdge = DrawerEdge.Right,
    layout: DrawerLayout = DrawerLayout.Scrolling,
    blurredScrim: Boolean = true,
    dismissOnEscape: Boolean = true,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    if (dismissOnEscape) {
        DismissOnEscape(onDismiss)
    }

    Scrim(onDismiss = onDismiss, blurred = blurredScrim)

    Div({
        classNames(drawerClasses(edge, layout))
        attr("role", "dialog")
        attr("aria-modal", "true")
        attr("aria-label", ariaLabel)
        // Focusable by script but not by Tab, so focus can be put here on open
        // without adding a tab stop nobody asked for.
        attr("tabindex", "-1")

        ref { element ->
            val previous = document.activeElement as? HTMLElement
            element.focus()
            onDispose { previous?.focus() }
        }

        attrs?.invoke(this)
    }) {
        content()
    }
}

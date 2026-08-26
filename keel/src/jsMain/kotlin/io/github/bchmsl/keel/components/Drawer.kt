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
 * [attrs] runs last; see [Button].
 */
@Composable
public fun Drawer(
    onDismiss: () -> Unit,
    ariaLabel: String,
    edge: DrawerEdge = DrawerEdge.Right,
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
        classNames(drawerClasses(edge))
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

package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.scrimClasses
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

/**
 * The dim behind something modal.
 *
 * Five hand-written versions across the two apps carried four different values, and
 * none of the differences was a decision. One of them was not a scrim at all - a
 * dropdown's click-catcher wearing this class, which then had to cancel both of the
 * scrim's own properties inline to work, and got one of them wrong. That case is
 * [DropdownMenu]'s catcher now, which is a separate thing with a separate z-index.
 *
 * [onDismiss] is optional, and its absence is the ten-foot case: a remote cannot
 * click the page behind a drawer, so there is nothing for a tap on the scrim to
 * mean. When it is absent the element takes no pointer handler at all rather than
 * one that does nothing.
 *
 * The scrim is `aria-hidden` in both cases. A full-viewport click target announces
 * itself as an unlabelled region sitting over the whole page, which is worse than
 * useless - Escape is the keyboard route out, and [Drawer] wires it.
 *
 * [blurred] is on by default and worth turning off for a surface that repaints: the
 * blur is a full-screen composite on every frame behind it.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun Scrim(
    onDismiss: (() -> Unit)? = null,
    blurred: Boolean = true,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
) {
    Div({
        classNames(scrimClasses(blurred))
        attr("aria-hidden", "true")
        onDismiss?.let { dismiss -> onClick { dismiss() } }
        attrs?.invoke(this)
    })
}

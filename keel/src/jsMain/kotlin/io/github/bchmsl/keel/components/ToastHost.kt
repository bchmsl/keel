package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.toastClasses
import io.github.bchmsl.keel.dom.toastHostClasses
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement

/** Which edge the notices gather at. */
public enum class ToastPlacement(internal val className: String) {
    Top("toast-host--top"),
    Bottom("toast-host--bottom"),
}

/** How a notice reads. Carried by the left edge only; see the CSS for why. */
public enum class ToastTone(internal val className: String?) {
    Neutral(null),
    Success("toast--success"),
    Error("toast--error"),
}

/**
 * One transient notice.
 *
 * [id] is the caller's, and it is what keeps a list of these stable across
 * recompositions: without a key the framework matches by position, so dismissing the
 * first of three re-labels the other two rather than removing one.
 */
public class Toast(
    public val id: String,
    public val message: String,
    public val tone: ToastTone = ToastTone.Neutral,
)

/**
 * Where transient notices appear.
 *
 * keel owns the presentation and nothing else. Deciding what is showing, for how
 * long, and when it goes away is the consumer's store - which is right, because that
 * is application state with a timer in it and no two apps want the same duration.
 * This takes the list as it stands and draws it.
 *
 * The host is `role="status"`, which is an `aria-live="polite"` region: a notice is
 * read after whatever the user is currently doing rather than interrupting it. One
 * region for every tone, including errors, because a live region's politeness is a
 * property of the region and not of the message - two regions would be the only way
 * to make errors assertive, and two live regions racing each other announce in an
 * order neither controls.
 *
 * `aria-atomic` is left at its default of false, so an arriving notice is read on its
 * own rather than re-reading the ones already on screen.
 *
 * Nothing here is interactive and the strip does not take pointer events, so an empty
 * or half-empty host cannot swallow a click meant for what is underneath it.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun ToastHost(
    toasts: List<Toast>,
    placement: ToastPlacement = ToastPlacement.Top,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
) {
    Div({
        classNames(toastHostClasses(placement))
        attr("role", "status")
        attrs?.invoke(this)
    }) {
        toasts.forEach { toast ->
            // Keyed, so the framework matches notices by identity rather than by
            // position. Without it, dismissing the first of three rewrites the text
            // of the other two and replays their entry animation.
            key(toast.id) {
                Div({ classNames(toastClasses(toast.tone)) }) {
                    Text(toast.message)
                }
            }
        }
    }
}

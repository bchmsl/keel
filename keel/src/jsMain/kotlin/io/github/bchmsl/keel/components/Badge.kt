package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLSpanElement

/**
 * What a [Badge] means, not what colour it is.
 *
 * Named by meaning so the same state reads the same everywhere, and so a palette can
 * move a colour without every call site becoming a lie. [Destructive] and [Success]
 * are the same two states they are on a button.
 */
public enum class BadgeTone(internal val className: String?) {
    /** A fact with no judgement attached: a count, a format, a language. */
    Neutral(null),

    /** Worth noticing. The brand colour, so at most a few per screen. */
    Primary("badge--primary"),

    /** Finished, available, passing. */
    Success("badge--success"),

    /** Failed, expired, removed. */
    Destructive("badge--destructive"),
}

/**
 * A short status word attached to something else: NEW, LIVE, 4K, S2 E7.
 *
 * Not a control. A badge that can be clicked is a [Pill], which is the pressable
 * form and takes an `onClick`. Keeping them apart is why neither has to grow a
 * parameter saying which it is.
 *
 * The text is uppercased and letterspaced by the stylesheet, so pass it in normal
 * case - the caller's string stays readable and stays what a screen reader reads.
 *
 * Where the badge sits is the caller's business. A badge overlaid on the corner of a
 * poster is one app's tile, so this carries no positioning of its own: put it in a
 * positioned parent, or pass `position` through [attrs].
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun Badge(
    label: String,
    tone: BadgeTone = BadgeTone.Neutral,
    attrs: (AttrsScope<HTMLSpanElement>.() -> Unit)? = null,
    leading: ContentBuilder<HTMLSpanElement>? = null,
) {
    Span({
        classNames("badge", tone.className)
        attrs?.invoke(this)
    }) {
        leading?.invoke(this)
        Text(label)
    }
}

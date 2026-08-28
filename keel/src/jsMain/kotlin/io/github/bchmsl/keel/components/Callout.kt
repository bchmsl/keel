package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.calloutBodyClasses
import io.github.bchmsl.keel.dom.calloutClasses
import io.github.bchmsl.keel.dom.classNames
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

/**
 * What a callout is about.
 *
 * Named to match [BadgeTone] and [ToastTone] rather than inventing a third vocabulary
 * for the same ideas.
 *
 * A tone tints the box and its border and never recolours the text, which is measured
 * rather than a preference: the `--destructive` ink on its own tint reaches only
 * 3.0-3.1:1 on the six light palettes, against the 4.5:1 that 13px text needs, while
 * `--foreground` on the same box is 12.1:1 or better in all twelve.
 */
public enum class CalloutTone(internal val className: String?) {
    /** A plain aside: a hint, a caveat, a note about what a screen will do. */
    Neutral(null),

    /** Something completed or reached - the end of a series, a finished import. */
    Primary("callout--primary"),

    /**
     * It worked: a mail was sent, a change was saved, an account was created.
     *
     * Distinct from [Primary], which is the palette's identity colour. Two of the six
     * palettes put `--primary` within 25 degrees of `--destructive`, so on those a
     * confirmation drawn in it is the same red box as a failure - and a callout shows
     * one at a time, with nothing beside it to compare against. `--success` is fixed
     * across every palette for exactly this reason, which is why [BadgeTone] and
     * [ToastTone] both already had it.
     */
    Success("callout--success"),

    /** Something went wrong, and it is about the screen rather than about an action
     *  just taken. A failure that *is* about an action wants a [ToastHost] notice. */
    Destructive("callout--destructive"),
}

/**
 * A bordered strip that says something about the screen it is on.
 *
 * A playback error, a "you have reached the end of this series", a note about what a
 * setting will do. Part of the page: it stays as long as the condition does.
 *
 * Not one of [ToastHost]'s notices, which arrive, are about an action the user just
 * took, and leave on a timer. Not an [EmptyState] either - that replaces content,
 * a callout sits beside it.
 *
 * [announce] makes it a live region, so a screen reader says it when it appears rather
 * than only when focus reaches it. That is right for something that arrives in response
 * to what the user did - a failed sign-in, a stream that dropped - and wrong for a
 * caveat that was on the page from the start, which would then interrupt whatever was
 * being read. It is off by default because the second case is the more common one and
 * an unwanted interruption is worse than a missed one that focus will reach anyway.
 *
 * [content] is the whole row, so a callout can end in a button - "watch it again" -
 * without the caller building a flex box around keel's box. Wrap the text side in
 * [CalloutBody] when there is a trailing control, so the text takes the room the
 * control does not.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun Callout(
    tone: CalloutTone = CalloutTone.Neutral,
    announce: Boolean = false,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    Div({
        classNames(calloutClasses(tone))
        // `alert` and not `status`: it carries an assertive live region, which is what
        // "tell the user now" means. `status` is polite and waits for a pause, which
        // for a playback failure is the wrong end of the trade.
        if (announce) attr("role", "alert")
        attrs?.invoke(this)
    }) {
        content()
    }
}

/**
 * The text side of a [Callout] that ends in a control.
 *
 * Takes the room the control does not, and no more. Needed only when there is a
 * trailing control: a text-only callout fills its box on its own.
 */
@Composable
public fun CalloutBody(
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    Div({
        classNames(calloutBodyClasses())
        attrs?.invoke(this)
    }) {
        content()
    }
}

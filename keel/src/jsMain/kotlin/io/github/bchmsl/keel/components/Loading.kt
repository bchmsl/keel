package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement

/** The form a [Skeleton] takes. */
public enum class SkeletonShape(internal val className: String?) {
    /** A rectangle. Give it a size, or let a parent give it one. */
    Block(null),

    /** One line of stand-in text, as tall as the type it sits in. */
    Line("skeleton--line"),

    /** A round placeholder, for an avatar. */
    Circle("skeleton--circle"),
}

/**
 * A shape standing in for content that has not arrived.
 *
 * It carries no size of its own beyond the line form, and that is deliberate: a
 * skeleton is only honest when it is the size of the thing it replaces, and only the
 * caller knows that. Give it one through [attrs], or let a grid cell do it.
 *
 * `aria-hidden`, because it is a picture of nothing. Announcing three placeholder
 * rectangles tells a screen reader user less than silence does. Mark the region they
 * fill `aria-busy="true"` instead, so the wait is announced once rather than per
 * shape.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun Skeleton(
    shape: SkeletonShape = SkeletonShape.Block,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
) {
    Div({
        classNames("skeleton", shape.className)
        attr("aria-hidden", "true")
        attrs?.invoke(this)
    })
}

/** The diameters. */
public enum class SpinnerSize(internal val className: String?) {
    /** Inside a button or beside a line of text. */
    Small("loader--sm"),
    Default(null),

    /** Alone on an empty screen. */
    Large("loader--lg"),
}

/**
 * A rotating ring, for a wait whose length is not known.
 *
 * Use a progress bar wherever there is a number. A spinner says only that something
 * is happening, so a spinner shown where progress is measurable throws away the one
 * thing the user wants.
 *
 * The rendered class is `loader`, not `spinner`. `base.css` already owns `.spinner`
 * as the utility that rotates an icon in place, and a component taking that name
 * would silently restyle every existing use of it. Both consuming apps have a
 * `.spinner` of their own too, which is the same collision from the other side. The
 * Kotlin name is the one you would look for; the class name is the one that is free.
 *
 * `role="status"` rather than `role="progressbar"`: there is no value to report, and
 * `status` is a polite live region, so a spinner appearing does not interrupt what a
 * screen reader is already saying. [ariaLabel] is what it says when it does.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun Spinner(
    size: SpinnerSize = SpinnerSize.Default,
    ariaLabel: String = "Loading",
    attrs: (AttrsScope<HTMLSpanElement>.() -> Unit)? = null,
) {
    Span({
        classNames("loader", size.className)
        attr("role", "status")
        attr("aria-label", ariaLabel)
        attrs?.invoke(this)
    })
}

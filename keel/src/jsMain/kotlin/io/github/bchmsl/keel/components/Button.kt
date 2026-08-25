package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLButtonElement

/** The colour treatments. */
public enum class ButtonVariant(internal val className: String) {
    /** The one action a screen is about. At most one per view. */
    Default("btn--default"),

    /** Deletes or discards. Reserved for that, so its colour keeps meaning something. */
    Destructive("btn--destructive"),

    /** A bordered button on the page background. The usual choice beside a Default. */
    Outline("btn--outline"),

    /** Filled, in the theme's second colour. */
    Secondary("btn--secondary"),

    /** No fill until hovered. For controls that should be quiet at rest. */
    Ghost("btn--ghost"),

    /** Looks like a link, behaves like a button. For actions inside a sentence. */
    Link("btn--link"),
}

/** The heights. [Icon] is the square form, for a button whose content is one glyph. */
public enum class ButtonSize(internal val className: String) {
    Default("btn--size-default"),
    Small("btn--size-sm"),
    Large("btn--size-lg"),
    Icon("btn--size-icon"),
}

/**
 * A button with a text label.
 *
 * [ariaLabel] is only needed where the visible text does not describe the action -
 * a "Save" that saves needs none. For a button with no text at all use [IconButton],
 * which requires one.
 *
 * [type] defaults to [ButtonType.Button], which is not the browser's default. A
 * button inside a form submits it unless told otherwise, so the default here makes
 * the safe case the one you get without thinking. A form's own submit control passes
 * [ButtonType.Submit] and gains the browser's validation and Enter-to-submit.
 */
@Composable
public fun Button(
    label: String,
    onClick: () -> Unit,
    variant: ButtonVariant = ButtonVariant.Default,
    size: ButtonSize = ButtonSize.Default,
    enabled: Boolean = true,
    ariaLabel: String? = null,
    type: ButtonType = ButtonType.Button,
    leading: ContentBuilder<HTMLButtonElement>? = null,
) {
    Button({
        classNames("btn", variant.className, size.className)
        type(type)
        if (!enabled) disabled()
        ariaLabel?.let { attr("aria-label", it) }
        onClick { onClick() }
    }) {
        leading?.invoke(this)
        Text(label)
    }
}

/**
 * A button whose content is an icon.
 *
 * [ariaLabel] is required rather than optional, and that is the point of having a
 * second function: an icon-only control has no accessible name otherwise, and a
 * default of null would make the unnamed version the easy one to write.
 *
 * [title] is separate because it does a different job - it is the hover tooltip for
 * people who can see the icon but cannot tell what it means. Passing the label
 * twice is the common and correct case.
 */
@Composable
public fun IconButton(
    ariaLabel: String,
    onClick: () -> Unit,
    variant: ButtonVariant = ButtonVariant.Ghost,
    size: ButtonSize = ButtonSize.Icon,
    enabled: Boolean = true,
    title: String? = null,
    type: ButtonType = ButtonType.Button,
    content: ContentBuilder<HTMLButtonElement>,
) {
    Button({
        classNames("btn", variant.className, size.className)
        type(type)
        if (!enabled) disabled()
        attr("aria-label", ariaLabel)
        title?.let { attr("title", it) }
        onClick { onClick() }
    }) {
        content()
    }
}

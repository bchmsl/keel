package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.buttonClasses
import io.github.bchmsl.keel.dom.classNames
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLAnchorElement
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

    /**
     * A translucent wash, for a control sitting over picture rather than over the page -
     * a player's transport buttons, a quality selector on a video frame.
     *
     * None of the six above can do this job: each one resolves against a palette colour,
     * and a video frame is not one. See `.btn--on-media`.
     *
     * The wash is the palette's own ink at `--on-media-alpha`, so an app that wants its
     * player chrome heavier or lighter moves one token rather than restating the rule.
     */
    OnMedia("btn--on-media"),
}

/** The heights. [Icon] is the square form, for a button whose content is one glyph. */
public enum class ButtonSize(internal val className: String) {
    Default("btn--size-default"),
    Small("btn--size-sm"),
    Large("btn--size-lg"),
    Icon("btn--size-icon"),

    /**
     * No box: the text's own size, no padding, and the surrounding sentence's baseline.
     *
     * The size [ButtonVariant.Link] usually wants. Every other size here sets a control
     * height, which is right for a control on its own row and wrong for a word inside a
     * paragraph - a 2.5rem box in a 0.875rem sentence inflates the line it sits in.
     *
     * Pairs with [ButtonVariant.Link] or [ButtonVariant.Ghost]; a filled variant at this
     * size is a fill with no padding, which is not a button anyone wants.
     */
    Inline("btn--size-inline"),
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
 *
 * [attrs] runs last, after everything above, so a caller can override what it needs
 * to and nothing here quietly wins. Classes accumulate rather than replace, which is
 * deliberate: adding a marker class is the common case and losing `btn` is never the
 * intent. To style a control keel does not build, see `buttonClasses`.
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
    attrs: (AttrsScope<HTMLButtonElement>.() -> Unit)? = null,
    leading: ContentBuilder<HTMLButtonElement>? = null,
) {
    Button({
        classNames(buttonClasses(variant, size))
        type(type)
        if (!enabled) disabled()
        ariaLabel?.let { attr("aria-label", it) }
        onClick { onClick() }
        attrs?.invoke(this)
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
 *
 * [attrs] runs last; see [Button].
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
    attrs: (AttrsScope<HTMLButtonElement>.() -> Unit)? = null,
    content: ContentBuilder<HTMLButtonElement>,
) {
    Button({
        classNames(buttonClasses(variant, size))
        type(type)
        if (!enabled) disabled()
        attr("aria-label", ariaLabel)
        title?.let { attr("title", it) }
        onClick { onClick() }
        attrs?.invoke(this)
    }) {
        content()
    }
}

/**
 * A link that looks like a button.
 *
 * An `<a href>`, not a button with an `onClick` that navigates. That distinction is
 * the whole reason this exists: a real anchor can be opened in a new tab, copied,
 * middle-clicked and read by a crawler, and it navigates on Enter without any script.
 * A button styled as a link throws all of that away, and every consumer that wanted
 * one was hand-spelling keel's classes onto its own `A` to get it back.
 *
 * There is no `enabled` parameter, because an anchor has no disabled state. HTML
 * offers none, `pointer-events: none` still leaves it in the tab order, and stripping
 * `href` turns it into an unfocusable span. A control that can be off is a [Button].
 *
 * [external] sets `target="_blank"` and `rel="noreferrer"`, which is the pair a link
 * leaving the app should carry. `noreferrer` implies `noopener`, so the new document
 * gets no handle back on this one.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun LinkButton(
    href: String,
    label: String,
    variant: ButtonVariant = ButtonVariant.Default,
    size: ButtonSize = ButtonSize.Default,
    external: Boolean = false,
    ariaLabel: String? = null,
    attrs: (AttrsScope<HTMLAnchorElement>.() -> Unit)? = null,
    leading: ContentBuilder<HTMLAnchorElement>? = null,
) {
    A(href = href, attrs = {
        classNames(buttonClasses(variant, size))
        if (external) {
            target(ATarget.Blank)
            attr("rel", "noreferrer")
        }
        ariaLabel?.let { attr("aria-label", it) }
        attrs?.invoke(this)
    }) {
        leading?.invoke(this)
        Text(label)
    }
}

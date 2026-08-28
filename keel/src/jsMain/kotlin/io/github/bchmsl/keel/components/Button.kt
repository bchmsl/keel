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

    /**
     * No fill *and* dimmed ink until hovered. One property further than [Ghost].
     *
     * Ghost keeps the surrounding ink, so at rest it still reads as a label the eye
     * should land on - right for a Cancel beside a Save. This one steps the ink back to
     * `--muted-foreground` too, so at rest it reads as merely available: a delete on a
     * list row, a close on a panel, a "New tag" under a set of tags.
     *
     * Pairs naturally with [ButtonSize.ExtraSmall] and [ButtonSize.IconExtraSmall],
     * which is the tier most of those controls live at. See `.btn--quiet`.
     */
    Quiet("btn--quiet"),

    /**
     * [Quiet], for an action that deletes.
     *
     * Dimmed at rest and `--destructive` under the pointer, so the button says what it
     * will do at the moment someone reaches for it. Prefer this over [Destructive] for
     * the *first* step of a destructive flow - a "Delete task" that only asks for
     * confirmation is not yet the dangerous press, and colouring it solid red spends
     * the warning early. [Destructive] is for the press that actually deletes.
     */
    QuietDestructive("btn--quiet-destructive"),

    /** Looks like a link, behaves like a button. For actions inside a sentence. */
    Link("btn--link"),

    /**
     * A translucent wash, for a control sitting over picture rather than over the page -
     * a player's transport buttons, a quality selector on a video frame.
     *
     * None of the eight above can do this job: each one resolves against a palette
     * colour, and a video frame is not one. See `.btn--on-media`.
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
     * The tier below [Small]: a control that sits *inside* another control's box.
     *
     * A card header's action, a row's delete, a panel's close. None of those can be a
     * [Small] without becoming the thing the row is about. The type size steps down with
     * the box, which is the one place this scale does not keep `--control-font-size`.
     *
     * Not new geometry: keel's own `Card` header action and formatting toolbar have both
     * been `--control-h-xs` since they were written. What is new is being able to ask for
     * that tier from a call site.
     */
    ExtraSmall("btn--size-xs"),

    /** The square form of [ExtraSmall], for a glyph-only control inside a row. */
    IconExtraSmall("btn--size-icon-xs"),

    /**
     * Start, pause, skip: the control that runs the thing the surface is about.
     *
     * Not part of the scale above, and named for the role rather than for a step in
     * it, because it is not one. [Large] is 2.75rem and this is 3.5rem, so calling it
     * a larger [Icon] would put two unrelated meanings on one word - a form control
     * grows with the density of the form around it, and this grows because it is aimed
     * at rather than read.
     *
     * There is no text form, deliberately: a word in a 3.5rem box is a banner.
     *
     * Both apps had written one of these by hand and neither used keel's sizes,
     * because the sizes stopped below them. See [ButtonShape.Circle], which is the
     * other half of what they were both reaching for.
     */
    Transport("btn--size-transport"),

    /**
     * The same control when it is the only thing on the screen.
     *
     * A player filling the window, or this app's timer card expanded. 5rem, which is
     * where both apps independently put it - one at 5rem and one at 78px.
     */
    TransportLarge("btn--size-transport-lg"),

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
 * Whether a button's box is a rounded rectangle or a circle.
 *
 * Separate from [ButtonSize] because it is a separate decision, and conflating the two
 * is what left `.btn--size-icon` carrying a radius at all. A 2.5rem square control and
 * a 2.5rem round one are the same control in two places.
 *
 * Only meaningful on the square sizes. A [Circle] on a size with horizontal padding is
 * a pill, which is what [ButtonSize.Default] at a pill radius already looks like - and
 * a pill that means something is a [Pill].
 */
public enum class ButtonShape(internal val className: String?) {
    /** [radius-md], or [radius-sm] at the extra-small tier. keel's default box. */
    Box(null),

    /**
     * A full circle.
     *
     * Four of these existed across the two apps before keel had the word for it: a
     * timer's transport controls, a player's centre play button, a tile's overlay
     * actions. Every one of them was a keel size and a keel variant with one line of
     * CSS added, which is the definition of a missing axis rather than a missing
     * component.
     */
    Circle("btn--circle"),
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
 * [shape] is on this function and not on [Button], because a circle is a box around a
 * glyph: see [ButtonShape].
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun IconButton(
    ariaLabel: String,
    onClick: () -> Unit,
    variant: ButtonVariant = ButtonVariant.Ghost,
    size: ButtonSize = ButtonSize.Icon,
    shape: ButtonShape = ButtonShape.Box,
    enabled: Boolean = true,
    title: String? = null,
    type: ButtonType = ButtonType.Button,
    attrs: (AttrsScope<HTMLButtonElement>.() -> Unit)? = null,
    content: ContentBuilder<HTMLButtonElement>,
) {
    Button({
        classNames(buttonClasses(variant, size, shape))
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

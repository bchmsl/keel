package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.dropdownCatchClasses
import io.github.bchmsl.keel.dom.dropdownCheckClasses
import io.github.bchmsl.keel.dom.dropdownClasses
import io.github.bchmsl.keel.dom.dropdownItemClasses
import io.github.bchmsl.keel.icons.Icon
import io.github.bchmsl.keel.icons.LucideIcon
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement

/** Which corner of the trigger the menu hangs from. */
public enum class DropdownAlign(internal val className: String) {
    /** Left edges aligned. For a trigger on the left of its row. */
    Start("dropdown--start"),

    /** Right edges aligned. For a trigger on the right, where a left-aligned menu
     *  would run off the page. */
    End("dropdown--end"),
}

/** Which way the menu opens from its trigger. */
public enum class DropdownSide(internal val className: String?) {
    /** Downward, below the trigger. What a menu does unless there is no room for it. */
    Below(null),

    /**
     * Upward, above the trigger.
     *
     * For a trigger near the foot of the viewport - a control bar pinned to the bottom
     * of a video, a toolbar on a page's last row - where a menu opening downward would
     * fall off the screen.
     *
     * The caller decides, the same way it decides [DropdownAlign], and for the same
     * reason: choosing automatically means measuring the trigger against the viewport
     * on every open and then following it, which is the portalled component this one
     * deliberately is not. A caller that knows its control bar is pinned to the bottom
     * already knows the answer and does not need it measured.
     */
    Above("dropdown--above"),
}

/** How strongly an item reads. */
public enum class DropdownItemTone(internal val className: String?) {
    Default(null),

    /** Sign out, delete, discard. Red text on the normal background rather than a red
     *  fill: a filled row among plain ones reads as the *recommended* action. */
    Danger("dropdown__item--danger"),
}

/**
 * A menu hung off the control that opened it.
 *
 * **Not `role="menu"`, deliberately.** That role promises arrow-key navigation, a
 * single tab stop and typeahead, and an implementation that claims it without
 * building roving tabindex is exactly the failure this library has been removing
 * elsewhere - a control that looks right and announces something it cannot do. What
 * this renders is what it is: a small group of buttons. Tab reaches each one, Enter
 * and Space activate it, and every one of those behaviours is the browser's rather
 * than a reimplementation. If a real menu is wanted later it can be added on top;
 * claiming one now would be a lie the markup cannot keep.
 *
 * Positioning is against the nearest positioned ancestor, so the trigger and the
 * menu go inside one element carrying `dropdownAnchorClasses()`. That element exists
 * only to be that ancestor and sets nothing else.
 *
 * [align] picks which corner it hangs from and [side] picks whether it opens down or
 * up; both are the caller's because both depend on where the trigger sits, and
 * deciding either automatically would mean measuring the trigger against the viewport
 * on every open and following it afterwards.
 *
 * That also means an ancestor with `overflow: hidden` clips the menu - a
 * [Surface] built with `clipped = true`, or a scrolling row. The escape is not a
 * parameter here: it is to put the trigger outside the clipped box, because the
 * alternative is rendering the menu at the document root and tracking the trigger's
 * position on every scroll and resize, which is a different component with a
 * different cost.
 *
 * [onDismiss] is wired to Escape and to a click anywhere outside, via a full-screen
 * catcher one layer below the menu. The catcher is not a [Scrim]: it must change
 * nothing visually. One app built it *from* the scrim and cancelled the dim inline,
 * which left the blur behind - so opening a season menu blurred the whole page - and
 * at the scrim's z-index it covered the very menu it was opened for.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun DropdownMenu(
    onDismiss: () -> Unit,
    ariaLabel: String,
    align: DropdownAlign = DropdownAlign.End,
    side: DropdownSide = DropdownSide.Below,
    dismissOnEscape: Boolean = true,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    if (dismissOnEscape) {
        DismissOnEscape(onDismiss)
    }

    Div({
        classNames(dropdownCatchClasses())
        attr("aria-hidden", "true")
        onClick { onDismiss() }
    })

    Div({
        classNames(dropdownClasses(align, side))
        // A group rather than a menu: it names the set without promising keyboard
        // behaviour the markup does not have. See the note above.
        attr("role", "group")
        attr("aria-label", ariaLabel)
        attrs?.invoke(this)
    }) {
        content()
    }
}

/**
 * One choice in a [DropdownMenu].
 *
 * A real `<button type="button">`, so it is reachable by Tab and activated by both
 * Enter and Space without any of that being written here. The explicit type matters:
 * a menu inside a form would otherwise submit it.
 *
 * [selected] marks the choice already in effect, for a menu that picks one of a set -
 * a video quality, a sort order. It is deliberately not a [DropdownItemTone] entry:
 * tone is how strongly an item reads and selection is whether it is the current value,
 * so an item can be both and the enum would have made them exclusive. It sets
 * `aria-current` as well as the class, which is what actually announces the state -
 * the check drawn beside the label is `aria-hidden`, because hearing "1080p, check
 * mark" adds nothing to hearing "1080p, current".
 *
 * There is no roving selection here and no `role="menuitemradio"`, for the reason
 * [DropdownMenu] gives: this is a group of buttons and says so.
 *
 * [leading] is for an icon. It is a slot rather than a [io.github.bchmsl.keel.icons.LucideIcon]
 * parameter so an app with its own glyphs can pass one.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun DropdownMenuItem(
    label: String,
    onClick: () -> Unit,
    tone: DropdownItemTone = DropdownItemTone.Default,
    selected: Boolean = false,
    attrs: (AttrsScope<HTMLButtonElement>.() -> Unit)? = null,
    leading: ContentBuilder<HTMLButtonElement>? = null,
) {
    Button({
        classNames(dropdownItemClasses(tone, selected))
        type(ButtonType.Button)
        if (selected) attr("aria-current", "true")
        onClick { onClick() }
        attrs?.invoke(this)
    }) {
        leading?.invoke(this)
        Text(label)
        if (selected) {
            Span({
                classNames(dropdownCheckClasses())
                attr("aria-hidden", "true")
            }) { Icon(LucideIcon.Check, size = SELECTED_ICON) }
        }
    }
}

/** Matches the check in a segmented control's chip, which does the same job. */
private const val SELECTED_ICON = 11

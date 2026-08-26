package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.dropdownCatchClasses
import io.github.bchmsl.keel.dom.dropdownClasses
import io.github.bchmsl.keel.dom.dropdownItemClasses
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
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
        classNames(dropdownClasses(align))
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
    attrs: (AttrsScope<HTMLButtonElement>.() -> Unit)? = null,
    leading: ContentBuilder<HTMLButtonElement>? = null,
) {
    Button({
        classNames(dropdownItemClasses(tone))
        type(ButtonType.Button)
        onClick { onClick() }
        attrs?.invoke(this)
    }) {
        leading?.invoke(this)
        Text(label)
    }
}

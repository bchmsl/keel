package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.icons.Icon
import io.github.bchmsl.keel.icons.LucideIcon
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement

/**
 * A modal, with a scrim behind it.
 *
 * [title] and [description] are rendered for assistive technology but never painted:
 * the content carries its own visible heading, and a dialog labelled twice reads
 * twice. They are required rather than optional so a dialog can never reach a screen
 * reader unnamed - which is the failure that is invisible to the person who wrote it.
 *
 * Dismissing works three ways: the scrim, the close button, and Escape. All three
 * by default, because a modal that traps someone is worse than one that closes too
 * easily - and because a dialog that documents Escape without listening for it is
 * worse still.
 *
 * [dismissOnEscape] turns the last one off for a dialog with its own Escape
 * semantics. A nested control does not need it turned off: a handler nearer the key
 * wins by calling `stopPropagation`, so Escape inside an inline editor abandons that
 * edit rather than the dialog around it.
 */
@Composable
public fun Dialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    dismissOnEscape: Boolean = true,
    content: ContentBuilder<HTMLDivElement>,
) {
    if (dismissOnEscape) {
        DismissOnEscape(onDismiss)
    }

    Div({
        classNames("dialog__overlay")
        onClick { onDismiss() }
    })

    Div({
        classNames("dialog__content")
        attr("role", "dialog")
        attr("aria-modal", "true")
        attr("aria-label", title)
        // The scrim sits behind this element, so without this a click inside would
        // bubble out to it and close the dialog the user is typing in.
        onClick { event -> event.stopPropagation() }
    }) {
        H2({ classNames("sr-only") }) { Text(title) }
        P({ classNames("sr-only") }) { Text(description) }

        Button({
            classNames("dialog__close")
            type(ButtonType.Button)
            attr("aria-label", "Close")
            onClick { onDismiss() }
        }) {
            Icon(LucideIcon.X, size = DIALOG_CLOSE_ICON)
        }

        content()
    }
}

private const val DIALOG_CLOSE_ICON = 16

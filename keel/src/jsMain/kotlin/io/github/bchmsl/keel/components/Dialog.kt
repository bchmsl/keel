package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.icons.Icon
import io.github.bchmsl.keel.icons.LucideIcon
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

/**
 * A modal, with a scrim behind it.
 *
 * [title] and [description] are rendered for assistive technology but never painted,
 * so the content can carry its own visible heading without the dialog saying
 * everything twice. They are required rather than optional, so a dialog can never
 * reach a screen reader unnamed - the failure that is invisible to whoever wrote it.
 *
 * They are wired with `aria-labelledby` and `aria-describedby` rather than
 * `aria-label`, and that distinction is the whole point of having both. An
 * `aria-label` naming the dialog, plus a hidden heading holding the same words, gets
 * the title announced on entry and then read again as the first line of content -
 * exactly the double reading this is meant to avoid. Pointing at the elements names
 * the dialog once, and gives the description a role of its own instead of leaving it
 * as body text nothing refers to.
 *
 * Focus moves to the dialog when it opens and returns to whatever held it when the
 * dialog goes away, which is what keeps the keyboard with the thing on screen. Tab
 * is deliberately **not** cycled inside: trapping it means either marking the rest
 * of the page inert or hand-rolling a focus ring, and which is right depends on what
 * else the page is doing. `aria-modal="true"` already tells assistive technology to
 * treat the outside as unavailable.
 *
 * Dismissing works three ways: the scrim, the close button, and Escape. All three by
 * default, because a modal that traps someone is worse than one that closes too
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

    // One number per dialog, so two stacked dialogs cannot point at each other's
    // heading. Remembered rather than recomputed: without that, every recomposition
    // would rewrite the ids and break the association it had just made.
    val instance = remember { nextDialogInstance++ }
    val titleId = "keel-dialog-title-$instance"
    val descriptionId = "keel-dialog-description-$instance"

    Div({
        classNames("dialog__overlay")
        onClick { onDismiss() }
    })

    Div({
        classNames("dialog__content")
        attr("role", "dialog")
        attr("aria-modal", "true")
        attr("aria-labelledby", titleId)
        attr("aria-describedby", descriptionId)
        // Focusable by script but not by Tab, so focus can be put here on open
        // without adding a tab stop nobody asked for.
        attr("tabindex", "-1")
        // The scrim sits behind this element, so without this a click inside would
        // bubble out to it and close the dialog the user is typing in.
        onClick { event -> event.stopPropagation() }

        ref { element ->
            val previous = document.activeElement as? HTMLElement
            element.focus()
            onDispose {
                // Back to the control that opened the dialog. Without this the
                // keyboard restarts from the top of the document, which for someone
                // tabbing means the whole page again.
                previous?.focus()
            }
        }
    }) {
        H2({
            classNames("sr-only")
            id(titleId)
        }) { Text(title) }

        P({
            classNames("sr-only")
            id(descriptionId)
        }) { Text(description) }

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

/**
 * Distinguishes one open dialog from another.
 *
 * A counter rather than a random value: the ids only have to be unique within one
 * document, and a counter is reproducible, which is a property a test can assert.
 */
private var nextDialogInstance = 0

private const val DIALOG_CLOSE_ICON = 16

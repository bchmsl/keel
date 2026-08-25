package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.bchmsl.keel.dom.classNames
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.rows
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

/**
 * A text field with the formatting toolbar under it.
 *
 * The pairing has to live in one place because the toolbar needs the *element*, not
 * its text: the work depends on the caret, and the caret only exists on the element.
 * [TextField] and [TextAreaField] are controlled and hand out no element, so they
 * cannot be used with the toolbar - use this instead.
 *
 * Deliberately **uncontrolled**: the DOM owns the text, and [onCommit] is called when
 * the user is finished with it rather than on every keystroke. Feeding every keystroke
 * back through composition puts the caret at the end of the field on every character
 * typed into the middle of a word, and means a write per keystroke.
 *
 * [resetKey] is what makes an uncontrolled field safe. Changing it rebuilds the
 * element with fresh text, which is what has to happen when a dialog switches to a
 * different record. Without it the second record opens showing the first one's words.
 * Pass whatever identifies the thing being edited.
 *
 * "Finished with it" means blurring, and for a single-line field also pressing Enter,
 * which blurs. Formatting commits too, because the toolbar deliberately does not blur
 * the field and closing a dialog straight after pressing Bold should not lose it.
 */
@Composable
public fun FormattingField(
    resetKey: String,
    initial: String,
    onCommit: (String) -> Unit,
    placeholder: String? = null,
    ariaLabel: String? = null,
    multiline: Boolean = false,
    textRows: Int = DEFAULT_FORMATTING_ROWS,
) {
    key(resetKey) {
        var field: FormattingTarget? by remember { mutableStateOf(null) }

        Div({ classNames("formatting-field") }) {
            if (multiline) {
                TextArea {
                    classNames("textarea")
                    rows(textRows)
                    placeholder?.let { placeholder(it) }
                    ariaLabel?.let { attr("aria-label", it) }
                    ref { element ->
                        element.value = initial
                        field = FormattingTarget(element)
                        onDispose { field = null }
                    }
                    // Read from the event rather than from the remembered field: this
                    // handler is attached while the element is being built, before the
                    // ref effect has recorded it, so a captured reference would be
                    // null for the element's whole first life.
                    onBlur { event ->
                        (event.target as? HTMLTextAreaElement)?.let { onCommit(it.value) }
                    }
                }
            } else {
                Input(InputType.Text) {
                    classNames("input")
                    placeholder?.let { placeholder(it) }
                    ariaLabel?.let { attr("aria-label", it) }
                    ref { element ->
                        element.value = initial
                        field = FormattingTarget(element)
                        onDispose { field = null }
                    }
                    onKeyDown { event ->
                        if (event.key == "Enter") {
                            // Stops a surrounding form submitting, and blurs, which is
                            // what actually commits.
                            event.preventDefault()
                            (event.target as? HTMLInputElement)?.blur()
                        }
                    }
                    // See the note on the multi-line branch: the event carries the
                    // element, so nothing has to have been remembered first.
                    onBlur { event ->
                        (event.target as? HTMLInputElement)?.let { onCommit(it.value) }
                    }
                }
            }

            FormattingToolbar(target = { field }, onTextChange = onCommit)
        }
    }
}

private const val DEFAULT_FORMATTING_ROWS = 6

package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.formattingEditorClasses
import io.github.bchmsl.keel.dom.formattingFieldClasses
import io.github.bchmsl.keel.editor.KeelEditor
import io.github.bchmsl.keel.text.FormattingMarker
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement

/**
 * A text field that shows the formatting rather than the markers, with a toolbar.
 *
 * Bold text is bold here. There is no `**` on screen at any point - the markers are
 * the storage format and stay in storage. [onCommit] still hands back marker text, so
 * nothing above this changes and nothing already stored has to be migrated.
 *
 * Deliberately **uncontrolled**, and more strictly so than a plain field: the editor
 * owns the element's contents outright, and Compose stops at its edge. That is not a
 * preference. A field rebuilt from state on every keystroke loses the caret, the input
 * method's half-typed word and the undo stack, and those are the three things an editor
 * cannot get wrong. So [onCommit] is called when the user is finished rather than as
 * they type.
 *
 * [resetKey] is what makes that safe. Changing it rebuilds the element with fresh
 * content, which is what has to happen when a dialog switches to a different record.
 * Without it the second record opens showing the first one's words. Pass whatever
 * identifies the thing being edited.
 *
 * "Finished with it" means blurring, and for a single-line field also pressing Enter,
 * which blurs. Formatting commits too, because the toolbar deliberately does not blur
 * the field and closing a dialog straight after pressing Bold should not lose it.
 *
 * ------------------------------------------------------------------- keyboard ----
 *
 * Ctrl/Cmd+B, I, U and E. All four are the editor's, including the three the browser
 * has commands for: the engine owns this document, and a browser command writing
 * whatever markup it likes into it leaves the two disagreeing about what is there.
 * Ctrl/Cmd+Z and Shift+Ctrl/Cmd+Z are the editor's own history, so undo steps through
 * formatting and typing alike - including back through a marker pair that fired.
 *
 * -------------------------------------------------------------- typed markers ----
 *
 * Typing the markers works as well as pressing the buttons: finishing `**bold**`,
 * `*it*`, `__u__`, `` `code` `` or `***both***` replaces the characters with the thing
 * they describe, there and then. Without that the buttons would be the only way to
 * format anything, because a field that draws formatting never draws a marker to be
 * read back. `findMarkerAtCaret` holds the rules and the reasoning; `MarkerRule.kt`
 * puts the answer into the document, and Backspace straight after one takes it back.
 *
 * ----------------------------------------------------------------- what is not ----
 *
 * **Markers that arrive by paste stay characters until the record is reopened.** The
 * rule fires on typed text, and a paste is not typed, so a pasted `**x**` sits there as
 * four asterisks round an x; committing writes those characters out unchanged and the
 * next open parses them as bold. It settles rather than drifting, but that first reopen
 * shows something the editor did not.
 *
 * It is the same hole the serializer documents from the other side - the format has no
 * escape, so a literal marker and a marker that means something are the same
 * characters, and no amount of care at this end separates them.
 *
 * Foreign markup pasted in is reduced instead of refused: the clipboard is parsed
 * against a schema that has four marks and one block, so a heading arrives as its text
 * and a bold run arrives bold. Nothing that cannot be stored can get in.
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
        // The editor below is built once, for the element's whole life, so a captured
        // `onCommit` would be the one from the composition that built it - and a dialog
        // whose save action changes would keep calling the old one.
        val commit by rememberUpdatedState(onCommit)

        val holder = remember { EditorHolder() }
        var active by remember { mutableStateOf(emptySet<FormattingMarker>()) }

        Div({ classNames(formattingFieldClasses()) }) {
            Div({
                classNames(formattingEditorClasses(multiline))
                attr("contenteditable", "true")
                attr("role", "textbox")
                attr("spellcheck", "true")
                if (multiline) attr("aria-multiline", "true")
                ariaLabel?.let { attr("aria-label", it) }
                // An editable has no `placeholder`, so the prompt is drawn from this by
                // CSS. See `.formatting-editor[data-empty='true']`.
                placeholder?.let { attr("data-placeholder", it) }

                if (multiline) {
                    // The equivalent of `rows` on a `textarea`, which this is not.
                    // Inline rather than in the stylesheet because it comes from the
                    // call site rather than from the theme - the same reason a pill's
                    // fill is inline. `em` so it still answers to the field's own type
                    // size, and `min-height` so the field still grows with its text.
                    style { property("min-height", "${textRows * LINE_HEIGHT}em") }
                }

                ref { element ->
                    holder.attach(
                        element = element,
                        initial = initial,
                        multiline = multiline,
                        onCommit = { commit(it) },
                        onActiveChange = { active = it },
                    )

                    onDispose { holder.detach() }
                }
            })

            FormattingToolbar(commands = { holder.commands }, active = { active })
        }
    }
}

/**
 * One editor's lifetime, tied to one element's.
 *
 * A class rather than a `DisposableEffect` because the editor has to be built inside
 * `ref` - it needs the element - and torn down in the matching `onDispose`, and
 * because [FormattingToolbar] has to be able to reach it from a later recomposition
 * without rebuilding it.
 */
private class EditorHolder {

    private var editor: KeelEditor? = null

    val commands: FormattingCommands? get() = editor

    fun attach(
        element: HTMLElement,
        initial: String,
        multiline: Boolean,
        onCommit: (String) -> Unit,
        onActiveChange: (Set<FormattingMarker>) -> Unit,
    ) {
        // Declared before it is built so its own callback can reach it: every report
        // the editor makes is a question about the state it has just moved to.
        var created: KeelEditor? = null

        val refresh = {
            created?.let { editor ->
                element.markEmptiness(editor.isEmpty())

                // Nothing is in force once the caret has left, and a toolbar still lit
                // up after that reads as a control that has stopped responding.
                onActiveChange(if (editor.hasFocus()) editor.active() else emptySet())
            }

            Unit
        }

        created = KeelEditor(
            element = element,
            initial = initial,
            multiline = multiline,
            onCommit = onCommit,
            onChanged = refresh,
        )

        editor = created
        refresh()
    }

    fun detach() {
        editor?.destroy()
        editor = null
    }
}

/**
 * Records whether there is anything in the field, for the placeholder rule.
 *
 * An attribute rather than Compose state on purpose: this changes on every keystroke,
 * and routing it through composition would make typing recompose the element whose
 * contents Compose must not touch.
 */
private fun HTMLElement.markEmptiness(empty: Boolean) {
    setAttribute("data-empty", empty.toString())
}

/**
 * Matches the `line-height` `.formatting-editor` is given, and has to.
 *
 * The two together are what make [FormattingField]'s `textRows` mean the same thing it
 * meant on the `textarea` this replaced. Change one and change the other.
 */
private const val LINE_HEIGHT = 1.5

private const val DEFAULT_FORMATTING_ROWS = 6

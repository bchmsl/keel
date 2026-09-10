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
import io.github.bchmsl.keel.text.FormattingMarker
import io.github.bchmsl.keel.text.parseFormattedText
import io.github.bchmsl.keel.text.serializeFormattedNodes
import kotlinx.browser.document
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent

/**
 * A text field that shows the formatting rather than the markers, with a toolbar.
 *
 * Bold text is bold here. There is no `**` on screen at any point - the markers are
 * the storage format and stay in storage. [onCommit] still hands back marker text, so
 * nothing above this changes and nothing already stored has to be migrated.
 *
 * Deliberately **uncontrolled**, and more strictly so than a plain field: the browser
 * owns the element's contents outright, and Compose stops at its edge. That is not a
 * preference. A `contenteditable` rebuilt from state on every keystroke loses the
 * caret, the input method's half-typed word and the undo stack, and those are the
 * three things an editor cannot get wrong. So [onCommit] is called when the user is
 * finished rather than as they type.
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
 * Ctrl/Cmd+B, I and U are the browser's own and are left to it, which is why they are
 * not listed here: intercepting them would mean reimplementing what the native
 * handler already does correctly, including its undo entry. Ctrl/Cmd+E adds code,
 * which has no native command.
 *
 * -------------------------------------------------------------- typed markers ----
 *
 * Typing the markers works as well as pressing the buttons: finishing `**bold**`,
 * `*it*`, `__u__`, `` `code` `` or `***both***` replaces the characters with the
 * thing they describe, there and then. Without that the buttons would be the only way
 * to format anything, because a field that draws formatting never draws a marker to
 * be read back. `findMarkerAtCaret` holds the rules and the reasoning.
 *
 * ----------------------------------------------------------------- what is not ----
 *
 * A pasted selection is forced through plain text, so no foreign markup enters the
 * document. A *dropped* one is not intercepted: the reader is an allow-list, so
 * anything dropped is already reduced to its text and the four marks on the next
 * commit, and swallowing the drop entirely would be a worse field than one that
 * accepts it and tidies up.
 *
 * **Markers that arrive by paste stay characters until the record is reopened.** The
 * input rule fires on a keystroke, and a paste is not one, so a pasted `**x**` sits
 * there as four asterisks round an x; committing writes those characters out
 * unchanged and the next open parses them as bold. It settles rather than drifting,
 * but that first reopen shows something the editor did not.
 *
 * It is the same hole the serializer documents from the other side - the format has
 * no escape, so a literal marker and a marker that means something are the same
 * characters, and no amount of care at this end separates them. Running a paste
 * through the parser instead would close it, at the cost of the undo entry the plain
 * insert buys; worth doing if pasted markup turns out to be common.
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
        // The listeners below are registered once, for the element's whole life, so a
        // captured `onCommit` would be the one from the composition that built it -
        // and a dialog whose save action changes would keep calling the old one.
        val commit by rememberUpdatedState(onCommit)

        val editor = remember { FormattingEditor() }
        var active by remember { mutableStateOf(emptySet<FormattingMarker>()) }

        Div({ classNames(formattingFieldClasses()) }) {
            Div({
                classNames(formattingEditorClasses(multiline))
                attr("contenteditable", "true")
                attr("role", "textbox")
                attr("spellcheck", "true")
                if (multiline) attr("aria-multiline", "true")
                ariaLabel?.let { attr("aria-label", it) }
                // A `contenteditable` has no `placeholder`, so the prompt is drawn from
                // this by CSS. See `.formatting-editor[data-empty='true']`.
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
                    editor.attach(
                        element = element,
                        initial = initial,
                        multiline = multiline,
                        onCommit = { commit(it) },
                        onActiveChange = { active = it },
                    )

                    onDispose { editor.detach() }
                }
            })

            FormattingToolbar(commands = { editor.commands }, active = { active })
        }
    }
}

/**
 * The listeners and the imperative content of one editable element.
 *
 * A class rather than a handful of `DisposableEffect`s because every one of these has
 * to be removed together when the element goes, and one of them is on the *document*
 * rather than on the element - a `selectionchange` listener outlives its element
 * otherwise, and then holds it alive and writes to a dead composition's state.
 */
private class FormattingEditor {

    private var element: HTMLElement? = null
    private var listeners: List<() -> Unit> = emptyList()

    var commands: FormattingCommands? = null
        private set

    fun attach(
        element: HTMLElement,
        initial: String,
        multiline: Boolean,
        onCommit: (String) -> Unit,
        onActiveChange: (Set<FormattingMarker>) -> Unit,
    ) {
        this.element = element

        element.renderFormatted(parseFormattedText(initial))
        element.markEmptiness()

        // Declared before it is built so its own callback can reach it. `toggleCode`
        // rearranges the DOM by hand and so fires no `input` event, which is what
        // would otherwise have refreshed the pressed state.
        var editable: EditableCommands? = null

        editable = EditableCommands(element) {
            // A formatting button changes the text and the pressed state at once, and
            // commits, because the toolbar does not blur the field.
            element.markEmptiness()
            onCommit(element.read())
            editable?.active()?.let(onActiveChange)
        }

        commands = editable

        val refreshActive = { editable.active().let(onActiveChange) }

        listeners = listOf(
            element.on("blur") {
                onCommit(element.read())
                // Nothing is in force once the caret has left, and a toolbar still
                // lit up after that reads as a control that has stopped responding.
                onActiveChange(emptySet())
            },

            element.on("input") { event ->
                // Before the two below, because it moves the caret and changes the
                // text: they should report where things ended up.
                if (event.isOneTypedCharacter()) editable.formatFinishedMarker()

                element.markEmptiness()
                refreshActive()
            },

            element.on("focus") { refreshActive() },

            element.on("paste") { event -> element.pasteAsPlainText(event) },

            element.on("keydown") { event ->
                (event as? KeyboardEvent)?.let { key ->
                    handleKeyDown(key, element, multiline, editable)
                }
            },

            // On the document, because a selection is the document's and an element
            // gets no event when the caret moves inside it.
            document.on("selectionchange") { refreshActive() },
        )
    }

    fun detach() {
        listeners.forEach { it() }
        listeners = emptyList()
        commands = null
        element = null
    }

    private fun handleKeyDown(
        event: KeyboardEvent,
        element: HTMLElement,
        multiline: Boolean,
        editable: FormattingCommands,
    ) {
        val accelerator = event.metaKey || event.ctrlKey

        when {
            // Code has no native command, so it is the one shortcut worth adding.
            // Slack's choice of letter, for the same action.
            accelerator && event.key.lowercase() == "e" -> {
                event.preventDefault()
                editable.toggle(FormattingMarker.Code)
            }

            // A single-line field commits on Enter by blurring, exactly as the
            // `input` it replaces did. `preventDefault` first, or the browser inserts
            // a line break into a field that is supposed to have one line.
            event.key == "Enter" && !multiline && !event.shiftKey -> {
                event.preventDefault()
                element.blur()
            }
        }
    }
}

/**
 * Replaces a paste with its plain text.
 *
 * The clipboard can hold a whole document's markup, and pasting it into an editable
 * inserts it wholesale. `insertText` is used rather than writing the node directly
 * because it goes through the native undo stack, so Ctrl+Z after a paste behaves.
 */
private fun HTMLElement.pasteAsPlainText(event: Event) {
    event.preventDefault()

    val text = event.asDynamic().clipboardData?.getData("text/plain") as? String ?: return
    document.asDynamic().execCommand("insertText", false, text)
}

/**
 * Whether this `input` event was one character being typed.
 *
 * The gate on the marker input rule, which fires on the keystroke that finishes a
 * pair and so has to be a keystroke. A deletion that happens to leave a finished pair
 * behind is not one, and neither is an autocorrection replacing a whole word.
 *
 * The length is what excludes a paste, which arrives as `insertText` too because that
 * is what [pasteAsPlainText] uses to keep the undo stack. A one-character paste gets
 * treated as typing, and nothing is harmed by that.
 */
private fun Event.isOneTypedCharacter(): Boolean {
    val event = asDynamic()
    return event.inputType == "insertText" && (event.data as? String)?.length == 1
}

/** The field's contents as the marker text everything above this layer speaks. */
private fun HTMLElement.read(): String = serializeFormattedNodes(readFormatted())

/**
 * Records whether there is anything in the field, for the placeholder rule.
 *
 * An attribute rather than Compose state on purpose: this changes on every keystroke,
 * and routing it through composition would make typing recompose the element whose
 * contents Compose must not touch.
 */
private fun HTMLElement.markEmptiness() {
    val empty = textContent?.replace(ZERO_WIDTH_SPACE, "").isNullOrEmpty()
    setAttribute("data-empty", empty.toString())
}

/** Registers a listener and hands back the call that removes it again. */
private fun EventTarget.on(type: String, handler: (Event) -> Unit): () -> Unit {
    val listener = EventListener { handler(it) }
    addEventListener(type, listener)

    return { removeEventListener(type, listener) }
}

/**
 * Matches the `line-height` `.formatting-editor` is given, and has to.
 *
 * The two together are what make [FormattingField]'s `textRows` mean the same thing it
 * meant on the `textarea` this replaced. Change one and change the other.
 */
private const val LINE_HEIGHT = 1.5

private const val DEFAULT_FORMATTING_ROWS = 6

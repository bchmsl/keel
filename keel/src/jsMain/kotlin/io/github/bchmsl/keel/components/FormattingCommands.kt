package io.github.bchmsl.keel.components

import io.github.bchmsl.keel.text.FormattingMarker
import io.github.bchmsl.keel.text.TextSelection
import io.github.bchmsl.keel.text.applyFormatting
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.asList

/**
 * What the formatting toolbar can ask of the thing it is formatting.
 *
 * Two implementations, because there are two genuinely different fields underneath and
 * the toolbar should not know which it has. A `contenteditable` shows the formatting
 * and hides the markers, so its answer to [active] is real; a plain `textarea` shows
 * the markers themselves, so it has no state to report.
 */
public interface FormattingCommands {

    /** Turns [marker] on across the selection, or off if it is already on. */
    public fun toggle(marker: FormattingMarker)

    /**
     * The markers in force at the caret, for the toolbar's pressed state.
     *
     * Read on demand rather than stored, because the browser owns the selection and it
     * moves for reasons no Kotlin state hears about - a drag, a double-click, an arrow
     * key.
     */
    public fun active(): Set<FormattingMarker>
}

/**
 * Formats a `contenteditable` through the browser's own editing commands.
 *
 * `execCommand` is deprecated and is still the right call here. Its replacement does
 * not exist: nothing else toggles a mark across a selection that partly overlaps an
 * existing one, and nothing else writes through the **native undo stack**, so Ctrl+Z
 * keeps working across formatting as well as typing. MDN says as much, keeping the
 * method documented for the cases with no alternative. Should a browser ever drop it,
 * the three calls are here and the replacement is the same range surgery [toggleCode]
 * already does.
 *
 * The whole surface is these few methods on purpose. Everything above this layer works
 * in terms of the parsed tree, which is testable without a browser; this file is the
 * part that cannot be, so it is kept small enough to check by hand in the gallery.
 */
internal class EditableCommands(
    private val element: HTMLElement,
    private val onChanged: () -> Unit,
) : FormattingCommands {

    override fun toggle(marker: FormattingMarker) {
        // The toolbar suppresses the blur that a mouse press would otherwise cause, so
        // focus is usually still here - but not when the field has never been touched
        // and somebody presses Bold first, and then there is no selection to act on.
        element.focus()
        preferTagsOverInlineStyles()

        when (marker) {
            FormattingMarker.Bold -> execute("bold")
            FormattingMarker.Italic -> execute("italic")
            FormattingMarker.Underline -> execute("underline")
            FormattingMarker.Code -> toggleCode()
        }

        onChanged()
    }

    override fun active(): Set<FormattingMarker> {
        // `queryCommandState` answers for the document's selection wherever it is, so
        // without this a selection in a different field would light up this toolbar.
        if (!holdsSelection()) return emptySet()

        return buildSet {
            if (queryState("bold")) add(FormattingMarker.Bold)
            if (queryState("italic")) add(FormattingMarker.Italic)
            if (queryState("underline")) add(FormattingMarker.Underline)
            if (enclosingCode() != null) add(FormattingMarker.Code)
        }
    }

    /**
     * Asks for `<b>` and `<i>` rather than `style="font-weight: bold"`.
     *
     * Document-wide state and cheap, so it is set before every command instead of once
     * at startup - which would be one ordering assumption about who runs first, for
     * nothing saved. Inline styles would still be read correctly, since the reader
     * treats an unrecognised `<span>` as see-through, but they would be read as *plain
     * text*: the mark would disappear on the next commit.
     */
    private fun preferTagsOverInlineStyles() {
        document.asDynamic().execCommand("styleWithCSS", false, "false")
    }

    private fun execute(command: String) {
        document.asDynamic().execCommand(command, false, null)
    }

    private fun queryState(command: String): Boolean =
        document.asDynamic().queryCommandState(command) == true

    /**
     * Wraps the selection in a code span, or unwraps the one it is already in.
     *
     * Hand-written because there is no `execCommand` for code. The selection's *text*
     * is taken rather than its nodes, which is not a shortcut: code holds a string in
     * the model precisely so markers inside it stay content, so any formatting inside
     * the selection has to be dropped rather than nested.
     */
    private fun toggleCode() {
        val selection = window.asDynamic().getSelection() ?: return
        if (selection.rangeCount == 0) return

        enclosingCode()?.let { unwrap(it) } ?: wrapSelectionInCode(selection)
    }

    private fun wrapSelectionInCode(selection: dynamic) {
        val range = selection.getRangeAt(0)
        // A range's `toString` is its text, which is what a code span holds.
        val text = range.toString()

        val code = document.createElement("code")
        code.setAttribute("class", CODE_CLASS)
        // An empty span has nowhere to put the caret, so pressing Code with nothing
        // selected would look like a dead button. The placeholder is stripped on read.
        code.textContent = text.ifEmpty { ZERO_WIDTH_SPACE }

        range.deleteContents()
        range.insertNode(code)

        caretToEndOf(code, selection)
    }

    private fun unwrap(code: Element) {
        val parent = code.parentNode ?: return

        // Snapshotted, because `childNodes` is live and moving a child out from under
        // the iteration would skip the next one.
        val contents = code.childNodes.asList().toList()
        contents.forEach { parent.insertBefore(it, code) }
        parent.removeChild(code)

        // Without this the caret is left pointing at an element that is no longer in
        // the document, and the next keystroke goes nowhere.
        val selection = window.asDynamic().getSelection() ?: return
        val restored = document.asDynamic().createRange()

        if (contents.isEmpty()) {
            restored.selectNodeContents(parent)
            restored.collapse(false)
        } else {
            restored.setStartBefore(contents.first())
            restored.setEndAfter(contents.last())
        }

        selection.removeAllRanges()
        selection.addRange(restored)
    }

    private fun caretToEndOf(node: Node, selection: dynamic) {
        val range = document.asDynamic().createRange()
        range.selectNodeContents(node)
        range.collapse(false)

        selection.removeAllRanges()
        selection.addRange(range)
    }

    /** The `<code>` the caret sits inside, if it is inside one of this field's. */
    private fun enclosingCode(): Element? {
        if (!holdsSelection()) return null

        var node: Node? = window.asDynamic().getSelection()?.focusNode as? Node

        while (node != null && node != element) {
            val candidate = node as? Element
            if (candidate != null && candidate.tagName.lowercase() == "code") return candidate
            node = node.parentNode
        }

        return null
    }

    private fun holdsSelection(): Boolean {
        val focus = window.asDynamic().getSelection()?.focusNode as? Node ?: return false
        return element.contains(focus)
    }
}

/**
 * Formats a plain field by writing the markers into its text.
 *
 * The original behaviour, kept for a caller that wants the toolbar over an ordinary
 * `textarea` - where the markers are the only thing that could show the formatting.
 * [active] is therefore empty by definition rather than by omission: the field already
 * displays its own state, in the characters themselves.
 */
internal class MarkerSplicingCommands(
    private val field: FormattingTarget,
    private val onTextChange: (String) -> Unit,
) : FormattingCommands {

    override fun toggle(marker: FormattingMarker) {
        val start = field.selectionStart ?: field.value.length
        val end = field.selectionEnd ?: start

        val result = applyFormatting(field.value, TextSelection(start, end), marker)

        field.value = result.text
        onTextChange(result.text)

        // Next frame rather than now: a browser puts the caret at the end of a field
        // whose value has just been set, and it does so after the current task
        // finishes. Restoring the selection before that would simply be undone.
        window.requestAnimationFrame {
            field.focus()
            field.select(result.selection.start, result.selection.end)
        }
    }

    override fun active(): Set<FormattingMarker> = emptySet()
}

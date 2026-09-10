package io.github.bchmsl.keel.components

import io.github.bchmsl.keel.text.FormattingMarker
import io.github.bchmsl.keel.text.MarkerAtCaret
import io.github.bchmsl.keel.text.TextSelection
import io.github.bchmsl.keel.text.applyFormatting
import io.github.bchmsl.keel.text.findMarkerAtCaret
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
     * Turns a marker pair the typist has just finished into the formatting it names.
     *
     * Without this the buttons are the only way to format, because a field that draws
     * formatting never draws a marker: `**x**` typed by hand stays four asterisks
     * round an x until the record is closed and reopened, and only then turns bold.
     * [findMarkerAtCaret] decides whether a pair has just been finished; this puts
     * the answer into the document.
     *
     * Written as DOM surgery rather than through `execCommand`, which is the opposite
     * of the choice [toggle] makes, for one reason: there is no command that swaps a
     * stretch of text for an element, and the two-step versions that get close all
     * have a failure in the middle where the markers are gone and the mark has not
     * landed. This cannot lose what was typed. The price is the native undo entry -
     * Ctrl+Z after a pair fires steps through the typing rather than putting the
     * asterisks back - and the mark is still on the toolbar to be pressed off.
     */
    fun formatFinishedMarker() {
        val selection = window.asDynamic().getSelection() ?: return
        if (selection.rangeCount == 0 || selection.isCollapsed != true) return

        val node = selection.focusNode as? Node ?: return
        if (node.nodeType != Node.TEXT_NODE || !element.contains(node)) return

        // Everything inside a code span is content, markers included. That is what a
        // code span is for, so a rule that formatted them would defeat it.
        if (enclosingCode() != null) return

        val text = node.textContent ?: return
        val match = findMarkerAtCaret(text, selection.focusOffset as Int) ?: return

        replaceWithFormatted(node, match, selection)
    }

    /**
     * Swaps `[start, end)` of [node] for the formatted run, leaving the caret after it.
     *
     * The caret lands at the start of the text that followed rather than inside what
     * was just built, so the next keystroke carries on the sentence instead of joining
     * the mark. A typist who finishes `**bold**` and keeps going does not expect the
     * rest of the line to be bold too.
     *
     * Which takes an invisible character when the pair was finished at the end of the
     * line, because then there is no text to land in. An **empty** text node is not a
     * caret position a browser keeps: asked to insert there it walks back into the
     * element in front, and the closing `__` of `` __u `c`__ `` ends up inside the code
     * span instead of finishing the underline. A [ZERO_WIDTH_SPACE] gives the caret a
     * real character to sit on and the insertion stays outside. Every read strips it,
     * so it reaches neither storage nor the emptiness test behind the placeholder.
     *
     * The cost is one dead keystroke: a Backspace pressed *immediately* after a pair
     * fires deletes the anchor and appears to do nothing, and the second press starts
     * on the text. It is the same bargain the empty code span already makes.
     */
    private fun replaceWithFormatted(node: Node, match: MarkerAtCaret, selection: dynamic) {
        val text = node.textContent ?: return
        val parent = node.parentNode ?: return

        val head = text.substring(0, match.start)
        val rest = text.substring(match.end)
        val tail = document.createTextNode(rest.ifEmpty { ZERO_WIDTH_SPACE })

        node.textContent = head

        // Both go in front of whatever followed this node, in order, so the line reads
        // head, mark, tail. A null anchor appends, which is what is wanted at the end.
        val anchor = node.nextSibling
        parent.insertBefore(formattedFragment(listOf(match.formatted)), anchor)
        parent.insertBefore(tail, anchor)

        // A text node holding nothing is not a place a caret can be put in every
        // browser, and it would be the field's first child if the line began with the
        // marker. Dropping it is safe: it carried no text to begin with.
        if (head.isEmpty()) parent.removeChild(node)

        val range = document.asDynamic().createRange()
        // After the anchor when that is all the tail holds, so the caret is past it
        // rather than between it and the element it is keeping the caret out of.
        range.setStart(tail, if (rest.isEmpty()) 1 else 0)
        range.collapse(true)
        selection.removeAllRanges()
        selection.addRange(range)

        // The caret is now outside the new element, but a browser can still hold the
        // mark as pending from having just passed through one - and then the next
        // character comes out bold. Asking for the state and toggling what is on is
        // the only way to clear that.
        clearPendingMarks()
    }

    /** Turns off any mark still in force at a caret that is outside every mark. */
    private fun clearPendingMarks() {
        preferTagsOverInlineStyles()

        listOf("bold", "italic", "underline").forEach { command ->
            if (queryState(command)) execute(command)
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

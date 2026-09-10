package io.github.bchmsl.keel.components

import io.github.bchmsl.keel.text.FormattingMarker
import io.github.bchmsl.keel.text.TextSelection
import io.github.bchmsl.keel.text.applyFormatting
import kotlinx.browser.window

/**
 * What the formatting toolbar can ask of the thing it is formatting.
 *
 * Two implementations, because there are two genuinely different fields underneath and
 * the toolbar should not know which it has. [FormattingField] shows the formatting and
 * hides the markers, so its answer to [active] is real; a plain `textarea` shows the
 * markers themselves, so it has no state to report.
 *
 * The interface is public and small, and the field's implementation is not visible
 * from here at all - it lives with the editor, in `KeelEditor`. That is the seam that
 * keeps the editing engine an implementation detail of one component: a consumer sees
 * these two methods and marker text, and nothing else.
 */
public interface FormattingCommands {

    /** Turns [marker] on across the selection, or off if it is already on. */
    public fun toggle(marker: FormattingMarker)

    /**
     * The markers in force at the caret, for the toolbar's pressed state.
     *
     * Read on demand rather than stored, because the selection moves for reasons no
     * Kotlin state hears about - a drag, a double-click, an arrow key.
     */
    public fun active(): Set<FormattingMarker>
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

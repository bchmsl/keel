package io.github.bchmsl.keel.editor

import io.github.bchmsl.keel.text.FormattedNode
import io.github.bchmsl.keel.text.findMarkerAtCaret
import io.github.bchmsl.keel.text.markerCut

/**
 * One input rule: keel's own marker decision, applied to a ProseMirror document.
 *
 * ------------------------------------------------------------------ one rule ----
 *
 * Editors built on this engine normally ship a rule per mark, each with its own
 * regular expression - Tiptap does, and so did the first version of this. That is a
 * second copy of what `**` means, written in a different notation from the first, and
 * the two are free to drift: keel's parser refuses `2 * 3 *` and a regular expression
 * has to be told to.
 *
 * So there is one rule, and its pattern decides nothing. It says only "the character
 * just typed was a marker character, and here is the text in front of it". Everything
 * after that is [findMarkerAtCaret], which is pure, lives in `commonMain` and has
 * eighteen tests behind it on two platforms. Run lengths matching, `snake_case`
 * surviving, `****x****` meaning nothing, a space next to a marker disqualifying the
 * pair: all of it is already decided there and none of it is restated here.
 *
 * -------------------------------------------------------------- the positions ----
 *
 * `markerCut` works out the four edits, and its documentation explains the one thing
 * about this that is genuinely hard: the keystroke that fired the rule is not in the
 * document yet. The markers are cut out and the wrapped text is left exactly where it
 * is, which is what lets a pair typed *around* a pair that already fired keep the
 * inner formatting.
 *
 * ----------------------------------------------------------------- what is not ----
 *
 * A paste does not come through here. The engine parses clipboard content against the
 * schema instead, so pasted markers stay characters until the record is next read -
 * the same behaviour the field had before, and the same reasoning: there is no escape
 * in this format, so a literal marker and a meant one are the same characters.
 *
 * A multi-character arrival is refused by `markerCut` rather than guessed at, which
 * covers an input method committing a whole word at once.
 */
internal fun markerInputRule(schema: Schema): InputRule {
    val handler = { state: dynamic, match: dynamic, start: Int, end: Int ->
        fireMarker(schema, state, window = match[0] as? String ?: "", start = start, end = end)
    }

    return InputRule(JsRegExp(MARKER_TYPED), handler)
}

/**
 * The transaction that replaces a finished pair, or null to let the character through.
 *
 * Returning null matters as much as returning a transaction: it is what leaves an
 * unfinished `**bold` on screen as the characters that were typed.
 */
private fun fireMarker(
    schema: Schema,
    state: dynamic,
    window: String,
    start: Int,
    end: Int,
): dynamic {
    val match = findMarkerAtCaret(window, window.length) ?: return null

    // Everything the document already holds ends at `end`; the difference is the
    // arrival that fired the rule.
    val pending = window.length - (end - start)
    val cut = markerCut(match, windowStart = start, pending = pending) ?: return null

    // Inside a code span every character is content, markers included - that is what
    // a code span is for, so a rule that formatted them would defeat it. `code: true`
    // on the mark does not cover this: the engine reads that flag on the *node* when it
    // decides whether to run rules, so the mark has to be asked about directly.
    //
    // Asked about the caret rather than about the whole match, because the question is
    // "is the typist inside a code span", and a match can start outside one. `code` is
    // `inclusive: false`, so the position just past a span answers no, which is what
    // lets a pair be typed immediately after `` `x` ``.
    if (hasMarkAt(state, end, schema.marks[CODE])) return null

    val marks = match.formatted.markNames()
    if (marks.isEmpty()) return null

    val transaction = state.tr

    // The closing run first. Its positions are after the opening run's, so removing
    // it leaves the opening run's positions still valid; the other order would not.
    if (cut.closeTo > cut.closeFrom) transaction.delete(cut.closeFrom, cut.closeTo)
    transaction.delete(cut.openFrom, cut.openTo)

    marks.forEach { name ->
        val type = schema.marks[name]
        transaction.addMark(cut.markFrom, cut.markTo, type.create())

        // Without this the mark is still pending at the caret and the next character
        // typed joins it. The schema's `inclusive: false` is the other half of the
        // same problem, and both are needed: one is about stored marks and the other
        // about the position.
        transaction.removeStoredMark(type)
    }

    // A fire is its own undo entry, so the first Ctrl+Z after one hands the markers
    // back rather than discarding the sentence they were typed in. See `closeHistory`.
    return closeHistory(transaction)
}

/**
 * The marks a node from [findMarkerAtCaret] stands for, outermost first.
 *
 * Read off the node the rule produced rather than looked up from the marker string,
 * so `***` meaning bold-and-italic is stated in exactly one place - the `RULES` table
 * beside `findMarkerAtCaret` - and this cannot come to disagree with it.
 */
private fun FormattedNode.markNames(): List<String> {
    val names = mutableListOf<String>()
    var node: FormattedNode? = this

    while (node != null) {
        val current: FormattedNode = node

        node = when (current) {
            is FormattedNode.Bold -> {
                names += STRONG
                current.children.singleOrNull()
            }

            is FormattedNode.Italic -> {
                names += EM
                current.children.singleOrNull()
            }

            is FormattedNode.Underline -> {
                names += UNDERLINE
                current.children.singleOrNull()
            }

            // Holds a string rather than children, so there is nowhere further down.
            is FormattedNode.Code -> {
                names += CODE
                null
            }

            is FormattedNode.Plain, is FormattedNode.Link -> null
        }
    }

    return names
}

/**
 * A marker character at the caret, and the text in front of it.
 *
 * The whole match is handed to [findMarkerAtCaret], so the pattern deliberately
 * describes as little as possible: anything that is not a line break, then one of the
 * three characters keel's markers are made of, then the caret. `.` excludes newlines
 * in this dialect, which is what keeps the window inside one line.
 *
 * The engine already caps how far back it looks. A pair whose opening run falls
 * outside that window does not fire, and settles on the next read instead - a wider
 * limit than the field this replaced, which could only see one stretch of unformatted
 * text.
 */
private const val MARKER_TYPED = """.*[*_`]$"""

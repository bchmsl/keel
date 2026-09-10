package io.github.bchmsl.keel.text

/** A finished marker pair sitting immediately behind the caret. */
internal data class MarkerAtCaret(
    /** Index of the first character of the opening run. */
    val start: Int,
    /** One past the closing run, which is where the caret is. */
    val end: Int,
    /** What the text between the two runs becomes. */
    val formatted: FormattedNode,
    /**
     * One run of markers, as typed: `**`, `*`, `__`, `` ` `` or `***`.
     *
     * Both runs are this, which is what [MarkerAtCaret] means by a finished pair, so
     * one string describes both and its length is how far in the wrapped text starts.
     * Reported rather than re-derived, because the caller that needs it is editing a
     * document by position and getting the length wrong there deletes content instead
     * of markers.
     */
    val marker: String,
)

/**
 * Looks for a marker pair the typist has just finished, so it can become formatting.
 *
 * A field that draws formatting rather than markers leaves typed markers with nothing
 * to do: `**x**` would sit there as four asterisks around an x and only turn bold the
 * next time the record was opened. This closes that gap. The moment a pair is
 * finished the characters are replaced by the thing they describe, the way a chat
 * composer behaves and the way the buttons already do.
 *
 * Text in, decision out, so all of the deciding is tested without a browser. Acting on
 * the answer belongs to the editor, which is the only part that can edit a document -
 * `MarkerRule.kt` on the JS side, and `markerCut` for the positions.
 *
 * -------------------------------------------------------------------- the rules ----
 *
 * A pair fires on the keystroke that closes it, and only when all three hold:
 *
 * - **The closing run is exactly as long as the opening one.** This is what lets
 *   `***both***` be typed at all. After the first of the three closing asterisks the
 *   line reads `***both*`, and a rule that took the nearest single asterisk would
 *   fire italic on `both` and leave the typist fighting it. Matching run lengths
 *   means nothing happens until the third one lands.
 * - **The two runs spell a marker [parseFormattedText] reads.** So a single `_` does
 *   nothing, which is what leaves `snake_case` alone, and neither does `****x****`.
 * - **There is something between them, and it neither starts nor ends with a space.**
 *   This is the rule that leaves arithmetic alone: `2 * 3 *` has a run on each side
 *   of ` 3 ` and is not emphasis.
 *
 * ------------------------------------------------------------------ the caller ----
 *
 * Whatever [text] holds is what gets searched, so how far a pair can reach is the
 * caller's decision rather than this function's. The editor hands it the line's text
 * up to the caret, formatting and all, which is what lets a pair typed *around* one
 * that has already fired still fire: `__**q**__` ends up bold and underlined, because
 * the bold `q` in the middle is still `q` when read as text.
 *
 * ----------------------------------------------------------------- what it wont ----
 *
 * Pairs fire innermost first, because that is the order they get finished in. Typing
 * `` `**x**` `` bolds the x on its fifth keystroke and so never sees a code span,
 * where the parser reading that same finished line would give code all of it. Nothing
 * can be done about that from inside a rule that fires as you type, and the result is
 * still exactly what was typed, so it is written down rather than fought.
 */
internal fun findMarkerAtCaret(text: String, caret: Int): MarkerAtCaret? {
    if (caret < 1 || caret > text.length) return null

    val char = text[caret - 1]
    val closing = text.runLengthEndingAt(caret, char)
    if (closing > LONGEST_MARKER) return null

    val run = char.toString().repeat(closing)
    val build = RULES[run] ?: return null

    val closeStart = caret - closing
    val openEnd = text.endOfNearestRun(before = closeStart, char = char, length = closing)
        ?: return null

    // Non-empty by construction: `endOfNearestRun` only accepts a run that finishes
    // before the closing one starts.
    val inner = text.substring(openEnd, closeStart)
    if (inner.first().isWhitespace() || inner.last().isWhitespace()) return null

    return MarkerAtCaret(
        start = openEnd - closing,
        end = caret,
        formatted = build(inner),
        marker = run,
    )
}

/**
 * Every marker run, and the node it makes.
 *
 * Keyed by the same constants [parseFormattedText] reads, so the rule cannot come to
 * disagree with the parser about what `***` means. `***` is listed first for readers
 * rather than for the lookup, which is exact.
 */
private val RULES: Map<String, (String) -> FormattedNode> = mapOf(
    BOLD_ITALIC_MARKER to { text ->
        FormattedNode.Bold(listOf(FormattedNode.Italic(listOf(FormattedNode.Plain(text)))))
    },
    BOLD_MARKER to { text -> FormattedNode.Bold(listOf(FormattedNode.Plain(text))) },
    ITALIC_MARKER to { text -> FormattedNode.Italic(listOf(FormattedNode.Plain(text))) },
    UNDERLINE_MARKER to { text -> FormattedNode.Underline(listOf(FormattedNode.Plain(text))) },
    // A code span holds a string rather than children, which is what keeps markers
    // inside it as content rather than as more markup.
    CODE_MARKER to { text -> FormattedNode.Code(text) },
)

private val LONGEST_MARKER = RULES.keys.maxOf { it.length }

/** How many [char]s in a row finish at [end]. */
private fun String.runLengthEndingAt(end: Int, char: Char): Int {
    var start = end

    while (start > 0 && this[start - 1] == char) start--

    return end - start
}

/**
 * Where the nearest run of **exactly** [length] [char]s that finishes before [before]
 * ends, or null if there is none.
 *
 * Whole runs are measured and then accepted or skipped, rather than the last few
 * characters of one being read as a match. Without that, `***x**` would find the last
 * two asterisks of its three-character opener and come out as bold with a stray
 * asterisk in front.
 */
private fun String.endOfNearestRun(before: Int, char: Char, length: Int): Int? {
    // The character at `before - 1` cannot be `char` - if it were, the closing run
    // would have been measured longer - so the scan always starts outside a run.
    var index = before - 1

    while (index >= 0) {
        if (this[index] != char) {
            index--
            continue
        }

        var start = index
        while (start > 0 && this[start - 1] == char) start--

        if (index + 1 - start == length) return index + 1

        index = start - 1
    }

    return null
}

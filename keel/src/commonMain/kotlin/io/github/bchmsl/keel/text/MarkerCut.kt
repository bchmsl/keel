package io.github.bchmsl.keel.text

/**
 * The four edits that turn a finished marker pair into a mark, as positions.
 *
 * Positions in the document being edited, not indices into a string, which is the
 * only reason this is a type rather than four lines inside the caller. It exists to
 * be tested: every one of these numbers was wrong at least once while the editor was
 * being written, and every one of those mistakes deleted the text the typist had just
 * written instead of the markers around it. Arithmetic that fails that way belongs
 * somewhere it can be checked without a browser.
 */
internal data class MarkerCut(
    /** The opening run of markers, to be removed. */
    val openFrom: Int,
    val openTo: Int,
    /**
     * The part of the closing run that is actually in the document.
     *
     * Empty when a single-character marker is being finished, because then the whole
     * closing run is the keystroke that has not arrived. See [markerCut].
     */
    val closeFrom: Int,
    val closeTo: Int,
    /** The wrapped text, once both runs are gone. What the mark is applied to. */
    val markFrom: Int,
    val markTo: Int,
)

/**
 * Where [match] falls in a document that has not been given the keystroke yet.
 *
 * ------------------------------------------------------------ the one hard part ----
 *
 * An editor's input rule is asked to decide **before** the character that fired it is
 * in the document. The text it matched against is what precedes the caret plus the
 * text arriving; the document holds only the first part. So a rule that finishes
 * `**bold**` on the fifth asterisk sees a document reading `**bold*`, and the
 * position one past the end of the match is not a position in it.
 *
 * That is what [pending] is: how many characters of the match have not landed. The
 * caller is expected to discard them rather than insert them, which is why the
 * closing run's edit stops at [MarkerCut.closeTo] and there is no insertion here at
 * all - the markers were never wanted, and the wrapped text is already in place.
 *
 * Leaving the wrapped text where it is rather than deleting the whole match and
 * writing it back is not an optimisation. Re-inserting it would insert *plain* text,
 * and a pair typed around a pair that has already fired would come back with the
 * inner formatting gone: `__**q**__` becomes an underlined `q` with no bold. Only the
 * markers may be touched.
 *
 * ------------------------------------------------------------------- arguments ----
 *
 * [windowStart] is the document position of the first character of the text [match]
 * was found in. [pending] is at least 1, because a rule fires on an arrival.
 *
 * Null when the numbers do not describe a single keystroke completing a pair - an
 * input method or a paste delivering several characters at once, or a caller passing
 * a match from different text. The caller should leave the document alone rather than
 * guess, and the typed markers then settle on the next read instead.
 */
internal fun markerCut(match: MarkerAtCaret, windowStart: Int, pending: Int): MarkerCut? {
    val run = match.marker.length

    // More arriving at once than the closing run holds is not this keystroke.
    if (pending < 1 || pending > run) return null

    val inner = match.end - match.start - 2 * run
    if (inner < 1) return null

    val openFrom = windowStart + match.start
    val openTo = openFrom + run

    // The closing run, less the characters still on their way.
    val closeTo = windowStart + match.end - pending
    val closeFrom = closeTo - (run - pending)

    // The two runs would overlap, so this is not a pair sitting in this text.
    if (closeFrom < openTo) return null

    return MarkerCut(
        openFrom = openFrom,
        openTo = openTo,
        closeFrom = closeFrom,
        closeTo = closeTo,
        // Both removals are at or before the wrapped text, and only the opening one
        // is in front of it, so it ends up one run earlier than it sits now.
        markFrom = openFrom,
        markTo = openFrom + inner,
    )
}

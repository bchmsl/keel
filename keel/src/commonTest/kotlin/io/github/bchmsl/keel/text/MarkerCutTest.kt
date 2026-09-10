package io.github.bchmsl.keel.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The position arithmetic that turns a finished pair into a mark.
 *
 * Every case here is a mistake that was made for real while the editor was written,
 * so the assertions are written as "what the document holds now" rather than as
 * numbers alone. The shape to keep in mind throughout: the keystroke that fired the
 * rule is **not** in the document, so a document finishing `**bold**` reads `**bold*`
 * at the moment these numbers are calculated.
 */
class MarkerCutTest {

    /**
     * The window is the whole text and it starts at document position 1, which is
     * where the content of a first paragraph begins.
     */
    private fun cut(text: String, windowStart: Int = 1, pending: Int = 1): MarkerCut? {
        val match = findMarkerAtCaret(text, text.length) ?: return null
        return markerCut(match, windowStart, pending)
    }

    @Test
    fun theClosingRunIsCutShortByTheKeystrokeStillArriving() {
        // `**y**`: the document holds `**y*`, so positions 1..5, and the fifth
        // asterisk does not exist yet. Cutting to 6 would take the `y`.
        val cut = cut("**y**")

        assertEquals(1, cut?.openFrom)
        assertEquals(3, cut?.openTo)
        assertEquals(4, cut?.closeFrom)
        assertEquals(5, cut?.closeTo)
    }

    @Test
    fun aSingleCharacterMarkerHasNothingLeftToCutAtTheEnd() {
        // The whole closing run of `` `w` `` is the keystroke, so the only edit in
        // the document is the opening backtick.
        val cut = cut("`w`")

        assertEquals(1, cut?.openFrom)
        assertEquals(2, cut?.openTo)
        assertEquals(cut?.closeFrom, cut?.closeTo)
    }

    @Test
    fun theMarkLandsOnTheWrappedTextAfterBothRunsAreGone() {
        // `say **this**` from position 1: `this` sits at 7..11 now, and at 5..9 once
        // the opening `**` has gone.
        val cut = cut("say **this**")

        assertEquals(5, cut?.openFrom)
        assertEquals(5, cut?.markFrom)
        assertEquals(9, cut?.markTo)
    }

    @Test
    fun aThreeCharacterMarkerCutsTwoOfItsClosingRun() {
        val cut = cut("***b***")

        assertEquals(1, cut?.openFrom)
        assertEquals(4, cut?.openTo)
        assertEquals(5, cut?.closeFrom)
        assertEquals(7, cut?.closeTo)
        assertEquals(1, cut?.markFrom)
        assertEquals(2, cut?.markTo)
    }

    @Test
    fun theWindowStartMovesEveryPosition() {
        val atOne = cut("**y**", windowStart = 1)
        val atForty = cut("**y**", windowStart = 40)

        assertEquals(39, (atForty?.openFrom ?: 0) - (atOne?.openFrom ?: 0))
        assertEquals(39, (atForty?.markTo ?: 0) - (atOne?.markTo ?: 0))
    }

    @Test
    fun everyEditIsInsideTheMatchAndInOrder() {
        listOf("**y**", "*i*", "__u__", "`c`", "***b***", "a **b** and **c**").forEach { text ->
            val match = findMarkerAtCaret(text, text.length)
            val cut = markerCut(match!!, windowStart = 1, pending = 1)!!

            val matchFrom = 1 + match.start
            val matchTo = 1 + match.end - 1

            assertEquals(matchFrom, cut.openFrom, text)
            assertEquals(matchTo, cut.closeTo, text)
            assertEquals(true, cut.openTo <= cut.closeFrom, text)
            assertEquals(true, cut.markTo > cut.markFrom, text)
        }
    }

    @Test
    fun theWrappedTextKeepsItsLength() {
        // The mark has to cover exactly what was between the runs. One character out
        // in either direction is a mark that stops short of a letter or eats the one
        // after it.
        mapOf("**bold**" to 4, "*i*" to 1, "__under__" to 5, "`code`" to 4).forEach { (text, length) ->
            val cut = cut(text)
            assertEquals(length, (cut?.markTo ?: 0) - (cut?.markFrom ?: 0), text)
        }
    }

    @Test
    fun moreArrivingAtOnceThanTheRunHoldsIsRefused() {
        // A paste or an input method delivering the whole pair at once. Nothing here
        // describes where those characters go, so the caller is told to stand back.
        val match = findMarkerAtCaret("**y**", 5)!!

        assertNull(markerCut(match, windowStart = 1, pending = 3))
        assertNull(markerCut(match, windowStart = 1, pending = 0))
    }

    @Test
    fun aRunFinishedByItsWholeSelfIsStillAccepted() {
        // Two characters arriving for a two-character run is the boundary case, not
        // an error: the document holds `**y` and both closing asterisks are new.
        val match = findMarkerAtCaret("**y**", 5)!!
        val cut = markerCut(match, windowStart = 1, pending = 2)

        assertEquals(4, cut?.closeFrom)
        assertEquals(4, cut?.closeTo)
        assertEquals(1, cut?.markFrom)
        assertEquals(2, cut?.markTo)
    }
}

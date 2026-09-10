package io.github.bchmsl.keel.text

import io.github.bchmsl.keel.text.FormattedNode.Bold
import io.github.bchmsl.keel.text.FormattedNode.Code
import io.github.bchmsl.keel.text.FormattedNode.Italic
import io.github.bchmsl.keel.text.FormattedNode.Plain
import io.github.bchmsl.keel.text.FormattedNode.Underline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarkerInputRuleTest {

    /** As the field calls it: the caret sits at the end of what has been typed. */
    private fun typed(text: String) = findMarkerAtCaret(text, text.length)

    // -------------------------------------------------------------- each marker

    @Test
    fun eachMarkerPairBecomesItsOwnMark() {
        assertEquals(Bold(listOf(Plain("x"))), typed("**x**")?.formatted)
        assertEquals(Italic(listOf(Plain("x"))), typed("*x*")?.formatted)
        assertEquals(Underline(listOf(Plain("x"))), typed("__x__")?.formatted)
        assertEquals(Code("x"), typed("`x`")?.formatted)
        assertEquals(Bold(listOf(Italic(listOf(Plain("x"))))), typed("***x***")?.formatted)
    }

    @Test
    fun theMatchCoversTheMarkersAndWhatTheyWrap() {
        // What the caller replaces. Both markers have to go with it, or the asterisks
        // would be left sitting either side of a bold run.
        val match = findMarkerAtCaret("say **this**", caret = 12)

        assertEquals(4, match?.start)
        assertEquals(12, match?.end)
        assertEquals(Bold(listOf(Plain("this"))), match?.formatted)
    }

    @Test
    fun aMarkerPairFiresAfterTextThatIsAlreadyFormatted() {
        // The ordinary case for a second run on one line: whatever was typed after an
        // element that is already there is its own stretch of characters.
        assertEquals(Bold(listOf(Plain("b"))), typed(" and **b**")?.formatted)
    }

    // ------------------------------------------------------- only on completion

    @Test
    fun nothingFiresUntilThePairIsFinished() {
        assertNull(typed("**x"))
        assertNull(typed("**x*"))
        assertNull(typed("*x"))
        assertNull(typed("`x"))
        assertNull(typed("__x_"))
    }

    @Test
    fun theClosingRunHasToMatchTheOpeningOne() {
        // The rule that makes `***both***` typeable. Every intermediate state below is
        // one keystroke on the way there, and firing on any of them would take the
        // text away from under the typist.
        assertNull(typed("***x*"))
        assertNull(typed("***x**"))
        assertEquals(Bold(listOf(Italic(listOf(Plain("x"))))), typed("***x***")?.formatted)
    }

    @Test
    fun aLongerOpeningRunIsNotReadByItsLastFewCharacters() {
        // `***x**` must not come out as bold with a stray asterisk in front, which is
        // what matching the last two characters of the opener would give.
        assertNull(typed("***x**"))
        assertNull(typed("****x**"))
    }

    @Test
    fun aRunNoMarkerSpellsDoesNothing() {
        assertNull(typed("****x****"))
        assertNull(typed("_x_"))
        assertNull(typed("```x```"))
    }

    // ------------------------------------------------------------- ordinary text

    @Test
    fun arithmeticIsNotEmphasis() {
        // The reason a space beside a marker disqualifies it. Someone writing out a
        // sum should not watch it turn italic.
        assertNull(typed("2 * 3 *"))
        assertNull(typed("a ** b **"))
    }

    @Test
    fun aSpaceBeforeTheClosingMarkerDisqualifiesItToo() {
        // The other end of the same rule, and the one that matters while typing: the
        // opener is real text, so only the space in front of the closer says this is a
        // list item or a footnote rather than emphasis.
        assertNull(typed("*x *"))
        assertNull(typed("**x **"))
    }

    @Test
    fun snakeCaseSurvives() {
        // A single underscore spells no marker, so none of these are even considered.
        assertNull(typed("some_name_"))
        assertNull(typed("read_formatted_"))
    }

    @Test
    fun aMarkerPairWrappingNothingDoesNothing() {
        assertNull(typed("****"))
        assertNull(typed("``"))
        assertNull(typed("____"))
    }

    @Test
    fun onlyTheEndOfTheTextIsConsidered() {
        // The caret is the only place a keystroke can have landed, so a finished pair
        // earlier in the line is somebody else's business - it fired when it was
        // typed, or it was pasted and stays as characters.
        assertNull(findMarkerAtCaret("**x** more", caret = 10))
        assertNull(findMarkerAtCaret("**x**", caret = 3))
    }

    @Test
    fun anOutOfRangeCaretIsRefusedRatherThanThrowing() {
        // Called with whatever the browser reports, and a selection can be stale for
        // a frame after the field is rebuilt.
        assertNull(findMarkerAtCaret("**x**", caret = 0))
        assertNull(findMarkerAtCaret("**x**", caret = 99))
        assertNull(findMarkerAtCaret("", caret = 0))
    }

    // ----------------------------------------------------------------- the edges

    @Test
    fun aMarkerPairMidWordStillFires() {
        // Asterisks work inside a word in the format this reads, so they do here.
        assertEquals(Italic(listOf(Plain("b"))), typed("a*b*")?.formatted)
    }

    @Test
    fun aStrayMarkerInsideThePairStaysAsText() {
        // `**a*b**` is bold text that happens to contain an asterisk, which is how
        // the parser reads it back too, so the round trip holds.
        assertEquals(Bold(listOf(Plain("a*b"))), typed("**a*b**")?.formatted)
    }

    @Test
    fun codeKeepsWhateverIsInsideItAsCharacters() {
        // Held as a string rather than parsed, which is the whole point of a code
        // span: it is how somebody writes about the markers themselves.
        assertEquals(Code("**x**"), typed("`**x**`")?.formatted)
    }

    @Test
    fun theNearestMatchingRunWins() {
        // Two candidates, and the pair being finished is the closer one.
        assertEquals(Italic(listOf(Plain("b"))), typed("*a* *b*")?.formatted)
        assertEquals(2, findMarkerAtCaret("a *b* c *d*", caret = 5)?.start)
    }

    @Test
    fun everythingItProducesReadsBackAsItself() {
        // The property that keeps the rule and the parser from drifting: what the rule
        // builds from a pair must be what the parser makes of the same characters.
        listOf("**x**", "*x*", "__x__", "`x`", "***x***", "**a*b**", "`**x**`")
            .forEach { source ->
                assertEquals(
                    parseFormattedText(source),
                    listOf(typed(source)?.formatted),
                    "the rule and the parser disagree about: $source",
                )
            }
    }
}

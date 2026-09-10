package io.github.bchmsl.keel.editor

import io.github.bchmsl.keel.text.FormattedNode.Bold
import io.github.bchmsl.keel.text.FormattedNode.Code
import io.github.bchmsl.keel.text.FormattedNode.Italic
import io.github.bchmsl.keel.text.FormattedNode.Plain
import io.github.bchmsl.keel.text.FormattedNode.Underline
import io.github.bchmsl.keel.text.parseFormattedText
import io.github.bchmsl.keel.text.serializeFormattedNodes
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs keel's own corpus through the bridge and back.
 *
 * The storage format is the contract every consumer already stores against, so the
 * standard here is not "the bridge works" but "the bridge changes nothing": each case
 * is checked against what the parser and serializer do with no bridge in the way, and
 * the list is deliberately the same one `readingThenWritingGivesBackWhatWasRead` uses.
 *
 * This is also the half of the editor keel's CI can check. The schema and the bridge
 * need no DOM and so run on Node; the view does need one, and keel's browser test task
 * is disabled, so `KeelEditor` is verified in the gallery by hand instead.
 */
class FormattedPmBridgeTest {

    /** Parse, cross into ProseMirror's shape, come back, write. */
    private fun throughBridge(source: String): String =
        serializeFormattedNodes(pmJsonToFormatted(formattedToPmJson(parseFormattedText(source))))

    /** What keel does today with no bridge in the way. */
    private fun direct(source: String): String =
        serializeFormattedNodes(parseFormattedText(source))

    @Test
    fun theBridgeChangesNothingTheParserAndSerializerAgreeOn() {
        // Deliberately the same list as `readingThenWritingGivesBackWhatWasRead`, so
        // the bridge is held to the standard the format already meets rather than to
        // one invented for it.
        listOf(
            "",
            "just some text",
            "a **b** c",
            "*soft* and **loud**",
            "__under__",
            "`code()`",
            "***both***",
            "__***all three***__",
            "**a *b* c**",
            "**`x`**",
            "see https://example.com now",
            "**bold across\nno lines**",
            "2 + 2",
            "**not closed",
            "****",
            "line one\nline two",
        ).forEach { source ->
            assertEquals(direct(source), throughBridge(source), "the bridge changed: $source")
        }
    }

    @Test
    fun aMarkAroundCodeSurvives() {
        // The shape that ruled out `excludes: "_"` on the code mark. Pinned on its own
        // because getting it wrong is silent: the text stays, the bold does not.
        assertEquals("**`x`**", throughBridge("**`x`**"))
    }

    @Test
    fun twoAdjacentRunsUnderOneMarkComeBackAsOneRun() {
        // ProseMirror's marks are a flat set per text node, so the trip out loses the
        // fact that these were one run. Coming back naively gives `**a****b**`, which
        // closes the bold in the middle of itself and reparses as neither run.
        val flat = formattedToPmJson(
            listOf(Bold(listOf(Plain("a"))), Bold(listOf(Plain("b")))),
        )

        assertEquals("**ab**", serializeFormattedNodes(pmJsonToFormatted(flat)))
    }

    @Test
    fun nestingOrderComesBackAsTheParserReadsIt() {
        // Underline outermost, italic innermost. Any other order interleaves the
        // markers and the parser sees none of them closed.
        val nested = formattedToPmJson(
            listOf(Underline(listOf(Bold(listOf(Italic(listOf(Plain("x")))))))),
        )

        assertEquals(
            listOf(Underline(listOf(Bold(listOf(Italic(listOf(Plain("x")))))))),
            pmJsonToFormatted(nested),
        )
    }

    @Test
    fun aLineIsAParagraph() {
        val doc = formattedToPmJson(listOf(Plain("one\ntwo")))

        assertEquals(2, doc.content.length as Int)
        assertEquals("one\ntwo", serializeFormattedNodes(pmJsonToFormatted(doc)))
    }

    @Test
    fun anEmptyFieldIsStillAValidDocument() {
        // `doc` is `paragraph+`, so a document with no paragraphs is one the schema
        // refuses to build - which would fail when a field opens on an empty record.
        val doc = formattedToPmJson(emptyList())

        assertEquals(1, doc.content.length as Int)
        assertEquals("", serializeFormattedNodes(pmJsonToFormatted(doc)))
    }

    @Test
    fun codeKeepsItsContentAsCharacters() {
        assertEquals(listOf(Code("**x**")), pmJsonToFormatted(formattedToPmJson(listOf(Code("**x**")))))
    }
}

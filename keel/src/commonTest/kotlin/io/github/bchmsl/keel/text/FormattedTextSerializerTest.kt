package io.github.bchmsl.keel.text

import io.github.bchmsl.keel.text.FormattedNode.Bold
import io.github.bchmsl.keel.text.FormattedNode.Code
import io.github.bchmsl.keel.text.FormattedNode.Italic
import io.github.bchmsl.keel.text.FormattedNode.Link
import io.github.bchmsl.keel.text.FormattedNode.Plain
import io.github.bchmsl.keel.text.FormattedNode.Underline
import kotlin.test.Test
import kotlin.test.assertEquals

class FormattedTextSerializerTest {

    // ------------------------------------------------------------------- leaves

    @Test
    fun nothingAtAllIsTheEmptyString() {
        assertEquals("", serializeFormattedNodes(emptyList()))
    }

    @Test
    fun plainTextComesBackUntouched() {
        assertEquals("just some text", serializeFormattedNodes(listOf(Plain("just some text"))))
    }

    @Test
    fun aLinkIsWrittenAsItsAddress() {
        // The address is the label too, so there is nothing else it could be written
        // as - the parser detects these rather than storing a target and a name.
        assertEquals(
            "see https://example.com now",
            serializeFormattedNodes(
                listOf(Plain("see "), Link("https://example.com"), Plain(" now")),
            ),
        )
    }

    // -------------------------------------------------------------------- marks

    @Test
    fun eachMarkIsWrittenWithItsOwnMarker() {
        assertEquals("**loud**", serializeFormattedNodes(listOf(Bold(listOf(Plain("loud"))))))
        assertEquals("*soft*", serializeFormattedNodes(listOf(Italic(listOf(Plain("soft"))))))
        assertEquals("__u__", serializeFormattedNodes(listOf(Underline(listOf(Plain("u"))))))
        assertEquals("`code()`", serializeFormattedNodes(listOf(Code("code()"))))
    }

    @Test
    fun boldAndItalicOnTheSameWordsIsThreeAsterisks() {
        assertEquals(
            "***both***",
            serializeFormattedNodes(listOf(Bold(listOf(Italic(listOf(Plain("both"))))))),
        )
    }

    @Test
    fun theNestingOrderOfBoldAndItalicDoesNotChangeTheOutput() {
        // The reason the tree is flattened before anything is written. Which of the
        // two buttons was pressed first decides the nesting, and the format has no
        // nesting to record it in, so both have to come out the same way.
        val boldOutside = Bold(listOf(Italic(listOf(Plain("both")))))
        val italicOutside = Italic(listOf(Bold(listOf(Plain("both")))))

        assertEquals(
            serializeFormattedNodes(listOf(boldOutside)),
            serializeFormattedNodes(listOf(italicOutside)),
        )
    }

    @Test
    fun allThreeMarksAtOnceNestInTheOrderTheParserReads() {
        assertEquals(
            "__***all three***__",
            serializeFormattedNodes(
                listOf(Underline(listOf(Bold(listOf(Italic(listOf(Plain("all three")))))))),
            ),
        )
    }

    @Test
    fun aMarkAroundCodeIsKept() {
        assertEquals("**`x`**", serializeFormattedNodes(listOf(Bold(listOf(Code("x"))))))
    }

    // ------------------------------------------------------------------ merging

    @Test
    fun neighbouringRunsUnderOneMarkAreWrittenAsOneRun() {
        // Written node by node this would be `**a**`b`**`, which closes the bold run
        // in the middle of itself and leaves the markers visible.
        assertEquals(
            "**a`b`c**",
            serializeFormattedNodes(
                listOf(Bold(listOf(Plain("a"), Code("b"), Plain("c")))),
            ),
        )
    }

    @Test
    fun twoSeparateBoldRunsStaySeparate() {
        assertEquals(
            "**a** and **b**",
            serializeFormattedNodes(
                listOf(
                    Bold(listOf(Plain("a"))),
                    Plain(" and "),
                    Bold(listOf(Plain("b"))),
                ),
            ),
        )
    }

    // ------------------------------------------------------------------- empties

    @Test
    fun aMarkWrappingNothingIsNotWrittenAtAll() {
        // `****` and `` are four and two ordinary characters to the parser, so writing
        // an empty mark would turn it into punctuation the user never typed.
        assertEquals("", serializeFormattedNodes(listOf(Bold(emptyList()))))
        assertEquals("", serializeFormattedNodes(listOf(Italic(listOf(Plain(""))))))
        assertEquals("", serializeFormattedNodes(listOf(Code(""))))
        assertEquals(
            "ab",
            serializeFormattedNodes(listOf(Plain("a"), Bold(emptyList()), Plain("b"))),
        )
    }

    // --------------------------------------------------------------- round trips

    @Test
    fun readingThenWritingGivesBackWhatWasRead() {
        // The property that matters for a WYSIWYG field: a record opened and closed
        // without being touched must be stored exactly as it was found.
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
        ).forEach { original ->
            assertEquals(
                original,
                serializeFormattedNodes(parseFormattedText(original)),
                "round trip changed: $original",
            )
        }
    }

    @Test
    fun writingIsStableWhenItCannotBeExact() {
        // Where a round trip cannot be exact, it must at least settle: whatever the
        // second pass produces, the third has to agree with it. Without that a record
        // would drift a little further every time it was opened.
        listOf(
            "*a***b**",
            "***a**b*",
            "*a**b***",
            "**a *b***",
            "___x___",
            "*a*__b__",
        ).forEach { original ->
            val once = serializeFormattedNodes(parseFormattedText(original))
            val twice = serializeFormattedNodes(parseFormattedText(once))

            assertEquals(twice, once, "writing did not settle for: $original")
        }
    }

    @Test
    fun anItalicRunTouchingABoldRunIsTheOneArrangementThatDegrades() {
        // Pinned because it is a real limit of the format and not a bug in this file.
        // `*a*` followed by `**b**` is the characters `*a***b**`, and there is no
        // reading of those that gives back two runs. Documented on
        // `serializeFormattedNodes`; a space between them is enough to avoid it.
        val touching = listOf(Italic(listOf(Plain("a"))), Bold(listOf(Plain("b"))))
        val written = serializeFormattedNodes(touching)

        assertEquals("*a***b**", written)
        assertEquals(
            listOf(Plain("*a"), Bold(listOf(Plain("*b")))),
            parseFormattedText(written),
        )

        // Separated by anything at all, both survive.
        val spaced = listOf(Italic(listOf(Plain("a"))), Plain(" "), Bold(listOf(Plain("b"))))
        assertEquals(spaced, parseFormattedText(serializeFormattedNodes(spaced)))
    }
}

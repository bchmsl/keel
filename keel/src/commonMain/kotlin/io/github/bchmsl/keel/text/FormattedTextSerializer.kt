package io.github.bchmsl.keel.text

/**
 * Writes a tree back out as the marker text [parseFormattedText] reads.
 *
 * This is the half a WYSIWYG field needs. The field shows rendered elements and never
 * shows a marker, so something has to turn what the user built back into the stored
 * format - and it must be the exact format the parser reads, or a note would come back
 * different from how it was left.
 *
 * ---------------------------------------------------------------- how it works ----
 *
 * The tree is **flattened into runs before anything is written**, rather than walked
 * and wrapped node by node. That is not a detour, it is what makes the output stable.
 *
 * A tree says `Bold(Italic(x))`; the marker format has no nesting, only a string of
 * characters, and `**` + `*` happens to spell `***`. Writing the nodes as they come
 * would emit whatever order the tree was built in, so the same visible text would come
 * out two ways depending on which button was pressed first - and one of those two
 * re-reads as something else. Flattening asks the only question the format can answer:
 * for this stretch of characters, which marks are on? Then one canonical order is
 * written, and it is the order the parser reads back.
 *
 * The runs are then written through a **stack of open marks**, which is the part that
 * has to be got right. Wrapping each run in its own markers independently looks
 * equivalent and is not: `**a *b* c**` would come out as `**a *****b***** c**`,
 * because the bold run gets closed and reopened around the italic in the middle of it.
 * A mark shared by one run and the next stays open across both, so a mark is written
 * exactly where it starts and ends.
 *
 * ------------------------------------------------------------- what it cannot do ----
 *
 * **The format cannot express an italic run directly touching a bold one.** `*a*`
 * followed by `**b**` is the ten characters `*a***b**`, and there is no reading of
 * those that gives back the two runs - the parser sees a bold run and a stray
 * asterisk. Real markdown has the same hole for the same reason.
 *
 * So that one arrangement degrades on the way through, and deliberately is not worked
 * around: the alternatives are escaping, which changes a stored format both apps and
 * every existing record already share, or inserting a separator, which changes the
 * user's text. Both are worse than a rare arrangement reading back plainly. Anything
 * separated by so much as a space is fine, and so is any nesting.
 */
public fun serializeFormattedNodes(nodes: List<FormattedNode>): String {
    val runs = mutableListOf<MarkedRun>()
    nodes.collectRuns(marks = emptySet(), into = runs)

    return buildString {
        // The marks currently written but not yet closed, outermost first.
        val open = mutableListOf<Mark>()

        // An empty run is dropped rather than written: it would open a mark and close
        // it again around nothing, and `****` is four ordinary asterisks to the
        // parser - so an empty mark would become punctuation the user never typed.
        runs.filter { it.text.isNotEmpty() }.forEach { run ->
            val wanted = Mark.entries.filter { it in run.marks }
            val shared = open.sharedPrefixLength(wanted)

            // Innermost first, or the markers would come out crossed.
            for (i in open.lastIndex downTo shared) append(open[i].marker)
            open.subList(shared, open.size).clear()

            wanted.drop(shared).forEach { mark ->
                append(mark.marker)
                open += mark
            }

            append(run.text)
        }

        for (i in open.lastIndex downTo 0) append(open[i].marker)
    }
}

/** How many marks these two agree on from the outside in. */
private fun List<Mark>.sharedPrefixLength(other: List<Mark>): Int {
    val limit = minOf(size, other.size)
    var shared = 0

    while (shared < limit && this[shared] == other[shared]) shared++

    return shared
}

/**
 * The marks that can sit on the same characters at once, **outermost first**.
 *
 * The order is the contract with the parser rather than a preference. Concatenating
 * the prefixes in this order turns bold-and-italic into `***`, which is exactly the
 * marker [parseFormattedText] reads as the two together; the reverse order would spell
 * the same three characters but every other pairing would come out nested the other
 * way, and a reader that only accepts one of the two would then be lossy.
 */
private enum class Mark(val marker: String) {
    Underline(UNDERLINE_MARKER),
    Bold(BOLD_MARKER),
    Italic(ITALIC_MARKER),
}

/** A stretch of finished text and the marks that apply to the whole of it. */
private data class MarkedRun(val marks: Set<Mark>, val text: String)

/**
 * Walks the tree, carrying the marks that are open, and emits a run per leaf.
 *
 * Code and a link are leaves rather than containers: a link's text is its address, and
 * code holds a string precisely so that markers inside it stay content. Both can still
 * carry marks from above, because `**` around a code span is a thing somebody can
 * write and the parser reads it.
 */
private fun List<FormattedNode>.collectRuns(marks: Set<Mark>, into: MutableList<MarkedRun>) {
    forEach { node ->
        when (node) {
            is FormattedNode.Plain -> into += MarkedRun(marks, node.text)
            is FormattedNode.Link -> into += MarkedRun(marks, node.url)

            is FormattedNode.Code ->
                // An empty code span would be written `` , which reads back as two
                // ordinary characters rather than as anything formatted.
                if (node.text.isNotEmpty()) {
                    into += MarkedRun(marks, CODE_MARKER + node.text + CODE_MARKER)
                }

            is FormattedNode.Bold -> node.children.collectRuns(marks + Mark.Bold, into)
            is FormattedNode.Italic -> node.children.collectRuns(marks + Mark.Italic, into)
            is FormattedNode.Underline -> node.children.collectRuns(marks + Mark.Underline, into)
        }
    }
}

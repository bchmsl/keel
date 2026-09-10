package io.github.bchmsl.keel.editor

import io.github.bchmsl.keel.text.FormattedNode

/**
 * Translates between keel's stored shape and ProseMirror's document JSON.
 *
 * This is the whole of what the engine costs the model. `parseFormattedText` and
 * `serializeFormattedNodes` are untouched and remain the only things that speak
 * `**` and `__`; nothing above this layer knows an editor library is involved, and
 * nothing already stored had to be migrated.
 *
 * The two shapes disagree in one way that matters. keel's marks **nest**
 * (`Underline(Bold(Plain)))`); ProseMirror's are a **flat set per text node**. Going
 * out is therefore easy and going back is not: a naive reverse would emit
 * `Bold(a), Bold(b)` for two adjacent runs, which the serializer writes as
 * `**a****b**` - four asterisks in the middle, and neither run survives a reparse.
 * So [pmJsonToFormatted] rebuilds a tree instead of a list, grouping consecutive runs
 * that share a mark, outermost first in the order the parser reads.
 *
 * Neither direction needs a DOM, so unlike the view this half runs in keel's CI:
 * `prosemirror-model` needs no browser. `FormattedPmBridgeTest` puts the same corpus
 * the serializer's own tests use through both directions.
 */

/**
 * Outermost first, and the order is not a preference: the serializer writes
 * `__***all three***__`, so underline has to be the outer wrapper or the markers
 * interleave and the parser sees none of them closed.
 */
private val ORDER = listOf(UNDERLINE, STRONG, EM)

/** One stretch of text and the marks in force over it. */
private data class Run(val text: String, val marks: Set<String>)

// -------------------------------------------------------------------- keel -> PM ----

/** keel's nodes as a ProseMirror `doc`, one paragraph per line. */
internal fun formattedToPmJson(nodes: List<FormattedNode>): dynamic {
    val runs = mutableListOf<Run>()
    flatten(nodes, emptySet(), runs)

    val paragraphs = splitLines(runs).map { line ->
        val paragraph = js("({ type: 'paragraph' })")
        val content = line.filter { it.text.isNotEmpty() }.map { it.asTextJson() }.toTypedArray()

        // An absent `content` is how ProseMirror spells an empty paragraph; an empty
        // array is rejected by the schema.
        if (content.isNotEmpty()) paragraph.content = content
        paragraph
    }.toTypedArray()

    val doc = js("({ type: 'doc' })")
    // `doc` is `paragraph+`, so a field holding nothing still needs one.
    doc.content = if (paragraphs.isEmpty()) arrayOf(js("({ type: 'paragraph' })")) else paragraphs
    return doc
}

private fun Run.asTextJson(): dynamic {
    val node = js("({ type: 'text' })")
    node.text = text

    if (marks.isNotEmpty()) {
        // Built with a local rather than `also`, because a lambda parameter on a
        // `dynamic` receiver has no type to infer and does not resolve.
        node.marks = ORDER.plus(CODE)
            .filter { it in marks }
            .map { name ->
                val mark = js("({})")
                mark.type = name
                mark
            }
            .toTypedArray()
    }

    return node
}

private fun flatten(nodes: List<FormattedNode>, inherited: Set<String>, out: MutableList<Run>) {
    nodes.forEach { node ->
        when (node) {
            is FormattedNode.Plain -> out += Run(node.text, inherited)
            // The address is the label, so it carries no mark of its own in a schema
            // that has no link mark. The parser detects it again on the way back.
            is FormattedNode.Link -> out += Run(node.url, inherited)
            // Keeps what it was inside. `**`x`**` is a real shape in this format,
            // so the code mark has to be able to sit under another one.
            is FormattedNode.Code -> out += Run(node.text, inherited + CODE)
            is FormattedNode.Bold -> flatten(node.children, inherited + STRONG, out)
            is FormattedNode.Italic -> flatten(node.children, inherited + EM, out)
            is FormattedNode.Underline -> flatten(node.children, inherited + UNDERLINE, out)
        }
    }
}

/** Breaks runs at every `\n`, because a line is a paragraph in [keelSchema]. */
private fun splitLines(runs: List<Run>): List<List<Run>> {
    val lines = mutableListOf(mutableListOf<Run>())

    runs.forEach { run ->
        run.text.split("\n").forEachIndexed { index, part ->
            if (index > 0) lines += mutableListOf<Run>()
            if (part.isNotEmpty()) lines.last() += Run(part, run.marks)
        }
    }

    return lines
}

// -------------------------------------------------------------------- PM -> keel ----

/** A ProseMirror `doc` as keel's nodes, with `\n` back between the paragraphs. */
internal fun pmJsonToFormatted(doc: dynamic): List<FormattedNode> {
    val out = mutableListOf<FormattedNode>()
    val paragraphs = doc.content
    val count = (paragraphs?.length as? Int) ?: 0

    for (index in 0 until count) {
        if (index > 0) out += FormattedNode.Plain("\n")
        out += rebuild(readRuns(paragraphs[index]), applied = emptySet())
    }

    return out
}

private fun readRuns(paragraph: dynamic): List<Run> {
    val content = paragraph?.content
    val count = (content?.length as? Int) ?: 0
    val runs = mutableListOf<Run>()

    for (index in 0 until count) {
        val child = content[index]
        val text = child?.text as? String ?: continue
        val marks = mutableSetOf<String>()
        val markCount = (child.marks?.length as? Int) ?: 0

        for (mark in 0 until markCount) {
            (child.marks[mark]?.type as? String)?.let { marks += it }
        }

        runs += Run(text, marks)
    }

    return runs
}

/**
 * Turns flat runs back into keel's nested marks.
 *
 * Consecutive runs sharing the outermost mark still to be applied become one wrapper,
 * which is what keeps two adjacent bold runs from being written as `**a****b**`.
 */
private fun rebuild(runs: List<Run>, applied: Set<String>): List<FormattedNode> {
    val out = mutableListOf<FormattedNode>()
    var index = 0

    while (index < runs.size) {
        val run = runs[index]

        val mark = ORDER.firstOrNull { it in run.marks && it !in applied }

        if (mark == null) {
            // Innermost: whatever marks are left decide which leaf this is.
            out += if (CODE in run.marks) {
                FormattedNode.Code(run.text)
            } else {
                FormattedNode.Plain(run.text)
            }
            index++
            continue
        }

        var end = index
        while (end < runs.size && mark in runs[end].marks) end++

        val inner = rebuild(runs.subList(index, end), applied + mark)
        out += when (mark) {
            UNDERLINE -> FormattedNode.Underline(inner)
            STRONG -> FormattedNode.Bold(inner)
            else -> FormattedNode.Italic(inner)
        }
        index = end
    }

    return out
}

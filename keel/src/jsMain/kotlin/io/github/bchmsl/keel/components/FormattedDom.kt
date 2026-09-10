package io.github.bchmsl.keel.components

import io.github.bchmsl.keel.text.FormattedNode
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.asList

/*
 * The two directions between a marker string and real elements, written by hand
 * rather than through Compose.
 *
 * This is the one place in keel that builds DOM imperatively, and it has to be.
 * `FormattingField` puts these elements inside a `contenteditable`, where the *browser*
 * owns the tree from then on: typing, Enter, the input method's candidate list and the
 * undo stack all rearrange it. Compose cannot be one of two owners. It would hold a
 * slot table describing a tree that no longer exists, and the next recomposition would
 * either wipe what was typed or throw trying to patch a node that had moved.
 *
 * So Compose builds the wrapper and stops at its edge, and everything inside is these
 * functions' business. `FormattedText` remains the Composable renderer for text that
 * is only being *shown*, where Compose owning the tree is exactly right.
 */

/**
 * Replaces everything inside this element with [nodes], drawn as real elements.
 *
 * Same classes as [FormattedText] emits, so an editor and a finished note read
 * identically - which is the whole point of editing in place.
 *
 * A link is drawn as its own text rather than as an `<a>`. That is deliberate and it
 * matches what a chat composer does: an anchor inside an editable region swallows
 * clicks meant to place the caret, and there is nothing to gain from it while the text
 * is still being written. The address becomes a link when the note is displayed.
 */
internal fun Element.renderFormatted(nodes: List<FormattedNode>) {
    textContent = ""
    appendFormatted(nodes)
}

private fun Element.appendFormatted(nodes: List<FormattedNode>) {
    nodes.forEach { node ->
        when (node) {
            is FormattedNode.Plain -> appendText(node.text)
            is FormattedNode.Link -> appendText(node.url)

            is FormattedNode.Bold -> appendChild(element("strong", node.children))
            is FormattedNode.Italic -> appendChild(element("em", node.children))

            is FormattedNode.Underline -> appendChild(
                element("span", node.children, UNDERLINE_CLASS),
            )

            is FormattedNode.Code -> appendChild(
                document.createElement("code").also {
                    it.setAttribute("class", CODE_CLASS)
                    it.textContent = node.text
                },
            )
        }
    }
}

/**
 * Appends text, turning each newline into a `<br>`.
 *
 * A bare newline in a text node is collapsed to a space by every white-space mode the
 * editor could reasonably use, so the line break has to be an element. `<br>` is also
 * what a browser itself inserts on Enter in an inline editable, which keeps what this
 * builds and what the user then types the same shape.
 */
private fun Element.appendText(text: String) {
    text.split("\n").forEachIndexed { index, line ->
        if (index > 0) appendChild(document.createElement("br"))
        if (line.isNotEmpty()) appendChild(document.createTextNode(line))
    }
}

private fun element(tag: String, children: List<FormattedNode>, className: String? = null) =
    document.createElement(tag).also { created ->
        className?.let { created.setAttribute("class", it) }
        created.appendFormatted(children)
    }

/**
 * Reads a `contenteditable`'s children back into a tree.
 *
 * Deliberately an **allow-list**: the five shapes below are recognised and everything
 * else contributes its text and nothing more. That single rule covers both problems
 * this has to survive.
 *
 * The first is that `execCommand` is not specified tightly enough to promise which
 * element it uses. Bold is `<b>` in one browser and `<strong>` in another, and either
 * may arrive wrapped in a `<span>` carrying styles. All of it reduces to the same five
 * marks or to plain text.
 *
 * The second is paste. A `contenteditable` will happily accept a whole stylesheet's
 * worth of markup from the clipboard, which is markup this library never built and has
 * no reason to trust. `FormattingField` already forces a paste through
 * `insertText`, so it should not arrive - and if a browser ever lets some through
 * anyway, the worst it can become here is its own text. Nothing read here is ever
 * turned back into markup: it becomes a marker string, and drawing that goes through
 * the parser and these builders, which create elements and never parse HTML.
 */
internal fun Node.readFormatted(): List<FormattedNode> = readFormatted(root = this)

/**
 * [root] is carried down the recursion rather than recomputed, because the trailing
 * `<br>` test below is a question about the end of the *editor*, not the end of
 * whichever element the walk happens to be inside.
 */
private fun Node.readFormatted(root: Node): List<FormattedNode> {
    val nodes = mutableListOf<FormattedNode>()
    collectFormatted(into = nodes, root = root)
    return nodes
}

private fun Node.collectFormatted(into: MutableList<FormattedNode>, root: Node) {
    childNodes.asList().forEach { child ->
        when {
            child.nodeType == Node.TEXT_NODE ->
                child.textContent
                    // The editor plants one of these to give an empty code span
                    // something to hold. It is scenery, never content, so it never
                    // reaches storage.
                    ?.replace(ZERO_WIDTH_SPACE, "")
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { into += FormattedNode.Plain(it) }

            child.nodeType != Node.ELEMENT_NODE -> Unit

            else -> into.appendElement(child as Element, root)
        }
    }
}

private fun MutableList<FormattedNode>.appendElement(element: Element, root: Node) {
    val tag = element.tagName.lowercase()

    when {
        tag == "br" ->
            // A browser keeps a trailing `<br>` at the end of an editable so the last
            // line can be reached at all. It is a scaffold rather than a line break
            // the user asked for, and counting it would grow a blank line onto the end
            // of a note every time it was opened and closed.
            if (!element.isTrailingScaffold(root)) this += FormattedNode.Plain("\n")

        tag == "code" ->
            element.textContent
                ?.replace(ZERO_WIDTH_SPACE, "")
                ?.takeIf { it.isNotEmpty() }
                ?.let { this += FormattedNode.Code(it) }

        tag in BOLD_TAGS -> this += FormattedNode.Bold(element.readFormatted(root))
        tag in ITALIC_TAGS -> this += FormattedNode.Italic(element.readFormatted(root))
        element.isUnderline() -> this += FormattedNode.Underline(element.readFormatted(root))

        // Enter in a multi-line editable produces a sibling block per line in most
        // browsers, and a `<br>` in the rest. Both have to read as one newline.
        tag in BLOCK_TAGS -> {
            if (isNotEmpty()) this += FormattedNode.Plain("\n")
            addAll(element.readFormatted(root))
        }

        // Anything left is see-through: a `<span>` holding styles, or an `<a>` a
        // browser made out of a typed address. Its text is the content.
        else -> addAll(element.readFormatted(root))
    }
}

/** Whether this is the `<br>` that sits last in the whole editor. */
private fun Element.isTrailingScaffold(root: Node): Boolean {
    var node: Node = this

    while (node != root) {
        if (node.nextSibling != null) return false
        node = node.parentNode ?: return false
    }

    return true
}

private fun Element.isUnderline(): Boolean =
    tagName.lowercase() == "u" || getAttribute("class")?.contains(UNDERLINE_CLASS) == true

private val BOLD_TAGS = setOf("strong", "b")
private val ITALIC_TAGS = setOf("em", "i")

/** The elements a browser reaches for when Enter starts a new line. */
private val BLOCK_TAGS = setOf("div", "p")

internal const val UNDERLINE_CLASS: String = "formatted__underline"
internal const val CODE_CLASS: String = "formatted__code"

/**
 * What an empty code span is given to hold.
 *
 * A `<code>` element with nothing in it has no place to put the caret, so pressing
 * Code with no selection would appear to do nothing at all. One zero-width character
 * gives the caret somewhere to sit, and every read above strips it, so it cannot reach
 * storage or be counted as text the user typed.
 */
internal const val ZERO_WIDTH_SPACE: String = "\u200B"

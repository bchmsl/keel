package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.copyToClipboard
import io.github.bchmsl.keel.text.FormattedNode
import io.github.bchmsl.keel.text.parseFormattedText
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Code
import org.jetbrains.compose.web.dom.Em
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.TagElement
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement

/**
 * Draws [text] with its inline markers applied and its web addresses clickable.
 *
 * The reading is [parseFormattedText], which is pure and tested; this only turns the
 * result into elements. Nothing here builds markup from the user's text, so there is
 * no escaping to get wrong - a note titled `<script>` is a title, drawn as eight
 * characters.
 *
 * [extraClasses] is a list rather than one string on purpose. Compose HTML's
 * `classes` puts each entry through `DOMTokenList.add`, which throws on a token
 * containing a space and takes the whole subtree's composition with it. A list makes
 * the several-classes case the obvious one to write correctly.
 *
 * ----------------------------------------------------------- copyable code ----
 *
 * A code span is the one piece of a note written to be taken somewhere else - a
 * command, an identifier, a key - so clicking one copies it. [copyableCode] is what
 * turns that off, and there is exactly one reason to: the text is inside something
 * that already handles clicks, such as a row that opens on click. There the row's
 * click is the one the reader means, and keel cannot see it from here - a Compose
 * `onClick` on a plain `div` leaves no attribute to find - so the caller has to say.
 *
 * The default is on, because most text drawn by this is text and nothing more.
 */
@Composable
public fun FormattedText(
    text: String,
    extraClasses: List<String> = emptyList(),
    copyableCode: Boolean = true,
) {
    Span({ classNames("formatted", *extraClasses.toTypedArray()) }) {
        FormattedNodes(parseFormattedText(text), copyableCode)
    }
}

@Composable
private fun FormattedNodes(nodes: List<FormattedNode>, copyableCode: Boolean) {
    nodes.forEach { node ->
        when (node) {
            is FormattedNode.Plain -> Text(node.text)

            is FormattedNode.Link -> A(href = node.url, attrs = {
                classNames("formatted__link")
                // A new tab, because following a link should not throw away whatever
                // was being written. `noopener` is what stops the opened page
                // reaching back through `window.opener`.
                target(ATarget.Blank)
                attr("rel", "noopener noreferrer")
                // A link inside a row sits on top of whatever the row itself does.
                // Without this, following the link would also open the row.
                onClick { event -> event.stopPropagation() }
            }) {
                Text(node.url)
            }

            // `strong` and `em` rather than `b` and `i`: the difference is meaning
            // rather than appearance, and it is what a screen reader announces.
            // Compose HTML has no `Strong`, hence the generic builder.
            is FormattedNode.Bold -> TagElement<HTMLElement>("strong", null) {
                FormattedNodes(node.children, copyableCode)
            }

            is FormattedNode.Italic -> Em { FormattedNodes(node.children, copyableCode) }

            // No element in HTML means "underlined for emphasis"; `u` means a
            // proper-noun or misspelling annotation. A span carrying the style says
            // exactly as much as is true.
            is FormattedNode.Underline -> Span({ classNames("formatted__underline") }) {
                FormattedNodes(node.children, copyableCode)
            }

            is FormattedNode.Code -> CodeSpan(node.text, copyableCode)
        }
    }
}

/**
 * One code span, and the click that takes it away.
 *
 * ------------------------------------------------------------------ the role ----
 *
 * The element stays a `code`. `role="button"` would be the usual way to say a `div`
 * or a `span` is pressable, but a role *replaces* the element's own, so a screen
 * reader would stop saying this is code in order to say it is a button - trading the
 * thing the text is for the thing it also does. `tabindex` and a title say the second
 * part without giving up the first, and Enter and Space do what the click does.
 *
 * ---------------------------------------------------------- the confirmation ----
 *
 * Copied is a state, not a flash: it stays until the reader has moved on, which is
 * the pointer leaving or the focus going elsewhere. Deliberately not a timer. keel
 * has no timers in it - see `ToastHost` for the reasoning - and a duration here would
 * be the library guessing at something it cannot know, while under
 * `prefers-reduced-motion` an animated flash collapses to nothing and leaves a reader
 * who asked for less motion with no confirmation at all.
 *
 * The state is keyed on the text, so editing a note does not leave a stale tick on
 * whatever now occupies that position.
 */
@Composable
private fun CodeSpan(text: String, copyable: Boolean) {
    if (!copyable) {
        Code({ classNames(CODE_CLASS) }) { Text(text) }
        return
    }

    var copied by remember(text) { mutableStateOf(false) }

    Code({
        classNames(CODE_CLASS, COPYABLE_CLASS, COPIED_CLASS.takeIf { copied })
        attr("title", if (copied) "Copied" else "Click to copy")
        attr("tabindex", "0")

        onClick { event ->
            // Nothing else gets this click. A caller that wanted the click for its
            // own row passes `copyableCode = false` and never reaches here.
            event.stopPropagation()
            copyToClipboard(text) { copied = true }
        }

        onKeyDown { event ->
            if (event.key != "Enter" && event.key != " ") return@onKeyDown

            // Space would scroll the page and Enter can submit a form around this.
            event.preventDefault()
            event.stopPropagation()
            copyToClipboard(text) { copied = true }
        }

        onMouseLeave { copied = false }
        onBlur { copied = false }
    }) {
        Text(text)
    }
}

private const val CODE_CLASS = "formatted__code"
private const val COPYABLE_CLASS = "formatted__code--copyable"
private const val COPIED_CLASS = "formatted__code--copied"

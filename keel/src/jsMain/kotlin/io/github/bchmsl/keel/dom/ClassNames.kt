package io.github.bchmsl.keel.dom

import org.jetbrains.compose.web.attributes.AttrsScope
import org.w3c.dom.Element

/**
 * Sets a class list, dropping the entries that are absent.
 *
 * Compose HTML's own `classes(...)` puts every entry through `DOMTokenList.add`,
 * which throws rather than ignoring two inputs that look harmless:
 *
 *   - an empty or blank token, from the `if (flag) "x" else ""` shape. Raises
 *     `SyntaxError: The token provided must not be empty`.
 *   - a token containing a space, from building a class string by hand. Raises
 *     `InvalidCharacterError`.
 *
 * Either one aborts the composition of that whole subtree. Nothing is logged as an
 * error in the usual place and nothing renders, so the symptom is a row that
 * silently fails to appear while its data is perfectly correct - which is a long
 * way from the cause. Both consuming apps hit this independently and each grew its
 * own partial guard; this is the one that handles both cases.
 *
 * Blank entries are dropped and multi-word entries are split, so every caller shape
 * is legal:
 *
 *     classNames("row", "row--done".takeIf { done })
 *     classNames("row", if (done) "row--done" else null)
 *     classNames(*sized("card", expanded))
 */
public fun <T : Element> AttrsScope<T>.classNames(vararg names: String?) {
    val tokens = names.asSequence()
        .filterNotNull()
        .flatMap { it.trim().split(WHITESPACE) }
        .filter { it.isNotEmpty() }
        .toList()

    if (tokens.isNotEmpty()) classes(*tokens.toTypedArray())
}

private val WHITESPACE = Regex("\\s+")

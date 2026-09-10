package io.github.bchmsl.keel.dom

import kotlinx.browser.window

/**
 * Writes [text] to the system clipboard, calling [onCopied] only if that worked.
 *
 * The async clipboard API can decline: it needs a secure context, and some browsers
 * add a permission on top. Both look the same from here - a promise that rejects -
 * and neither is worth a message of its own, because a reader who is holding a mouse
 * can select the text instead.
 *
 * What matters is that [onCopied] runs *after* the write resolves rather than beside
 * it, so nothing ever says "copied" when nothing was.
 */
internal fun copyToClipboard(text: String, onCopied: () -> Unit) {
    val clipboard = window.navigator.asDynamic().clipboard

    // An insecure context has no clipboard at all, and reading `writeText` off
    // `undefined` throws rather than declining.
    if (clipboard == null || clipboard == undefined) return

    clipboard.writeText(text).then({ onCopied() }, { /* Declined. Say nothing. */ })
}

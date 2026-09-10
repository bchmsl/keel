@file:JsModule("prosemirror-commands")

package io.github.bchmsl.keel.editor

/**
 * The bindings any editor needs: Enter, Backspace, Delete and the rest.
 *
 * Bound last, so every one of keel's own bindings gets the key first.
 */
internal external val baseKeymap: dynamic

/**
 * Turns a mark on across the selection, or off if the whole selection has it.
 *
 * What the toolbar buttons and Ctrl/Cmd+B, I, U and E all run. Handles the case that
 * has no replacement outside `execCommand` and was the argument for keeping it: a
 * selection that only partly overlaps an existing run of the mark.
 */
internal external fun toggleMark(type: dynamic, attrs: dynamic = definedExternally): dynamic

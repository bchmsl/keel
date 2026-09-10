@file:JsModule("prosemirror-state")

package io.github.bchmsl.keel.editor

/**
 * The editor's state: a document, a selection, and the plugins over both.
 *
 * A state is immutable and a transaction produces the next one, which is the property
 * the whole design leans on. Undo is a list of transactions rather than a guess at
 * what the browser did to the DOM, and that is the difference between this and
 * `execCommand`, whose undo entry disappears the moment a script edits the document.
 *
 * See [Schema] for why these are `internal` and `dynamic`.
 */
internal external object EditorState {
    fun create(config: dynamic): dynamic
}

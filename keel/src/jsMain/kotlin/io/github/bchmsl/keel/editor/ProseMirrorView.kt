@file:JsModule("prosemirror-view")

package io.github.bchmsl.keel.editor

import org.w3c.dom.HTMLElement

/**
 * The editable element and the state it draws.
 *
 * The one module here that needs a DOM. The schema, the document and the bridge all
 * run on Node, which is what lets the model half of this component be tested in
 * keel's CI while the view half stays browser-only.
 *
 * [place] is `dynamic` rather than an element on purpose. Handed a node, ProseMirror
 * *appends* its own editable inside it; handed `{ mount: node }` it takes that node
 * over instead. keel passes the second, so the element the field styles and the
 * element the user types into are the same one and `.formatting-editor` keeps
 * applying - its border, its focus glow, its placeholder rule and its `white-space`
 * were all written for an editable and still describe one.
 *
 * See [Schema] for why these are `internal` and `dynamic`.
 */
internal external class EditorView(place: dynamic, props: dynamic) {
    val state: dynamic

    /** The editable element, which with `mount` is the one that was handed in. */
    val dom: HTMLElement

    fun updateState(state: dynamic)
    fun focus()
    fun hasFocus(): Boolean

    /** Puts a transaction through `dispatchTransaction`, which is what applies it. */
    fun dispatch(tr: dynamic)

    /** Removes the DOM, the listeners and the plugin state together. */
    fun destroy()
}

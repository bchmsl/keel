@file:JsModule("prosemirror-history")

package io.github.bchmsl.keel.editor

/**
 * Undo and redo over the editor's own transactions.
 *
 * The reason keel took the dependency. The field this replaced formatted through
 * `execCommand`, and a script-driven DOM edit clears the browser's native undo stack
 * outright: after a marker pair fired, Ctrl+Z did nothing at all, however many times
 * it was pressed. There is no version of that bug that can be fixed from the outside,
 * because the stack that was lost belonged to the browser.
 *
 * Here a rule firing is one entry like any other, so one undo gives the markers back
 * and a second carries on through the typing before them.
 */
internal external fun history(config: dynamic = definedExternally): dynamic

internal external val undo: dynamic

internal external val redo: dynamic

/**
 * Makes a transaction start its own undo entry instead of joining the one before it.
 *
 * Needed for exactly one transaction: the marker rule's. Entries are grouped by time
 * and adjacency, so without this a pair finished in the middle of fast typing lands in
 * the same entry as the words around it, and the first Ctrl+Z throws all of them away
 * rather than handing the markers back. Measured while this was built: without it the
 * markers only came back if the typist had paused for half a second first, which is
 * not a rule anyone can be expected to know.
 */
internal external fun closeHistory(tr: dynamic): dynamic

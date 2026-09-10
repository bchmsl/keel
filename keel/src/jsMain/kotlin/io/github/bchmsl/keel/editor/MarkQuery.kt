package io.github.bchmsl.keel.editor

/**
 * The marks the next character typed at [pos] would be born with.
 *
 * Two sources, in order. A keystroke or a button can leave a mark *pending* without
 * changing the document - pressing Bold with nothing selected does exactly that - and
 * a pending set, once there is one, is the whole answer. With nothing pending the
 * position itself is asked, and it answers with the marks of the text beside it,
 * excluding any that are `inclusive: false` and end there. That exclusion is why a
 * caret immediately after `` `x` `` is not in code, and it is the schema's doing
 * rather than this function's.
 *
 * Both callers need the same answer for different reasons: the toolbar draws its
 * pressed state from it, and the marker rule refuses to fire when the answer contains
 * `code`.
 */
internal fun marksAt(state: dynamic, pos: Int): dynamic {
    val stored = state.storedMarks
    return if (stored != null) stored else state.doc.resolve(pos).marks()
}

/** Whether [type] is one of [marksAt]. */
internal fun hasMarkAt(state: dynamic, pos: Int, type: dynamic): Boolean =
    type.isInSet(marksAt(state, pos)) != null

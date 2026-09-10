package io.github.bchmsl.keel.editor

import io.github.bchmsl.keel.text.FormattingMarker

/**
 * Everything the field's editor does, as the engine's plugin list.
 *
 * Order is precedence: the first plugin that claims a key or a piece of typed text
 * wins, and everything below it never sees the event. So the reading is top to bottom.
 *
 * 1. **The marker rule**, which has to see typed text before anything inserts it.
 * 2. **History**, which needs to be installed before the commands that drive it.
 * 3. **keel's keys**, which override two of the engine's defaults deliberately -
 *    Backspace and, in a single-line field, Enter.
 * 4. **The engine's base keys**, for everything not named above: Enter splitting a
 *    paragraph in a multi-line field, Backspace joining one, arrow keys, and the
 *    handful of platform bindings that make selection behave like the rest of the OS.
 *
 * There is no `dropCursor` or `gapCursor` here, and no placeholder plugin. The field
 * has one block type and cannot have a gap between two of them, and the placeholder is
 * a CSS rule on `data-empty` that predates this and still works.
 */
internal fun keelPlugins(schema: Schema, onEnter: (() -> Unit)?): Array<dynamic> {
    val rules = js("({})")
    rules.rules = arrayOf(markerInputRule(schema))

    return arrayOf(
        inputRules(rules),
        history(),
        keymap(keelKeys(schema, onEnter)),
        keymap(baseKeymap),
    )
}

/**
 * The bindings keel adds or takes over.
 *
 * `Mod-` is the engine's way of writing "Cmd on a Mac, Ctrl everywhere else", which is
 * the one piece of platform handling worth not writing by hand.
 *
 * Bold, italic and underline were left to the browser by the field this replaces, on
 * the argument that the native handler already did them correctly. That argument does
 * not survive the move: the browser's own bold writes whatever markup it likes into a
 * document the engine believes it owns, and the two then disagree about what is there.
 * All four are bound here, and Ctrl/Cmd+E for code keeps the letter it had.
 */
private fun keelKeys(schema: Schema, onEnter: (() -> Unit)?): dynamic {
    val keys = js("({})")

    keys["Mod-b"] = toggleMark(schema.marks[STRONG])
    keys["Mod-i"] = toggleMark(schema.marks[EM])
    keys["Mod-u"] = toggleMark(schema.marks[UNDERLINE])
    keys["Mod-e"] = toggleMark(schema.marks[CODE])

    keys["Mod-z"] = undo
    keys["Shift-Mod-z"] = redo
    // What Windows and Linux users reach for; harmless on a Mac, where it is free.
    keys["Mod-y"] = redo

    // Straight after a pair fires, this puts the markers back rather than deleting a
    // character - the correction a typist makes when the field guessed wrong. At any
    // other time the command declines and Backspace falls through to the engine's own.
    keys["Backspace"] = undoInputRule

    // A single-line field commits on Enter by blurring, exactly as the `input` it
    // replaced did. Left unbound in a multi-line field, where the engine's Enter
    // splits the paragraph and the serializer writes that out as a newline.
    if (onEnter != null) {
        keys["Enter"] = { _: dynamic, _: dynamic ->
            onEnter()
            // Claims the key, so the engine never inserts the break.
            true
        }
    }

    return keys
}

/**
 * The schema mark one toolbar button stands for.
 *
 * keel's four markers and the schema's four marks are the same four things named
 * twice, and this is the only place that says so. `FormattingMarker` is public and
 * stays the toolbar's vocabulary; the mark names never leave this module.
 */
internal val FormattingMarker.markName: String
    get() = when (this) {
        FormattingMarker.Bold -> STRONG
        FormattingMarker.Italic -> EM
        FormattingMarker.Underline -> UNDERLINE
        FormattingMarker.Code -> CODE
    }

@file:JsModule("prosemirror-keymap")

package io.github.bchmsl.keel.editor

/**
 * Binds key names to commands.
 *
 * `Mod-` is the platform's accelerator, so one binding covers Cmd on a Mac and Ctrl
 * everywhere else. Order matters between several of these plugins: the first to
 * handle a key wins, which is how Backspace reaches `undoInputRule` before
 * [baseKeymap] gets to treat it as an ordinary deletion.
 */
internal external fun keymap(bindings: dynamic): dynamic

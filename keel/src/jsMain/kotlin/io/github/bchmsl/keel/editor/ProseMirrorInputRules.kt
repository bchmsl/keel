@file:JsModule("prosemirror-inputrules")

package io.github.bchmsl.keel.editor

/**
 * A pattern matched against the text behind the caret, and what to do about it.
 *
 * The handler is called with the state, the match, and the start and end of the match
 * **in the document**, and the character that fired the rule is not in the document
 * yet. Returning a transaction suppresses the ordinary insertion, so the handler owns
 * the result completely. `markerCut` is where that is worked out and tested.
 */
internal external class InputRule(match: dynamic, handler: dynamic)

/** The plugin that runs a set of [InputRule]s. */
internal external fun inputRules(config: dynamic): dynamic

/**
 * Puts the characters a rule consumed back, undoing only that rule.
 *
 * Bound to Backspace, which is what makes a rule that fired by mistake feel like a
 * mistake the typist can take back rather than a field fighting them. Pressing it
 * straight after `**bold**` turns bold gives the four asterisks back; pressing it at
 * any other time deletes a character as usual, because the command declines and the
 * key falls through.
 */
internal external val undoInputRule: dynamic

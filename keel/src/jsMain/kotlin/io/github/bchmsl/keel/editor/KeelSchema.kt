package io.github.bchmsl.keel.editor

/**
 * The mark names, in one place because three files have to agree on them: this schema
 * declares them, the bridge maps keel's nodes onto them, and the input rule looks one
 * up to apply it.
 */
internal const val STRONG: String = "strong"
internal const val EM: String = "em"
internal const val UNDERLINE: String = "underline"
internal const val CODE: String = "code"

/**
 * keel's storage format expressed as a ProseMirror schema.
 *
 * Four marks and one block, which is the whole of what the format can say. Nothing
 * here is a general-purpose editor's schema: there are no lists, no headings and no
 * tables, because `parseFormattedText` cannot write any of them down and a field
 * whose contents cannot be stored is worse than one that refuses them.
 *
 * Choices worth noting, because they are where the engine's semantics and keel's
 * happen to agree - or where they do not, and this file is the seam:
 *
 * - **A line is a paragraph.** Marks cannot span a paragraph boundary in ProseMirror,
 *   and bold, italic and underline cannot span `\n` in keel's parser, so the two
 *   agree for free. It also closes the mismatch the spec records as H2: code *can*
 *   span a newline in keel's parser but cannot here, so the reopened record would
 *   stop disagreeing with the field.
 * - **`code` is `code: true` but excludes nothing.** `excludes: "_"` is the obvious
 *   setting and it is wrong here: keel's format allows a mark *around* a code span -
 *   `` **`x`** `` - and `aMarkAroundCodeIsKept` pins it, so a code mark that drops
 *   every other mark cannot express the format. The cost is that `code: true` alone
 *   does not stop an input rule firing inside a span, because
 *   `prosemirror-inputrules` reads `spec.code` on the *node* and not on the mark. So
 *   the rule in `MarkerRule.kt` checks `rangeHasMark` itself, which is the one guard
 *   the engine does not hand over.
 *
 * - **Every mark is `inclusive: false`.** The default is the other way, and it makes
 *   the field behave unlike a chat composer: after `**x**` fires, the caret sits at
 *   the end of the bold run and the *next* character joins it, so a typist who meant
 *   one bold word gets a bold sentence. `removeStoredMark` in the rule is not enough,
 *   because the mark is re-derived from the position rather than from stored marks.
 *   This is also what the shipping field does, by putting its caret in a zero-width
 *   text node outside the element.
 *
 * `u` rather than a class-carrying span, matching the fix on the shipping branch: a
 * bare `<u>` is the one shape the browser's own underline command can remove again.
 */
internal fun keelSchema(): Schema = Schema(
    js(
        """({
          nodes: {
            doc: { content: "paragraph+" },
            paragraph: {
              content: "inline*",
              group: "block",
              parseDOM: [{ tag: "p" }, { tag: "div" }],
              toDOM: function () { return ["p", 0]; }
            },
            text: { group: "inline" }
          },
          marks: {
            strong: {
              inclusive: false,
              parseDOM: [{ tag: "strong" }, { tag: "b" }],
              toDOM: function () { return ["strong", 0]; }
            },
            em: {
              inclusive: false,
              parseDOM: [{ tag: "em" }, { tag: "i" }],
              toDOM: function () { return ["em", 0]; }
            },
            underline: {
              inclusive: false,
              parseDOM: [{ tag: "u" }],
              toDOM: function () { return ["u", 0]; }
            },
            code: {
              code: true,
              inclusive: false,
              parseDOM: [{ tag: "code" }],
              toDOM: function () { return ["code", { class: "formatted__code" }, 0]; }
            }
          }
        })""",
    ),
)

package io.github.bchmsl.keel.editor

import io.github.bchmsl.keel.components.FormattingCommands
import io.github.bchmsl.keel.text.FormattingMarker
import io.github.bchmsl.keel.text.parseFormattedText
import io.github.bchmsl.keel.text.serializeFormattedNodes
import org.w3c.dom.HTMLElement

/**
 * One live editor over one element: marker text in, marker text out.
 *
 * The whole of the engine's surface is behind this class. Above it, `FormattingField`
 * deals in strings, a set of [FormattingMarker]s and a lifecycle; nothing in
 * `components` names a ProseMirror type or imports one, which is what keeps the
 * dependency an implementation detail of a single component instead of part of keel's
 * shape. That is not only tidiness: it is the property that lets Kotlin's dead-code
 * pass drop all seven npm packages out of a consumer that never opens one of these,
 * measured at zero bytes.
 *
 * -------------------------------------------------------------- what it reports ----
 *
 * [onChanged] fires for every transaction, and the field uses it for the two things
 * that have to track the document exactly: the placeholder's emptiness flag and the
 * toolbar's pressed state. A transaction covers a caret move as well as an edit, so
 * this also replaces the `selectionchange` listener the previous field had to put on
 * the *document* - the one listener there that could outlive its element.
 *
 * Committing is separate and deliberately rarer. See [FormattingCommands] and
 * `FormattingField` for why the field is uncontrolled.
 */
internal class KeelEditor(
    element: HTMLElement,
    initial: String,
    multiline: Boolean,
    private val onCommit: (String) -> Unit,
    private val onChanged: () -> Unit,
) : FormattingCommands {

    private val schema = keelSchema()

    private val view: EditorView

    init {
        val config = js("({})")
        config.doc = schema.nodeFromJSON(formattedToPmJson(parseFormattedText(initial)))
        config.plugins = keelPlugins(
            schema = schema,
            // A single-line field commits on Enter by blurring, which is what the
            // `input` this component replaced did. A multi-line one lets the key
            // through to the engine and gains a paragraph.
            onEnter = if (multiline) null else ({ blur() }),
        )

        val events = js("({})")

        // Both return false rather than true: the field wants to know about the event,
        // not to take it over, and the engine has its own work to do on either.
        events.blur = { _: dynamic, _: dynamic ->
            commit()
            // A blur is not a transaction, so nothing else would tell the field the
            // caret has gone and its toolbar should go quiet.
            onChanged()
            false
        }

        events.focus = { _: dynamic, _: dynamic ->
            onChanged()
            false
        }

        val props = js("({})")
        props.state = EditorState.create(config)
        props.handleDOMEvents = events
        props.dispatchTransaction = { transaction: dynamic ->
            view.updateState(view.state.apply(transaction))
            onChanged()
        }

        // `mount`, so the editable is the element the field already styled rather than
        // a new one inside it. See [EditorView].
        val place = js("({})")
        place.mount = element

        view = EditorView(place, props)
    }

    /** The document as the marker text every layer above this one speaks. */
    fun read(): String = serializeFormattedNodes(pmJsonToFormatted(view.state.doc.toJSON()))

    fun commit() {
        onCommit(read())
    }

    /**
     * Whether the field has nothing in it, for the placeholder rule.
     *
     * An empty document is one empty paragraph rather than nothing, so this asks about
     * the text and not about the node count.
     */
    fun isEmpty(): Boolean = (view.state.doc.textContent as? String).isNullOrEmpty()

    fun hasFocus(): Boolean = view.hasFocus()

    fun blur() {
        view.dom.blur()
    }

    fun destroy() {
        view.destroy()
    }

    override fun toggle(marker: FormattingMarker) {
        // The toolbar suppresses the blur a mouse press would otherwise cause, so focus
        // is usually still in the field - but not when it has never been touched and
        // somebody presses Bold first, and a field nobody can see the caret in reads as
        // a button that did nothing.
        view.focus()

        val command = toggleMark(schema.marks[marker.markName])
        command(view.state, { transaction: dynamic -> view.dispatch(transaction) })

        // A formatting button changes the text without blurring the field, and closing
        // a dialog straight after pressing Bold should not lose what it did.
        commit()
    }

    override fun active(): Set<FormattingMarker> =
        FormattingMarker.entries.filterTo(mutableSetOf()) { it.isInForce() }

    /**
     * Whether [this] applies to what would be typed next.
     *
     * Two questions rather than one, because a caret and a selection mean different
     * things. With a selection, a mark counts as on when it covers the whole of it, so
     * pressing Bold over a partly bold phrase bolds the rest instead of clearing it.
     * With a bare caret there is no range to ask about, and the answer is the marks the
     * next character would be born with - the ones a previous keystroke left pending,
     * or failing that whatever the position itself carries.
     */
    private fun FormattingMarker.isInForce(): Boolean {
        val state = view.state
        val selection = state.selection
        val type = schema.marks[markName]

        if (selection.empty != true) {
            return state.doc.rangeHasMark(selection.from, selection.to, type) == true
        }

        return hasMarkAt(state, selection.from as Int, type)
    }
}

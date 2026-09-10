package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.toolbarButtonClasses
import io.github.bchmsl.keel.dom.toolbarClasses
import io.github.bchmsl.keel.icons.Icon
import io.github.bchmsl.keel.icons.LucideIcon
import io.github.bchmsl.keel.text.FormattingMarker
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div

/**
 * A text field the toolbar can format by rewriting its text.
 *
 * `input` and `textarea` carry the same four members this needs - a value, a
 * selection, focus, and a way to put the selection back - and share no Kotlin type
 * that declares any of them. Rather than two near-identical wrappers, the element is
 * held untyped and the four members are named here, in one place, where a typo shows
 * up as a formatting button that does nothing rather than anywhere else.
 */
public class FormattingTarget(private val element: dynamic) {

    public var value: String
        get() = element.value as String
        set(newValue) {
            element.value = newValue
        }

    /** Null when the field has never been focused, in which case there is no caret. */
    public val selectionStart: Int? get() = element.selectionStart as? Int
    public val selectionEnd: Int? get() = element.selectionEnd as? Int

    public fun focus() {
        element.focus()
    }

    public fun select(start: Int, end: Int) {
        element.setSelectionRange(start, end)
    }
}

/**
 * The four formatting buttons that sit under a text field.
 *
 * [commands] hands back the field's command surface rather than its text, because
 * every one of these needs the selection and the selection lives on the element.
 * It is a lambda because the element does not exist yet when this is composed.
 *
 * [active] is read during *this* composable's composition rather than passed as a
 * value, so a moving caret invalidates the toolbar and nothing else. Passing the set
 * in would invalidate the caller, which owns the editable element - and re-running
 * that scope on every arrow key is exactly the pressure the whole design avoids.
 */
@Composable
public fun FormattingToolbar(
    commands: () -> FormattingCommands?,
    active: () -> Set<FormattingMarker> = { emptySet() },
) {
    val pressed = active()

    Div({
        classNames(toolbarClasses())
        // Without this the field blurs the moment a button is pressed. A field that
        // saves on blur would then save the text as it was *before* the button did
        // anything, and be overwritten by it.
        onMouseDown { event -> event.preventDefault() }
    }) {
        FormattingMarker.entries.forEach { marker ->
            val on = marker in pressed

            Button({
                classNames(toolbarButtonClasses())
                type(ButtonType.Button)
                attr("title", marker.label)
                attr("aria-label", marker.label)
                // The pressed look is keyed off this rather than a modifier class, for
                // the reason `switchClasses` gives: the attribute is what a screen
                // reader announces, so the two cannot drift apart.
                attr("aria-pressed", on.toString())
                onClick { commands()?.toggle(marker) }
            }) {
                Icon(marker.icon, size = TOOLBAR_ICON_SIZE)
            }
        }
    }
}

/**
 * The toolbar over a field whose text holds the markers.
 *
 * Kept for a caller pairing the toolbar with an ordinary `textarea`. [FormattingField]
 * no longer takes this route - it shows the formatting instead of the markers - but
 * the behaviour is still correct for a field that does not, and `applyFormatting` is
 * still the tested description of it.
 */
@Composable
public fun FormattingToolbar(target: () -> FormattingTarget?, onTextChange: (String) -> Unit) {
    FormattingToolbar(
        commands = { target()?.let { MarkerSplicingCommands(it, onTextChange) } },
    )
}

/** The picture for each button, in the order the toolbar shows them. */
private val FormattingMarker.icon: LucideIcon
    get() = when (this) {
        FormattingMarker.Bold -> LucideIcon.Bold
        FormattingMarker.Italic -> LucideIcon.Italic
        FormattingMarker.Underline -> LucideIcon.Underline
        FormattingMarker.Code -> LucideIcon.Code
    }

private const val TOOLBAR_ICON_SIZE = 14

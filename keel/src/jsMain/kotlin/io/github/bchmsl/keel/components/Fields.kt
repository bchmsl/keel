package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.max
import org.jetbrains.compose.web.attributes.min
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.rows
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.TextArea

/**
 * An on/off control.
 *
 * A button with `role="switch"` rather than a checkbox. The visual is a 36x20 track
 * with a sliding 16px knob, which no native control offers - and building it from a
 * checkbox would mean hiding the checkbox and reimplementing its semantics anyway.
 *
 * `aria-checked` does two jobs, and that is the point: it tells assistive technology
 * what the visual tells everyone else, and it is also the selector the "on" colour
 * is keyed off. The two therefore cannot disagree, which is the failure a separate
 * `--on` class would eventually produce.
 */
@Composable
public fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, ariaLabel: String) {
    Button({
        classNames("switch")
        type(ButtonType.Button)
        attr("role", "switch")
        attr("aria-checked", checked.toString())
        attr("aria-label", ariaLabel)
        onClick { onCheckedChange(!checked) }
    }) {
        Span({ classNames("switch__knob") })
    }
}

/**
 * A value slider.
 *
 * A native range input with its appearance replaced, so keyboard stepping, touch
 * dragging and assistive-technology support come for free rather than being
 * reimplemented badly.
 *
 * Fires on every movement rather than on release, so a number shown beside it tracks
 * the thumb instead of jumping when the finger lifts.
 */
@Composable
public fun Slider(
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    ariaLabel: String,
) {
    Input(InputType.Range) {
        classNames("slider")
        min(min.toString())
        max(max.toString())
        value(value)
        attr("aria-label", ariaLabel)
        // A range input carries a Number rather than the String a text input would,
        // so there is nothing to parse. It is nullable only because the event type is
        // shared with inputs that can genuinely be empty; a range always has a value,
        // so ignoring the null needs no fallback guess.
        onInput { event -> event.value?.let { onValueChange(it.toInt()) } }
    }
}

/** A single-line text field. */
@Composable
public fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    ariaLabel: String? = null,
    type: InputType<String> = InputType.Text,
) {
    Input(type) {
        classNames("input")
        value(value)
        placeholder?.let { placeholder(it) }
        ariaLabel?.let { attr("aria-label", it) }
        onInput { event -> onValueChange(event.value) }
    }
}

/** A multi-line text field, vertically resizable. */
@Composable
public fun TextAreaField(
    value: String,
    onValueChange: (String) -> Unit,
    rows: Int = DEFAULT_TEXTAREA_ROWS,
    placeholder: String? = null,
    ariaLabel: String? = null,
) {
    TextArea(value = value) {
        classNames("textarea")
        rows(rows)
        placeholder?.let { placeholder(it) }
        ariaLabel?.let { attr("aria-label", it) }
        onInput { event -> onValueChange(event.value) }
    }
}

private const val DEFAULT_TEXTAREA_ROWS = 6

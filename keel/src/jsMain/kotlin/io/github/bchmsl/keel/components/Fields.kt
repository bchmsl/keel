package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.inputClasses
import io.github.bchmsl.keel.dom.sliderClasses
import io.github.bchmsl.keel.dom.switchClasses
import io.github.bchmsl.keel.dom.switchKnobClasses
import io.github.bchmsl.keel.dom.textAreaClasses
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.builders.InputAttrsScope
import org.jetbrains.compose.web.attributes.builders.TextAreaAttrsScope
import org.jetbrains.compose.web.attributes.max
import org.jetbrains.compose.web.attributes.min
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.rows
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.HTMLButtonElement

/** The track sizes. */
public enum class SwitchSize(internal val className: String?) {
    /** A switch standing on its own, at the end of a settings row. */
    Default(null),

    /**
     * A switch sitting inside another control - a labelled pill in a player's
     * control bar, a toggle in a toolbar - where the default track is taller than
     * the thing containing it.
     *
     * Geometry only. It is the same control at a different size, so the colours and
     * the `aria-checked` state stay one definition; see `.switch--size-sm`.
     */
    Small("switch--size-sm"),
}

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
 *
 * [attrs] runs last, after everything above, so a caller can override what it needs
 * to. See [Button] for the reasoning.
 */
@Composable
public fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    ariaLabel: String,
    size: SwitchSize = SwitchSize.Default,
    attrs: (AttrsScope<HTMLButtonElement>.() -> Unit)? = null,
) {
    Button({
        classNames(switchClasses(size))
        type(ButtonType.Button)
        attr("role", "switch")
        attr("aria-checked", checked.toString())
        attr("aria-label", ariaLabel)
        onClick { onCheckedChange(!checked) }
        attrs?.invoke(this)
    }) {
        Span({ classNames(switchKnobClasses()) })
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
 *
 * [attrs] runs last; see [Switch].
 */
@Composable
public fun Slider(
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    ariaLabel: String,
    attrs: (InputAttrsScope<Number?>.() -> Unit)? = null,
) {
    Input(InputType.Range) {
        classNames(sliderClasses())
        min(min.toString())
        max(max.toString())
        value(value)
        attr("aria-label", ariaLabel)
        // A range input carries a Number rather than the String a text input would,
        // so there is nothing to parse. It is nullable only because the event type is
        // shared with inputs that can genuinely be empty; a range always has a value,
        // so ignoring the null needs no fallback guess.
        onInput { event -> event.value?.let { onValueChange(it.toInt()) } }
        attrs?.invoke(this)
    }
}

/**
 * A single-line text field.
 *
 * [attrs] runs last; see [Switch]. It is the slot for the attributes a form needs and
 * this signature does not name - `name`, `autocomplete`, `required`, `minLength` - as
 * well as for a `ref`.
 */
@Composable
public fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    ariaLabel: String? = null,
    type: InputType<String> = InputType.Text,
    attrs: (InputAttrsScope<String>.() -> Unit)? = null,
) {
    Input(type) {
        classNames(inputClasses())
        value(value)
        placeholder?.let { placeholder(it) }
        ariaLabel?.let { attr("aria-label", it) }
        onInput { event -> onValueChange(event.value) }
        attrs?.invoke(this)
    }
}

/**
 * A multi-line text field, vertically resizable.
 *
 * [attrs] runs last; see [TextField].
 */
@Composable
public fun TextAreaField(
    value: String,
    onValueChange: (String) -> Unit,
    rows: Int = DEFAULT_TEXTAREA_ROWS,
    placeholder: String? = null,
    ariaLabel: String? = null,
    attrs: (TextAreaAttrsScope.() -> Unit)? = null,
) {
    TextArea(value = value) {
        classNames(textAreaClasses())
        rows(rows)
        placeholder?.let { placeholder(it) }
        ariaLabel?.let { attr("aria-label", it) }
        onInput { event -> onValueChange(event.value) }
        attrs?.invoke(this)
    }
}

private const val DEFAULT_TEXTAREA_ROWS = 6

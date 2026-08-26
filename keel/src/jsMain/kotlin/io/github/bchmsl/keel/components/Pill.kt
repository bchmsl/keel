package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.color.SwatchShade
import io.github.bchmsl.keel.color.swatchBackground
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.pillClasses
import io.github.bchmsl.keel.dom.pillEmojiClasses
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLButtonElement

/**
 * How much room a pill has around it.
 *
 * Not a general type scale: these are the three densities the same label appears at -
 * inside a packed row, in a list of chips, and in a dialog with space to spare.
 */
public enum class PillSize(internal val className: String?) {
    /** A dialog or an expanded row, where there is room. */
    Default(null),

    /** A list of chips, where several sit side by side. */
    Small("pill--small"),

    /** Inside a dense row, competing with the text beside it. */
    Inline("pill--inline"),
}

/**
 * A coloured label.
 *
 * [color] is the swatch as stored, six hex digits. The fill is the same colour at
 * [shade]'s strength and the text is the colour itself, which is what keeps a pill
 * legible on both a light and a dark background without either being named here.
 *
 * `null` is the one pill that stands for no particular thing - the "All" at the head of
 * a filter row, an "Any", a "None" - and it takes the theme's own ink instead. It is
 * null rather than a caller-supplied neutral hex because a hex cannot follow a theme:
 * the pill would stay dark on a dark palette and dark on a light one.
 *
 * A span, not a button: a pill that only states something must not take a tab stop or
 * announce itself as a control. Use [PillButton] where pressing it does something.
 */
@Composable
public fun Pill(
    label: String,
    color: String?,
    shade: SwatchShade = SwatchShade.Pill,
    emoji: String? = null,
    size: PillSize = PillSize.Default,
) {
    Span({
        classNames(pillClasses(size, neutral = color == null))
        color?.let { swatchStyle(it, shade) }
    }) {
        PillBody(label = label, emoji = emoji)
    }
}

/**
 * A coloured label that can be pressed.
 *
 * [ariaLabel] is required because the visible text is the label's name and rarely the
 * action - "Work" does not say whether pressing it filters, attaches or removes.
 *
 * [selected] turns the pill into a toggle. `null`, the default, is a pill that does
 * something else when pressed - attaches a tag, opens it, removes it - and carries no
 * selection semantics at all. `true` and `false` are a filter that is on or off, and
 * set `aria-pressed` as well as the look, so the state is announced rather than only
 * drawn. Three states and not a boolean because "off" and "not a toggle" look the same
 * and mean different things: a boolean would have made every pill announce a pressed
 * state it does not have.
 *
 * On is drawn as full strength plus a ring, and off as a dim, rather than as two
 * different fills. See `.pill--selectable`: this pill's colour is its data, so a
 * "not selected" grey would hide which tag it is.
 *
 * [trailing] is where a remove cross goes. It is a slot rather than a boolean so the
 * caller decides both whether there is one and what it is; a pill that can be taken
 * off and one that merely toggles a filter look the same otherwise.
 */
@Composable
public fun PillButton(
    label: String,
    color: String?,
    ariaLabel: String,
    onClick: () -> Unit,
    shade: SwatchShade = SwatchShade.Pill,
    emoji: String? = null,
    size: PillSize = PillSize.Default,
    selected: Boolean? = null,
    trailing: ContentBuilder<HTMLButtonElement>? = null,
) {
    Button({
        classNames(
            pillClasses(
                size = size,
                pressable = true,
                neutral = color == null,
                selectable = selected != null,
                selected = selected == true,
            ),
        )
        type(ButtonType.Button)
        attr("aria-label", ariaLabel)
        selected?.let { attr("aria-pressed", it.toString()) }
        color?.let { swatchStyle(it, shade) }
        onClick { onClick() }
    }) {
        PillBody(label = label, emoji = emoji)
        trailing?.invoke(this)
    }
}

/**
 * The swatch's fill and text colour, inline.
 *
 * Inline and not a class because the value is stored data: a tag's colour is a column
 * in a database, so there is no finite set of rules that could cover it.
 */
private fun AttrsScope<*>.swatchStyle(color: String, shade: SwatchShade) {
    style {
        property("background-color", swatchBackground(color, shade))
        property("color", color)
    }
}

/**
 * The emoji and the name, in that order.
 *
 * The emoji is wrapped rather than prefixed onto the string, because it needs its own
 * size: at the pill's type size most emoji read as a smudge.
 */
@Composable
private fun PillBody(label: String, emoji: String?) {
    emoji?.let { Span({ classNames(pillEmojiClasses()) }) { Text(it) } }
    Text(label)
}

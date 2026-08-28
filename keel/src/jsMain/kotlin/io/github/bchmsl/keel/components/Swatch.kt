package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.swatchClasses
import io.github.bchmsl.keel.dom.swatchTileClasses
import io.github.bchmsl.keel.dom.swatchTileDotClasses
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLButtonElement

/**
 * How much room a [Swatch] takes.
 *
 * Not a type scale - a swatch has no type. These are the two densities the same
 * choice appears at, and the difference is what is being picked rather than where it
 * sits: ten tag colours are a *set*, and the job is telling them apart; a handful of
 * theme accents are separate decisions, and each gets room.
 */
public enum class SwatchSize(internal val className: String?) {
    /** A short row of choices - a theme accent, a highlight colour. */
    Default(null),

    /** A grid of many, where the set matters more than any one of them. */
    Small("swatch--small"),
}

/**
 * A colour to pick, drawn as the colour.
 *
 * [color] is a plain CSS colour and is set inline, which is the one place in this
 * library where that is the correct answer rather than a shortcut: a picker paints
 * every choice at once while only one palette's variables are live, so these colours
 * cannot come from tokens. `Swatches.All` and `Theme.accentHex` both store a bare hex
 * for exactly this reason.
 *
 * [ariaLabel] is required and has no sensible default, because there is nothing else
 * to go on. A swatch has no text, and "button" is what a screen reader says without
 * it. Name the colour, or better, name what picking it does.
 *
 * [selected] is a real `aria-pressed`, not only a ring. While the swatch has keyboard
 * focus the focus ring replaces that ring - see `.swatch--on` for why - so the
 * announcement is the only thing carrying the state at that moment, and it has to be
 * right.
 *
 * Use [SwatchTile] where the choice has a name worth showing.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun Swatch(
    color: String,
    ariaLabel: String,
    selected: Boolean,
    onSelect: () -> Unit,
    size: SwatchSize = SwatchSize.Default,
    title: String? = null,
    attrs: (AttrsScope<HTMLButtonElement>.() -> Unit)? = null,
) {
    Button({
        classNames(swatchClasses(size, selected))
        type(ButtonType.Button)
        attr("aria-label", ariaLabel)
        attr("aria-pressed", selected.toString())
        title?.let { attr("title", it) }
        style { property("background-color", color) }
        onClick { onSelect() }
        attrs?.invoke(this)
    })
}

/**
 * A colour to pick, with its name beside it.
 *
 * The tile form of [Swatch], for a picker with room for words: six theme names down
 * the side of a settings panel rather than ten wordless colours in a dialog. Once
 * there is text the colour stops being the target and becomes a preview of one, which
 * is why the dot is smaller than the smallest bare swatch and the whole tile is what
 * gets pressed.
 *
 * [label] is visible, so unlike [Swatch] there is no `ariaLabel` here - the text is
 * the name. Pass [ariaLabel] only where the visible word is not the whole story.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun SwatchTile(
    color: String,
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    ariaLabel: String? = null,
    attrs: (AttrsScope<HTMLButtonElement>.() -> Unit)? = null,
) {
    Button({
        classNames(swatchTileClasses(selected))
        type(ButtonType.Button)
        ariaLabel?.let { attr("aria-label", it) }
        attr("aria-pressed", selected.toString())
        onClick { onSelect() }
        attrs?.invoke(this)
    }) {
        Span({
            classNames(swatchTileDotClasses())
            style { property("background-color", color) }
        })
        Text(label)
    }
}

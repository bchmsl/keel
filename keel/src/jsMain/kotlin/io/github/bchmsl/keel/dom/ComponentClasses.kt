package io.github.bchmsl.keel.dom

import io.github.bchmsl.keel.components.ButtonSize
import io.github.bchmsl.keel.components.ButtonVariant
import io.github.bchmsl.keel.components.PillSize

/*
 * The class names keel's components carry, for markup keel does not build itself.
 *
 * Until this file existed, a consumer in that position copied the strings by hand:
 * four dozen literal spellings of the button classes in one app alone, checked by
 * nothing. That is not a hypothetical - a shared screen was changed to emit keel's
 * classes, one of the two shells that render it did not link keel's stylesheet, and
 * the result compiled clean, logged nothing, and shipped an unstyled sign-in form.
 * A hand-copied class name has no definition to be wrong about.
 *
 * There are two reasons a consumer cannot simply call the composable, and both are
 * legitimate rather than laziness:
 *
 *   - the element is not the one keel builds. A ten-foot interface renders its
 *     controls as `Div role="button"` on purpose, because a real `<button>` brings a
 *     UA stylesheet, its own focus ring and its own activation behaviour, all three
 *     of which fight a surface where the focus ring *is* the cursor.
 *   - the element is keel's, but the wiring is not. A D-pad focus engine needs `ref`
 *     and its own data attributes on the element itself.
 *
 * The second case is served by the `attrs` slot each composable now takes. This file
 * serves the first, and is the reason `ButtonVariant.className` can stay `internal`:
 * the mapping is public, the field is not, so a variant can be renamed in one place.
 *
 * ------------------------------------------------------------------ using ----
 *
 * Every function returns a space-separated list, so pass the result through
 * [classNames] rather than Compose HTML's own `classes(...)`:
 *
 *     Div({ classNames(buttonClasses(ButtonVariant.Default)) })   // right
 *     Div({ classes(buttonClasses(ButtonVariant.Default)) })      // throws
 *
 * `classes(...)` puts the whole string through `DOMTokenList.add`, which raises
 * `InvalidCharacterError` on a token containing a space and takes the composition of
 * that subtree down with it. Splitting on whitespace is exactly what [classNames]
 * exists to do.
 *
 * Adding the consumer's own marker class is one call, which is the common shape - a
 * TV control keeps its `.tv-*` class for the focus rules that are the app's own:
 *
 *     classNames("tv-btn", buttonClasses(variant, size))
 *
 * These are functions rather than constants deliberately. A constant that later needs
 * a parameter cannot get one without breaking every call site; a function that gains
 * a defaulted parameter is source-compatible.
 */

/**
 * The classes on a [io.github.bchmsl.keel.components.Button] or
 * [io.github.bchmsl.keel.components.IconButton].
 *
 * The defaults match `Button`'s, not `IconButton`'s: an icon-only control passes
 * [ButtonSize.Icon] and usually [ButtonVariant.Ghost] explicitly.
 */
public fun buttonClasses(
    variant: ButtonVariant = ButtonVariant.Default,
    size: ButtonSize = ButtonSize.Default,
): String = joined("btn", variant.className, size.className)

/** The class on a [io.github.bchmsl.keel.components.TextField]. */
public fun inputClasses(): String = joined("input")

/** The class on a [io.github.bchmsl.keel.components.TextAreaField]. */
public fun textAreaClasses(): String = joined("textarea")

/** The class on a [io.github.bchmsl.keel.components.Slider]. */
public fun sliderClasses(): String = joined("slider")

/**
 * The class on a [io.github.bchmsl.keel.components.Switch]'s track, which is the
 * element itself.
 *
 * The "on" colour is keyed off `aria-checked="true"` rather than a class, so a
 * consumer building its own switch must set that attribute and not look for a
 * modifier class here. See [switchKnobClasses] for the child it needs.
 */
public fun switchClasses(): String = joined("switch")

/**
 * The class on the knob inside a [io.github.bchmsl.keel.components.Switch].
 *
 * A switch is two elements. Rendering only the track leaves a control that changes
 * colour with nothing sliding in it, which reads as a broken toggle rather than an
 * unstyled one.
 */
public fun switchKnobClasses(): String = joined("switch__knob")

/**
 * The classes on a [io.github.bchmsl.keel.components.Pill] or
 * [io.github.bchmsl.keel.components.PillButton].
 *
 * [pressable] adds the button form's class. It is a separate parameter rather than a
 * [PillSize] entry because it is orthogonal: any of the three densities can be
 * pressable.
 *
 * The fill and text colours are not here. They are inline styles derived from the
 * swatch, because a pill's colour is data rather than a variant - see
 * `swatchBackground`.
 */
public fun pillClasses(
    size: PillSize = PillSize.Default,
    pressable: Boolean = false,
): String = joined("pill", "pill--button".takeIf { pressable }, size.className)

/** The class on the emoji span inside a pill. */
public fun pillEmojiClasses(): String = joined("pill__emoji")

/**
 * Drops the absent entries and joins the rest with a single space.
 *
 * Absent rather than blank: [PillSize.Default] and any future "no modifier" variant
 * carry a null `className`, and a naive join would emit a double space, which is a
 * token `DOMTokenList.add` rejects.
 */
private fun joined(vararg names: String?): String =
    names.asSequence()
        .filterNotNull()
        .filter { it.isNotBlank() }
        .joinToString(" ")

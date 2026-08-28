package io.github.bchmsl.keel.dom

import io.github.bchmsl.keel.components.ButtonShape
import io.github.bchmsl.keel.components.ButtonSize
import io.github.bchmsl.keel.components.ButtonVariant
import io.github.bchmsl.keel.components.CalloutTone
import io.github.bchmsl.keel.components.CheckboxSize
import io.github.bchmsl.keel.components.DrawerEdge
import io.github.bchmsl.keel.components.DrawerLayout
import io.github.bchmsl.keel.components.DropdownAlign
import io.github.bchmsl.keel.components.DropdownItemTone
import io.github.bchmsl.keel.components.DropdownSide
import io.github.bchmsl.keel.components.PillSize
import io.github.bchmsl.keel.components.SegmentedStyle
import io.github.bchmsl.keel.components.SurfacePadding
import io.github.bchmsl.keel.components.SurfaceRadius
import io.github.bchmsl.keel.components.SwatchSize
import io.github.bchmsl.keel.components.SwitchSize
import io.github.bchmsl.keel.components.ToastPlacement
import io.github.bchmsl.keel.components.ToastTone

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
    shape: ButtonShape = ButtonShape.Box,
): String = joined("btn", variant.className, size.className, shape.className)

/** The class on a [io.github.bchmsl.keel.components.TextField]. */
public fun inputClasses(): String = joined("input")

/** The class on a [io.github.bchmsl.keel.components.TextAreaField]. */
public fun textAreaClasses(): String = joined("textarea")

/** The class on a [io.github.bchmsl.keel.components.Slider]. */
public fun sliderClasses(): String = joined("slider")

/**
 * The classes on a [io.github.bchmsl.keel.components.Switch]'s track, which is the
 * element itself.
 *
 * The "on" colour is keyed off `aria-checked="true"` rather than a class, so a
 * consumer building its own switch must set that attribute and not look for a
 * modifier class here. See [switchKnobClasses] for the child it needs.
 */
public fun switchClasses(size: SwitchSize = SwitchSize.Default): String =
    joined("switch", size.className)

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
 * [selectable] and [selected] are the filter form. Both, because they are two facts: a
 * pill that participates in a selection is dimmed when off, and a pill that is not part
 * of one is never dimmed at all. A caller with a `Boolean?` maps `null` to neither.
 *
 * [neutral] is the pill standing for no particular tag, which takes the theme's ink.
 * Every other pill's fill and text colour are not here at all - they are inline styles
 * derived from the swatch, because a pill's colour is data rather than a variant. See
 * `swatchBackground`.
 */
public fun pillClasses(
    size: PillSize = PillSize.Default,
    pressable: Boolean = false,
    neutral: Boolean = false,
    selectable: Boolean = false,
    selected: Boolean = false,
): String = joined(
    "pill",
    "pill--neutral".takeIf { neutral },
    "pill--button".takeIf { pressable },
    "pill--selectable".takeIf { selectable },
    "pill--selected".takeIf { selected },
    size.className,
)

/** The class on the emoji span inside a pill. */
public fun pillEmojiClasses(): String = joined("pill__emoji")

/** The classes on a [io.github.bchmsl.keel.components.Swatch]. */
public fun swatchClasses(
    size: SwatchSize = SwatchSize.Default,
    selected: Boolean = false,
): String = joined("swatch", size.className, "swatch--on".takeIf { selected })

/** The classes on a [io.github.bchmsl.keel.components.SwatchTile]. */
public fun swatchTileClasses(selected: Boolean = false): String =
    joined("swatch-tile", "swatch-tile--on".takeIf { selected })

/** The class on the colour dot inside a [io.github.bchmsl.keel.components.SwatchTile]. */
public fun swatchTileDotClasses(): String = joined("swatch-tile__dot")

/**
 * The classes on a [io.github.bchmsl.keel.components.Surface].
 *
 * [radius] is the only geometry here that a tile-sized surface changes. [elevated] and
 * `clipped` are on the composable and not published as arguments because a consumer
 * building its own box has no reason to want keel's shadow without keel's element.
 */
public fun surfaceClasses(
    padding: SurfacePadding = SurfacePadding.Default,
    radius: SurfaceRadius = SurfaceRadius.Default,
): String = joined("surface", padding.className, radius.className)

/** The classes on a [io.github.bchmsl.keel.components.Callout]. */
public fun calloutClasses(tone: CalloutTone = CalloutTone.Neutral): String =
    joined("callout", tone.className)

/** The class on the text side of a callout, beside a trailing control. */
public fun calloutBodyClasses(): String = joined("callout__body")

/**
 * The classes on a [io.github.bchmsl.keel.components.Checkbox].
 *
 * The ticked look is keyed off `aria-checked="true"` rather than a class, exactly as
 * [switchClasses] describes, so a consumer building its own box must set that
 * attribute. The tick itself is the element's content and gets no class: it is hidden
 * by colour, so anything drawn in `currentColor` works.
 */
public fun checkboxClasses(size: CheckboxSize = CheckboxSize.Default): String =
    joined("checkbox", size.className)

/**
 * The classes on a [io.github.bchmsl.keel.components.SegmentedControl]'s container.
 *
 * The inner classes are published separately below, because a consumer that cannot
 * call the composable has to build all four levels of it: the container, one label
 * per choice, the hidden input, and the visible chip.
 */
public fun segmentedClasses(
    style: SegmentedStyle = SegmentedStyle.Track,
    fill: Boolean = false,
): String = joined("segmented", style.className, "segmented--fill".takeIf { fill })

/** The class on the label wrapping one choice. */
public fun segmentedItemClasses(): String = joined("segmented__item")

/**
 * The class on the radio input inside one choice.
 *
 * A consumer rebuilding this markup should keep the real input. The selected
 * appearance is keyed off `:checked`, so a version that leaves the input out gets no
 * selected state at all - and loses arrow-key navigation with it.
 */
public fun segmentedInputClasses(): String = joined("segmented__input")

/**
 * The class on the visible chip. Must be a *following sibling* of the input.
 *
 * A consumer that cannot use a native radio at all - a ten-foot shell rendering each
 * choice as one flat `Div role="radio"` - puts this class on that div and sets
 * `aria-checked`, which the selected-state rules read as a second branch. It gets the
 * look and the announcement; it does not get the arrow-key navigation, which is the
 * native input's and is the reason the native form is the default.
 */
public fun segmentedLabelClasses(): String = joined("segmented__label")

/** The class on the "finished this one" check beside a chip's label. */
public fun segmentedCompleteClasses(): String = joined("segmented__complete")

/**
 * The classes on a [io.github.bchmsl.keel.components.Scrim].
 *
 * [blurred] matches the composable's default. A shell with no pointer usually wants
 * it off: nothing behind the scrim was clickable to begin with, and the blur is a
 * full-screen composite on every frame.
 */
public fun scrimClasses(blurred: Boolean = true): String =
    joined("scrim", "scrim--blurred".takeIf { blurred })

/** The classes on a [io.github.bchmsl.keel.components.Drawer]. */
public fun drawerClasses(
    edge: DrawerEdge = DrawerEdge.Right,
    layout: DrawerLayout = DrawerLayout.Scrolling,
): String = joined("drawer", edge.className, layout.className)

/**
 * The class on the element a [io.github.bchmsl.keel.components.DropdownMenu] is
 * positioned against.
 *
 * The trigger and the menu go inside it. It sets `position: relative` and nothing
 * else, and without it the menu resolves against whatever positioned ancestor it
 * happens to find - usually the page.
 */
public fun dropdownAnchorClasses(): String = joined("dropdown-anchor")

/** The classes on a [io.github.bchmsl.keel.components.DropdownMenu]. */
public fun dropdownClasses(
    align: DropdownAlign = DropdownAlign.End,
    side: DropdownSide = DropdownSide.Below,
): String = joined("dropdown", align.className, side.className)

/**
 * The classes on a [io.github.bchmsl.keel.components.DropdownMenuItem].
 *
 * [selected] is separate from [tone] because they are different facts and an item can
 * carry both: tone is how strongly the choice reads, selection is whether it is the
 * value already in effect.
 */
public fun dropdownItemClasses(
    tone: DropdownItemTone = DropdownItemTone.Default,
    selected: Boolean = false,
): String = joined("dropdown__item", "dropdown__item--selected".takeIf { selected }, tone.className)

/**
 * The class on the check marking the selected item.
 *
 * Decoration only: `aria-current` on the item itself is what announces the state, so a
 * consumer building this markup should set that and mark the check `aria-hidden`.
 */
public fun dropdownCheckClasses(): String = joined("dropdown__check")

/**
 * The class on the full-screen catcher that dismisses an open dropdown.
 *
 * Not a scrim: it changes nothing visually and sits one layer *below* its menu. Both
 * of those are load-bearing - see the CSS.
 */
public fun dropdownCatchClasses(): String = joined("dropdown__catch")

/** The classes on a [io.github.bchmsl.keel.components.ToastHost]. */
public fun toastHostClasses(placement: ToastPlacement = ToastPlacement.Top): String =
    joined("toast-host", placement.className)

/** The classes on one notice inside the host. */
public fun toastClasses(tone: ToastTone = ToastTone.Neutral): String =
    joined("toast", tone.className)

/**
 * Drops the absent entries and joins the rest with a single space.
 *
 * Absent rather than blank: [PillSize.Default] and any future "no modifier" variant
 * carry a null `className`, and a naive join would emit a double space, which is a
 * token `DOMTokenList.add` rejects.
 */
private fun joined(vararg names: String?): String = names.asSequence()
    .filterNotNull()
    .filter { it.isNotBlank() }
    .joinToString(" ")

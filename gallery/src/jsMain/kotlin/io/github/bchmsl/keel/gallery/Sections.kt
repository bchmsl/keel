package io.github.bchmsl.keel.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.bchmsl.keel.color.SwatchShade
import io.github.bchmsl.keel.color.Swatches
import io.github.bchmsl.keel.components.Badge
import io.github.bchmsl.keel.components.BadgeTone
import io.github.bchmsl.keel.components.Button
import io.github.bchmsl.keel.components.ButtonSize
import io.github.bchmsl.keel.components.ButtonVariant
import io.github.bchmsl.keel.components.Card
import io.github.bchmsl.keel.components.EmptyState
import io.github.bchmsl.keel.components.FormattedText
import io.github.bchmsl.keel.components.FormattingField
import io.github.bchmsl.keel.components.IconButton
import io.github.bchmsl.keel.components.LinkButton
import io.github.bchmsl.keel.components.Pill
import io.github.bchmsl.keel.components.PillButton
import io.github.bchmsl.keel.components.PillSize
import io.github.bchmsl.keel.components.Skeleton
import io.github.bchmsl.keel.components.SkeletonShape
import io.github.bchmsl.keel.components.Slider
import io.github.bchmsl.keel.components.Spinner
import io.github.bchmsl.keel.components.SpinnerSize
import io.github.bchmsl.keel.components.Surface
import io.github.bchmsl.keel.components.SurfacePadding
import io.github.bchmsl.keel.components.Switch
import io.github.bchmsl.keel.components.TextAreaField
import io.github.bchmsl.keel.components.TextField
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.icons.Icon
import io.github.bchmsl.keel.icons.LucideIcon
import io.github.bchmsl.keel.theme.KeelTokens
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

// --------------------------------------------------------------------- palette

@Composable
internal fun PaletteSection() {
    Section(
        title = "Palette",
        note = "Every colour the components read. Each value is a bare HSL triple " +
            "rather than a finished colour, so a rule can add alpha with " +
            "hsl(var(--primary) / 0.1) - which a hex token cannot do.",
    ) {
        Div({ classNames("grid") }) {
            KeelTokens.AllColors.forEach { token ->
                Div({ classNames("token") }) {
                    Div({
                        classNames("token__chip")
                        style { property("background-color", "hsl(var($token))") }
                    })
                    Span({ classNames("token__name") }) { Text(token) }
                }
            }
        }
    }
}

// --------------------------------------------------------------------- buttons

@Composable
internal fun ButtonSection() {
    Section(
        title = "Buttons",
        note = "Six variants and four sizes. The focus ring is held off the control " +
            "by a background-coloured gap, so it stays visible on any surface - press " +
            "Tab to see it. The last row is LinkButton, which is a real anchor: " +
            "middle-click one.",
    ) {
        Div({ classNames("row") }) {
            ButtonVariant.entries.forEach { variant ->
                Button(label = variant.name, onClick = {}, variant = variant)
            }
        }

        Div({ classNames("section__note") })

        Div({ classNames("row") }) {
            ButtonSize.entries.forEach { size ->
                if (size == ButtonSize.Icon) {
                    IconButton(
                        ariaLabel = "Settings",
                        onClick = {},
                        variant = ButtonVariant.Outline,
                        title = "Settings",
                    ) {
                        Icon(LucideIcon.Settings)
                    }
                } else {
                    Button(
                        label = size.name,
                        onClick = {},
                        variant = ButtonVariant.Outline,
                        size = size,
                    )
                }
            }

            Button(
                label = "With an icon",
                onClick = {},
                variant = ButtonVariant.Secondary,
                leading = { Icon(LucideIcon.Plus) },
            )

            Button(label = "Disabled", onClick = {}, enabled = false)
        }

        Div({ classNames("section__note") })

        Div({ classNames("row") }) {
            LinkButton(href = "#buttons", label = "Same page")

            LinkButton(
                href = "https://github.com/bchmsl/keel",
                label = "Leaves the app",
                variant = ButtonVariant.Outline,
                external = true,
                leading = { Icon(LucideIcon.Link) },
            )

            LinkButton(
                href = "#buttons",
                label = "Quiet",
                variant = ButtonVariant.Link,
                size = ButtonSize.Small,
            )
        }
    }
}

// ----------------------------------------------------------------------- cards

@Composable
internal fun CardSection() {
    var collapsed by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Section(
        title = "Card",
        note = "A titled panel. Everything past the title is optional, so a second " +
            "app does not have to opt out of a drag handle and a full-screen button it " +
            "has never heard of. Hover the third one to see its handle appear.",
    ) {
        Div({ classNames("grid") }) {
            Card(title = "Plain") {
                Span({ classNames("readout") }) { Text("Just a title and a body.") }
            }

            Card(
                title = if (collapsed) "Collapsed" else "Collapsible",
                collapsed = collapsed,
                onToggleCollapsed = { collapsed = !collapsed },
            ) {
                Span({ classNames("readout") }) { Text("Press the title to roll this up.") }
            }

            Card(
                title = "Draggable",
                draggable = true,
                onDragStart = {},
                onToggleExpanded = { expanded = !expanded },
            ) {
                Span({ classNames("readout") }) {
                    Text(if (expanded) "Expanded is the caller's to render." else "Hover me.")
                }
            }

            Card(title = "Centered", centerContent = true) {
                Icon(LucideIcon.Timer, size = CENTERED_ICON)
                Span({ classNames("readout") }) { Text("centerContent = true") }
            }
        }
    }
}

// -------------------------------------------------------------------- surfaces

@Composable
internal fun SurfaceSection() {
    Section(
        title = "Surface",
        note = "The panel box with no header, which is what a card is built on. The " +
            "padding is a variant rather than one value because it is the part that " +
            "actually differs between a login panel, a stat tile and a poster that " +
            "reaches its own border. Elevation is off by default: a shadow inside " +
            "another shadowed panel reads as a mistake. The last one is unpadded and " +
            "clipped, which is what a poster needs and what a card must not have - " +
            "clipping would cut the focus ring off the button in a card's header.",
    ) {
        Div({ classNames("grid") }) {
            Surface(padding = SurfacePadding.Small) {
                Span({ classNames("readout") }) { Text("padding = Small") }
            }

            Surface {
                Span({ classNames("readout") }) { Text("padding = Default") }
            }

            Surface(padding = SurfacePadding.Large) {
                Span({ classNames("readout") }) { Text("padding = Large") }
            }

            Surface(elevated = true) {
                Span({ classNames("readout") }) { Text("elevated = true") }
            }

            Surface(padding = SurfacePadding.None, clipped = true) {
                Skeleton(attrs = { style { property("aspect-ratio", "16 / 9") } })
            }
        }
    }
}

// ---------------------------------------------------------------------- badges

@Composable
internal fun BadgeSection() {
    Section(
        title = "Badge",
        note = "A status word attached to something else, named by meaning rather than " +
            "by colour - so the same state reads the same everywhere and a palette can " +
            "move a colour without every call site becoming a lie. The stylesheet " +
            "uppercases the text, so what you pass stays what a screen reader reads.",
    ) {
        Div({ classNames("row") }) {
            Badge(label = "1080p")
            Badge(label = "New", tone = BadgeTone.Primary)
            Badge(label = "Watched", tone = BadgeTone.Success) {
                Icon(LucideIcon.Check, size = BADGE_ICON)
            }
            Badge(label = "Expired", tone = BadgeTone.Destructive)
        }
    }
}

// ---------------------------------------------------------------------- waiting

@Composable
internal fun WaitingSection() {
    Section(
        title = "Waiting and empty",
        note = "A skeleton for content on its way, a spinner for a wait with no " +
            "number, and an empty state for a region that is legitimately empty. " +
            "Skeletons carry no size of their own: one is only honest when it is the " +
            "size of the thing it replaces, and only the caller knows that. The " +
            "rendered class is loader, not spinner - base.css already owns that name " +
            "as the utility that rotates an icon.",
    ) {
        Div({ classNames("stack") }) {
            Div({ classNames("row") }) {
                Spinner(size = SpinnerSize.Small)
                Spinner()
                Spinner(size = SpinnerSize.Large)
            }

            Div({ classNames("row") }) {
                Skeleton(
                    shape = SkeletonShape.Circle,
                    attrs = {
                        style {
                            property("width", "2.5rem")
                            property("height", "2.5rem")
                        }
                    },
                )
                Div({ classNames("stack") }) {
                    Skeleton(shape = SkeletonShape.Line, attrs = {
                        style { property("width", "12rem") }
                    })
                    Skeleton(shape = SkeletonShape.Line, attrs = {
                        style { property("width", "7rem") }
                    })
                }
            }

            EmptyState(
                title = "Nothing saved yet",
                body = "Anything you keep shows up here, newest first.",
                leading = { Icon(LucideIcon.LayoutGrid, size = CENTERED_ICON) },
                action = { Button(label = "Add the first one", onClick = {}) },
            )
        }
    }
}

// ---------------------------------------------------------------------- fields

@Composable
internal fun FieldSection() {
    var text by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var on by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(DEFAULT_VOLUME) }

    Section(
        title = "Fields",
        note = "The switch is a button with role=switch, and its \"on\" colour is keyed " +
            "off the same aria-checked that assistive technology reads - so the two " +
            "cannot disagree. The slider is a native range input with its appearance " +
            "replaced, which keeps keyboard stepping and touch dragging for free.",
    ) {
        Div({ classNames("stack") }) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "A single-line field",
                ariaLabel = "Example text field",
            )

            TextAreaField(
                value = notes,
                onValueChange = { notes = it },
                rows = 3,
                placeholder = "A resizable multi-line field",
                ariaLabel = "Example text area",
            )

            Div({ classNames("row") }) {
                Switch(checked = on, onCheckedChange = { on = it }, ariaLabel = "Example switch")
                Span({ classNames("field-label") }) { Text(if (on) "On" else "Off") }
            }

            Div({ classNames("row") }) {
                Slider(
                    value = volume,
                    min = 0,
                    max = MAX_VOLUME,
                    onValueChange = { volume = it },
                    ariaLabel = "Example slider",
                )
            }
            Span({ classNames("readout") }) { Text("$volume%") }
        }
    }
}

// ----------------------------------------------------------------------- pills

@Composable
internal fun PillSection() {
    var selected by remember { mutableStateOf(Swatches.Default) }

    Section(
        title = "Pills",
        note = "For labels the user colours themselves. This is the one colour that " +
            "deliberately escapes the theme: someone picked it to tell two of their own " +
            "things apart, so a palette change must not make them the same colour. Ten " +
            "fixed swatches, each at four strengths.",
    ) {
        Div({ classNames("stack") }) {
            SwatchShade.entries.forEach { shade ->
                Div({ classNames("row") }) {
                    Span({ classNames("field-label") }) { Text(shade.name) }
                    Swatches.All.forEach { color ->
                        Pill(label = "Label", color = color, shade = shade)
                    }
                }
            }
        }

        Div({ classNames("section__note") })

        Div({ classNames("row") }) {
            Swatches.All.forEach { color ->
                PillButton(
                    label = "Filter",
                    color = color,
                    ariaLabel = "Filter by this label",
                    onClick = { selected = color },
                    shade = if (color == selected) SwatchShade.Selected else SwatchShade.Faint,
                    emoji = "⭐",
                    size = PillSize.Small,
                )
            }
        }

        Div({ classNames("row") }) {
            Pill(
                label = "Inline",
                color = selected,
                shade = SwatchShade.Inline,
                size = PillSize.Inline,
            )
            PillButton(
                label = "Removable",
                color = selected,
                ariaLabel = "Remove this label",
                onClick = {},
                trailing = { Icon(LucideIcon.X, size = PILL_ICON) },
            )
        }
    }
}

// ------------------------------------------------------------------------ text

@Composable
internal fun TextSection() {
    var committed by remember { mutableStateOf(SAMPLE_TEXT) }

    Section(
        title = "Text",
        note = "Inline markers, read by a hand-written scanner rather than a regular " +
            "expression: the expression this replaces needs lookbehind to tell * from " +
            "**, which older Safari throws while compiling. Nothing here builds markup " +
            "from the text, so a note titled <script> is a title, drawn as eight " +
            "characters. Type, then click away or press a formatting button.",
    ) {
        Div({ classNames("stack") }) {
            FormattingField(
                // Constant, because there is only one record on this page. In an app
                // this is whatever identifies the thing being edited.
                resetKey = "gallery",
                initial = SAMPLE_TEXT,
                onCommit = { committed = it },
                multiline = true,
                textRows = SAMPLE_ROWS,
                ariaLabel = "Formatting example",
            )

            Div { FormattedText(committed) }
        }
    }
}

// ----------------------------------------------------------------------- icons

@Composable
internal fun IconSection() {
    Section(
        title = "Icons",
        note = "Generated from the lucide repository at a pinned tag, as the inner " +
            "markup of a 24x24 stroke icon. Each takes the colour of the text around " +
            "it, so a hover rule recolours an icon without naming it.",
    ) {
        Div({ classNames("icon-wall") }) {
            LucideIcon.entries.forEach { icon ->
                Div({ classNames("icon-cell") }) {
                    Icon(icon, size = ICON_WALL_SIZE)
                    Span({ classNames("icon-cell__name") }) { Text(icon.name) }
                }
            }
        }
    }
}

private const val CENTERED_ICON = 32
private const val ICON_WALL_SIZE = 20
private const val BADGE_ICON = 10
private const val PILL_ICON = 10
private const val DEFAULT_VOLUME = 70
private const val MAX_VOLUME = 100
private const val SAMPLE_ROWS = 4

private const val SAMPLE_TEXT =
    "**Bold**, *italic*, __underlined__ and `code`. Addresses become links: " +
        "https://github.com/bchmsl/keel - and an unclosed **marker stays as typed."

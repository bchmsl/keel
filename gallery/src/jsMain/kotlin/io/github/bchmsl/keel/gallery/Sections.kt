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
import io.github.bchmsl.keel.components.Callout
import io.github.bchmsl.keel.components.CalloutBody
import io.github.bchmsl.keel.components.CalloutTone
import io.github.bchmsl.keel.components.Card
import io.github.bchmsl.keel.components.Checkbox
import io.github.bchmsl.keel.components.CheckboxSize
import io.github.bchmsl.keel.components.Drawer
import io.github.bchmsl.keel.components.DrawerEdge
import io.github.bchmsl.keel.components.DropdownAlign
import io.github.bchmsl.keel.components.DropdownItemTone
import io.github.bchmsl.keel.components.DropdownMenu
import io.github.bchmsl.keel.components.DropdownMenuItem
import io.github.bchmsl.keel.components.DropdownSide
import io.github.bchmsl.keel.components.EmptyState
import io.github.bchmsl.keel.components.FormattedText
import io.github.bchmsl.keel.components.FormattingField
import io.github.bchmsl.keel.components.IconButton
import io.github.bchmsl.keel.components.LinkButton
import io.github.bchmsl.keel.components.Pill
import io.github.bchmsl.keel.components.PillButton
import io.github.bchmsl.keel.components.PillSize
import io.github.bchmsl.keel.components.ProgressBar
import io.github.bchmsl.keel.components.ProgressBarSize
import io.github.bchmsl.keel.components.ProgressHandle
import io.github.bchmsl.keel.components.Scrub
import io.github.bchmsl.keel.components.ScrubHandle
import io.github.bchmsl.keel.components.Segment
import io.github.bchmsl.keel.components.SegmentedControl
import io.github.bchmsl.keel.components.SegmentedStyle
import io.github.bchmsl.keel.components.Skeleton
import io.github.bchmsl.keel.components.SkeletonShape
import io.github.bchmsl.keel.components.Slider
import io.github.bchmsl.keel.components.Spinner
import io.github.bchmsl.keel.components.SpinnerSize
import io.github.bchmsl.keel.components.Surface
import io.github.bchmsl.keel.components.SurfacePadding
import io.github.bchmsl.keel.components.SurfaceRadius
import io.github.bchmsl.keel.components.Switch
import io.github.bchmsl.keel.components.SwitchSize
import io.github.bchmsl.keel.components.TextAreaField
import io.github.bchmsl.keel.components.TextField
import io.github.bchmsl.keel.components.Toast
import io.github.bchmsl.keel.components.ToastHost
import io.github.bchmsl.keel.components.ToastPlacement
import io.github.bchmsl.keel.components.ToastTone
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.dropdownAnchorClasses
import io.github.bchmsl.keel.dom.segmentedClasses
import io.github.bchmsl.keel.dom.segmentedItemClasses
import io.github.bchmsl.keel.dom.segmentedLabelClasses
import io.github.bchmsl.keel.icons.Icon
import io.github.bchmsl.keel.icons.LucideIcon
import io.github.bchmsl.keel.theme.KeelTokens
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
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
        note = "Seven variants and five sizes. The focus ring is held off the control " +
            "by a background-coloured gap, so it stays visible on any surface - press " +
            "Tab to see it. OnMedia has its own row because it is the one variant that " +
            "resolves against no palette colour: it is a translucent wash for a control " +
            "sitting over picture, and on the page background there is nothing for it " +
            "to be translucent over. Inline is missing from the size row because it " +
            "has no box; it is in the sentence at the foot of this section, which is " +
            "the only place it belongs. The last row is LinkButton, which is a real " +
            "anchor: middle-click one.",
    ) {
        Div({ classNames("row") }) {
            ButtonVariant.entries.filter { it != ButtonVariant.OnMedia }.forEach { variant ->
                Button(label = variant.name, onClick = {}, variant = variant)
            }
        }

        Div({ classNames("media-strip") }) {
            Div({ classNames("row") }) {
                Button(label = "OnMedia", onClick = {}, variant = ButtonVariant.OnMedia)
                Button(
                    label = "1080p",
                    onClick = {},
                    variant = ButtonVariant.OnMedia,
                    size = ButtonSize.Small,
                )
                IconButton(
                    ariaLabel = "Play",
                    onClick = {},
                    variant = ButtonVariant.OnMedia,
                ) {
                    Icon(LucideIcon.Play)
                }
            }
        }

        Div({ classNames("section__note") })

        Div({ classNames("row") }) {
            ButtonSize.entries.forEach { size ->
                when (size) {
                    ButtonSize.Icon -> IconButton(
                        ariaLabel = "Settings",
                        onClick = {},
                        variant = ButtonVariant.Outline,
                        title = "Settings",
                    ) {
                        Icon(LucideIcon.Settings)
                    }
                    // Not in this row: it has no box, so an outlined one is a border
                    // drawn tight around a word. It is shown in a sentence below,
                    // which is the only place it belongs.
                    ButtonSize.Inline -> Unit
                    else -> Button(
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

        Div({ classNames("section__note") })

        // `ButtonSize.Inline` in the one place it is for. The point of the row is that
        // the line height does not change: at any other size this button would be a
        // 2.5rem box wedged into a 0.875rem sentence and push the two lines apart.
        P({ classNames("section__note") }) {
            Text("A link button at ")
            LinkButton(
                href = "#buttons",
                label = "this size",
                variant = ButtonVariant.Link,
                size = ButtonSize.Inline,
            )
            Text(" sits in a sentence without disturbing it, and so does a ")
            Button(
                label = "real button",
                onClick = {},
                variant = ButtonVariant.Link,
                size = ButtonSize.Inline,
            )
            Text(" beside it.")
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
            "clipping would cut the focus ring off the button in a card's header. The " +
            "radius is a two-entry variant for the same reason the padding is: a tile " +
            "the size of a stat takes the panel radius as a third of its own box.",
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

            Surface(padding = SurfacePadding.Small, radius = SurfaceRadius.Small) {
                Span({ classNames("readout") }) { Text("radius = Small") }
            }
        }
    }
}

// ------------------------------------------------------------------- segmented

private enum class DemoView { Board, List, Calendar }

@Composable
internal fun SegmentedSection() {
    var view by remember { mutableStateOf(DemoView.Board) }
    var language by remember { mutableStateOf("EN") }
    var season by remember { mutableStateOf(1) }
    var flat by remember { mutableStateOf("On") }

    Section(
        title = "Segmented control",
        note = "One component, two treatments, because the difference between them " +
            "is entirely in the container. A real radio group underneath: tab to it " +
            "and the arrow keys move between choices, Home and End jump to the ends, " +
            "and the whole group is one tab stop - none of which a row of buttons " +
            "with role=radio gets without reimplementing roving tabindex. The " +
            "selected colour is keyed off :checked rather than a class, so what you " +
            "see and what is announced cannot drift apart. The last row is the same " +
            "control built by hand from the published class names and aria-checked, " +
            "which is the escape hatch for a shell that cannot use a native input at " +
            "all - a D-pad interface, where a real radio brings activation behaviour " +
            "that fights a focus ring the app owns. It gets the look and the " +
            "announcement; it does not get the arrow keys, which is why it is not the " +
            "default.",
    ) {
        Div({ classNames("stack") }) {
            SegmentedControl(
                segments = DemoView.entries.map { Segment(it, it.name) },
                selected = view,
                onSelect = { view = it },
                ariaLabel = "View",
                fill = true,
            )

            SegmentedControl(
                segments = listOf(Segment("EN", "English"), Segment("KA", "ქართული")),
                selected = language,
                onSelect = { language = it },
                ariaLabel = "Language",
            )

            Span({ classNames("readout") }) { Text("$view / $language / season $season") }

            // The rail: more choices than fit, so it scrolls, and each carries
            // whether it has been finished. Which ones are finished is unrelated to
            // which one is open, so season 1 below is both - and its check takes the
            // chip's own colour rather than staying green on a red fill.
            SegmentedControl(
                segments = (1..RAIL_SEASONS).map {
                    Segment(it, "Season $it", complete = it in WATCHED_SEASONS)
                },
                selected = season,
                onSelect = { season = it },
                ariaLabel = "Season",
                style = SegmentedStyle.Rail,
            )

            FlatSegmented(
                labels = listOf("Off", "On", "Auto"),
                selected = flat,
                onSelect = { flat = it },
            )
        }
    }
}

/**
 * A segmented control with no native input, built the way a ten-foot shell has to.
 *
 * One flat `Div role="radio" aria-checked` per choice, carrying the published chip
 * class. Here it is a demonstration that the second CSS branch actually paints; in the
 * shell it exists because focus there is a cursor the app draws itself.
 */
@Composable
private fun FlatSegmented(labels: List<String>, selected: String, onSelect: (String) -> Unit) {
    Div({
        classNames(segmentedClasses())
        attr("role", "radiogroup")
        attr("aria-label", "Built by hand")
    }) {
        labels.forEach { label ->
            Div({ classNames(segmentedItemClasses()) }) {
                Div({
                    classNames(segmentedLabelClasses())
                    attr("role", "radio")
                    attr("aria-checked", (label == selected).toString())
                    attr("tabindex", if (label == selected) "0" else "-1")
                    onClick { onSelect(label) }
                }) { Text(label) }
            }
        }
    }
}

// -------------------------------------------------------------------- overlays

@Composable
internal fun OverlaySection() {
    var drawerEdge by remember { mutableStateOf<DrawerEdge?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var dropUpOpen by remember { mutableStateOf(false) }
    var quality by remember { mutableStateOf("720p") }
    var toasts by remember { mutableStateOf(emptyList<Toast>()) }
    var placement by remember { mutableStateOf(ToastPlacement.Top) }

    Section(
        title = "Overlays",
        note = "A scrim, a drawer on either edge, a dropdown and the toast host. " +
            "The four of them settle a z-index band that was renumbered by hand in " +
            "both apps: scrim, drawer, catcher, dropdown, dialog, toast. The " +
            "dropdown's catcher is the interesting one - it is not a scrim, it " +
            "changes nothing visually, and it sits one layer below its own menu, " +
            "which is the bug it exists to have fixed.",
    ) {
        Div({ classNames("row") }) {
            Button(label = "Drawer from left", onClick = { drawerEdge = DrawerEdge.Left })
            Button(label = "Drawer from right", onClick = { drawerEdge = DrawerEdge.Right })
        }

        Div({ classNames("row") }) {
            // The anchor: the trigger and the menu share it, and the menu is
            // positioned against it rather than against the page.
            Div({ classNames(dropdownAnchorClasses()) }) {
                Button(
                    label = "Menu",
                    onClick = { menuOpen = !menuOpen },
                    variant = ButtonVariant.Outline,
                )

                if (menuOpen) {
                    DropdownMenu(
                        onDismiss = { menuOpen = false },
                        ariaLabel = "Example actions",
                        align = DropdownAlign.Start,
                    ) {
                        DropdownMenuItem("Rename", onClick = { menuOpen = false }) {
                            Icon(LucideIcon.Pencil, size = MENU_ICON)
                        }
                        DropdownMenuItem("Copy link", onClick = { menuOpen = false }) {
                            Icon(LucideIcon.Link, size = MENU_ICON)
                        }
                        DropdownMenuItem(
                            "Delete",
                            onClick = { menuOpen = false },
                            tone = DropdownItemTone.Danger,
                        ) {
                            Icon(LucideIcon.Trash2, size = MENU_ICON)
                        }
                    }
                }
            }

            // The same menu opening upward, which is what a trigger pinned to the
            // foot of the viewport needs - a video's control bar is the case that
            // asked for it.
            Div({ classNames(dropdownAnchorClasses()) }) {
                Button(
                    label = "Menu, upward",
                    onClick = { dropUpOpen = !dropUpOpen },
                    variant = ButtonVariant.Outline,
                )

                if (dropUpOpen) {
                    DropdownMenu(
                        onDismiss = { dropUpOpen = false },
                        ariaLabel = "Example actions, opening upward",
                        align = DropdownAlign.Start,
                        side = DropdownSide.Above,
                    ) {
                        // A real single-select, so the selected state has something
                        // to be: `selected` draws the check and sets `aria-current`,
                        // and the two cannot drift because one flag sets both.
                        listOf("1080p", "720p", "Auto").forEach { label ->
                            DropdownMenuItem(
                                label = label,
                                onClick = {
                                    quality = label
                                    dropUpOpen = false
                                },
                                selected = label == quality,
                            )
                        }
                    }
                }
            }

            Button(
                label = "Add a notice",
                onClick = { toasts = toasts + sampleToast(toasts.size) },
                variant = ButtonVariant.Secondary,
            )

            Button(
                label = "Clear",
                onClick = { toasts = emptyList() },
                variant = ButtonVariant.Ghost,
            )
        }

        SegmentedControl(
            segments = ToastPlacement.entries.map { Segment(it, it.name) },
            selected = placement,
            onSelect = { placement = it },
            ariaLabel = "Notice placement",
        )
    }

    drawerEdge?.let { edge ->
        Drawer(
            onDismiss = { drawerEdge = null },
            ariaLabel = "Example drawer",
            edge = edge,
        ) {
            Span({ classNames("readout") }) { Text("edge = ${edge.name}") }
            Text(
                "A drawer rather than a dialog: a list of settings that each apply " +
                    "as they are changed has nothing to confirm and nothing to " +
                    "cancel. Escape closes it, and so does the scrim.",
            )
            Button(label = "Close", onClick = { drawerEdge = null })
        }
    }

    ToastHost(toasts, placement)
}

/** Cycles the three tones, so all three can be seen without a control for it. */
private fun sampleToast(index: Int): Toast {
    val tone = ToastTone.entries[index % ToastTone.entries.size]
    return Toast(
        id = "toast-$index",
        message = "${tone.name}: notice number ${index + 1}",
        tone = tone,
    )
}

// -------------------------------------------------------------------- progress

@Composable
internal fun ProgressSection() {
    var handle by remember { mutableStateOf<ProgressHandle?>(null) }

    Section(
        title = "Progress and scrub",
        note = "One bar replaces four hand-written ones that agreed on nothing: " +
            "three thicknesses, three track alphas, one radius between them and one " +
            "width transition. The on-media pair takes its track from " +
            "--primary-foreground, the palette's ink for sitting on a saturated fill " +
            "of unknown colour, which is what a poster is. The scrub bar is separate " +
            "because it needs a buffered layer and is painted from a frame loop " +
            "rather than from state - drag it, and use the arrow keys once it has " +
            "focus.",
    ) {
        Div({ classNames("stack") }) {
            ProgressBar(
                fraction = PROGRESS_SAMPLE,
                ariaLabel = "Sample progress",
                size = ProgressBarSize.Small,
            )
            ProgressBar(fraction = PROGRESS_SAMPLE, ariaLabel = "Sample progress")
            ProgressBar(
                fraction = PROGRESS_SAMPLE,
                ariaLabel = "Sample progress",
                size = ProgressBarSize.Large,
            )
            ProgressBar(
                fraction = 1.0,
                ariaLabel = "Finished",
                size = ProgressBarSize.Large,
                done = true,
            )

            // The on-media pair needs something behind them to be judged against.
            Div({
                classNames("media-strip")
            }) {
                ProgressBar(
                    fraction = PROGRESS_SAMPLE,
                    ariaLabel = "Sample progress over media",
                    onMedia = true,
                )
                ProgressBar(
                    fraction = 1.0,
                    ariaLabel = "Finished, over media",
                    onMedia = true,
                    done = true,
                )
            }

            // Driven the way a player drives it: the handle writes the width and the
            // ARIA value directly, with no recomposition and no `fraction` change.
            ProgressBar(
                fraction = 0.0,
                ariaLabel = "Driven by its handle",
                size = ProgressBarSize.Large,
                onHandleReady = { handle = it },
            )
            Div({ classNames("row") }) {
                Button(
                    label = "Paint 30%",
                    onClick = { handle?.setFraction(PROGRESS_LOW) },
                    variant = ButtonVariant.Outline,
                    size = ButtonSize.Small,
                )
                Button(
                    label = "Paint 80%",
                    onClick = { handle?.setFraction(PROGRESS_HIGH) },
                    variant = ButtonVariant.Outline,
                    size = ButtonSize.Small,
                )
            }

            Div({ classNames("media-strip") }) {
                ScrubExample()
            }
        }
    }
}

/**
 * The scrub bar with a stand-in for a player behind it.
 *
 * There is no video here, so the frame loop a real player runs is replaced by the
 * seek callback painting itself. That is enough to exercise the whole contract: the
 * drag guard, the buffered layer and the announced time all go through the handle.
 */
@Composable
private fun ScrubExample() {
    var handle by remember { mutableStateOf<ScrubHandle?>(null) }
    var position by remember { mutableStateOf(SCRUB_START) }

    Div({ classNames("stack") }) {
        Scrub(
            ariaLabel = "Timeline",
            onSeek = { position = it },
            onHandleReady = { ready ->
                handle = ready
                ready.setBuffered(SCRUB_BUFFERED)
                ready.setPosition(SCRUB_START, formatSeconds(SCRUB_START))
            },
        )

        Div({ classNames("row") }) {
            Span({ classNames("readout") }) { Text(formatSeconds(position)) }
            Button(
                label = "Jump to the middle",
                onClick = {
                    position = SCRUB_MIDDLE
                    handle?.setPosition(SCRUB_MIDDLE, formatSeconds(SCRUB_MIDDLE))
                },
                variant = ButtonVariant.Outline,
                size = ButtonSize.Small,
            )
        }
    }
}

private fun formatSeconds(fraction: Double): String {
    val total = (fraction * SAMPLE_DURATION).toInt()
    val minutes = total / SECONDS_PER_MINUTE
    val seconds = total % SECONDS_PER_MINUTE
    return "$minutes:${seconds.toString().padStart(2, '0')}"
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

// --------------------------------------------------------------------- callouts

@Composable
internal fun CalloutSection() {
    Section(
        title = "Callout",
        note = "A bordered strip about the screen it is on, which is what separates it " +
            "from the other two boxes that look like it: a toast arrives, is about an " +
            "action just taken and leaves on a timer, and an empty state replaces " +
            "content rather than sitting beside it. The tone tints the box and never " +
            "recolours the text: the destructive ink on its own tint measures 3.0:1 on " +
            "the light palettes, where 13px text needs 4.5:1, while the plain " +
            "foreground is 12:1 or better in all twelve. Switch the theme above and " +
            "read all three.",
    ) {
        Div({ classNames("stack") }) {
            Callout {
                Text("Timers keep running when this tab is in the background.")
            }

            Callout(tone = CalloutTone.Destructive, announce = true) {
                Text("That stream stopped responding. Try a lower quality.")
            }

            Callout(tone = CalloutTone.Primary) {
                CalloutBody {
                    Text("You have reached the end of this series.")
                }
                Button(
                    label = "Watch it again",
                    onClick = {},
                    variant = ButtonVariant.Outline,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------- fields

@Composable
internal fun FieldSection() {
    var text by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var on by remember { mutableStateOf(true) }
    var done by remember { mutableStateOf(false) }
    var subDone by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(DEFAULT_VOLUME) }

    Section(
        title = "Fields",
        note = "The switch is a button with role=switch, and its \"on\" colour is keyed " +
            "off the same aria-checked that assistive technology reads - so the two " +
            "cannot disagree. The checkbox is the same construction, and the choice " +
            "between them is not style: a switch takes effect the moment it moves, a " +
            "checkbox states a value something else commits. Hover an unticked box - " +
            "the tick is always there and hidden by colour, so previewing it cannot " +
            "resize the row. The slider is a native range input with its appearance " +
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

                // Both sizes on one row, which is the case the small one exists for:
                // the two have to look like one control at two scales, and that is
                // only checkable side by side.
                Switch(
                    checked = on,
                    onCheckedChange = { on = it },
                    ariaLabel = "Example switch, small",
                    size = SwitchSize.Small,
                )
                Span({ classNames("field-label") }) { Text("Small") }
            }

            Div({ classNames("row") }) {
                Checkbox(
                    checked = done,
                    onCheckedChange = { done = it },
                    ariaLabel = "Example checkbox",
                )
                Span({ classNames("field-label") }) { Text(if (done) "Done" else "Not done") }

                Checkbox(
                    checked = subDone,
                    onCheckedChange = { subDone = it },
                    ariaLabel = "Example checkbox, small",
                    size = CheckboxSize.Small,
                )
                Span({ classNames("field-label") }) { Text("Small") }

                Checkbox(
                    checked = false,
                    onCheckedChange = {},
                    ariaLabel = "Example checkbox, disabled",
                    enabled = false,
                )
                Span({ classNames("field-label") }) { Text("Disabled") }
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
    var selected by remember { mutableStateOf<String?>(Swatches.Default) }

    Section(
        title = "Pills",
        note = "For labels the user colours themselves. This is the one colour that " +
            "deliberately escapes the theme: someone picked it to tell two of their own " +
            "things apart, so a palette change must not make them the same colour. Ten " +
            "fixed swatches, each at four strengths. The filter row is the exception " +
            "that proves it: an unselected pill dims rather than turning grey, because " +
            "its colour is the tag it stands for and a \"not selected\" grey would hide " +
            "which tag that is. \"All\" is the one pill with no swatch, so it is the one " +
            "that takes the theme's own ink.",
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
            PillButton(
                label = "All",
                color = null,
                ariaLabel = "Clear the filter",
                onClick = { selected = null },
                selected = selected == null,
                size = PillSize.Small,
            )

            Swatches.All.forEach { color ->
                PillButton(
                    label = "Filter",
                    color = color,
                    ariaLabel = "Filter by this label",
                    onClick = { selected = color },
                    emoji = "⭐",
                    size = PillSize.Small,
                    selected = color == selected,
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
            Pill(label = "No swatch", color = null)
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
private const val RAIL_SEASONS = 12
private const val MENU_ICON = 14
private val WATCHED_SEASONS = setOf(1, 3, 4, 8)
private const val PROGRESS_SAMPLE = 0.62
private const val PROGRESS_LOW = 0.3
private const val PROGRESS_HIGH = 0.8
private const val SCRUB_START = 0.18
private const val SCRUB_MIDDLE = 0.5
private const val SCRUB_BUFFERED = 0.45
private const val SAMPLE_DURATION = 2470.0
private const val SECONDS_PER_MINUTE = 60
private const val PILL_ICON = 10
private const val DEFAULT_VOLUME = 70
private const val MAX_VOLUME = 100
private const val SAMPLE_ROWS = 4

private const val SAMPLE_TEXT =
    "**Bold**, *italic*, __underlined__ and `code`. Addresses become links: " +
        "https://github.com/bchmsl/keel - and an unclosed **marker stays as typed."

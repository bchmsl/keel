package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.icons.Icon
import io.github.bchmsl.keel.icons.LucideIcon
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement

/**
 * A titled panel: a header with a title and optional controls, and a body.
 *
 * Everything past [title] is optional, so the plain case is `Card("Recent") { ... }`
 * and each control appears only for an app that asks for it. That is deliberate -
 * the panel with a drag handle, a collapse toggle and a full-screen button is one
 * app's board, and a second app should not have to opt out of three behaviours it
 * has never heard of.
 *
 * Where the panel sits and whether it is full-screen are the board's business, not
 * the panel's, so both arrive as parameters. That is exactly what lets the same call
 * render inline in a column or filling the screen without knowing which it is.
 *
 * When [expanded], the drag handle and the collapse toggle disappear: a panel filling
 * the screen cannot be reordered against anything, and collapsing it would leave an
 * empty screen with one row at the top. It also becomes a column that fills its
 * container with the header pinned and the body scrolling, which is the only shape a
 * full-screen panel can have: without it a long body carries the close button off the
 * top of the screen.
 *
 * [dragging] lifts it - a larger shadow and a faint ring - while the pointer is
 * carrying it. Separate from [draggable], which is whether the handle is drawn at all:
 * one is a capability and the other is a moment. It is a parameter rather than
 * something the caller styles itself because the class saying a drag is in progress
 * belongs to the slot *around* the card, and reaching the card from there means naming
 * keel's own selector in an app's stylesheet.
 *
 * The box itself is a [Surface], so the border, radius, colour and shadow of a titled
 * panel and an untitled one cannot drift apart. Padding is [SurfacePadding.None]
 * because the header and the body pad themselves - a card's two regions are inset by
 * different amounts, which one padding on the outer box cannot express.
 */
@Composable
public fun Card(
    title: String,
    collapsed: Boolean = false,
    expanded: Boolean = false,
    draggable: Boolean = false,
    dragging: Boolean = false,
    centerContent: Boolean = false,
    onToggleCollapsed: (() -> Unit)? = null,
    onToggleExpanded: (() -> Unit)? = null,
    onDragStart: (() -> Unit)? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    Surface(
        padding = SurfacePadding.None,
        elevated = true,
        attrs = {
            classNames(
                "card",
                "card--expanded".takeIf { expanded },
                "card--dragging".takeIf { dragging },
            )
        },
    ) {
        Div({ classNames("card__header") }) {
            Div({ classNames("card__title-group") }) {
                if (draggable && !expanded) {
                    CardGrip(onDragStart)
                }

                CardTitle(
                    title = title,
                    collapsed = collapsed,
                    expanded = expanded,
                    onToggleCollapsed = onToggleCollapsed,
                )
            }

            Div({ classNames("card__actions") }) {
                onToggleExpanded?.let { toggle ->
                    Button({
                        classNames("card__icon-button")
                        type(ButtonType.Button)
                        attr("aria-label", if (expanded) "Minimize" else "Maximize")
                        onClick { toggle() }
                    }) {
                        Icon(
                            if (expanded) LucideIcon.Minimize2 else LucideIcon.Maximize2,
                            size = CARD_ACTION_ICON,
                        )
                    }
                }
            }
        }

        // Collapsed hides the body outright rather than shrinking it, so a collapsed
        // panel is exactly one row tall.
        if (!collapsed) {
            Div({
                classNames(
                    "card__content",
                    "card__content--centered".takeIf { centerContent },
                )
            }) {
                content()
            }
        }
    }
}

/**
 * The drag handle.
 *
 * Both pointer kinds are handled, and neither is incidental. A right-click on a
 * handle should open a context menu rather than begin a drag the user cannot see
 * themselves having started, so only the left button counts. Touch reports no button
 * at all, and its default action is the page scrolling under the finger instead of
 * the panel following it.
 */
@Composable
private fun CardGrip(onDragStart: (() -> Unit)?) {
    Span({
        classNames("card__grip")
        onDragStart?.let { start ->
            onMouseDown { event ->
                if (event.button.toInt() == PRIMARY_MOUSE_BUTTON) {
                    event.preventDefault()
                    start()
                }
            }
            onTouchStart { event ->
                event.preventDefault()
                start()
            }
        }
    }) {
        Icon(LucideIcon.GripVertical, size = CARD_TITLE_ICON)
    }
}

/**
 * The title, as a button when it collapses the panel and as plain text otherwise.
 *
 * A span rather than a disabled button in the non-collapsing case: a control that
 * does nothing still announces itself as a control, and still takes a tab stop.
 */
@Composable
private fun CardTitle(
    title: String,
    collapsed: Boolean,
    expanded: Boolean,
    onToggleCollapsed: (() -> Unit)?,
) {
    if (expanded || onToggleCollapsed == null) {
        Span({ classNames("card__title") }) { Text(title) }
        return
    }

    Button({
        classNames("card__title")
        type(ButtonType.Button)
        attr("aria-expanded", (!collapsed).toString())
        onClick { onToggleCollapsed() }
    }) {
        Icon(
            if (collapsed) LucideIcon.ChevronDown else LucideIcon.ChevronUp,
            size = CARD_TITLE_ICON,
        )
        Text(title)
    }
}

private const val PRIMARY_MOUSE_BUTTON = 0
private const val CARD_TITLE_ICON = 16
private const val CARD_ACTION_ICON = 14

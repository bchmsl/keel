package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.dom.segmentedClasses
import io.github.bchmsl.keel.dom.segmentedCompleteClasses
import io.github.bchmsl.keel.dom.segmentedInputClasses
import io.github.bchmsl.keel.dom.segmentedItemClasses
import io.github.bchmsl.keel.dom.segmentedLabelClasses
import io.github.bchmsl.keel.icons.Icon
import io.github.bchmsl.keel.icons.LucideIcon
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.RadioInput
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement

/** The two treatments. */
public enum class SegmentedStyle(internal val className: String?) {
    /**
     * Choices inside a shared track, all visible at once.
     *
     * For a closed set that fits: a view switcher, a language pair, three sort
     * orders.
     */
    Track(null),

    /**
     * Detached chips on a horizontal scroller.
     *
     * For a set too long to show at once - twelve seasons, forty tags - where the
     * user is expected to scroll to find the one they want.
     */
    Rail("segmented--rail"),
}

/**
 * One choice in a [SegmentedControl].
 *
 * [complete] marks a choice the user has finished with, drawn as a check beside the
 * label. It is a separate fact from which choice is selected: the chosen chip can
 * also be a finished one, and an unchosen chip can carry the mark. On the chosen chip
 * the check takes that chip's text colour rather than its own, because the two
 * treatments fill the chosen chip differently and a fixed colour would be invisible
 * against one of them.
 */
public class Segment<out T>(
    public val value: T,
    public val label: String,
    public val complete: Boolean = false,
)

/**
 * Pick exactly one of a set.
 *
 * A real radio group, not a row of buttons. That is the load-bearing decision here:
 * a set of buttons carrying `role="radio"` promises arrow-key navigation and a
 * single tab stop for the whole group, and delivers neither unless somebody
 * reimplements roving tabindex by hand. Native radios sharing a name give both away
 * for nothing, along with grouping, Home and End, and a correct announcement of
 * "two of five".
 *
 * The selected chip's colour is keyed off `:checked` in CSS rather than off a class
 * this function adds. Same reason as the switch: `:checked` is what the browser and
 * assistive technology report, so keying the appearance off it means the two cannot
 * disagree. A class can, and it fails in the direction that matters - looking right
 * while announcing wrong.
 *
 * [style] chooses between the two treatments; see [SegmentedStyle]. [fill] gives
 * every choice an equal share of the width instead of only the room its word needs,
 * which is usually what a `Track` in a fixed-width panel wants. It does nothing on a
 * `Rail`, where the chips are the scrolling content.
 *
 * [ariaLabel] names the group - "View", "Season", "Language". Required: the choices
 * announce themselves, but without this there is nothing to say what they are
 * choices *of*.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun <T> SegmentedControl(
    segments: List<Segment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    ariaLabel: String,
    style: SegmentedStyle = SegmentedStyle.Track,
    fill: Boolean = false,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
) {
    // Radios group by `name`, so two controls on one page sharing a name would fight
    // over one selection. Generated rather than asked for: the name is never seen and
    // never submitted here, so making the caller invent a unique string would be
    // asking them to solve a problem that is ours.
    val groupName = remember { "keel-segmented-${nextGroupId++}" }

    Div({
        classNames(segmentedClasses(style, fill))
        attr("role", "radiogroup")
        attr("aria-label", ariaLabel)
        attrs?.invoke(this)
    }) {
        segments.forEach { segment ->
            Label(attrs = { classNames(segmentedItemClasses()) }) {
                RadioInput(checked = segment.value == selected) {
                    classNames(segmentedInputClasses())
                    name(groupName)
                    onChange { onSelect(segment.value) }
                }

                Span({ classNames(segmentedLabelClasses()) }) {
                    Text(segment.label)

                    if (segment.complete) {
                        Span({ classNames(segmentedCompleteClasses()) }) {
                            Icon(LucideIcon.Check, size = COMPLETE_ICON)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Distinguishes one control's radios from another's.
 *
 * Module-level rather than derived from anything, because nothing about a control's
 * inputs is unique: two identical language switchers on one page are a legitimate
 * thing to render, and they must not share a selection.
 */
private var nextGroupId = 0

private const val COMPLETE_ICON = 11

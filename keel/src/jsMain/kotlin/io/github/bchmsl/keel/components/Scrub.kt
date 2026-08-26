package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.geometry.percentOf
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.max
import org.jetbrains.compose.web.attributes.min
import org.jetbrains.compose.web.attributes.step
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import kotlin.math.roundToInt

/**
 * Writes a [Scrub]'s position, without recomposing and without fighting the pointer.
 *
 * Handed to the caller once, by `onHandleReady`. A scrub bar has no `value`
 * parameter at all, unlike [ProgressBar] which has both: a timeline is *always*
 * driven from a frame loop, so a value parameter would be a second way to do the
 * same thing that nobody would use.
 *
 * [setPosition] does nothing while a drag is in progress. That is the whole reason
 * this is a class and not a raw element: the frame loop and the user's finger are
 * two sources for one position, and without the guard the loop drags the knob back
 * to the video's current time on every frame while the user is still moving it. Both
 * consuming players had to learn this separately.
 */
public class ScrubHandle internal constructor(private val root: HTMLElement) {
    /** Set by [Scrub]'s own pointer listeners. See [setPosition]. */
    internal var scrubbing: Boolean = false

    private val position: HTMLElement? by lazy { find(POSITION_CLASS) }
    private val buffered: HTMLElement? by lazy { find(BUFFERED_CLASS) }
    private val knob: HTMLElement? by lazy { find(KNOB_CLASS) }
    private val input: HTMLInputElement? by lazy { find(INPUT_CLASS) as? HTMLInputElement }

    /**
     * Moves the played fill and the knob. Ignored mid-drag.
     *
     * [valueText] is what assistive technology announces in place of the raw step
     * number, which on its own is a count out of a thousand and means nothing. Pass
     * the elapsed time the way a person would read it.
     *
     * The invisible range input's own value is written here too. It is the element
     * the keyboard and the accessibility tree actually address, so leaving it behind
     * makes arrow-key seeking jump from a stale position and makes assistive
     * technology report one time while the bar shows another.
     */
    public fun setPosition(fraction: Double, valueText: String? = null) {
        if (scrubbing) return
        paint(fraction)
        input?.let { field ->
            field.value = (percentOf(fraction) / PERCENT * STEPS).roundToInt().toString()
            valueText?.let { field.setAttribute("aria-valuetext", it) }
        }
    }

    /**
     * Moves the buffered fill.
     *
     * Not guarded by the drag, unlike [setPosition]: how much has downloaded is
     * independent of where the user is pointing, and freezing it during a drag would
     * hide the one thing they are usually dragging to find out.
     */
    public fun setBuffered(fraction: Double) {
        buffered?.style?.width = "${percentOf(fraction)}%"
    }

    /** The visible half of [setPosition], also used while the user drags. */
    internal fun paint(fraction: Double) {
        val percent = percentOf(fraction)
        position?.style?.width = "$percent%"
        knob?.style?.left = "$percent%"
    }

    private fun find(className: String): HTMLElement? =
        root.querySelector(".$className") as? HTMLElement

    internal companion object {
        const val POSITION_CLASS: String = "scrub__position"
        const val BUFFERED_CLASS: String = "scrub__buffered"
        const val KNOB_CLASS: String = "scrub__knob"
        const val INPUT_CLASS: String = "scrub__input"

        /**
         * The range input's resolution.
         *
         * A thousand steps rather than a hundred, so one arrow-key press moves a
         * tenth of a percent instead of a whole one. On a forty-minute episode that
         * is two seconds rather than twenty-four.
         */
        const val STEPS: Int = 1000
        const val PERCENT: Double = 100.0
    }
}

/**
 * A media timeline: draggable, with a second layer showing what has downloaded.
 *
 * Separate from [Slider] and from [ProgressBar], and the reasons are worth stating
 * because folding it into either looks tempting. Against `Slider`: it needs the
 * buffered layer, which no slider has, and its position comes from a frame loop
 * rather than from state - so `Slider` would have to leak an element handle it was
 * designed not to, or force a recomposition per frame. Against `ProgressBar`: a
 * progress bar is not draggable, and adding a knob and a seek callback there would
 * make every read-only bar in both apps carry both.
 *
 * The bar you see is decoration. The control is a real `input[type=range]` covering
 * it at zero opacity, which brings keyboard stepping, touch dragging and a correct
 * accessibility tree for free - none of which is reimplementable by hand without
 * getting one of the three wrong.
 *
 * [onSeek] receives a `0.0..1.0` fraction of the duration, not a time: the component
 * knows where along itself the user let go and nothing else. It fires continuously
 * during a drag, which is what makes the picture follow the finger; a caller that
 * wants only the final position should watch its own media element instead of asking
 * this to buffer.
 *
 * [onHandleReady] is required, not optional - a scrub bar that nobody paints stays at
 * zero for ever, so there is no sensible call without it.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun Scrub(
    ariaLabel: String,
    onSeek: (Double) -> Unit,
    onHandleReady: (ScrubHandle) -> Unit,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
) {
    // Not state: the handle changing must not recompose anything, and the pointer
    // listeners below need to reach the same instance the caller was given.
    val holder = remember { ScrubHolder() }

    Div({
        classNames("scrub")
        ref { element ->
            val handle = ScrubHandle(element)
            holder.handle = handle
            onHandleReady(handle)
            onDispose { holder.handle = null }
        }
        attrs?.invoke(this)
    }) {
        Div({ classNames("scrub__track") }) {
            Div({ classNames(ScrubHandle.BUFFERED_CLASS) })
            Div({ classNames(ScrubHandle.POSITION_CLASS) })
        }

        // Before the knob in document order, because the focus rule reaches the knob
        // as a following sibling - the input is invisible, so its own ring would be
        // too. The knob is above it and `pointer-events: none`, so the input still
        // receives every press.
        Input(InputType.Range) {
            classNames(ScrubHandle.INPUT_CLASS)
            min("0")
            max(ScrubHandle.STEPS.toString())
            step(1.0)
            attr("aria-label", ariaLabel)
            onInput { event ->
                val steps = event.value?.toString()?.toDoubleOrNull() ?: return@onInput
                val fraction = steps / ScrubHandle.STEPS
                // Painted here as well as reported, so the bar follows the finger
                // even though the frame loop is being ignored mid-drag.
                holder.handle?.paint(fraction)
                onSeek(fraction)
            }
            addEventListener("pointerdown") { holder.handle?.scrubbing = true }
            addEventListener("pointerup") { holder.handle?.scrubbing = false }
            addEventListener("pointercancel") { holder.handle?.scrubbing = false }
        }

        Div({ classNames(ScrubHandle.KNOB_CLASS) })
    }
}

/** Holds the handle across recompositions without being state. */
private class ScrubHolder {
    var handle: ScrubHandle? = null
}

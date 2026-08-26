package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.geometry.percentOf
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import kotlin.math.roundToInt

/** The thicknesses. */
public enum class ProgressBarSize(internal val className: String?) {
    /** A hairline, for a bar riding the bottom edge of something else. */
    Small("progress--sm"),
    Default(null),

    /** A bar that is the point of its own row. */
    Large("progress--lg"),
}

/**
 * Writes a [ProgressBar]'s position without recomposing.
 *
 * Handed to the caller once, by `onHandleReady`. It exists for one case: a value
 * that changes faster than composition should run. A bar following a media element
 * is updated from a `requestAnimationFrame` loop, and recomposing sixty times a
 * second to move one element is what a player is careful not to do.
 *
 * A caller using this passes a constant `fraction` and drives the bar from here.
 * Everything else should pass a real `fraction` and let recomposition do it, which
 * is simpler and cannot go stale.
 *
 * [setFraction] moves the fill *and* rewrites `aria-valuenow`, so the two cannot
 * disagree - which is the mistake a caller writing `style.width` itself makes every
 * time, because only one of the two is visible.
 */
public class ProgressHandle internal constructor(private val root: HTMLElement) {
    /**
     * Looked up once and kept.
     *
     * keel builds this subtree, so exactly one element carries the class; the query
     * is by class rather than by position so that adding an element to the markup
     * later cannot silently retarget it.
     */
    private val fill: HTMLElement? by lazy {
        root.querySelector(".$FILL_CLASS") as? HTMLElement
    }

    /** [fraction] is clamped to `0.0..1.0`, so a duration of zero cannot produce NaN%. */
    public fun setFraction(fraction: Double) {
        val percent = percentOf(fraction)
        fill?.style?.width = "$percent%"
        root.setAttribute("aria-valuenow", percent.roundToInt().toString())
    }

    internal companion object {
        const val FILL_CLASS: String = "progress__fill"
    }
}

/**
 * A determinate bar: how much of something is done.
 *
 * [fraction] is `0.0..1.0` rather than a value and a maximum. A fraction makes the
 * ARIA range always 0-100, so the announcement is a percentage in every case; a
 * `value`/`max` pair invites passing a duration in seconds as the maximum, which
 * then announces "417 out of 2680" and needs `aria-valuetext` to mean anything.
 *
 * [ariaLabel] is required. A bar has no text of its own, so without one it announces
 * as a percentage of nothing.
 *
 * [onMedia] switches to the treatment for a bar lying over a poster or over video,
 * where the page's own colours say nothing about what is behind it.
 *
 * [done] recolours the fill so that full and *finished* do not look identical.
 *
 * [onHandleReady] is the escape for a caller driving the bar imperatively; see
 * [ProgressHandle]. Passing it does not change what is rendered.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun ProgressBar(
    fraction: Double,
    ariaLabel: String,
    size: ProgressBarSize = ProgressBarSize.Default,
    onMedia: Boolean = false,
    done: Boolean = false,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
    onHandleReady: ((ProgressHandle) -> Unit)? = null,
) {
    val percent = percentOf(fraction)

    Div({
        classNames(
            "progress",
            size.className,
            "progress--on-media".takeIf { onMedia },
            "progress--done".takeIf { done },
        )
        attr("role", "progressbar")
        attr("aria-label", ariaLabel)
        attr("aria-valuemin", "0")
        attr("aria-valuemax", "100")
        attr("aria-valuenow", percent.roundToInt().toString())
        onHandleReady?.let { ready ->
            ref { element ->
                ready(ProgressHandle(element))
                onDispose {}
            }
        }
        attrs?.invoke(this)
    }) {
        Div({
            classNames(ProgressHandle.FILL_CLASS)
            style { property("width", "$percent%") }
        })
    }
}

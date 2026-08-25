package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/**
 * Calls [onDismiss] when Escape is pressed, for as long as this is composed.
 *
 * A window listener rather than a handler on the dialog, and it has to be: Escape
 * only reaches an element that has focus, and a modal opened by a button leaves the
 * focus on the button. A key handler on the dialog would work exactly until someone
 * opened it without clicking into it, which is every time.
 *
 * [rememberUpdatedState] is what makes the listener safe to register once. Without
 * it the captured lambda would be the first one for the element's whole life, so a
 * dialog whose dismiss action changed - a wizard moving between steps - would keep
 * running the action from the step it opened on.
 *
 * A handler nearer the key can win by calling `stopPropagation`, which is the
 * behaviour a nested control wants: Escape inside an inline editor should abandon
 * that edit rather than throwing away the whole dialog around it.
 */
@Composable
public fun DismissOnEscape(onDismiss: () -> Unit) {
    val current = rememberUpdatedState(onDismiss)

    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { event ->
            if ((event as? KeyboardEvent)?.key == ESCAPE) {
                // Stops a browser doing anything else with it - leaving full screen,
                // or cancelling a native picker that is not what the user meant.
                event.preventDefault()
                current.value()
            }
        }

        window.addEventListener("keydown", listener)
        onDispose { window.removeEventListener("keydown", listener) }
    }
}

private const val ESCAPE = "Escape"

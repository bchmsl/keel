package io.github.bchmsl.keel.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.bchmsl.keel.components.Button
import io.github.bchmsl.keel.components.ButtonSize
import io.github.bchmsl.keel.components.ButtonVariant
import io.github.bchmsl.keel.components.Dialog
import io.github.bchmsl.keel.dom.classNames
import io.github.bchmsl.keel.theme.Theme
import io.github.bchmsl.keel.theme.ThemeController
import io.github.bchmsl.keel.theme.bootScript
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Button as HtmlButton

/**
 * The whole page.
 *
 * One section per primitive, each showing every state that primitive has rather than
 * a flattering example of it - the disabled button, the collapsed panel, the faintest
 * pill. A gallery that shows only the good cases is a screenshot, not a reference.
 */
@Composable
fun Gallery(theme: ThemeController) {
    var dialogOpen by remember { mutableStateOf(false) }

    Div({ classNames("page") }) {
        Masthead(theme)

        PaletteSection()
        ButtonSection()
        CardSection()
        FieldSection()
        PillSection()

        Section(
            title = "Dialog",
            note = "A modal with a scrim. Its title and description are read by " +
                "assistive technology and never painted, so the content can carry its " +
                "own visible heading without saying everything twice. Focus moves in " +
                "on open and returns to this button on close; Escape, the scrim and " +
                "the close button all dismiss it.",
        ) {
            Div({ classNames("row") }) {
                Button(label = "Open dialog", onClick = { dialogOpen = true })
            }
        }

        TextSection()
        IconSection()
        BootScriptSection(theme)
    }

    if (dialogOpen) {
        ExampleDialog(onDismiss = { dialogOpen = false })
    }
}

@Composable
private fun ExampleDialog(onDismiss: () -> Unit) {
    Dialog(
        title = "An example dialog",
        description = "Shows the scrim, the close button and the content padding.",
        onDismiss = onDismiss,
    ) {
        H2({ classNames("section__title") }) { Text("An example dialog") }
        P({ classNames("section__note") }) {
            Text(
                "Try Escape, and try Tab: focus started here rather than back at the " +
                    "top of the page. A control inside a dialog can still keep Escape " +
                    "for itself by stopping the event propagating.",
            )
        }
        Div({ classNames("row") }) {
            Button(label = "Cancel", onClick = onDismiss, variant = ButtonVariant.Outline)
            Button(label = "Confirm", onClick = onDismiss)
        }
    }
}

@Composable
private fun Masthead(theme: ThemeController) {
    Div({ classNames("masthead") }) {
        Div {
            H1({ classNames("masthead__name") }) { Text("keel") }
            P({ classNames("masthead__tagline") }) {
                Text(
                    "A Compose HTML design system: themeable tokens, primitives and " +
                        "lucide icons. Every control below is the library's own, drawn " +
                        "from whichever palette you pick.",
                )
            }
        }

        Div({ classNames("masthead__controls") }) {
            if (theme.catalog.hasChoice) {
                Div({ classNames("control-row") }) {
                    Span({ classNames("control-row__label") }) { Text("Theme") }
                    theme.catalog.themes.forEach { entry ->
                        Swatch(
                            entry = entry,
                            active = entry == theme.theme,
                            onSelect = { theme.setTheme(entry) },
                        )
                    }
                }
            }

            // Only where the active theme has a choice to make. A dark-only palette
            // shows nothing here rather than buttons that cannot change anything.
            if (theme.availableColorModes.size > 1) {
                Div({ classNames("control-row") }) {
                    Span({ classNames("control-row__label") }) { Text("Mode") }
                    theme.availableColorModes.forEach { mode ->
                        Button(
                            label = mode.label,
                            onClick = { theme.setColorMode(mode) },
                            variant = if (mode == theme.colorMode) {
                                ButtonVariant.Default
                            } else {
                                ButtonVariant.Outline
                            },
                            size = ButtonSize.Small,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One theme swatch.
 *
 * The colour comes from `Theme.accentHex` and is set inline, which is exactly why
 * that field is a plain hex rather than a token: all six are painted at once while
 * only one palette's variables are live.
 */
@Composable
private fun Swatch(entry: Theme, active: Boolean, onSelect: () -> Unit) {
    HtmlButton({
        classNames("swatch", "swatch--active".takeIf { active })
        attr("type", "button")
        attr("aria-label", entry.label)
        attr("aria-pressed", active.toString())
        attr("title", entry.label)
        style { property("background-color", entry.accentHex) }
        onClick { onSelect() }
    })
}

/** A titled block with a note under it. Page furniture, not a library primitive. */
@Composable
internal fun Section(title: String, note: String, content: @Composable () -> Unit) {
    Div({ classNames("section") }) {
        H2({ classNames("section__title") }) { Text(title) }
        P({ classNames("section__note") }) { Text(note) }
        content()
    }
}

@Composable
private fun BootScriptSection(theme: ThemeController) {
    Section(
        title = "The boot script",
        note = "Inline this in <head>, before the bundle. Without it every reload " +
            "paints one frame of the default palette before Kotlin runs, which is a " +
            "white flash for anyone in dark mode. It is generated from the catalogue " +
            "in use, so the keys in the page and the keys in Kotlin cannot drift.",
    ) {
        Pre({ classNames("snippet") }) {
            Text(bootScript(theme.catalog))
        }
    }
}

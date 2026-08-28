package io.github.bchmsl.keel.components

import androidx.compose.runtime.Composable
import io.github.bchmsl.keel.dom.classNames
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement

/**
 * Nothing here, and what to do about it.
 *
 * [title] says what is missing and is required, because a placeholder that does not
 * say what would have been here is indistinguishable from a screen that failed to
 * load. [body] is the sentence explaining why, for the cases where the title alone
 * leaves a question.
 *
 * [action] is a slot rather than a label and a callback: what belongs there is a
 * [Button] in one app, a [LinkButton] in another, and two of them in a third. The
 * component's job is where it sits.
 *
 * The border is dashed rather than solid, so an empty region does not read as a panel
 * whose content failed to paint.
 *
 * This is for a region that is legitimately empty, not for one that is still loading.
 * Use [Skeleton] while content is on its way - showing "No episodes yet" during a
 * fetch tells the user something false.
 *
 * [attrs] runs last; see [Button].
 */
@Composable
public fun EmptyState(
    title: String,
    body: String? = null,
    attrs: (AttrsScope<HTMLDivElement>.() -> Unit)? = null,
    leading: ContentBuilder<HTMLDivElement>? = null,
    action: ContentBuilder<HTMLDivElement>? = null,
) {
    Div({
        classNames("empty-state")
        attrs?.invoke(this)
    }) {
        leading?.invoke(this)
        Span({ classNames("empty-state__title") }) { Text(title) }
        body?.let { P({ classNames("empty-state__body") }) { Text(it) } }
        action?.invoke(this)
    }
}

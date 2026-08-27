package io.github.bchmsl.keel.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Holds the class names keel's Kotlin emits against the classes its CSS defines.
 *
 * This is the alarm that was missing when a shared sign-in screen was changed to emit
 * keel's `btn` classes and one of the two shells rendering it did not link keel's
 * stylesheet. Nothing failed: a class name is a string, an undefined class selects
 * nothing, and an element with no rules is a perfectly valid element. It compiled,
 * the console stayed clean, and the screen shipped as unstyled browser boxes.
 *
 * A JVM test cannot call the JS target, so this reads both halves as text. That is
 * narrower than invoking `buttonClasses()` and comparing, and it is the right width
 * for the failure: on both sides the thing that can be wrong is a literal.
 *
 * Two rules, and between them they cover every shape a class literal takes here:
 *
 *   - anything passed to `classNames(...)` or joined by `joined(...)`. Catches the
 *     bare element classes - `btn`, `input`, `card` - wherever a component states
 *     them.
 *   - any string anywhere in jsMain shaped like a modifier or an element part, which
 *     is to say containing `--` or `__`. Catches the enum entries, where the literal
 *     sits in a constructor call and not in a `classNames` argument list. Nothing
 *     else in this codebase is spelled that way, so the shape is a reliable tell.
 *
 * The reverse direction is deliberately not checked. A class keel defines but never
 * emits is not necessarily dead: `dialog__overlay` and the `switch` pair exist partly
 * so a consumer that cannot call the composable can still build the markup.
 */
class ClassNameContractTest {

    private val cssDir = File(
        System.getProperty("keel.css.dir")
            ?: fail("keel.css.dir is not set; see the systemProperty in keel/build.gradle.kts"),
    )

    private val jsSrcDir = File(
        System.getProperty("keel.js.src.dir")
            ?: fail("keel.js.src.dir is not set; see the systemProperty in keel/build.gradle.kts"),
    )

    /** Every class any of keel's own sheets defines a rule for. */
    private val defined: Set<String> = STYLESHEETS
        .flatMap { name ->
            val css = File(cssDir, name)
                .takeIf { it.isFile }
                ?.readText()
                ?: fail("$name is missing from ${cssDir.absolutePath}")

            CLASS_SELECTOR.findAll(css.withoutComments()).map { it.groupValues[1] }
        }
        .toSet()

    private val sources: List<Pair<String, String>> = jsSrcDir.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .map { it.relativeTo(jsSrcDir).path to it.readText() }
        .toList()

    @Test
    fun sourcesWereActuallyFound() {
        // Without this the two checks below pass by reading nothing, which is the
        // failure mode the system properties exist to prevent in the first place.
        assertTrue(sources.isNotEmpty(), "no .kt files under ${jsSrcDir.absolutePath}")
        assertTrue(
            defined.size > MIN_EXPECTED_CLASSES,
            "only ${defined.size} classes found in the stylesheets",
        )
    }

    @Test
    fun everyClassPassedToClassNamesIsDefinedInCss() {
        val undefined = sources.flatMap { (path, text) ->
            val body = text.withoutComments()

            classListArguments(body)
                .flatMap { args -> STRING_LITERAL.findAll(args) }
                .map { it.groupValues[1] }
                .filter { it.isNotBlank() && it !in defined && it !in CONSUMER_STYLED_HOOKS }
                .map { "$path: \"$it\"" }
        }

        assertTrue(
            undefined.isEmpty(),
            "class names emitted by Kotlin with no rule in keel's stylesheets: $undefined",
        )
    }

    @Test
    fun everyModifierLiteralIsDefinedInCss() {
        val undefined = sources.flatMap { (path, text) ->
            STRING_LITERAL.findAll(text.withoutComments())
                .map { it.groupValues[1] }
                .filter {
                    MODIFIER_SHAPE.matches(it) &&
                        it !in defined &&
                        it !in CONSUMER_STYLED_HOOKS
                }
                .map { "$path: \"$it\"" }
        }

        assertTrue(
            undefined.isEmpty(),
            "modifier class names with no rule in keel's stylesheets: $undefined",
        )
    }

    private companion object {
        /**
         * `palettes.css` is not here on purpose: it declares custom properties on
         * `[data-theme]` blocks and defines no component classes, so including it
         * would widen the allowed set without adding a single real rule.
         */
        val STYLESHEETS = listOf("base.css", "components.css")

        /**
         * Classes keel emits for a consumer to style, and therefore defines no rule
         * for itself.
         *
         * **Empty, and worth keeping empty.** It held `card--expanded` on the argument
         * that keel owns components rather than layout, so the size of an expanded card
         * was the app's to pick. That was half right and the wrong half mattered. Where
         * the card *sits* is the app's; that it becomes a column filling that space with
         * its header pinned and its body scrolling is the component's, and it is the only
         * shape a full-screen card can have - without it a long body carries the close
         * button off the top of the screen. So the class has a rule now, and the one
         * consumer of `Card` stopped writing those four lines against keel's own
         * selector from its own sheet.
         *
         * Adding to this list should feel expensive. Every entry is a class name with
         * no definition anywhere in this repository, which is exactly the shape of the
         * bug these tests exist to catch. An entry has to be a hook a consumer is
         * *meant* to reach, not a class that simply has no rule yet - and the one entry
         * that ever qualified turned out not to.
         */
        val CONSUMER_STYLED_HOOKS = emptySet<String>()

        /**
         * A class in a selector position. The leading `(?<![\w.-])` is what keeps
         * `.card__title` from also registering the fragment `title`, and stops a
         * decimal such as `0.5rem` from contributing `5rem`.
         */
        val CLASS_SELECTOR = Regex("(?<![\\w.-])\\.(-?[A-Za-z_][\\w-]*)")

        /** The opening of a `classNames(` or `joined(` call. See [classListArguments]. */
        val CLASS_LIST_CALL = Regex("\\b(?:classNames|joined)\\(")

        /**
         * The argument text of every `classNames(...)` / `joined(...)` call, found by
         * counting depth rather than by matching a regex.
         *
         * A regex stopping at the first `)` was the first attempt and it was quietly
         * useless: the arguments routinely contain calls of their own now, as in
         * `classNames(buttonClasses(variant, size))`, and `[^()]*` cannot span those.
         * Every such call was skipped, so the check reported a pass over almost
         * nothing. Caught by injecting a bad class name into one and watching this
         * test stay green while its sibling failed.
         */
        fun classListArguments(text: String): List<String> {
            val found = mutableListOf<String>()

            for (call in CLASS_LIST_CALL.findAll(text)) {
                val start = call.range.last + 1
                var depth = 1
                var i = start

                while (i < text.length && depth > 0) {
                    when (text[i]) {
                        '(' -> depth++
                        ')' -> depth--
                    }
                    i++
                }

                // An unbalanced tail means the file does not compile, which is not
                // this test's business to report.
                if (depth == 0) found += text.substring(start, i - 1)
            }

            return found
        }

        /** A Kotlin string literal with no escapes or interpolation, which is all of these. */
        val STRING_LITERAL = Regex("\"([^\"\\\\$\\n]*)\"")

        /**
         * The BEM shapes: `btn--default`, `switch__knob`, `btn--size-icon`. Anchored,
         * so a sentence in a KDoc that happens to contain `--` cannot match.
         */
        val MODIFIER_SHAPE = Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*(?:__|--)[a-z0-9]+(?:-[a-z0-9]+)*")

        /** A floor, not a count: this only has to prove the sheets were read at all. */
        const val MIN_EXPECTED_CLASSES = 20

        val BLOCK_COMMENT = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
        val LINE_COMMENT = Regex("//[^\\n]*")

        /**
         * Comments are stripped from both halves before matching. keel's sources
         * document themselves heavily and those comments name classes and show
         * example calls - `classNames(*sized("card", expanded))` is in ClassNames.kt
         * right now - so without this the tests would report prose as production code.
         */
        fun String.withoutComments(): String =
            replace(BLOCK_COMMENT, " ").replace(LINE_COMMENT, " ")
    }
}

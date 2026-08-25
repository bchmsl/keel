package io.github.bchmsl.keel.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Holds the Kotlin half of the design system against the CSS half.
 *
 * Everything these check is a real failure mode with no other alarm attached to it. A
 * palette with no CSS block paints the fallback while reporting its own name. A token
 * read but never defined resolves to the property's initial value, so the rule using
 * it renders as though it had been written wrong rather than erroring. Neither shows
 * up in a compiler, a browser console, or a screenshot of the default theme.
 *
 * On the JVM rather than in `commonTest` because it reads files, and the JVM target
 * exists here anyway. The path arrives as a system property from the build file.
 */
class TokenContractTest {

    private val cssDir = File(
        System.getProperty("keel.css.dir")
            ?: fail("keel.css.dir is not set; see the systemProperty in keel/build.gradle.kts"),
    )

    private val tokens = read("tokens.css")
    private val palettes = read("palettes.css")
    private val base = read("base.css")
    private val components = read("components.css")

    /** What a component sees: the contract and a palette sheet, linked together. */
    private val linked = tokens + palettes

    // -------------------------------------------------- themes against CSS

    @Test
    fun everyShippedThemeHasAPaletteBlock() {
        val inCss = THEME_SELECTOR.findAll(palettes).map { it.groupValues[1] }.toSet()
        val inKotlin = KeelThemes.All.map { it.id }.toSet()

        assertEquals(
            inKotlin,
            inCss,
            "the palettes in palettes.css and the themes in KeelThemes must be the same set",
        )
    }

    @Test
    fun everyShippedThemeHasADarkBlock() {
        // Each of the six declares `supportsDark`, and this is what makes that true.
        KeelThemes.All.forEach { theme ->
            assertTrue(
                "[data-theme='${theme.id}'].dark" in palettes,
                "${theme.id} claims a dark form but palettes.css has no dark block for it",
            )
        }
    }

    @Test
    fun theDefaultThemeIsAlsoDeclaredOnBareRoot() {
        // This is what makes an unknown or missing `data-theme` paint a complete
        // palette instead of leaving the page unstyled.
        val default = KeelThemes.Standard.default.id
        assertTrue(
            Regex(":root,\\s*\\[data-theme='$default']").containsMatchIn(palettes),
            "the default palette '$default' must also be declared on bare :root",
        )
    }

    // --------------------------------------------------- the token contract

    @Test
    fun everyTokenReadIsAlsoDefined() {
        val defined = DEFINITION.findAll(linked).map { it.groupValues[1] }.toSet()

        val missing = listOf(
            "base.css" to base,
            "components.css" to components,
            "tokens.css" to tokens,
            "palettes.css" to palettes,
        )
            .flatMap { (name, css) -> USAGE.findAll(css).map { name to it.groupValues[1] } }
            .filter { (_, token) -> token !in defined }
            .distinct()

        assertTrue(missing.isEmpty(), "tokens read but never defined: $missing")
    }

    @Test
    fun everyPaletteDefinesTheSameColours() {
        // A palette missing one colour inherits whichever palette was declared last,
        // which in practice means it inherits the default's - so the page paints, and
        // one colour is quietly wrong.
        val blocks = paletteBlocks()
        val expected = blocks.getValue(":root, [data-theme='coral']")

        blocks.forEach { (selector, declared) ->
            assertEquals(
                expected - OPTIONAL_PER_PALETTE,
                declared - OPTIONAL_PER_PALETTE,
                "$selector does not declare the same colours as the default palette",
            )
        }
    }

    @Test
    fun noComponentRuleHardcodesAColour() {
        // The whole contract is that a component names a token. A literal colour is a
        // rule that one theme will get wrong, and it will be the theme nobody tested.
        val offenders = listOf("base.css" to base, "components.css" to components)
            .flatMap { (name, css) -> literalColourLines(name, css) }
            .filterNot { (_, line) -> ALLOWED_LITERAL_COLOURS.any { it in line } }

        assertTrue(offenders.isEmpty(), "literal colours outside tokens.css: $offenders")
    }

    // ------------------------------------------------------------- reading

    /**
     * The stylesheet with its comments blanked out.
     *
     * Necessary, not tidiness: these files document themselves with worked examples,
     * and a comment showing what a palette block looks like is otherwise read as a
     * palette. The newlines are kept so a reported line still matches the file.
     */
    private fun read(name: String): String {
        val file = File(cssDir, name)
        assertTrue(file.isFile, "expected a stylesheet at ${file.absolutePath}")

        return COMMENT.replace(file.readText()) { match ->
            match.value.count { it == '\n' }.let { "\n".repeat(it) }
        }
    }

    /**
     * The colour names each palette block declares, keyed by a readable form of its
     * selector. Every block in `palettes.css` naming a `data-theme`, which excludes
     * nothing there today but would exclude a future global block added to that file.
     */
    private fun paletteBlocks(): Map<String, Set<String>> = BLOCK.findAll(palettes)
        .map { it.groupValues[1].trim() to it.groupValues[2] }
        .filter { (selector, _) -> "data-theme" in selector }
        .associate { (selector, body) ->
            selector.replace(Regex("\\s+"), " ") to
                DEFINITION.findAll(body).map { it.groupValues[1] }.toSet()
        }

    private fun literalColourLines(file: String, css: String): List<Pair<String, String>> =
        css.lineSequence()
            .filter { LITERAL_COLOUR.containsMatchIn(it) }
            .map { file to it.trim() }
            .toList()

    private companion object {
        val THEME_SELECTOR = Regex("\\[data-theme='([a-z-]+)']")
        val DEFINITION = Regex("(--[a-z-]+)\\s*:")
        val USAGE = Regex("var\\((--[a-z-]+)")
        val BLOCK = Regex("([^{}]+)\\{([^{}]*)}")
        val COMMENT = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)

        /** A colour written out rather than named. */
        val LITERAL_COLOUR = Regex("#[0-9a-fA-F]{3,8}\\b|\\brgba?\\(|\\bhsla?\\((?!var)")

        /**
         * Declared by some palettes and not others, legitimately.
         *
         * `--secondary-foreground` has a global default that the two palettes with a
         * pale secondary override, and `--primary` differs between the modes of one
         * palette only. Both are deliberate, and both are explained in tokens.css.
         */
        val OPTIONAL_PER_PALETTE = setOf("--secondary-foreground")

        /**
         * The only literal colour allowed outside tokens.css.
         *
         * Black at low alpha, for the modal scrim and the shadow values. Neither is a
         * theme decision - a scrim dims whatever is behind it, and a shadow is the
         * absence of light - and a token for either would let a palette break them.
         */
        val ALLOWED_LITERAL_COLOURS = listOf("rgb(0 0 0 /")
    }
}

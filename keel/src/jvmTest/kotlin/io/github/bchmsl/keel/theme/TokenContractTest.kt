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
    fun noComponentRuleHardcodesAColour() {
        // The whole contract is that a component names a token. A literal colour is a
        // rule that one theme will get wrong, and it will be the theme nobody tested.
        val offenders = listOf("base.css" to base, "components.css" to components)
            .flatMap { (name, css) -> literalColourLines(name, css) }
            .filterNot { (_, line) -> ALLOWED_LITERAL_COLOURS.any { it in line } }

        assertTrue(offenders.isEmpty(), "literal colours outside tokens.css: $offenders")
    }

    // ------------------------------------------ the list against the CSS

    @Test
    fun everyPaletteDeclaresExactlyTheContract() {
        // Stronger than the palettes merely agreeing with each other: it holds them
        // against the list `KeelTokens` publishes, so a colour quietly added to all
        // twelve blocks and never declared as API still fails.
        val expected = KeelTokens.PerPalette.toSet()

        paletteBlocks().forEach { (selector, declared) ->
            assertEquals(
                expected,
                declared - OPTIONAL_PER_PALETTE,
                "$selector does not declare exactly the per-palette contract",
            )
        }
    }

    @Test
    fun everyGlobalAndDerivedTokenIsDeclaredInTheContractSheet() {
        val declared = DEFINITION.findAll(tokens).map { it.groupValues[1] }.toSet()

        val missing = (KeelTokens.Global + KeelTokens.Derived).filterNot { it in declared }
        assertTrue(missing.isEmpty(), "listed as API but not declared in tokens.css: $missing")
    }

    @Test
    fun theContractSheetDeclaresNoColourTheListHasMissed() {
        // The direction that was missing. Without it a colour could be added to
        // tokens.css and used by a component while never appearing in `KeelTokens`,
        // so an app reading the published list would not know it existed.
        val listed = KeelTokens.AllColors.toSet()

        val undeclared = DEFINITION.findAll(tokens)
            .map { it.groupValues[1] }
            .filter { it !in listed && it !in NON_COLOUR_TOKENS }
            .toList()

        assertTrue(
            undeclared.isEmpty(),
            "declared in tokens.css but absent from KeelTokens: $undeclared. " +
                "Add it to KeelTokens, or to NON_COLOUR_TOKENS if it is not a colour.",
        )
    }

    @Test
    fun tokensMustBeLinkedBeforeAPaletteSheet() {
        // `:root` and `[data-theme='...']` carry the same specificity, so a token
        // declared in both is settled by source order alone. That makes the link
        // order load-bearing for exactly the tokens in this overlap, and silent when
        // wrong - the palette simply keeps the global value.
        val globals = DEFINITION.findAll(tokens).map { it.groupValues[1] }.toSet()
        val overridden = paletteBlocks().values.flatten().toSet()

        val collisions = (globals intersect overridden).sorted()

        assertEquals(
            listOf("--secondary-foreground"),
            collisions,
            "the tokens whose value depends on tokens.css being linked first. If this " +
                "list has grown, the ordering note in base.css and ARCHITECTURE.md " +
                "needs to name the new ones too.",
        )
    }

    @Test
    fun everyListedColourResolvesToSomething() {
        // The gallery paints straight from this list. A name in it with no declaration
        // anywhere paints nothing at all, and nothing else would say so.
        val declared = DEFINITION.findAll(linked).map { it.groupValues[1] }.toSet()

        val missing = KeelTokens.AllColors.filterNot { it in declared }
        assertTrue(missing.isEmpty(), "listed but never declared: $missing")
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

        /**
         * A colour written out rather than named as a token.
         *
         * Covers hex, the functional notations including the modern wide-gamut ones,
         * and the CSS named colours a stylesheet actually reaches for. Not all 148
         * names: `orange` and `tomato` would false-positive on any rule mentioning
         * them in a comment, and comments are stripped but selectors are not.
         *
         * The `hsl(` case allows whitespace before `var`, or a legitimate
         * `hsl( var(--x) )` would be reported as a literal.
         */
        val LITERAL_COLOUR = Regex(
            "#[0-9a-fA-F]{3,8}\\b" +
                "|\\brgba?\\(" +
                "|\\bhsla?\\(\\s*(?!var)" +
                "|\\b(?:oklch|oklab|lab|lch|color|color-mix|device-cmyk)\\(" +
                "|(?<![-\\w])(?:white|black|red|green|blue|gray|grey|silver|navy|teal" +
                "|olive|maroon|aqua|fuchsia|lime|purple|yellow|orange)(?![-\\w])",
        )

        /**
         * Declared in tokens.css and deliberately not a colour.
         *
         * Listed rather than pattern-matched, so adding a token is a decision about
         * which half of the contract it belongs to rather than something that slips
         * past on a naming coincidence.
         */
        val NON_COLOUR_TOKENS = setOf(
            "--radius", "--radius-xs", "--radius-sm", "--radius-md", "--radius-lg",
            "--radius-xl", "--radius-pill",
            "--ease", "--duration-fast", "--duration-slow",
            "--font-sans", "--font-mono",
            "--shadow-sm", "--shadow-lg",
        )

        /**
         * Declared by some palettes and not others, legitimately.
         *
         * One entry. `--secondary-foreground` has a global default in tokens.css that
         * the two palettes with a pale secondary override with a dark ink, so it
         * appears in two of the twelve blocks and not the other ten.
         *
         * `--primary` is NOT here, and is worth the note: one palette gives it a
         * different value in its dark block, but every block declares it, so it is
         * part of the per-palette contract rather than an exception to it.
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

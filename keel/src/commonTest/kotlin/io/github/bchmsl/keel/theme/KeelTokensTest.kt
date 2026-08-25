package io.github.bchmsl.keel.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The shape of the token list.
 *
 * What the list means - that every name in it is really declared in the stylesheets,
 * and that the stylesheets declare nothing it has missed - is `TokenContractTest`,
 * which has to read files and so runs only on the JVM. This half runs everywhere and
 * checks the list is well-formed in the first place.
 */
class KeelTokensTest {

    @Test
    fun allColorsIsTheThreeListsInOrder() {
        assertEquals(
            KeelTokens.PerPalette + KeelTokens.Global + KeelTokens.Derived,
            KeelTokens.AllColors,
        )
    }

    @Test
    fun noTokenAppearsInTwoLists() {
        // The lists say where a token is declared, so a name in two of them is a
        // claim that cannot be true of both.
        val duplicates = KeelTokens.AllColors.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(duplicates.isEmpty(), "listed more than once: ${duplicates.sorted()}")
    }

    @Test
    fun everyNameIsAWellFormedCustomProperty() {
        val customProperty = Regex("^--[a-z]+(-[a-z]+)*$")
        KeelTokens.AllColors.forEach {
            assertTrue(customProperty.matches(it), "'$it' is not a usable custom-property name")
        }
    }

    @Test
    fun theListsAreNotEmpty() {
        // A test that reads a list against a stylesheet passes trivially if the list
        // is empty, so this is what stops the rest of the suite going quiet.
        assertTrue(KeelTokens.PerPalette.isNotEmpty())
        assertTrue(KeelTokens.Global.isNotEmpty())
        assertTrue(KeelTokens.Derived.isNotEmpty())
    }
}

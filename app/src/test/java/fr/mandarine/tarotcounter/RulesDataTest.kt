package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the Rules page tables (RulesData.kt, issue #203). */
class RulesDataTest {

    @Test
    fun `bout thresholds come from the scoring rules`() {
        assertEquals(listOf(0 to 56, 1 to 51, 2 to 41, 3 to 36), boutThresholdRows())
    }

    @Test
    fun `contract multipliers are listed weakest first`() {
        assertEquals(
            listOf(
                Contract.PRISE to "×1",
                Contract.GARDE to "×2",
                Contract.GARDE_SANS to "×4",
                Contract.GARDE_CONTRE to "×6"
            ),
            contractMultiplierRows()
        )
    }

    @Test
    fun `settings and rules strings are localized`() {
        val en = appStrings(AppLocale.EN)
        val fr = appStrings(AppLocale.FR)
        assertEquals("Appearance", en.appearanceLabel)
        assertEquals("Apparence", fr.appearanceLabel)
        assertEquals("Light", en.themeLight)
        assertEquals("Sombre", fr.themeDark)
        assertEquals("Points needed", en.rulesNeededColumn)
        assertEquals("Multiplicateur", fr.rulesMultiplierColumn)
        // Bodies no longer repeat the tables as bullet lists.
        assertTrue(!en.rulesObjectiveBody.contains("•"))
        assertTrue(!fr.rulesContractsBody.contains("•"))
    }
}

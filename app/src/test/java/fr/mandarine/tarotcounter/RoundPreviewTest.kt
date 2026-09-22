package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [previewRound] (GameModels.kt, issue #199): the live result of the
 * round-entry view, which is also the scoring path used by GameViewModel.recordPlayed.
 */
class RoundPreviewTest {

    private val three = listOf("Alice", "Bob", "Chloé")
    private val five  = listOf("Alice", "Bob", "Chloé", "David", "Eve")

    private fun details(
        bouts: Int,
        points: Int,
        partner: String? = null,
        petit: String? = null,
        poignees: List<String> = emptyList(),
        chelem: Chelem = Chelem.NONE
    ) = RoundDetails(
        bouts = bouts, points = points, partnerName = partner, petitAuBout = petit,
        poignees = poignees, chelem = chelem
    )

    @Test
    fun `made round - margin and taker collects from each defender`() {
        // Garde, 0 bouts → needs 56; 60 → made by 4; (25 + 4) × 2 = 58 per defender.
        val p = previewRound(three, "Alice", Contract.GARDE, details(0, 60))
        assertTrue(p.won)
        assertEquals(4, p.margin)
        assertEquals(mapOf("Alice" to 116, "Bob" to -58, "Chloé" to -58), p.playerScores)
    }

    @Test
    fun `short round - margin and taker pays each defender`() {
        val p = previewRound(three, "Alice", Contract.GARDE, details(0, 52))
        assertFalse(p.won)
        assertEquals(4, p.margin)
        assertEquals(mapOf("Alice" to -116, "Bob" to 58, "Chloé" to 58), p.playerScores)
    }

    @Test
    fun `exactly the required points is a win with margin zero`() {
        val p = previewRound(three, "Alice", Contract.PRISE, details(2, 41))
        assertTrue(p.won)
        assertEquals(0, p.margin)
        assertEquals(50, p.playerScores["Alice"])   // 25 × 1 × 2 defenders
    }

    @Test
    fun `five players - partner gets one share, taker two`() {
        // Garde, 2 bouts → needs 41; 50 → made by 9; (25 + 9) × 2 = 68.
        val p = previewRound(five, "Alice", Contract.GARDE, details(2, 50, partner = "Bob"))
        assertEquals(9, p.margin)
        assertEquals(136, p.playerScores["Alice"])
        assertEquals(68, p.playerScores["Bob"])
        assertEquals(-68, p.playerScores["Chloé"])
    }

    @Test
    fun `petit au bout by the taker is included`() {
        // +10 × 2 (Garde) = 20 from each of the 2 defenders.
        val p = previewRound(three, "Alice", Contract.GARDE, details(0, 60, petit = "Alice"))
        assertEquals(116 + 40, p.playerScores["Alice"])
        assertEquals(-58 - 20, p.playerScores["Bob"])
    }

    @Test
    fun `poignee goes to the winning camp`() {
        // A defender declares a simple poignée (20); the taker lost → defenders collect.
        val p = previewRound(three, "Alice", Contract.GARDE, details(0, 52, poignees = listOf("Bob")))
        assertEquals(-116 - 40, p.playerScores["Alice"])
        assertEquals(58 + 20, p.playerScores["Bob"])
    }

    @Test
    fun `announced and realized chelem is included`() {
        val p = previewRound(three, "Alice", Contract.GARDE,
            details(3, 91, chelem = Chelem.ANNOUNCED_REALIZED))
        // Garde, 3 bouts → needs 36; 91 → made by 55; (25 + 55) × 2 = 160; + 400 chelem.
        assertEquals(55, p.margin)
        assertEquals((160 + 400) * 2, p.playerScores["Alice"])
        assertEquals(-160 - 400, p.playerScores["Chloé"])
    }

    @Test
    fun `every preview is zero-sum`() {
        val cases = listOf(
            previewRound(three, "Alice", Contract.GARDE_SANS, details(1, 30, petit = "Bob")),
            previewRound(five, "Eve", Contract.GARDE_CONTRE, details(2, 70, partner = "Alice",
                poignees = listOf("Eve"), chelem = Chelem.NOT_ANNOUNCED_REALIZED))
        )
        for (p in cases) assertEquals(0, p.playerScores.values.sum())
    }

    @Test
    fun `round entry strings are localized`() {
        val en = appStrings(AppLocale.EN)
        val fr = appStrings(AppLocale.FR)
        assertEquals("Made by 6 → Chloé +186", en.resultMade(6, "Chloé", "+186"))
        assertEquals("Short by 4 → Chloé -174", en.resultShort(4, "Chloé", "-174"))
        assertEquals("Fait de 6 → Chloé +186", fr.resultMade(6, "Chloé", "+186"))
        assertEquals("Chuté de 4 → Chloé -174", fr.resultShort(4, "Chloé", "-174"))
        assertEquals("needs 41", en.boutsNeeds(41))
        assertEquals("il faut 41", fr.boutsNeeds(41))
    }
}


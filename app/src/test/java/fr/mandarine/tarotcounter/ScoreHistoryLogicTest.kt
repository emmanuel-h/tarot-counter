package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the Score history layout logic (ScoreHistoryLogic.kt, issue #202). */
class ScoreHistoryLogicTest {

    // ── historyTableScrolls ───────────────────────────────────────────────────

    @Test
    fun `five players fit on a wide screen`() {
        // 48 + 5 × 64 = 368 dp needed.
        assertFalse(historyTableScrolls(400f, 5))
        assertFalse(historyTableScrolls(368f, 5))
    }

    @Test
    fun `five players scroll on a narrow phone`() {
        assertTrue(historyTableScrolls(367f, 5))
        assertTrue(historyTableScrolls(318f, 5))
    }

    @Test
    fun `three players fit even on a small phone`() {
        // 48 + 3 × 64 = 240 dp.
        assertFalse(historyTableScrolls(240f, 3))
        assertTrue(historyTableScrolls(239f, 3))
    }

    // ── leaderColumns ─────────────────────────────────────────────────────────

    private val players = listOf("Alice", "Bob", "Chloé")

    @Test
    fun `no leader column before a scored round`() {
        assertEquals(emptySet<String>(), leaderColumns(players, emptyList()))
        assertEquals(
            emptySet<String>(),
            leaderColumns(players, listOf(RoundResult(1, "Alice", null, null, null)))
        )
    }

    @Test
    fun `leader columns follow the current leaders, ties included`() {
        val tie = RoundResult(1, "Alice", Contract.PRISE, null, true,
            mapOf("Alice" to 20, "Bob" to 20, "Chloé" to -40))
        assertEquals(setOf("Alice", "Bob"), leaderColumns(players, listOf(tie)))
    }

    @Test
    fun `history strings are localized`() {
        assertEquals("2 bouts · 47 pts", appStrings(AppLocale.EN).boutsPoints(2, 47))
        assertTrue(appStrings(AppLocale.FR).historyEmpty.startsWith("Aucune manche"))
    }
}

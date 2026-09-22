package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the Salon game screen logic in Standings.kt (issue #198). */
class StandingsTest {

    private val players = listOf("Alice", "Bruno", "Chloé", "David")

    private fun played(n: Int, vararg scores: Pair<String, Int>) =
        RoundResult(n, "Alice", Contract.GARDE, null, true, mapOf(*scores))

    private fun skipped(n: Int) = RoundResult(n, "Alice", null, null, null)

    // ── computeStandings ──────────────────────────────────────────────────────

    @Test
    fun `no rounds - everybody at zero, rank 1, no leader, no trend`() {
        val standings = computeStandings(players, emptyList())
        assertEquals(players, standings.map { it.name })          // seat order kept
        assertTrue(standings.all { it.total == 0 && it.rank == 1 })
        assertTrue(standings.none { it.isLeader })
        assertTrue(standings.all { it.lastDelta == null })
        assertEquals(listOf(0, 1, 2, 3), standings.map { it.seatIndex })
    }

    @Test
    fun `sorted by total, best first, with seat indexes preserved`() {
        val rounds = listOf(
            played(1, "Alice" to -30, "Bruno" to 90, "Chloé" to -30, "David" to -30),
            played(2, "Alice" to 60, "Bruno" to -20, "Chloé" to -20, "David" to -20)
        )
        val standings = computeStandings(players, rounds)
        assertEquals(listOf("Bruno", "Alice", "Chloé", "David"), standings.map { it.name })
        assertEquals(listOf(70, 30, -50, -50), standings.map { it.total })
        assertEquals(listOf(1, 0, 2, 3), standings.map { it.seatIndex })
    }

    @Test
    fun `ties share a rank and the next rank is skipped`() {
        val rounds = listOf(played(1, "Alice" to 20, "Bruno" to 20, "Chloé" to -20, "David" to -20))
        val standings = computeStandings(players, rounds)
        assertEquals(listOf(1, 1, 3, 3), standings.map { it.rank })
        // Tied players keep seat order.
        assertEquals(listOf("Alice", "Bruno", "Chloé", "David"), standings.map { it.name })
    }

    @Test
    fun `every tied leader is marked`() {
        val rounds = listOf(played(1, "Alice" to 20, "Bruno" to 20, "Chloé" to -20, "David" to -20))
        val leaders = computeStandings(players, rounds).filter { it.isLeader }.map { it.name }
        assertEquals(listOf("Alice", "Bruno"), leaders)
    }

    @Test
    fun `only skipped rounds - no leader`() {
        val standings = computeStandings(players, listOf(skipped(1)))
        assertTrue(standings.none { it.isLeader })
    }

    @Test
    fun `single leader after a scored round`() {
        val rounds = listOf(played(1, "Alice" to 90, "Bruno" to -30, "Chloé" to -30, "David" to -30))
        val standings = computeStandings(players, rounds)
        assertTrue(standings.first().isLeader)
        assertFalse(standings.drop(1).any { it.isLeader })
    }

    @Test
    fun `last delta comes from the most recent round`() {
        val rounds = listOf(
            played(1, "Alice" to 90, "Bruno" to -30, "Chloé" to -30, "David" to -30),
            played(2, "Alice" to -60, "Bruno" to 20, "Chloé" to 20, "David" to 20)
        )
        val byName = computeStandings(players, rounds).associateBy { it.name }
        assertEquals(-60, byName.getValue("Alice").lastDelta)
        assertEquals(20, byName.getValue("Bruno").lastDelta)
    }

    @Test
    fun `last delta is null after a skipped round`() {
        val rounds = listOf(
            played(1, "Alice" to 90, "Bruno" to -30, "Chloé" to -30, "David" to -30),
            skipped(2)
        )
        assertTrue(computeStandings(players, rounds).all { it.lastDelta == null })
    }

    // ── takerGridColumns ──────────────────────────────────────────────────────

    @Test
    fun `taker grid is 3, 2x2 and 3+2`() {
        assertEquals(3, takerGridColumns(3))
        assertEquals(2, takerGridColumns(4))
        assertEquals(3, takerGridColumns(5))
    }

    // ── lastRounds ────────────────────────────────────────────────────────────

    @Test
    fun `last rounds are the latest three, newest first`() {
        val rounds = (1..5).map { skipped(it) }
        assertEquals(listOf(5, 4, 3), lastRounds(rounds).map { it.roundNumber })
    }

    @Test
    fun `last rounds with fewer rounds than the count`() {
        assertEquals(listOf(2, 1), lastRounds(listOf(skipped(1), skipped(2))).map { it.roundNumber })
        assertEquals(emptyList<Int>(), lastRounds(emptyList()).map { it.roundNumber })
    }

    @Test
    fun `last rounds with a custom count`() {
        val rounds = (1..5).map { skipped(it) }
        assertEquals(listOf(5), lastRounds(rounds, count = 1).map { it.roundNumber })
        assertNull(lastRounds(rounds, count = 0).firstOrNull())
    }

    // ── Strings ───────────────────────────────────────────────────────────────

    @Test
    fun `game screen strings are localized`() {
        val en = appStrings(AppLocale.EN)
        val fr = appStrings(AppLocale.FR)
        assertEquals("R4", en.roundBadge(4))
        assertEquals("M4", fr.roundBadge(4))
        assertEquals("Chloé takes", en.takerTakes("Chloé"))
        assertEquals("Chloé prend", fr.takerTakes("Chloé"))
        assertEquals("Who took?", en.whoTook)
        assertEquals("Qui prend ?", fr.whoTook)
    }
}

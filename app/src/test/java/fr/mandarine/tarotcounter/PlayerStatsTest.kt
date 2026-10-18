package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests for the statistics logic of PlayerStats.kt (issue #229).
class PlayerStatsTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    // A played round: `scores` are the per-player deltas.
    private fun played(n: Int, taker: String, contract: Contract, won: Boolean, scores: Map<String, Int>) =
        RoundResult(n, taker, contract, null, won, scores)

    private fun skipped(n: Int, dealer: String) = RoundResult(n, dealer, null, null, null)

    private fun game(id: String, names: List<String>, vararg rounds: RoundResult) = SavedGame(
        id          = id,
        datestamp   = 0L,
        playerNames = names,
        rounds      = rounds.toList(),
        finalScores = computeFinalTotals(names, rounds.toList())
    )

    private val abc = listOf("Alice", "Bob", "Chloé")

    // Alice takes a Garde and makes it, Bob takes a Prise and fails it, one skipped round.
    private val game1 = game(
        "g1", abc,
        played(1, "Alice", Contract.GARDE, true, mapOf("Alice" to 120, "Bob" to -60, "Chloé" to -60)),
        played(2, "Bob", Contract.PRISE, false, mapOf("Alice" to 30, "Bob" to -60, "Chloé" to 30)),
        skipped(3, "Chloé")
    )

    // Chloé takes twice and wins both.
    private val game2 = game(
        "g2", abc,
        played(1, "Chloé", Contract.GARDE_SANS, true, mapOf("Alice" to -100, "Bob" to -100, "Chloé" to 200)),
        played(2, "Chloé", Contract.PRISE, true, mapOf("Alice" to -30, "Bob" to -30, "Chloé" to 60))
    )

    private fun stats(vararg games: SavedGame) = computePlayerStats(games.toList())
    private fun List<PlayerStats>.of(name: String) = first { it.name == name }

    // ── percentOrNull ─────────────────────────────────────────────────────────

    @Test fun `percent is null for an empty whole`() = assertNull(percentOrNull(0, 0))
    @Test fun `percent rounds to nearest`() {
        assertEquals(33, percentOrNull(1, 3))
        assertEquals(67, percentOrNull(2, 3))
        assertEquals(100, percentOrNull(4, 4))
        assertEquals(0, percentOrNull(0, 5))
        assertEquals(50, percentOrNull(1, 2))
    }

    // ── playerKey ─────────────────────────────────────────────────────────────

    @Test fun `player key ignores case and surrounding spaces`() {
        assertEquals("alice", playerKey("  ALICE "))
        assertEquals(playerKey("Alice"), playerKey("alice"))
    }

    // ── computePlayerStats ────────────────────────────────────────────────────

    @Test fun `no games gives no stats`() = assertTrue(stats().isEmpty())

    @Test fun `games without a scored round are ignored`() {
        val empty   = game("e", abc)
        val skipped = game("s", abc, skipped(1, "Alice"))
        assertTrue(stats(empty, skipped).isEmpty())
        assertEquals(0, countedGames(listOf(empty, skipped)))
        assertEquals(1, countedGames(listOf(empty, game1)))
        assertEquals(0, countedGames(emptyList()))
        assertEquals(2, countedGames(listOf(game1, game2, skipped)))
    }

    @Test fun `games played and wins`() {
        val s = stats(game1, game2)
        assertEquals(2, s.of("Alice").gamesPlayed)
        assertEquals(1, s.of("Alice").wins)  // game 1: Alice +150
        assertEquals(0, s.of("Bob").wins)
        assertEquals(1, s.of("Chloé").wins)  // game 2: Chloé +260
        assertEquals(50, s.of("Alice").winPercent)
    }

    @Test fun `a tie for first counts as a win for every co-winner`() {
        val tie = game(
            "t", abc,
            played(1, "Alice", Contract.PRISE, false, mapOf("Alice" to -50, "Bob" to 25, "Chloé" to 25))
        )
        val s = stats(tie)
        assertEquals(1, s.of("Bob").wins)
        assertEquals(1, s.of("Chloé").wins)
        assertEquals(0, s.of("Alice").wins)
    }

    @Test fun `skipped rounds do not count as played or taken`() {
        val s = stats(game1)
        assertEquals(2, s.of("Alice").roundsPlayed)
        assertEquals(1, s.of("Alice").roundsTaken)
        assertEquals(0, s.of("Chloé").roundsTaken)  // Chloé only dealt the skipped round
        assertEquals(50, s.of("Alice").takePercent)
        assertEquals(0, s.of("Chloé").takePercent)
    }

    @Test fun `success per contract`() {
        val s = stats(game1, game2)
        val chloe = s.of("Chloé").contracts
        assertEquals(ContractStats(taken = 1, won = 1), chloe[Contract.GARDE_SANS])
        assertEquals(ContractStats(taken = 1, won = 1), chloe[Contract.PRISE])
        assertEquals(ContractStats(taken = 0, won = 0), chloe[Contract.GARDE])
        assertEquals(ContractStats(taken = 1, won = 0), s.of("Bob").contracts[Contract.PRISE])
        assertEquals(0, s.of("Bob").contracts.getValue(Contract.PRISE).successPercent)
        assertNull(chloe.getValue(Contract.GARDE_CONTRE).successPercent)
        assertEquals(100, chloe.getValue(Contract.PRISE).successPercent)
        // Contracts are listed in the enum order.
        assertEquals(Contract.entries.toList(), chloe.keys.toList())
    }

    @Test fun `average score is rounded per game`() {
        val s = stats(game1, game2)
        // Alice: +150 then -130 → total 20 → average 10.
        assertEquals(20, s.of("Alice").totalScore)
        assertEquals(10, s.of("Alice").averageScore)
        // Bob: -120 then -130 → -250 / 2 = -125.
        assertEquals(-125, s.of("Bob").averageScore)
        // Chloé: -30 then +260 → 230 / 2 = 115.
        assertEquals(115, s.of("Chloé").averageScore)
    }

    @Test fun `average rounds halves away from zero for positives`() {
        val p = PlayerStats("X", 2, 0, 0, 0, 3, emptyMap())
        assertEquals(2, p.averageScore)  // 1.5 → 2
        val none = PlayerStats("X", 0, 0, 0, 0, 0, emptyMap())
        assertEquals(0, none.averageScore)
        assertNull(none.takePercent)
        assertNull(none.winPercent)
    }

    @Test fun `names are merged case-insensitively and keep the newest spelling`() {
        val newer = game(
            "n", listOf("ALICE ", "Bob", "Chloé"),
            played(1, "ALICE ", Contract.GARDE, true, mapOf("ALICE " to 120, "Bob" to -60, "Chloé" to -60))
        )
        // Saved games are newest first, so `newer` comes first.
        val s = stats(newer, game1)
        assertEquals(3, s.size)
        val alice = s.first { playerKey(it.name) == "alice" }
        assertEquals("ALICE", alice.name)  // trimmed, newest spelling
        assertEquals(2, alice.gamesPlayed)
        assertEquals(2, alice.roundsTaken)
        assertEquals(2, alice.contracts.getValue(Contract.GARDE).won)
        assertEquals(2, alice.wins)
        assertEquals(270, alice.totalScore)
    }

    @Test fun `sorted by wins then games then name`() {
        val fourPlayers = game(
            "4", listOf("Alice", "Bob", "Chloé", "David"),
            played(1, "David", Contract.GARDE, true,
                mapOf("Alice" to -60, "Bob" to -60, "Chloé" to -60, "David" to 180))
        )
        val s = stats(game1, game2, fourPlayers)
        // Wins: Alice 1, Chloé 1, David 1, Bob 0. Games: Alice 3, Chloé 3, David 1.
        assertEquals(listOf("Alice", "Chloé", "David", "Bob"), s.map { it.name })
    }

    @Test fun `name tie-breaker is case-insensitive`() {
        val g = game(
            "x", listOf("bob", "Alice", "Carl"),
            played(1, "Carl", Contract.PRISE, false, mapOf("bob" to 25, "Alice" to 25, "Carl" to -50))
        )
        // bob and Alice: one win and one game each → alphabetical, ignoring case.
        assertEquals(listOf("Alice", "bob", "Carl"), stats(g).map { it.name })
    }

    @Test fun `a taker who is not in the player list is ignored`() {
        val g = game(
            "x", abc,
            played(1, "Ghost", Contract.PRISE, true, mapOf("Alice" to 10, "Bob" to -5, "Chloé" to -5))
        )
        val s = stats(g)
        assertEquals(0, s.sumOf { it.roundsTaken })
        assertEquals(1, s.of("Alice").roundsPlayed)
    }

    @Test fun `duplicate names inside one game count once`() {
        val g = game(
            "d", listOf("Alice", "alice", "Bob"),
            played(1, "Bob", Contract.PRISE, true, mapOf("Alice" to -25, "Bob" to 50))
        )
        assertEquals(1, stats(g).of("Alice").gamesPlayed)
    }

    // ── Strings (EN + FR) ─────────────────────────────────────────────────────

    @Test fun `statistics strings in both languages`() {
        assertEquals("Based on 1 saved game", EnStrings.statsBasedOn(1))
        assertEquals("Based on 7 saved games", EnStrings.statsBasedOn(7))
        assertEquals("Sur 1 partie enregistrée", FrStrings.statsBasedOn(1))
        assertEquals("Sur 7 parties enregistrées", FrStrings.statsBasedOn(7))
        assertEquals("3/4 made", EnStrings.statMadeOutOf(3, 4))
        assertEquals("3/4 réussies", FrStrings.statMadeOutOf(3, 4))
        assertEquals("Statistics", EnStrings.statistics)
        assertEquals("Statistiques", FrStrings.statistics)
    }

    @Test fun `rounds a player sat out as dead dealer do not count as played`() {
        val four = listOf("A", "B", "C", "D")
        val r1 = played(1, "A", Contract.GARDE, true, mapOf("A" to 100, "B" to -50, "C" to -50, "D" to 0))
            .copy(deadPlayer = "D")
        val r2 = played(2, "B", Contract.PRISE, false, mapOf("A" to 25, "B" to -50, "C" to 25, "D" to 0))
            .copy(deadPlayer = " d ")   // matched with the same key as the player name
        val r3 = played(3, "D", Contract.PRISE, true, mapOf("A" to -25, "B" to 0, "C" to -25, "D" to 50))
            .copy(deadPlayer = "B")
        val result = stats(game("g", four, r1, r2, r3))
        assertEquals(1, result.of("D").roundsPlayed)
        assertEquals(2, result.of("B").roundsPlayed)
        assertEquals(3, result.of("A").roundsPlayed)
        assertEquals(100, result.of("D").takePercent)
    }
}

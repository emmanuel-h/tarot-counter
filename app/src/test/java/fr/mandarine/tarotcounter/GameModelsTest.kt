package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the data models defined in GameModels.kt.
 *
 * These run on the local JVM (no device needed) because they test pure Kotlin
 * logic — no Android or Compose dependencies.
 *
 * Each test is named after the spec it validates (see docs/game-flow.md).
 */
class GameModelsTest {

    // ── Contract ──────────────────────────────────────────────────────────────

    @Test
    fun `Contract has exactly four values`() {
        // Spec: four contracts — Prise, Garde, Garde Sans, Garde Contre.
        // POUSSE was removed; PETITE was renamed to PRISE.
        assertEquals(4, Contract.entries.size)
    }

    @Test
    fun `Contract display names match the spec`() {
        assertEquals("Prise",         Contract.PRISE.displayName)
        assertEquals("Garde",         Contract.GARDE.displayName)
        assertEquals("Garde Sans",    Contract.GARDE_SANS.displayName)
        assertEquals("Garde Contre",  Contract.GARDE_CONTRE.displayName)
    }

    @Test
    fun `Contract multipliers match the spec`() {
        // Spec: Prise ×1, Garde ×2, Garde Sans ×4, Garde Contre ×6.
        assertEquals(1, Contract.PRISE.multiplier)
        assertEquals(2, Contract.GARDE.multiplier)
        assertEquals(4, Contract.GARDE_SANS.multiplier)
        assertEquals(6, Contract.GARDE_CONTRE.multiplier)
    }

    @Test
    fun `Contract values are declared weakest to strongest`() {
        // `entries` preserves declaration order in Kotlin enums.
        val inOrder = Contract.entries
        assertEquals(Contract.PRISE,         inOrder[0])
        assertEquals(Contract.GARDE,         inOrder[1])
        assertEquals(Contract.GARDE_SANS,    inOrder[2])
        assertEquals(Contract.GARDE_CONTRE,  inOrder[3])
    }

    // ── Chelem ────────────────────────────────────────────────────────────────

    @Test
    fun `Chelem has exactly four values`() {
        // Spec: None, Announced+realized, Announced+not realized, Not announced+realized.
        assertEquals(4, Chelem.entries.size)
    }

    @Test
    fun `Chelem display names match the spec`() {
        assertEquals("None",                    Chelem.NONE.displayName)
        assertEquals("Announced & realized",    Chelem.ANNOUNCED_REALIZED.displayName)
        assertEquals("Announced, not realized", Chelem.ANNOUNCED_NOT_REALIZED.displayName)
        assertEquals("Not announced, realized", Chelem.NOT_ANNOUNCED_REALIZED.displayName)
    }

    // ── RoundResult — skipped rounds ──────────────────────────────────────────

    @Test
    fun `Skipped round has null contract, null details, and null won`() {
        // Spec: "Skip round to record the round without any details."
        val result = RoundResult(
            roundNumber = 1,
            takerName   = "Alice",
            contract    = null,   // null = skipped
            details     = null,   // null = no scoring info
            won         = null    // null = no outcome (skipped)
        )
        assertNull("Skipped round should have no contract", result.contract)
        assertNull("Skipped round should have no details",  result.details)
        assertNull("Skipped round should have no outcome",  result.won)
        assertTrue("Skipped round should have empty playerScores", result.playerScores.isEmpty())
    }

    // ── RoundResult — played rounds ───────────────────────────────────────────

    @Test
    fun `Played round has a non-null contract and non-null details`() {
        val details = RoundDetails(
            bouts         = 2,
            points        = 56,
            partnerName   = null,
            petitAuBout   = null,
            misere        = null,
            doubleMisere  = null,
            poignee       = null,
            doublePoignee = null,
            chelem        = Chelem.NONE
        )
        val result = RoundResult(
            roundNumber = 3,
            takerName   = "Alice",
            contract    = Contract.GARDE,
            details     = details,
            won         = takerWon(details.bouts, details.points)
        )
        assertNotNull(result.contract)
        assertNotNull(result.details)
        assertEquals(Contract.GARDE, result.contract)
        // 2 bouts requires 41 pts; 56 >= 41, so the taker won.
        assertEquals(true, result.won)
    }

    @Test
    fun `RoundResult stores the correct round number and taker name`() {
        val result = RoundResult(
            roundNumber = 7,
            takerName   = "Bob",
            contract    = null,
            details     = null,
            won         = null
        )
        assertEquals(7,     result.roundNumber)
        assertEquals("Bob", result.takerName)
    }

    // ── RoundDetails ──────────────────────────────────────────────────────────

    @Test
    fun `RoundDetails stores bouts and points correctly`() {
        val details = RoundDetails(
            bouts         = 3,
            points        = 91,
            partnerName   = null,
            petitAuBout   = null,
            misere        = null,
            doubleMisere  = null,
            poignee       = null,
            doublePoignee = null,
            chelem        = Chelem.NONE
        )
        assertEquals(3,  details.bouts)
        assertEquals(91, details.points)
    }

    @Test
    fun `RoundDetails player-assigned bonuses default to null when not claimed`() {
        // Spec: each bonus is either a player name (String) or null (nobody).
        val details = RoundDetails(
            bouts         = 0,
            points        = 0,
            partnerName   = null,
            petitAuBout   = null,
            misere        = null,
            doubleMisere  = null,
            poignee       = null,
            doublePoignee = null,
            chelem        = Chelem.NONE
        )
        assertNull(details.partnerName)
        assertNull(details.petitAuBout)
        assertNull(details.misere)
        assertNull(details.doubleMisere)
        assertNull(details.poignee)
        assertNull(details.doublePoignee)
        assertEquals(Chelem.NONE, details.chelem)
    }

    @Test
    fun `RoundDetails partnerName is stored correctly for 5-player game`() {
        val details = RoundDetails(
            bouts         = 2,
            points        = 50,
            partnerName   = "Bob",  // taker called Bob as partner
            petitAuBout   = null,
            misere        = null,
            doubleMisere  = null,
            poignee       = null,
            doublePoignee = null,
            chelem        = Chelem.NONE
        )
        assertEquals("Bob", details.partnerName)
    }

    // ── Win condition — requiredPoints ─────────────────────────────────────────

    @Test
    fun `requiredPoints returns correct thresholds for each bout count`() {
        // Spec table: 0 bouts → 56 pts, 1 → 51, 2 → 41, 3 → 36.
        assertEquals(56, requiredPoints(0))
        assertEquals(51, requiredPoints(1))
        assertEquals(41, requiredPoints(2))
        assertEquals(36, requiredPoints(3))
    }

    // ── Win condition — takerWon ───────────────────────────────────────────────

    @Test
    fun `takerWon returns true when points equal the threshold`() {
        // Exact boundary: reaching the threshold is a win.
        assertTrue(takerWon(bouts = 0, points = 56))
        assertTrue(takerWon(bouts = 1, points = 51))
        assertTrue(takerWon(bouts = 2, points = 41))
        assertTrue(takerWon(bouts = 3, points = 36))
    }

    @Test
    fun `takerWon returns true when points exceed the threshold`() {
        assertTrue(takerWon(bouts = 0, points = 57))
        assertTrue(takerWon(bouts = 1, points = 60))
        assertTrue(takerWon(bouts = 2, points = 91))
        assertTrue(takerWon(bouts = 3, points = 80))
    }

    @Test
    fun `takerWon returns false when points are below the threshold`() {
        // One point short of the threshold is a loss.
        assertFalse(takerWon(bouts = 0, points = 55))
        assertFalse(takerWon(bouts = 1, points = 50))
        assertFalse(takerWon(bouts = 2, points = 40))
        assertFalse(takerWon(bouts = 3, points = 35))
    }

    @Test
    fun `takerWon returns false when points are zero`() {
        // Zero points never wins regardless of bout count.
        assertFalse(takerWon(bouts = 0, points = 0))
        assertFalse(takerWon(bouts = 3, points = 0))
    }

    @Test
    fun `RoundResult stores won correctly for a winning played round`() {
        // 1 bout requires 51 pts; 51 >= 51 → won.
        val result = RoundResult(
            roundNumber = 1,
            takerName   = "Alice",
            contract    = Contract.PRISE,
            details     = RoundDetails(
                bouts = 1, points = 51,
                partnerName = null,
                petitAuBout = null, misere = null, doubleMisere = null,
                poignee = null, doublePoignee = null, chelem = Chelem.NONE
            ),
            won = takerWon(bouts = 1, points = 51)
        )
        assertEquals(true, result.won)
    }

    @Test
    fun `RoundResult stores won correctly for a losing played round`() {
        // 0 bouts requires 56 pts; 40 < 56 → lost.
        val result = RoundResult(
            roundNumber = 2,
            takerName   = "Bob",
            contract    = Contract.GARDE,
            details     = RoundDetails(
                bouts = 0, points = 40,
                partnerName = null,
                petitAuBout = null, misere = null, doubleMisere = null,
                poignee = null, doublePoignee = null, chelem = Chelem.NONE
            ),
            won = takerWon(bouts = 0, points = 40)
        )
        assertEquals(false, result.won)
    }

    @Test
    fun `RoundDetails stores the player name when a bonus is assigned`() {
        val details = RoundDetails(
            bouts         = 1,
            points        = 51,
            partnerName   = null,
            petitAuBout   = "Alice",
            misere        = "Bob",
            doubleMisere  = null,
            poignee       = "Alice",
            doublePoignee = null,
            chelem        = Chelem.ANNOUNCED_REALIZED
        )
        assertEquals("Alice", details.petitAuBout)
        assertEquals("Bob",   details.misere)
        assertNull(details.doubleMisere)
        assertEquals("Alice", details.poignee)
        assertNull(details.doublePoignee)
        assertEquals(Chelem.ANNOUNCED_REALIZED, details.chelem)
    }

    // ── calculateRoundScore ────────────────────────────────────────────────────

    @Test
    fun `calculateRoundScore returns 25 times multiplier at exact threshold`() {
        // Exact threshold → diff = 0 → (25 + 0) × multiplier.
        // 2 bouts threshold = 41; Garde (×2) → 25 × 2 = 50.
        assertEquals(50, calculateRoundScore(Contract.GARDE, bouts = 2, points = 41))
        // 0 bouts threshold = 56; Prise (×1) → 25 × 1 = 25.
        assertEquals(25, calculateRoundScore(Contract.PRISE, bouts = 0, points = 56))
    }

    @Test
    fun `calculateRoundScore adds margin above threshold for a win`() {
        // 2 bouts threshold = 41; scored 56 → diff = 15; Garde (×2) → (25+15)×2 = 80.
        assertEquals(80, calculateRoundScore(Contract.GARDE, bouts = 2, points = 56))
    }

    @Test
    fun `calculateRoundScore adds margin below threshold for a loss`() {
        // 0 bouts threshold = 56; scored 50 → diff = 6; Prise (×1) → (25+6)×1 = 31.
        assertEquals(31, calculateRoundScore(Contract.PRISE, bouts = 0, points = 50))
    }

    @Test
    fun `calculateRoundScore applies multiplier correctly for each contract`() {
        // Use 2 bouts (threshold 41), scored exactly 41 (diff 0) to isolate the multiplier.
        assertEquals(25,  calculateRoundScore(Contract.PRISE,        bouts = 2, points = 41))
        assertEquals(50,  calculateRoundScore(Contract.GARDE,        bouts = 2, points = 41))
        assertEquals(100, calculateRoundScore(Contract.GARDE_SANS,   bouts = 2, points = 41))
        assertEquals(150, calculateRoundScore(Contract.GARDE_CONTRE, bouts = 2, points = 41))
    }

    // ── computePlayerScores ───────────────────────────────────────────────────

    @Test
    fun `computePlayerScores 3-player win — taker gets 2x, defenders get -1x, sum is 0`() {
        val players = listOf("Alice", "Bob", "Charlie")
        val scores = computePlayerScores(
            allPlayers  = players,
            takerName   = "Alice",
            partnerName = null,
            won         = true,
            roundScore  = 50
        )
        assertEquals(+100, scores["Alice"])   // taker: +2 × 50
        assertEquals(-50,  scores["Bob"])     // defender: −50
        assertEquals(-50,  scores["Charlie"]) // defender: −50
        assertEquals(0, scores.values.sum())  // zero-sum check
    }

    @Test
    fun `computePlayerScores 3-player loss — taker gets -2x, defenders get +1x, sum is 0`() {
        val players = listOf("Alice", "Bob", "Charlie")
        val scores = computePlayerScores(
            allPlayers  = players,
            takerName   = "Alice",
            partnerName = null,
            won         = false,
            roundScore  = 50
        )
        assertEquals(-100, scores["Alice"])
        assertEquals(+50,  scores["Bob"])
        assertEquals(+50,  scores["Charlie"])
        assertEquals(0, scores.values.sum())
    }

    @Test
    fun `computePlayerScores 4-player loss — taker gets -3x, defenders get +1x, sum is 0`() {
        val players = listOf("Alice", "Bob", "Charlie", "Dave")
        val scores = computePlayerScores(
            allPlayers  = players,
            takerName   = "Alice",
            partnerName = null,
            won         = false,
            roundScore  = 40
        )
        assertEquals(-120, scores["Alice"])  // taker: −3 × 40
        assertEquals(+40,  scores["Bob"])
        assertEquals(+40,  scores["Charlie"])
        assertEquals(+40,  scores["Dave"])
        assertEquals(0, scores.values.sum())
    }

    @Test
    fun `computePlayerScores 5-player win — taker 2x, partner 1x, 3 defenders -1x, sum 0`() {
        val players = listOf("Alice", "Bob", "Charlie", "Dave", "Eve")
        val scores = computePlayerScores(
            allPlayers  = players,
            takerName   = "Alice",
            partnerName = "Bob",   // Alice called Bob as partner
            won         = true,
            roundScore  = 30
        )
        assertEquals(+60, scores["Alice"])   // taker: +2 × 30
        assertEquals(+30, scores["Bob"])     // partner: +1 × 30
        assertEquals(-30, scores["Charlie"]) // defender: −30
        assertEquals(-30, scores["Dave"])    // defender: −30
        assertEquals(-30, scores["Eve"])     // defender: −30
        assertEquals(0, scores.values.sum())
    }

    @Test
    fun `computePlayerScores 5-player loss — all signs flip, sum is still 0`() {
        val players = listOf("Alice", "Bob", "Charlie", "Dave", "Eve")
        val scores = computePlayerScores(
            allPlayers  = players,
            takerName   = "Alice",
            partnerName = "Bob",
            won         = false,
            roundScore  = 30
        )
        assertEquals(-60, scores["Alice"])
        assertEquals(-30, scores["Bob"])
        assertEquals(+30, scores["Charlie"])
        assertEquals(+30, scores["Dave"])
        assertEquals(+30, scores["Eve"])
        assertEquals(0, scores.values.sum())
    }
}

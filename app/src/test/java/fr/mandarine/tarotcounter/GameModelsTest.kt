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

    // ── RoundDetails.chelemPlayer ─────────────────────────────────────────────

    @Test
    fun `RoundDetails chelemPlayer defaults to null`() {
        // When no chelemPlayer is specified the field must be null (backward-compatible default).
        val details = RoundDetails(
            bouts         = 2,
            points        = 50,
            partnerName   = null,
            petitAuBout   = null,
            poignee       = null,
            doublePoignee = null,
            chelem        = Chelem.NONE
        )
        assertNull(details.chelemPlayer)
    }

    @Test
    fun `RoundDetails records chelemPlayer when set`() {
        // The player who called the chelem should be stored without modification.
        val details = RoundDetails(
            bouts         = 2,
            points        = 50,
            partnerName   = null,
            petitAuBout   = null,
            poignee       = null,
            doublePoignee = null,
            chelem        = Chelem.ANNOUNCED_REALIZED,
            chelemPlayer  = "Alice"
        )
        assertEquals("Alice", details.chelemPlayer)
    }

    @Test
    fun `RoundDetails chelemPlayer can be the partner in 5-player game`() {
        // In a 5-player game the partner can call the chelem. The field should accept
        // any non-null string — it is not restricted to the taker.
        val details = RoundDetails(
            bouts         = 3,
            points        = 91,
            partnerName   = "Bob",
            petitAuBout   = null,
            poignee       = null,
            doublePoignee = null,
            chelem        = Chelem.ANNOUNCED_REALIZED,
            chelemPlayer  = "Bob"  // partner called the chelem
        )
        assertNotNull(details.chelemPlayer)
        assertEquals("Bob", details.chelemPlayer)
        assertEquals("Bob", details.partnerName)
    }

    // ── petitAuBoutBonus ──────────────────────────────────────────────────────

    @Test
    fun `petitAuBoutBonus is 10 times the contract multiplier`() {
        // The bonus scales with the contract, not a flat value.
        assertEquals(10,  petitAuBoutBonus(Contract.PRISE))         // 10 × 1
        assertEquals(20,  petitAuBoutBonus(Contract.GARDE))         // 10 × 2
        assertEquals(40,  petitAuBoutBonus(Contract.GARDE_SANS))    // 10 × 4
        assertEquals(60,  petitAuBoutBonus(Contract.GARDE_CONTRE))  // 10 × 6
    }

    @Test
    fun `petitAuBoutBonus — taker achieves it, bonus distribution is zero-sum (4 players)`() {
        // Taker's camp achieved petit au bout → sign = +1.
        // Amount = 10 × 2 = 20 (Garde). 3 defenders.
        // Taker delta = +20 × 3 = +60; each defender delta = -20. Sum = 0.
        val amount = petitAuBoutBonus(Contract.GARDE)
        val sign = +1 // taker's camp
        val numDefenders = 3
        val takerDelta    = sign * amount * numDefenders  // +60
        val defenderDelta = -sign * amount                // -20 each
        assertEquals(0, takerDelta + defenderDelta * numDefenders)
        assertEquals(+60, takerDelta)
        assertEquals(-20, defenderDelta)
    }

    @Test
    fun `petitAuBoutBonus — defender achieves it, bonus distribution is zero-sum (4 players)`() {
        // Defender's camp achieved petit au bout → sign = -1.
        // Amount = 10 × 2 = 20 (Garde). 3 defenders.
        // Taker delta = -20 × 3 = -60; each defender delta = +20. Sum = 0.
        val amount = petitAuBoutBonus(Contract.GARDE)
        val sign = -1 // defenders' camp
        val numDefenders = 3
        val takerDelta    = sign * amount * numDefenders  // -60
        val defenderDelta = -sign * amount                // +20 each
        assertEquals(0, takerDelta + defenderDelta * numDefenders)
        assertEquals(-60, takerDelta)
        assertEquals(+20, defenderDelta)
    }

    @Test
    fun `petitAuBoutBonus — awarded regardless of round outcome`() {
        // The bonus is fixed (10 × multiplier) regardless of who won.
        // This test verifies the amount itself is not affected by the won/lost state.
        val amount = petitAuBoutBonus(Contract.GARDE_CONTRE) // 60 pts
        assertEquals(60, amount)
        // Whether the taker won or lost, the amount stays 60.
        // The sign (who pays whom) is determined by which camp achieved it, not by won/lost.
    }

    // ── poigneeBonus ──────────────────────────────────────────────────────────

    @Test
    fun `poigneeBonus returns 0 when no poignee is declared`() {
        assertEquals(0, poigneeBonus(poignee = null, doublePoignee = null, triplePoignee = null))
    }

    @Test
    fun `poigneeBonus returns 20 for a simple poignee`() {
        assertEquals(20, poigneeBonus(poignee = "Alice", doublePoignee = null, triplePoignee = null))
    }

    @Test
    fun `poigneeBonus returns 30 for a double poignee`() {
        assertEquals(30, poigneeBonus(poignee = null, doublePoignee = "Bob", triplePoignee = null))
    }

    @Test
    fun `poigneeBonus returns 40 for a triple poignee`() {
        assertEquals(40, poigneeBonus(poignee = null, doublePoignee = null, triplePoignee = "Charlie"))
    }

    @Test
    fun `poigneeBonus bonus goes to winner — taker wins, taker collects from each defender`() {
        // 4 players: 1 taker + 3 defenders. Simple poignée (20 pts).
        // Taker wins: each defender pays 20 to the taker.
        // Taker delta = +20 × 3 = +60; each defender delta = -20. Sum = 0.
        val bonus = poigneeBonus(poignee = "Alice", doublePoignee = null, triplePoignee = null)
        val numDefenders = 3
        val sign = 1 // taker won
        val takerDelta    = sign * bonus * numDefenders // +60
        val defenderDelta = -sign * bonus               // -20 each
        assertEquals(0, takerDelta + defenderDelta * numDefenders)
        assertEquals(+60, takerDelta)
        assertEquals(-20, defenderDelta)
    }

    @Test
    fun `poigneeBonus bonus goes to winner — taker loses, each defender collects from taker`() {
        // 4 players: 1 taker + 3 defenders. Double poignée (30 pts) declared by a defender.
        // Taker loses: the taker pays 30 to each defender regardless of who declared it.
        // Taker delta = -30 × 3 = -90; each defender delta = +30. Sum = 0.
        val bonus = poigneeBonus(poignee = null, doublePoignee = "Bob", triplePoignee = null)
        val numDefenders = 3
        val sign = -1 // taker lost
        val takerDelta    = sign * bonus * numDefenders // -90
        val defenderDelta = -sign * bonus               // +30 each
        assertEquals(0, takerDelta + defenderDelta * numDefenders)
        assertEquals(-90, takerDelta)
        assertEquals(+30, defenderDelta)
    }

    @Test
    fun `poigneeBonus triple poignee distribution is zero-sum`() {
        // Triple poignée (40 pts). Taker wins in a 3-player game (2 defenders).
        val bonus = poigneeBonus(poignee = null, doublePoignee = null, triplePoignee = "Alice")
        val numDefenders = 2
        val sign = 1 // taker won
        val takerDelta    = sign * bonus * numDefenders // +80
        val defenderDelta = -sign * bonus               // -40 each
        assertEquals(0, takerDelta + defenderDelta * numDefenders)
    }

    // ── chelemBonus ───────────────────────────────────────────────────────────

    @Test
    fun `chelemBonus returns 0 when there is no chelem`() {
        assertEquals(0, chelemBonus(Chelem.NONE))
    }

    @Test
    fun `chelemBonus returns 400 when announced and realized`() {
        assertEquals(400, chelemBonus(Chelem.ANNOUNCED_REALIZED))
    }

    @Test
    fun `chelemBonus returns 200 when not announced but realized`() {
        assertEquals(200, chelemBonus(Chelem.NOT_ANNOUNCED_REALIZED))
    }

    @Test
    fun `chelemBonus returns -200 when announced but not realized`() {
        assertEquals(-200, chelemBonus(Chelem.ANNOUNCED_NOT_REALIZED))
    }

    @Test
    fun `chelemBonus distribution is zero-sum in a 4-player game`() {
        // In a 4-player game: 1 taker + 3 defenders.
        // Taker delta = +400 × 3 = +1200; each defender delta = −400.
        // Total = 1200 − 400 − 400 − 400 = 0.
        val bonus = chelemBonus(Chelem.ANNOUNCED_REALIZED)
        val numDefenders = 3 // 4 players, no partner
        val takerDelta = bonus * numDefenders
        val defenderDelta = -bonus
        assertEquals(0, takerDelta + defenderDelta * numDefenders)
    }

    @Test
    fun `chelemBonus penalty distribution is zero-sum in a 4-player game`() {
        // Announced but not realized: taker pays −200 per defender.
        // Taker delta = −200 × 3 = −600; each defender delta = +200.
        // Total = −600 + 200 + 200 + 200 = 0.
        val bonus = chelemBonus(Chelem.ANNOUNCED_NOT_REALIZED)
        val numDefenders = 3
        val takerDelta = bonus * numDefenders
        val defenderDelta = -bonus
        assertEquals(0, takerDelta + defenderDelta * numDefenders)
    }

    @Test
    fun `chelemBonus distribution is zero-sum in a 5-player game — partner unaffected`() {
        // In a 5-player game: 1 taker + 1 partner + 3 defenders.
        // Chelem only involves taker and the 3 defenders; partner delta = 0.
        val bonus = chelemBonus(Chelem.NOT_ANNOUNCED_REALIZED) // +200
        val numDefenders = 3
        val takerDelta    = bonus * numDefenders   // +600
        val partnerDelta  = 0
        val defenderDelta = -bonus                 // −200 each
        assertEquals(0, takerDelta + partnerDelta + defenderDelta * numDefenders)
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
            poignee       = null,
            doublePoignee = null,
            chelem        = Chelem.NONE
        )
        assertNull(details.partnerName)
        assertNull(details.petitAuBout)
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
                petitAuBout = null, poignee = null, doublePoignee = null, chelem = Chelem.NONE
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
                petitAuBout = null, poignee = null, doublePoignee = null, chelem = Chelem.NONE
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
            poignee       = "Alice",
            doublePoignee = null,
            chelem        = Chelem.ANNOUNCED_REALIZED
        )
        assertEquals("Alice", details.petitAuBout)
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

    // ── applyBonuses ──────────────────────────────────────────────────────────

    // Helper: build a RoundDetails with all bonuses set to null/NONE.
    // Only override the fields relevant to each test.
    private fun details(
        partnerName: String?   = null,
        petitAuBout: String?   = null,
        poignee: String?       = null,
        doublePoignee: String? = null,
        triplePoignee: String? = null,
        chelem: Chelem         = Chelem.NONE
    ) = RoundDetails(
        bouts         = 2,
        points        = 50,
        partnerName   = partnerName,
        petitAuBout   = petitAuBout,
        poignee       = poignee,
        doublePoignee = doublePoignee,
        triplePoignee = triplePoignee,
        chelem        = chelem
    )

    @Test
    fun `applyBonuses — no bonuses, returns base scores unchanged`() {
        // All bonus fields absent: the function should be a no-op.
        // 4-player game: Alice (taker) won.
        val base = mapOf("Alice" to +120, "Bob" to -40, "Charlie" to -40, "Dave" to -40)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(base, result)
    }

    @Test
    fun `applyBonuses — petit au bout by taker adjusts scores and stays zero-sum (4-player)`() {
        // Contract: Garde (×2) → pabAmount = 20. Taker's camp achieved it → pabSign = +1.
        // numDefenders = 3.
        // Alice delta = +1 × 20 × 3 = +60 → 120 + 60 = +180
        // Bob/Charlie/Dave delta = -1 × 20 = -20 → -40 - 20 = -60 each
        val base = mapOf("Alice" to +120, "Bob" to -40, "Charlie" to -40, "Dave" to -40)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(petitAuBout = "Alice"),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(+180, result["Alice"])
        assertEquals(-60,  result["Bob"])
        assertEquals(-60,  result["Charlie"])
        assertEquals(-60,  result["Dave"])
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — petit au bout by defender adjusts scores and stays zero-sum (4-player)`() {
        // Garde (×2) → pabAmount = 20. Defenders' camp achieved it → pabSign = -1.
        // Alice delta = -1 × 20 × 3 = -60 → +120 - 60 = +60
        // Bob/Charlie/Dave delta = -(-1) × 20 = +20 → -40 + 20 = -20 each
        val base = mapOf("Alice" to +120, "Bob" to -40, "Charlie" to -40, "Dave" to -40)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(petitAuBout = "Bob"),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(+60,  result["Alice"])
        assertEquals(-20,  result["Bob"])
        assertEquals(-20,  result["Charlie"])
        assertEquals(-20,  result["Dave"])
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — poignee bonus goes to winning camp (taker wins, 4-player)`() {
        // Simple poignée → pBonus = 20. Taker won → pSign = +1.
        // Alice delta = +1 × 20 × 3 = +60 → +120 + 60 = +180
        // Bob/Charlie/Dave delta = -1 × 20 = -20 → -40 - 20 = -60 each
        val base = mapOf("Alice" to +120, "Bob" to -40, "Charlie" to -40, "Dave" to -40)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(poignee = "Alice"),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(+180, result["Alice"])
        assertEquals(-60,  result["Bob"])
        assertEquals(-60,  result["Charlie"])
        assertEquals(-60,  result["Dave"])
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — poignee bonus goes to winning camp (taker loses, 4-player)`() {
        // Simple poignée → pBonus = 20. Taker lost → pSign = -1.
        // Alice delta = -1 × 20 × 3 = -60 → -120 - 60 = -180
        // Bob/Charlie/Dave delta = -(-1) × 20 = +20 → +40 + 20 = +60 each
        val base = mapOf("Alice" to -120, "Bob" to +40, "Charlie" to +40, "Dave" to +40)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(poignee = "Alice"),
            takerName    = "Alice",
            won          = false,
            numDefenders = 3
        )
        assertEquals(-180, result["Alice"])
        assertEquals(+60,  result["Bob"])
        assertEquals(+60,  result["Charlie"])
        assertEquals(+60,  result["Dave"])
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — chelem announced and realized adds large bonus (4-player)`() {
        // cBonus = +400.
        // Alice delta = +400 × 3 = +1200 → +120 + 1200 = +1320
        // Bob/Charlie/Dave delta = -400 → -40 - 400 = -440 each
        val base = mapOf("Alice" to +120, "Bob" to -40, "Charlie" to -40, "Dave" to -40)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(chelem = Chelem.ANNOUNCED_REALIZED),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(+1320, result["Alice"])
        assertEquals(-440,  result["Bob"])
        assertEquals(-440,  result["Charlie"])
        assertEquals(-440,  result["Dave"])
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — chelem announced but not realized is a penalty (4-player)`() {
        // cBonus = -200.
        // Alice delta = -200 × 3 = -600 → +120 - 600 = -480
        // Bob/Charlie/Dave delta = -(-200) = +200 → -40 + 200 = +160 each
        val base = mapOf("Alice" to +120, "Bob" to -40, "Charlie" to -40, "Dave" to -40)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(chelem = Chelem.ANNOUNCED_NOT_REALIZED),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(-480, result["Alice"])
        assertEquals(+160, result["Bob"])
        assertEquals(+160, result["Charlie"])
        assertEquals(+160, result["Dave"])
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — partner is unaffected by all three bonuses (5-player)`() {
        // Alice (taker), Bob (partner), Charlie/Dave/Eve (defenders). numDefenders = 3.
        // Poignée (20) + chelem announced & realized (+400): only taker and defenders adjust.
        // Base: Alice=+60, Bob=+30, Charlie/Dave/Eve=-30 each.
        // Poignée (won → +): Alice +60, defenders -20 each → Alice=120, Bob=30, others=-50
        // Chelem (+400 × 3 = +1200 to taker): Alice=+1320, defenders -400 each → -450
        val base = mapOf(
            "Alice"   to +60,
            "Bob"     to +30,
            "Charlie" to -30,
            "Dave"    to -30,
            "Eve"     to -30
        )
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(
                partnerName = "Bob",
                poignee     = "Alice",
                chelem      = Chelem.ANNOUNCED_REALIZED
            ),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        // Bob (partner) must not change from his base score.
        assertEquals(+30, result["Bob"])
        // All scores must still sum to zero.
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — all three bonuses applied together are zero-sum`() {
        // Garde (×2), taker wins, 4-player game.
        // Petit au bout by taker (+1): pabAmount=20, pabSign=+1
        // Double poignée: pBonus=30, pSign=+1 (won)
        // Chelem not announced but realized: cBonus=+200
        // Base: Alice=+120, Bob/Charlie/Dave=-40 each
        val base = mapOf("Alice" to +120, "Bob" to -40, "Charlie" to -40, "Dave" to -40)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(
                petitAuBout   = "Alice",
                doublePoignee = "Alice",
                chelem        = Chelem.NOT_ANNOUNCED_REALIZED
            ),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        // Zero-sum is the key invariant — every point the taker gains is paid by defenders.
        assertEquals(0, result.values.sum())
    }

    // ── computeFinalTotals ────────────────────────────────────────────────────

    @Test
    fun `computeFinalTotals returns all zeros when there are no rounds`() {
        val players = listOf("Alice", "Bob", "Charlie")
        val totals = computeFinalTotals(players, emptyList())
        assertEquals(mapOf("Alice" to 0, "Bob" to 0, "Charlie" to 0), totals)
    }

    @Test
    fun `computeFinalTotals sums per-round scores correctly across multiple rounds`() {
        // Round 1: Alice +50, Bob -25, Charlie -25
        // Round 2: Alice -30, Bob +15, Charlie +15
        // Expected: Alice +20, Bob -10, Charlie -10
        val players = listOf("Alice", "Bob", "Charlie")
        val history = listOf(
            RoundResult(1, "Alice", Contract.GARDE, null, true,
                mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25)),
            RoundResult(2, "Bob", Contract.PRISE, null, false,
                mapOf("Alice" to -30, "Bob" to 15, "Charlie" to 15))
        )
        val totals = computeFinalTotals(players, history)
        assertEquals(+20, totals["Alice"])
        assertEquals(-10, totals["Bob"])
        assertEquals(-10, totals["Charlie"])
    }

    @Test
    fun `computeFinalTotals skipped rounds contribute zero to the total`() {
        // Round 1 played: Alice +50, others -25
        // Round 2 skipped: no change
        val players = listOf("Alice", "Bob", "Charlie")
        val history = listOf(
            RoundResult(1, "Alice", Contract.GARDE, null, true,
                mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25)),
            RoundResult(2, "Bob", null, null, null)  // skipped — playerScores defaults to emptyMap()
        )
        val totals = computeFinalTotals(players, history)
        // Totals unchanged from the first round because the second was skipped.
        assertEquals(+50, totals["Alice"])
        assertEquals(-25, totals["Bob"])
        assertEquals(-25, totals["Charlie"])
    }

    @Test
    fun `computeFinalTotals result is zero-sum (sum of all totals equals zero)`() {
        val players = listOf("Alice", "Bob", "Charlie")
        val history = listOf(
            RoundResult(1, "Alice", Contract.GARDE, null, true,
                mapOf("Alice" to 100, "Bob" to -50, "Charlie" to -50)),
            RoundResult(2, "Bob", Contract.PRISE, null, false,
                mapOf("Alice" to -25, "Bob" to 50, "Charlie" to -25))
        )
        val totals = computeFinalTotals(players, history)
        assertEquals(0, totals.values.sum())
    }

    // ── findWinners ───────────────────────────────────────────────────────────

    @Test
    fun `findWinners returns empty list when totals map is empty`() {
        val winners = findWinners(emptyMap())
        assertTrue("Empty totals should produce no winners", winners.isEmpty())
    }

    @Test
    fun `findWinners returns the single player with the highest score`() {
        // Alice has the most points.
        val totals = mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25)
        val winners = findWinners(totals)
        assertEquals(listOf("Alice"), winners)
    }

    @Test
    fun `findWinners returns all tied players when two share the highest score`() {
        // Alice and Bob are tied; Charlie is behind.
        val totals = mapOf("Alice" to 10, "Bob" to 10, "Charlie" to -20)
        val winners = findWinners(totals)
        assertEquals(2, winners.size)
        assertTrue("Alice should be a co-winner", "Alice" in winners)
        assertTrue("Bob should be a co-winner", "Bob" in winners)
    }

    @Test
    fun `findWinners returns all players when everyone has the same score`() {
        // All tied at zero (e.g. no rounds played yet).
        val totals = mapOf("Alice" to 0, "Bob" to 0, "Charlie" to 0)
        val winners = findWinners(totals)
        assertEquals(3, winners.size)
    }

    @Test
    fun `findWinners works correctly with a single player`() {
        val totals = mapOf("Alice" to 42)
        val winners = findWinners(totals)
        assertEquals(listOf("Alice"), winners)
    }
}

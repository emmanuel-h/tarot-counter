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

    // ── Int.withSign ──────────────────────────────────────────────────────────

    @Test
    fun `withSign prefixes positive integer with plus sign`() {
        assertEquals("+42", 42.withSign())
    }

    @Test
    fun `withSign zero is treated as positive`() {
        assertEquals("+0", 0.withSign())
    }

    @Test
    fun `withSign negative integer keeps its minus sign`() {
        assertEquals("-5", (-5).withSign())
    }

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
    fun `Chelem has exactly five values`() {
        // Spec: None, Announced+realized, Announced+not realized, Not announced+realized,
        // and DEFENDERS_REALIZED (R-RO201206.pdf p.6 — defenders win all tricks).
        assertEquals(5, Chelem.entries.size)
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

    // ── totalPoigneeBonus (issue #149) ────────────────────────────────────────

    @Test
    fun `totalPoigneeBonus returns 0 when no poignees are declared`() {
        assertEquals(0, totalPoigneeBonus(emptyList(), emptyList(), emptyList()))
    }

    @Test
    fun `totalPoigneeBonus returns 20 for one simple poignee`() {
        assertEquals(20, totalPoigneeBonus(listOf("Alice"), emptyList(), emptyList()))
    }

    @Test
    fun `totalPoigneeBonus returns 30 for one double poignee`() {
        assertEquals(30, totalPoigneeBonus(emptyList(), listOf("Bob"), emptyList()))
    }

    @Test
    fun `totalPoigneeBonus returns 40 for one triple poignee`() {
        assertEquals(40, totalPoigneeBonus(emptyList(), emptyList(), listOf("Charlie")))
    }

    @Test
    fun `totalPoigneeBonus accumulates when two players each declare a simple poignee`() {
        // Alice (simple 20) + Bob (simple 20) = 40 total.
        assertEquals(40, totalPoigneeBonus(listOf("Alice", "Bob"), emptyList(), emptyList()))
    }

    @Test
    fun `totalPoigneeBonus accumulates across different types`() {
        // Alice (simple 20) + Bob (double 30) = 50 total.
        assertEquals(50, totalPoigneeBonus(listOf("Alice"), listOf("Bob"), emptyList()))
    }

    @Test
    fun `totalPoigneeBonus accumulates all three types together`() {
        // Simple 20 + double 30 + triple 40 = 90.
        assertEquals(
            90,
            totalPoigneeBonus(listOf("Alice"), listOf("Bob"), listOf("Charlie"))
        )
    }

    // ── totalAtoutsAnnounced (issue #149) ─────────────────────────────────────

    @Test
    fun `totalAtoutsAnnounced returns 0 when no poignees are declared`() {
        assertEquals(0, totalAtoutsAnnounced(emptyList(), emptyList(), emptyList(), 4))
    }

    @Test
    fun `totalAtoutsAnnounced returns simple threshold for one simple poignee (4 players)`() {
        // 4-player threshold: simple = 10.
        assertEquals(10, totalAtoutsAnnounced(listOf("Alice"), emptyList(), emptyList(), 4))
    }

    @Test
    fun `totalAtoutsAnnounced returns double threshold for one double poignee (4 players)`() {
        // 4-player threshold: double = 13.
        assertEquals(13, totalAtoutsAnnounced(emptyList(), listOf("Bob"), emptyList(), 4))
    }

    @Test
    fun `totalAtoutsAnnounced returns triple threshold for one triple poignee (4 players)`() {
        // 4-player threshold: triple = 15.
        assertEquals(15, totalAtoutsAnnounced(emptyList(), emptyList(), listOf("Alice"), 4))
    }

    @Test
    fun `totalAtoutsAnnounced accumulates two simple poignees for 4 players`() {
        // Alice (10) + Bob (10) = 20. Still within the 22-card limit.
        assertEquals(20, totalAtoutsAnnounced(listOf("Alice", "Bob"), emptyList(), emptyList(), 4))
    }

    @Test
    fun `totalAtoutsAnnounced exceeds limit for triple plus simple (4 players)`() {
        // Alice declares triple (15) + Bob declares simple (10) = 25 > 22.
        val total = totalAtoutsAnnounced(listOf("Bob"), emptyList(), listOf("Alice"), 4)
        assertTrue("Triple + simple should exceed 22 atouts", total > TOTAL_ATOUTS_IN_DECK)
        assertEquals(25, total)
    }

    @Test
    fun `totalAtoutsAnnounced uses correct thresholds for 3 players`() {
        // 3-player threshold: simple = 13, double = 15, triple = 18.
        assertEquals(13, totalAtoutsAnnounced(listOf("Alice"), emptyList(), emptyList(), 3))
        assertEquals(15, totalAtoutsAnnounced(emptyList(), listOf("Alice"), emptyList(), 3))
        assertEquals(18, totalAtoutsAnnounced(emptyList(), emptyList(), listOf("Alice"), 3))
    }

    @Test
    fun `totalAtoutsAnnounced uses correct thresholds for 5 players`() {
        // 5-player threshold: simple = 8, double = 10, triple = 13.
        assertEquals(8,  totalAtoutsAnnounced(listOf("Alice"), emptyList(), emptyList(), 5))
        assertEquals(10, totalAtoutsAnnounced(emptyList(), listOf("Alice"), emptyList(), 5))
        assertEquals(13, totalAtoutsAnnounced(emptyList(), emptyList(), listOf("Alice"), 5))
    }

    // ── RoundDetails effectivePoignees (issue #149 backward compat) ───────────

    @Test
    fun `effectivePoignees returns new list when populated`() {
        val d = RoundDetails(
            bouts = 2, points = 50, partnerName = null, petitAuBout = null,
            poignees = listOf("Alice", "Bob"), chelem = Chelem.NONE
        )
        assertEquals(listOf("Alice", "Bob"), d.effectivePoignees)
    }

    @Test
    fun `effectivePoignees falls back to legacy field when new list is empty`() {
        // Old saved-game format: `poignee = "Alice"`, `poignees` absent → defaults to [].
        val d = RoundDetails(
            bouts = 2, points = 50, partnerName = null, petitAuBout = null,
            poignee = "Alice", chelem = Chelem.NONE
        )
        assertEquals(listOf("Alice"), d.effectivePoignees)
    }

    @Test
    fun `effectivePoignees returns empty list when both fields are empty or null`() {
        val d = RoundDetails(
            bouts = 2, points = 50, partnerName = null, petitAuBout = null,
            chelem = Chelem.NONE
        )
        assertEquals(emptyList<String>(), d.effectivePoignees)
    }

    // ── applyBonuses — multi-player poignée (issue #149) ─────────────────────

    @Test
    fun `applyBonuses — two simple poignees by different players (taker wins, 4-player)`() {
        // Alice (taker) and Bob (defender) each declare a simple poignée.
        // totalPoigneeBonus = 20 + 20 = 40. Taker wins → pSign = +1.
        // Alice delta = +1 × 40 × 3 = +120
        // Bob/Charlie/Dave delta = -1 × 40 = -40 each
        val base = mapOf("Alice" to +120, "Bob" to -40, "Charlie" to -40, "Dave" to -40)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = RoundDetails(
                bouts = 2, points = 50, partnerName = null, petitAuBout = null,
                poignees = listOf("Alice", "Bob"), chelem = Chelem.NONE
            ),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(+240, result["Alice"])  // +120 base + 120 bonus
        assertEquals(-80,  result["Bob"])    // -40 base - 40 bonus
        assertEquals(-80,  result["Charlie"])
        assertEquals(-80,  result["Dave"])
        assertEquals(0, result.values.sum())  // zero-sum
    }

    @Test
    fun `applyBonuses — mixed simple and double poignees (taker loses, 4-player)`() {
        // Alice (simple 20) + Bob (double 30) = 50 total. Taker loses → pSign = -1.
        // Alice delta = -1 × 50 × 3 = -150
        // Bob/Charlie/Dave delta = +1 × 50 = +50 each
        val base = mapOf("Alice" to -120, "Bob" to +40, "Charlie" to +40, "Dave" to +40)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = RoundDetails(
                bouts = 2, points = 50, partnerName = null, petitAuBout = null,
                poignees = listOf("Alice"), doublePoignees = listOf("Bob"),
                chelem = Chelem.NONE
            ),
            takerName    = "Alice",
            won          = false,
            numDefenders = 3
        )
        assertEquals(-270, result["Alice"])  // -120 - 150
        assertEquals(+90,  result["Bob"])    // +40 + 50
        assertEquals(+90,  result["Charlie"])
        assertEquals(+90,  result["Dave"])
        assertEquals(0, result.values.sum())  // zero-sum
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

    // ── requiredPoints — invalid input ────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `requiredPoints throws IllegalArgumentException for negative bouts`() {
        // The function documents that bouts must be 0–3.
        // A negative value is clearly invalid and should not silently return a wrong threshold.
        requiredPoints(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `requiredPoints throws IllegalArgumentException for bouts greater than 3`() {
        // The maximum number of oudlers in French Tarot is 3 (21, Petit, Excuse).
        // Passing 4 is a caller bug and should be caught immediately.
        requiredPoints(4)
    }

    // ── computePlayerScores — 4-player win ────────────────────────────────────

    @Test
    fun `computePlayerScores 4-player win — taker gets 3x, defenders get -1x, sum is 0`() {
        // 4 players, no partner: numDefenders = 3, takerMultiplier = 3.
        // Taker wins: Alice gets +3 × 40 = +120; each defender gets −40.
        // Sum: 120 − 40 − 40 − 40 = 0.
        val players = listOf("Alice", "Bob", "Charlie", "Dave")
        val scores = computePlayerScores(
            allPlayers  = players,
            takerName   = "Alice",
            partnerName = null,
            won         = true,
            roundScore  = 40
        )
        assertEquals(+120, scores["Alice"])   // taker: +3 × 40
        assertEquals(-40,  scores["Bob"])     // defender: −40
        assertEquals(-40,  scores["Charlie"]) // defender: −40
        assertEquals(-40,  scores["Dave"])    // defender: −40
        assertEquals(0, scores.values.sum())  // zero-sum invariant
    }

    // ── applyBonuses — partner achieves petit au bout (5-player) ─────────────

    @Test
    fun `applyBonuses — petit au bout by partner benefits taker, partner score unchanged (5-player)`() {
        // In a 5-player game Alice (taker) called Bob (partner). Charlie, Dave, Eve are defenders.
        // Garde (×2) → pabAmount = 20. Bob (partner) captured the Petit → pabSign = +1
        // because the partner belongs to the taker's camp.
        //
        // Per-player delta:
        //   Alice (taker) : +pabSign × 20 × 3 = +60   → 60 + 60 = +120
        //   Bob (partner) : score unchanged              → stays at +30
        //   Each defender : -pabSign × 20 = -20         → -30 - 20 = -50
        //
        // The partner's score is NOT modified even though they achieved the bonus — that
        // point flows entirely to the taker (see applyBonuses comment in GameModels.kt).
        val base = mapOf(
            "Alice" to +60, "Bob" to +30,
            "Charlie" to -30, "Dave" to -30, "Eve" to -30
        )
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(partnerName = "Bob", petitAuBout = "Bob"),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(+120, result["Alice"])
        assertEquals(+30,  result["Bob"])     // partner: unchanged despite capturing the Petit
        assertEquals(-50,  result["Charlie"])
        assertEquals(-50,  result["Dave"])
        assertEquals(-50,  result["Eve"])
        assertEquals(0, result.values.sum())
    }

    // ── applyBonuses — 3-player game (numDefenders = 2) ──────────────────────

    @Test
    fun `applyBonuses — petit au bout in 3-player game uses numDefenders of 2`() {
        // 3-player: Alice (taker), Bob and Charlie (defenders). numDefenders = 2.
        // Prise (×1) → pabAmount = 10. Taker (Alice) achieved petit au bout → pabSign = +1.
        // Alice delta = +1 × 10 × 2 = +20 → 50 + 20 = +70
        // Bob/Charlie delta = -1 × 10 = -10 → -25 - 10 = -35 each
        val base = mapOf("Alice" to +50, "Bob" to -25, "Charlie" to -25)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.PRISE,
            details      = details(petitAuBout = "Alice"),
            takerName    = "Alice",
            won          = true,
            numDefenders = 2
        )
        assertEquals(+70, result["Alice"])
        assertEquals(-35, result["Bob"])
        assertEquals(-35, result["Charlie"])
        assertEquals(0, result.values.sum())
    }

    // ── Defender-to-taker point conversion ───────────────────────────────────
    //
    // When the user toggles to "defender mode" in the UI, the entered points are
    // converted to taker points with: takerPoints = 91 − defenderPoints.
    // These tests verify that the conversion produces correct scoring outcomes.

    @Test
    fun `defender points convert to correct taker points — taker wins`() {
        // Defenders scored 30 pts → taker scored 61 pts.
        // With 2 bouts the threshold is 41 → taker wins.
        val defenderPoints = 30
        val takerPoints = 91 - defenderPoints   // 61
        assertTrue(takerWon(bouts = 2, points = takerPoints))
    }

    @Test
    fun `defender points convert to correct taker points — taker loses`() {
        // Defenders scored 60 pts → taker scored 31 pts.
        // With 2 bouts the threshold is 41 → taker loses.
        val defenderPoints = 60
        val takerPoints = 91 - defenderPoints   // 31
        assertFalse(takerWon(bouts = 2, points = takerPoints))
    }

    @Test
    fun `defender points conversion is symmetric`() {
        // For any valid defender score d, converting twice returns the original:
        // takerPoints = 91 − d; back = 91 − takerPoints == d.
        for (d in 0..91) {
            assertEquals(d, 91 - (91 - d))
        }
    }

    @Test
    fun `defender score 0 means taker scored all points`() {
        // If defenders scored nothing (0), the taker scored all 91 points.
        assertEquals(91, 91 - 0)
        assertTrue(takerWon(bouts = 0, points = 91))  // easily wins regardless of bouts
    }

    @Test
    fun `defender score 91 means taker scored nothing`() {
        // If defenders scored everything (91), the taker scored 0.
        assertEquals(0, 91 - 91)
        assertFalse(takerWon(bouts = 3, points = 0))  // loses even with 3 bouts (needs ≥36)
    }

    // ── buildScoreTableData ───────────────────────────────────────────────────
    //
    // Spec: the function accumulates per-round deltas into running totals, formats
    // each total with an explicit sign (+N / -N), and returns one ScoreRowData per
    // round with (a) the formatted cell strings and (b) raw integer values for
    // colour coding. It was extracted from ScoreHistoryScreen / FinalScoreScreen
    // in issue #75 to eliminate the duplicated running-totals loop.

    @Test
    fun `buildScoreTableData returns empty list when roundHistory is empty`() {
        val result = buildScoreTableData(listOf("Alice", "Bob"), emptyList())
        assertTrue("Expected empty list for empty history", result.isEmpty())
    }

    @Test
    fun `buildScoreTableData returns one row per round`() {
        val rounds = listOf(
            RoundResult(1, "Alice", null, null, null,
                mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25)),
            RoundResult(2, "Bob",   null, null, null,
                mapOf("Alice" to -30, "Bob" to 15, "Charlie" to 15))
        )
        val result = buildScoreTableData(listOf("Alice", "Bob", "Charlie"), rounds)
        assertEquals(2, result.size)
    }

    @Test
    fun `buildScoreTableData first cell is the round number`() {
        val rounds = listOf(
            RoundResult(3, "Alice", null, null, null,
                mapOf("Alice" to 10, "Bob" to -10))
        )
        val result = buildScoreTableData(listOf("Alice", "Bob"), rounds)
        assertEquals("3", result[0].cells[0])
    }

    @Test
    fun `buildScoreTableData formats positive total with plus sign`() {
        val rounds = listOf(
            RoundResult(1, "Alice", null, null, null,
                mapOf("Alice" to 50, "Bob" to -25))
        )
        val result = buildScoreTableData(listOf("Alice", "Bob"), rounds)
        // cells[1] is Alice's cumulative total after round 1 → +50
        assertEquals("+50", result[0].cells[1])
    }

    @Test
    fun `buildScoreTableData formats negative total without plus sign`() {
        val rounds = listOf(
            RoundResult(1, "Alice", null, null, null,
                mapOf("Alice" to 50, "Bob" to -25))
        )
        val result = buildScoreTableData(listOf("Alice", "Bob"), rounds)
        // cells[2] is Bob's cumulative total after round 1 → -25
        assertEquals("-25", result[0].cells[2])
    }

    @Test
    fun `buildScoreTableData formats zero total with plus sign`() {
        val rounds = listOf(
            RoundResult(1, "Alice", null, null, null,
                mapOf("Alice" to 0, "Bob" to 0))
        )
        val result = buildScoreTableData(listOf("Alice", "Bob"), rounds)
        assertEquals("+0", result[0].cells[1])
        assertEquals("+0", result[0].cells[2])
    }

    @Test
    fun `buildScoreTableData accumulates running totals across rounds`() {
        // Round 1: Alice +50, Bob -25  →  Alice 50, Bob -25
        // Round 2: Alice -30, Bob +15  →  Alice 20, Bob -10
        val rounds = listOf(
            RoundResult(1, "Alice", null, null, null,
                mapOf("Alice" to 50, "Bob" to -25)),
            RoundResult(2, "Bob",   null, null, null,
                mapOf("Alice" to -30, "Bob" to 15))
        )
        val result = buildScoreTableData(listOf("Alice", "Bob"), rounds)
        // After round 1
        assertEquals("+50", result[0].cells[1])
        assertEquals("-25", result[0].cells[2])
        // After round 2 — cumulative, not just the round delta
        assertEquals("+20", result[1].cells[1])
        assertEquals("-10", result[1].cells[2])
    }

    @Test
    fun `buildScoreTableData scoreValues index 0 is null (round-number column)`() {
        val rounds = listOf(
            RoundResult(1, "Alice", null, null, null,
                mapOf("Alice" to 50, "Bob" to -25))
        )
        val result = buildScoreTableData(listOf("Alice", "Bob"), rounds)
        // Index 0 is the round-number column — no semantic colour.
        assertNull(result[0].scoreValues[0])
    }

    @Test
    fun `buildScoreTableData scoreValues for player columns hold raw integer totals`() {
        val rounds = listOf(
            RoundResult(1, "Alice", null, null, null,
                mapOf("Alice" to 50, "Bob" to -25))
        )
        val result = buildScoreTableData(listOf("Alice", "Bob"), rounds)
        assertEquals(50,  result[0].scoreValues[1])
        assertEquals(-25, result[0].scoreValues[2])
    }

    @Test
    fun `buildScoreTableData skipped rounds (empty playerScores) do not change totals`() {
        // Round 1: normal round — Alice +50, Bob -25.
        // Round 2: skipped (no playerScores) — totals unchanged.
        val rounds = listOf(
            RoundResult(1, "Alice", null, null, null,
                mapOf("Alice" to 50, "Bob" to -25)),
            RoundResult(2, "Bob",   null, null, null)  // skipped: playerScores = emptyMap()
        )
        val result = buildScoreTableData(listOf("Alice", "Bob"), rounds)
        // Round 2 row: totals must still be +50 / -25 (unchanged from round 1).
        assertEquals("+50", result[1].cells[1])
        assertEquals("-25", result[1].cells[2])
    }

    @Test
    fun `buildScoreTableData cells list length equals playerCount plus 1`() {
        // 1 extra cell for the round-number column.
        val players = listOf("Alice", "Bob", "Charlie")
        val rounds  = listOf(RoundResult(1, "Alice", null, null, null))
        val result  = buildScoreTableData(players, rounds)
        assertEquals(players.size + 1, result[0].cells.size)
        assertEquals(players.size + 1, result[0].scoreValues.size)
    }

    // ── poigneeThresholds — official FFT thresholds per player count ──────────
    //
    // Source: R-RO201206.pdf
    //   Page 11 (3 players):  simple 13, double 15, triple 18
    //   Standard (4 players): simple 10, double 13, triple 15
    //   Page 12 (5 players):  simple  8, double 10, triple 13

    @Test
    fun `poigneeThresholds for 3 players returns 13-15-18`() {
        // 3-player game uses more trumps per hand (24 cards each, 78-card deck).
        // The bar for showing a Poignée is therefore higher.
        val (simple, double, triple) = poigneeThresholds(3)
        assertEquals(13, simple)
        assertEquals(15, double)
        assertEquals(18, triple)
    }

    @Test
    fun `poigneeThresholds for 4 players returns 10-13-15`() {
        // Standard 4-player thresholds used in most Tarot games.
        val (simple, double, triple) = poigneeThresholds(4)
        assertEquals(10, simple)
        assertEquals(13, double)
        assertEquals(15, triple)
    }

    @Test
    fun `poigneeThresholds for 5 players returns 8-10-13`() {
        // In 5-player games each player holds only 15 cards (distributed 3 by 3),
        // so the Poignée bars are lower: 8, 10, and 13 trumps respectively.
        // This matches page 12 of R-RO201206.pdf.
        val (simple, double, triple) = poigneeThresholds(5)
        assertEquals(8,  simple)
        assertEquals(10, double)
        assertEquals(13, triple)
    }

    @Test
    fun `poigneeThresholds thresholds are strictly increasing within each player count`() {
        // A double Poignée always requires more trumps than a simple Poignée,
        // and a triple always requires more than a double — for every player count.
        for (n in 3..5) {
            val (simple, double, triple) = poigneeThresholds(n)
            assertTrue("simple < double for $n players", simple < double)
            assertTrue("double < triple for $n players", double < triple)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `poigneeThresholds throws for player count below 3`() {
        poigneeThresholds(2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `poigneeThresholds throws for player count above 5`() {
        poigneeThresholds(6)
    }

    // ── 5-player scoring — taker plays alone (called King in the Dog) ─────────
    //
    // When the called King is in the Dog, the taker plays 1 vs 4.
    // The official rule (page 12): the taker collects ALL points in + or −.
    // In computePlayerScores this is the case where partnerName = null in a 5-player
    // game (allPlayers.size = 5), so numDefenders = 4 and takerMultiplier = 4.

    @Test
    fun `computePlayerScores 5-player taker-alone win — taker gets 4x, each defender gets -1x`() {
        // 5 players, no partner (King was in the Dog): numDefenders = 4, takerMultiplier = 4.
        // roundScore = 30 → Alice gets +120; Bob/Charlie/Dave/Eve each get -30.
        // Sum: 120 - 30 - 30 - 30 - 30 = 0.
        val players = listOf("Alice", "Bob", "Charlie", "Dave", "Eve")
        val scores  = computePlayerScores(
            allPlayers  = players,
            takerName   = "Alice",
            partnerName = null,   // taker plays alone — no partner
            won         = true,
            roundScore  = 30
        )
        assertEquals(+120, scores["Alice"])   // taker: +4 × 30
        assertEquals( -30, scores["Bob"])     // defender: −30
        assertEquals( -30, scores["Charlie"]) // defender: −30
        assertEquals( -30, scores["Dave"])    // defender: −30
        assertEquals( -30, scores["Eve"])     // defender: −30
        assertEquals(0, scores.values.sum())  // zero-sum invariant
    }

    @Test
    fun `computePlayerScores 5-player taker-alone loss — taker pays 4x, each defender gains 1x`() {
        // Same setup, taker loses: sign flips.
        val players = listOf("Alice", "Bob", "Charlie", "Dave", "Eve")
        val scores  = computePlayerScores(
            allPlayers  = players,
            takerName   = "Alice",
            partnerName = null,
            won         = false,
            roundScore  = 30
        )
        assertEquals(-120, scores["Alice"])   // taker: −4 × 30
        assertEquals( +30, scores["Bob"])     // defender: +30
        assertEquals( +30, scores["Charlie"])
        assertEquals( +30, scores["Dave"])
        assertEquals( +30, scores["Eve"])
        assertEquals(0, scores.values.sum())
    }

    @Test
    fun `computePlayerScores 5-player with partner win — taker 2x, partner 1x, defenders -1x`() {
        // Standard 5-player game with a called partner.
        // numDefenders = 3, takerMultiplier = 2, partnerMultiplier = 1.
        // roundScore = 40 → Alice +80, Bob +40, Charlie/Dave/Eve -40 each.
        val players = listOf("Alice", "Bob", "Charlie", "Dave", "Eve")
        val scores  = computePlayerScores(
            allPlayers  = players,
            takerName   = "Alice",
            partnerName = "Bob",
            won         = true,
            roundScore  = 40
        )
        assertEquals(+80, scores["Alice"])    // taker: +2 × 40
        assertEquals(+40, scores["Bob"])      // partner: +1 × 40
        assertEquals(-40, scores["Charlie"])  // defender: −40
        assertEquals(-40, scores["Dave"])
        assertEquals(-40, scores["Eve"])
        assertEquals(0, scores.values.sum())
    }

    @Test
    fun `computePlayerScores 5-player zero-sum holds for all roles`() {
        // Regardless of who wins or what roundScore is, the sum of all 5 players is always 0.
        for (won in listOf(true, false)) {
            val scores = computePlayerScores(
                allPlayers  = listOf("A", "B", "C", "D", "E"),
                takerName   = "A",
                partnerName = "B",
                won         = won,
                roundScore  = 57
            )
            assertEquals("Zero-sum must hold when won=$won", 0, scores.values.sum())
        }
    }

    // ── Chelem.DEFENDERS_REALIZED — new case from R-RO201206.pdf p.6 ─────────
    //
    // "Paradoxalement, il arrive que la défense inflige un Chelem au déclarant.
    // Dans ce cas, chaque défenseur reçoit, en plus de la marque normale,
    // une prime de 200 points."
    //
    // Financial effect: each defender +200, taker pays -200 per defender.
    // Same sign as ANNOUNCED_NOT_REALIZED but distinct semantics.

    @Test
    fun `Chelem has exactly five values after adding DEFENDERS_REALIZED`() {
        // Adding DEFENDERS_REALIZED brings the total from 4 to 5.
        assertEquals(5, Chelem.entries.size)
    }

    @Test
    fun `chelemBonus returns -200 for DEFENDERS_REALIZED`() {
        // Each defender gains 200 from the taker → bonus = -200 (taker's perspective).
        assertEquals(-200, chelemBonus(Chelem.DEFENDERS_REALIZED))
    }

    @Test
    fun `chelemBonus DEFENDERS_REALIZED distribution is zero-sum in a 4-player game`() {
        // 4-player: taker delta = -200 × 3 = -600; each defender delta = +200. Sum = 0.
        val bonus = chelemBonus(Chelem.DEFENDERS_REALIZED)
        val numDefenders = 3
        val takerDelta    = bonus * numDefenders   // -600
        val defenderDelta = -bonus                  // +200 each
        assertEquals(0, takerDelta + defenderDelta * numDefenders)
        assertEquals(-600, takerDelta)
        assertEquals(+200, defenderDelta)
    }

    @Test
    fun `chelemBonus DEFENDERS_REALIZED distribution is zero-sum in a 3-player game`() {
        // 3-player: 2 defenders. Taker delta = -200 × 2 = -400; each defender +200.
        val bonus = chelemBonus(Chelem.DEFENDERS_REALIZED)
        val numDefenders = 2
        assertEquals(0, bonus * numDefenders + (-bonus) * numDefenders)
    }

    @Test
    fun `applyBonuses — DEFENDERS_REALIZED adds 200 to each defender, taker pays (4-player)`() {
        // Base: taker=-120, each defender=+40 (taker chuted with 3 defenders).
        // DEFENDERS_REALIZED bonus = -200 per defender to taker.
        // Taker delta = -200 × 3 = -600 → -120 - 600 = -720
        // Each defender delta = +200 → +40 + 200 = +240
        val base = mapOf("Alice" to -120, "Bob" to +40, "Charlie" to +40, "Dave" to +40)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(chelem = Chelem.DEFENDERS_REALIZED),
            takerName    = "Alice",
            won          = false,
            numDefenders = 3
        )
        assertEquals(-720, result["Alice"])
        assertEquals(+240, result["Bob"])
        assertEquals(+240, result["Charlie"])
        assertEquals(+240, result["Dave"])
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — DEFENDERS_REALIZED partner is unaffected (5-player)`() {
        // In a 5-player game the partner does not participate in the chelem bonus.
        // Base: Alice=-60, Bob=-30, Charlie/Dave/Eve=+30 each.
        val base = mapOf(
            "Alice" to -60, "Bob" to -30,
            "Charlie" to +30, "Dave" to +30, "Eve" to +30
        )
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(
                partnerName = "Bob",
                chelem      = Chelem.DEFENDERS_REALIZED
            ),
            takerName    = "Alice",
            won          = false,
            numDefenders = 3
        )
        // Partner (Bob) must not change.
        assertEquals(-30, result["Bob"])
        // Result must be zero-sum.
        assertEquals(0, result.values.sum())
    }

    // ── Petit au Bout with each contract multiplier (complete coverage) ────────
    //
    // The issue requires "Petit au Bout: taker wins it, defender wins it,
    // and both with each contract multiplier."
    //
    // petitAuBoutBonus() is already tested for each contract multiplier in isolation.
    // These tests verify the full applyBonuses pipeline for each of the four contracts.

    @Test
    fun `applyBonuses — PAB by taker with Prise (×1) — taker gains 10 per defender`() {
        // Prise → pabAmount = 10. Taker achieved → pabSign = +1. 3 defenders.
        // Taker delta = +10 × 3 = +30; each defender delta = -10.
        // Base uses roundScore=25 (Prise at exact threshold): taker=+75, defenders=-25 each.
        val base = mapOf("Alice" to +75, "Bob" to -25, "Charlie" to -25, "Dave" to -25)
        // We only care about the PAB component here, so Chelem.NONE and no Poignée.
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.PRISE,
            details      = details(petitAuBout = "Alice"),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(+75 + 30, result["Alice"])   // +105
        assertEquals(-25 - 10, result["Bob"])     // -35
        assertEquals(-25 - 10, result["Charlie"]) // -35
        assertEquals(-25 - 10, result["Dave"])    // -35
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — PAB by defender with Prise (×1) — each defender gains 10`() {
        // Prise → pabAmount = 10. Defender achieved → pabSign = -1. 3 defenders.
        // Taker delta = -10 × 3 = -30; each defender delta = +10.
        // Base uses roundScore=25 (Prise at exact threshold): taker=+75, defenders=-25 each.
        val base = mapOf("Alice" to +75, "Bob" to -25, "Charlie" to -25, "Dave" to -25)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.PRISE,
            details      = details(petitAuBout = "Bob"),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(+75 - 30, result["Alice"])   // +45
        assertEquals(-25 + 10, result["Bob"])     // -15
        assertEquals(-25 + 10, result["Charlie"]) // -15
        assertEquals(-25 + 10, result["Dave"])    // -15
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — PAB by taker with Garde Sans (×4) — taker gains 40 per defender`() {
        // Garde Sans → pabAmount = 40. Taker achieved → pabSign = +1. 3 defenders.
        // Taker delta = +40 × 3 = +120; each defender delta = -40.
        val base = mapOf("Alice" to +300, "Bob" to -100, "Charlie" to -100, "Dave" to -100)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE_SANS,
            details      = details(petitAuBout = "Alice"),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(+300 + 120, result["Alice"])  // +420
        assertEquals(-100 - 40,  result["Bob"])    // -140
        assertEquals(-100 - 40,  result["Charlie"])
        assertEquals(-100 - 40,  result["Dave"])
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — PAB by defender with Garde Sans (×4) — each defender gains 40`() {
        // Garde Sans → pabAmount = 40. Defender achieved → pabSign = -1. 3 defenders.
        // Taker delta = -40 × 3 = -120; each defender delta = +40.
        val base = mapOf("Alice" to +300, "Bob" to -100, "Charlie" to -100, "Dave" to -100)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE_SANS,
            details      = details(petitAuBout = "Bob"),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(+300 - 120, result["Alice"])  // +180
        assertEquals(-100 + 40,  result["Bob"])    // -60
        assertEquals(-100 + 40,  result["Charlie"])
        assertEquals(-100 + 40,  result["Dave"])
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — PAB by taker with Garde Contre (×6) — taker gains 60 per defender`() {
        // Garde Contre → pabAmount = 60. Taker achieved → pabSign = +1. 3 defenders.
        // Taker delta = +60 × 3 = +180; each defender delta = -60.
        val base = mapOf("Alice" to +450, "Bob" to -150, "Charlie" to -150, "Dave" to -150)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE_CONTRE,
            details      = details(petitAuBout = "Alice"),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(+450 + 180, result["Alice"])  // +630
        assertEquals(-150 - 60,  result["Bob"])    // -210
        assertEquals(-150 - 60,  result["Charlie"])
        assertEquals(-150 - 60,  result["Dave"])
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — PAB by defender with Garde Contre (×6) — each defender gains 60`() {
        // Garde Contre → pabAmount = 60. Defender achieved → pabSign = -1. 3 defenders.
        // Taker delta = -60 × 3 = -180; each defender delta = +60.
        val base = mapOf("Alice" to +450, "Bob" to -150, "Charlie" to -150, "Dave" to -150)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE_CONTRE,
            details      = details(petitAuBout = "Bob"),
            takerName    = "Alice",
            won          = true,
            numDefenders = 3
        )
        assertEquals(+450 - 180, result["Alice"])  // +270
        assertEquals(-150 + 60,  result["Bob"])    // -90
        assertEquals(-150 + 60,  result["Charlie"])
        assertEquals(-150 + 60,  result["Dave"])
        assertEquals(0, result.values.sum())
    }

    // ── Poignée: each bonus level when the taker loses ─────────────────────────
    //
    // When the taker loses the Poignée bonus goes to the defenders (winning camp).
    // Tests for the taker-wins case already exist above; these cover the taker-loses path
    // for simple, double, and triple Poignée explicitly.

    @Test
    fun `applyBonuses — simple poignee when taker loses goes to defenders (4-player)`() {
        // Simple poignée: 20 pts. Taker lost → pSign = -1.
        // Taker delta = -1 × 20 × 3 = -60; each defender delta = +20.
        val base = mapOf("Alice" to -75, "Bob" to +25, "Charlie" to +25, "Dave" to +25)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.PRISE,
            details      = details(poignee = "Alice"),
            takerName    = "Alice",
            won          = false,
            numDefenders = 3
        )
        assertEquals(-75 - 60, result["Alice"])   // -135
        assertEquals(+25 + 20, result["Bob"])     // +45
        assertEquals(+25 + 20, result["Charlie"])
        assertEquals(+25 + 20, result["Dave"])
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — double poignee when taker loses goes to defenders (4-player)`() {
        // Double poignée: 30 pts. Taker lost → pSign = -1.
        // Taker delta = -1 × 30 × 3 = -90; each defender delta = +30.
        val base = mapOf("Alice" to -75, "Bob" to +25, "Charlie" to +25, "Dave" to +25)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE,
            details      = details(doublePoignee = "Bob"),
            takerName    = "Alice",
            won          = false,
            numDefenders = 3
        )
        assertEquals(-75 - 90, result["Alice"])   // -165
        assertEquals(+25 + 30, result["Bob"])     // +55
        assertEquals(+25 + 30, result["Charlie"])
        assertEquals(+25 + 30, result["Dave"])
        assertEquals(0, result.values.sum())
    }

    @Test
    fun `applyBonuses — triple poignee when taker loses goes to defenders (4-player)`() {
        // Triple poignée: 40 pts. Taker lost → pSign = -1.
        // Taker delta = -1 × 40 × 3 = -120; each defender delta = +40.
        val base = mapOf("Alice" to -75, "Bob" to +25, "Charlie" to +25, "Dave" to +25)
        val result = applyBonuses(
            baseScores   = base,
            contract     = Contract.GARDE_SANS,
            details      = details(triplePoignee = "Alice"),
            takerName    = "Alice",
            won          = false,
            numDefenders = 3
        )
        assertEquals(-75 - 120, result["Alice"])  // -195
        assertEquals(+25 + 40,  result["Bob"])    // +65
        assertEquals(+25 + 40,  result["Charlie"])
        assertEquals(+25 + 40,  result["Dave"])
        assertEquals(0, result.values.sum())
    }

    // ── End-to-end scoring examples from R-RO201206.pdf page 9 ────────────────
    //
    // "La marque en donnes libres" — five worked examples from the official rulebook.
    // These tests provide the highest-confidence verification that the scoring pipeline
    // (calculateRoundScore → computePlayerScores → applyBonuses) produces the exact
    // values published by the FFT.
    //
    // The helper pdfExample() wires the full pipeline together.
    //
    // All examples are 4-player games unless stated otherwise.

    private fun pdfExample(
        contract: Contract,
        bouts: Int,
        points: Int,
        partnerName: String?      = null,
        petitAuBout: String?      = null,
        poignee: String?          = null,
        doublePoignee: String?    = null,
        triplePoignee: String?    = null,
        chelem: Chelem            = Chelem.NONE,
        allPlayers: List<String>  = listOf("Alice", "Bob", "Charlie", "Dave"),
        takerName: String         = "Alice"
    ): Map<String, Int> {
        val won         = takerWon(bouts, points)
        val roundScore  = calculateRoundScore(contract, bouts, points)
        val numDefenders = if (partnerName != null) 3 else allPlayers.size - 1
        val base = computePlayerScores(allPlayers, takerName, partnerName, won, roundScore)
        val det  = RoundDetails(
            bouts         = bouts,
            points        = points,
            partnerName   = partnerName,
            petitAuBout   = petitAuBout,
            poignee       = poignee,
            doublePoignee = doublePoignee,
            chelem        = chelem
        )
        return applyBonuses(base, contract, det, takerName, won, numDefenders)
    }

    @Test
    fun `PDF example 1 — Garde, 2 bouts, 49 pts, simple poignee, PAB by taker`() {
        // Source: R-RO201206.pdf p.9, example 1.
        //
        // "Le preneur tente une Garde, présente une Poignée de 10 Atouts.
        //  Il mène le Petit au Bout et réalise 49 points en détenant deux Bouts."
        //
        // Breakdown:
        //   diff = 49 - 41 = 8
        //   base = (25 + 8) × 2 = 66
        //   Poignée = 20 (flat)
        //   PAB = 10 × 2 = 20 (Garde)
        //   Total per defender = 66 + 20 + 20 = 106
        //   Taker = 106 × 3 = +318
        //
        // Expected: each defender −106, taker +318.
        val scores = pdfExample(
            contract    = Contract.GARDE,
            bouts       = 2,
            points      = 49,
            poignee     = "Alice",
            petitAuBout = "Alice"
        )
        assertEquals(+318, scores["Alice"])
        assertEquals(-106, scores["Bob"])
        assertEquals(-106, scores["Charlie"])
        assertEquals(-106, scores["Dave"])
        assertEquals(0, scores.values.sum())
    }

    @Test
    fun `PDF example 2 — Garde Sans, 1 bout, 29 pts, PAB by defender, taker wins`() {
        // Source: R-RO201206.pdf p.9, example 2.
        //
        // "Le preneur gagne une Garde Sans de 4 points, mais le Petit est mené
        //  au Bout par la Défense."
        //
        // Note: "4 points" = 4 points above the threshold.
        // With 1 bout, threshold = 51. So taker scored 51 + 4 - wait, let me re-read.
        // "une Garde Sans de 4 points" means the taker gained 4 points above threshold.
        // But the PDF says score=116 for base and 116 - 40 = 76 per defender.
        //
        // Actually the PDF text says: "(25+4) × 4 (Garde Sans) = 116" and
        // "Il faut retrancher 40 pour le Petit au Bout". So diff = 4.
        //
        // With any bout count, diff = 4 means takerPoints = requiredPoints + 4.
        // We use bouts=0, points=60 → required=56, diff=4. But that gives (25+4)×4=116. ✅
        //
        // PAB by defender: pabAmount = 10 × 4 = 40. pabSign = -1.
        // Taker delta = -40 × 3 = -120; each defender delta = +40.
        // Net per defender = 116 - 40 = 76. Taker = 76 × 3 = +228.
        //
        // Expected: each defender −76, taker +228.
        val scores = pdfExample(
            contract    = Contract.GARDE_SANS,
            bouts       = 0,
            points      = 60,           // 60 - 56 = 4 above threshold
            petitAuBout = "Bob"         // a defender captured the Petit
        )
        assertEquals(+228, scores["Alice"])
        assertEquals(-76,  scores["Bob"])
        assertEquals(-76,  scores["Charlie"])
        assertEquals(-76,  scores["Dave"])
        assertEquals(0, scores.values.sum())
    }

    @Test
    fun `PDF example 3 — Prise, 0 bouts, taker loses by 7, simple poignee, PAB by taker`() {
        // Source: R-RO201206.pdf p.9, example 3.
        //
        // "Le preneur chute une Prise de 7 après avoir présenté une Poignée de 10 Atouts,
        //  mais en menant le Petit au Bout."
        //
        // "chute de 7" = lost by 7 points below threshold.
        // Threshold for 0 bouts = 56. Taker scored 56 - 7 = 49.
        //
        // Breakdown:
        //   diff = 7; Prise ×1 → base = (25 + 7) × 1 = 32 per defender (taker lost)
        //   Poignée = 20 → goes to winners (defenders, since taker lost): +20 per defender
        //   PAB by taker → pabSign = +1: taker gains 10×1 per defender
        //   Net per defender = 32 + 20 - 10 = 42
        //   Taker = -(42 × 3) = -126
        //
        // Expected: each defender +42, taker -126.
        val scores = pdfExample(
            contract    = Contract.PRISE,
            bouts       = 0,
            points      = 49,           // 56 - 7 = 49
            poignee     = "Alice",      // taker declared the Poignée
            petitAuBout = "Alice"       // taker captured the Petit on the last trick
        )
        assertEquals(-126, scores["Alice"])
        assertEquals(+42,  scores["Bob"])
        assertEquals(+42,  scores["Charlie"])
        assertEquals(+42,  scores["Dave"])
        assertEquals(0, scores.values.sum())
    }

    @Test
    fun `PDF example 4 — Garde, 2 bouts, taker wins by 11, defender declared simple poignee`() {
        // Source: R-RO201206.pdf p.9, example 4.
        //
        // "Le preneur gagne une Garde de 11, la Défense ayant présenté une Poignée."
        //
        // Threshold for 2 bouts = 41. Taker scored 41 + 11 = 52.
        //
        // Breakdown:
        //   diff = 11; Garde ×2 → base = (25 + 11) × 2 = 72
        //   Poignée = 20 → goes to taker (wins): taker +20 per defender
        //   Net per defender = 72 + 20 = 92
        //   Taker = 92 × 3 = +276
        //
        // Expected: each defender −92, taker +276.
        val scores = pdfExample(
            contract = Contract.GARDE,
            bouts    = 2,
            points   = 52,              // 41 + 11
            poignee  = "Bob"            // a defender declared the Poignée
        )
        assertEquals(+276, scores["Alice"])
        assertEquals(-92,  scores["Bob"])
        assertEquals(-92,  scores["Charlie"])
        assertEquals(-92,  scores["Dave"])
        assertEquals(0, scores.values.sum())
    }

    @Test
    fun `PDF example 5 — Garde, 2 bouts, 87 pts, announced chelem, simple poignee, PAB`() {
        // Source: R-RO201206.pdf p.9, example 5.
        //
        // "Sur une Garde, le preneur annonce et réussit le Chelem, montre une Poignée de
        //  10 Atouts. Avec 2 Bouts, le preneur réalise 87 points."
        //
        // Breakdown:
        //   diff = 87 - 41 = 46; Garde ×2 → base = (46 + 25) × 2 = 142
        //   Poignée = 20 (taker wins → taker collects)
        //   PAB = 10 × 2 = 20 (taker achieved)
        //   Chelem announced & realized = +400
        //   Total per defender = 142 + 20 + 20 + 400 = 582
        //   Taker = 582 × 3 = +1746
        //
        // Expected: each defender −582, taker +1746.
        val scores = pdfExample(
            contract    = Contract.GARDE,
            bouts       = 2,
            points      = 87,
            poignee     = "Alice",
            petitAuBout = "Alice",
            chelem      = Chelem.ANNOUNCED_REALIZED
        )
        assertEquals(+1746, scores["Alice"])
        assertEquals(-582,  scores["Bob"])
        assertEquals(-582,  scores["Charlie"])
        assertEquals(-582,  scores["Dave"])
        assertEquals(0, scores.values.sum())
    }

    // ── 3-player end-to-end scoring ───────────────────────────────────────────
    //
    // In a 3-player game (R-RO201206.pdf p.11):
    //   - Contracts and scoring formula are the same as 4 players.
    //   - Each defender receives the same per-defender amount as in a 4-player game.
    //   - The taker's score is multiplied by 2 (numDefenders = 2, takerMultiplier = 2).
    //   - The total of 3 scores is zero.

    @Test
    fun `3-player — Garde win, taker gets 2x, two defenders get -1x, zero-sum`() {
        // Garde ×2, 2 bouts, scored 56. diff = 56 - 41 = 15. base = (25+15)×2 = 80.
        // 3 players: taker gets +2 × 80 = +160; each defender gets -80.
        val players = listOf("Alice", "Bob", "Charlie")
        val won        = takerWon(bouts = 2, points = 56)          // true
        val roundScore = calculateRoundScore(Contract.GARDE, 2, 56) // 80
        val scores = computePlayerScores(players, "Alice", null, won, roundScore)
        assertEquals(+160, scores["Alice"])
        assertEquals(-80,  scores["Bob"])
        assertEquals(-80,  scores["Charlie"])
        assertEquals(0, scores.values.sum())
    }

    @Test
    fun `3-player — Garde loss, taker pays 2x, two defenders gain 1x, zero-sum`() {
        // Garde ×2, 1 bout, scored 40. diff = 51 - 40 = 11. base = (25+11)×2 = 72.
        // 3 players, taker lost: taker gets -2 × 72 = -144; each defender gets +72.
        val players = listOf("Alice", "Bob", "Charlie")
        val won        = takerWon(bouts = 1, points = 40)          // false
        val roundScore = calculateRoundScore(Contract.GARDE, 1, 40) // 72
        val scores = computePlayerScores(players, "Alice", null, won, roundScore)
        assertEquals(-144, scores["Alice"])
        assertEquals(+72,  scores["Bob"])
        assertEquals(+72,  scores["Charlie"])
        assertEquals(0, scores.values.sum())
    }

    @Test
    fun `3-player — full round with PAB and poignee is zero-sum`() {
        // 3-player: Alice (taker) wins a Prise, 1 bout, 60 pts.
        // diff = 60 - 51 = 9. base = (25+9)×1 = 34.
        // With 2 defenders: taker = +2×34 = +68; each defender = -34.
        // Poignée by Alice (taker wins → taker collects): +20 per defender, taker +40.
        // PAB by Alice → pabSign=+1: taker +10×2=+20, each defender -10.
        val players = listOf("Alice", "Bob", "Charlie")
        val won        = takerWon(1, 60)
        val roundScore = calculateRoundScore(Contract.PRISE, 1, 60) // 34
        val base = computePlayerScores(players, "Alice", null, won, roundScore)
        val det  = RoundDetails(
            bouts = 1, points = 60, partnerName = null,
            petitAuBout = "Alice", poignee = "Alice",
            doublePoignee = null, chelem = Chelem.NONE
        )
        val result = applyBonuses(base, Contract.PRISE, det, "Alice", won, numDefenders = 2)
        assertEquals(0, result.values.sum())
        // Verify taker's total: +68 (base) + 40 (poignée) + 20 (PAB) = +128
        assertEquals(+128, result["Alice"])
        // Each defender: -34 (base) - 20 (poignée) - 10 (PAB) = -64
        assertEquals(-64, result["Bob"])
        assertEquals(-64, result["Charlie"])
    }

    // ── 5-player end-to-end scoring ───────────────────────────────────────────

    @Test
    fun `5-player — Garde win with partner, all three bonuses, zero-sum`() {
        // 5-player: Alice (taker) called Bob (partner). 3 defenders.
        // Garde ×2, 2 bouts, scored 56. diff = 15. base = (25+15)×2 = 80.
        // Alice +2×80=+160, Bob +1×80=+80, Charlie/Dave/Eve -80 each.
        //
        // Bonuses (all three):
        //   PAB by Alice (taker's camp, pabSign=+1): taker +20×3=+60, each defender -20.
        //   Poignée simple (taker wins, pSign=+1):   taker +20×3=+60, each defender -20.
        //   Chelem NOT_ANNOUNCED_REALIZED (+200):     taker +200×3=+600, each defender -200.
        //   Partner (Bob): unaffected by all three bonuses.
        val players = listOf("Alice", "Bob", "Charlie", "Dave", "Eve")
        val won        = takerWon(2, 56)
        val roundScore = calculateRoundScore(Contract.GARDE, 2, 56)
        val base = computePlayerScores(players, "Alice", "Bob", won, roundScore)
        val det  = RoundDetails(
            bouts = 2, points = 56, partnerName = "Bob",
            petitAuBout = "Alice", poignee = "Alice",
            doublePoignee = null, chelem = Chelem.NOT_ANNOUNCED_REALIZED
        )
        val result = applyBonuses(base, Contract.GARDE, det, "Alice", won, numDefenders = 3)
        assertEquals(0, result.values.sum())
        // Bob (partner) must match his base score — untouched by bonuses.
        assertEquals(base["Bob"], result["Bob"])
    }

    @Test
    fun `5-player — taker alone (no partner), Prise win — taker gets 4x, zero-sum`() {
        // King was in the Dog, so taker plays 1v4. partnerName = null, 5 players.
        // computePlayerScores: numDefenders = 4, takerMultiplier = 4.
        // Prise ×1, 3 bouts, scored 36 (exact threshold). diff = 0. base = 25.
        // Alice +4×25=+100; each of Bob/Charlie/Dave/Eve -25.
        val players = listOf("Alice", "Bob", "Charlie", "Dave", "Eve")
        val won        = takerWon(3, 36)
        val roundScore = calculateRoundScore(Contract.PRISE, 3, 36)
        val scores = computePlayerScores(players, "Alice", null, won, roundScore)
        assertEquals(+100, scores["Alice"])
        assertEquals(-25,  scores["Bob"])
        assertEquals(-25,  scores["Charlie"])
        assertEquals(-25,  scores["Dave"])
        assertEquals(-25,  scores["Eve"])
        assertEquals(0, scores.values.sum())
    }
}

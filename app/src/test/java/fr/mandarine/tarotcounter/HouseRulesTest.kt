package fr.mandarine.tarotcounter

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the house scoring rules (issue #236): petit au bout flat or
 * multiplied, poignée flat or multiplied. FFT is the default everywhere.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HouseRulesTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val flatPetit      = ScoringRules(petitAuBoutMultiplied = false)
    private val timesPoignee   = ScoringRules(poigneeMultiplied = true)
    private val bothHouseRules = ScoringRules(petitAuBoutMultiplied = false, poigneeMultiplied = true)

    private val three = listOf("A", "B", "C")

    // ── Model ─────────────────────────────────────────────────────────────────

    @Test
    fun `defaults are the FFT rules`() {
        val rules = ScoringRules()
        assertTrue(rules.petitAuBoutMultiplied)
        assertFalse(rules.poigneeMultiplied)
        assertEquals(ScoringRules.FFT, rules)
        assertTrue(rules.isFft)
        assertFalse(flatPetit.isFft)
        assertFalse(timesPoignee.isFft)
    }

    // ── Petit au bout ─────────────────────────────────────────────────────────

    @Test
    fun `petit au bout is multiplied by the contract under FFT`() {
        assertEquals(10, petitAuBoutBonus(Contract.PRISE))
        assertEquals(20, petitAuBoutBonus(Contract.GARDE, ScoringRules.FFT))
        assertEquals(40, petitAuBoutBonus(Contract.GARDE_SANS))
        assertEquals(60, petitAuBoutBonus(Contract.GARDE_CONTRE))
    }

    @Test
    fun `petit au bout is a flat 10 with the house rule, whatever the contract`() {
        Contract.entries.forEach { assertEquals(10, petitAuBoutBonus(it, flatPetit)) }
    }

    // ── Poignée ───────────────────────────────────────────────────────────────

    @Test
    fun `poignee stays flat under FFT`() {
        Contract.entries.forEach { assertEquals(30, scaledPoigneeBonus(30, it)) }
    }

    @Test
    fun `poignee is multiplied by the contract with the house rule`() {
        assertEquals(20, scaledPoigneeBonus(20, Contract.PRISE, timesPoignee))
        assertEquals(40, scaledPoigneeBonus(20, Contract.GARDE, timesPoignee))
        assertEquals(120, scaledPoigneeBonus(30, Contract.GARDE_SANS, timesPoignee))
        assertEquals(240, scaledPoigneeBonus(40, Contract.GARDE_CONTRE, timesPoignee))
        assertEquals(0, scaledPoigneeBonus(0, Contract.GARDE_CONTRE, timesPoignee))
    }

    // ── Whole round ───────────────────────────────────────────────────────────

    // Garde Sans (×4), 2 bouts, 51 pts → made by 10 → base (25+10)×4 = 140 per defender.
    // Taker won the Petit on the last trick and declared a simple poignée.
    private fun round(rules: ScoringRules) = previewRound(
        three, "A", Contract.GARDE_SANS,
        RoundDetails(
            bouts = 2, points = 51, partnerName = null, petitAuBout = "A",
            poignees = listOf("A"), chelem = Chelem.NONE
        ),
        rules = rules
    )

    @Test
    fun `FFT round - petit 40, poignee 20`() {
        val p = round(ScoringRules.FFT).playerScores
        assertEquals(-(140 + 40 + 20), p["B"])
        assertEquals(2 * 200, p["A"])
    }

    @Test
    fun `flat petit round - petit 10, poignee 20`() {
        val p = round(flatPetit).playerScores
        assertEquals(-(140 + 10 + 20), p["B"])
        assertEquals(0, p.values.sum())
    }

    @Test
    fun `multiplied poignee round - petit 40, poignee 80`() {
        val p = round(timesPoignee).playerScores
        assertEquals(-(140 + 40 + 80), p["C"])
        assertEquals(0, p.values.sum())
    }

    @Test
    fun `both house rules - petit 10, poignee 80`() {
        val p = round(bothHouseRules).playerScores
        assertEquals(-(140 + 10 + 80), p["B"])
        assertEquals(2 * 230, p["A"])
    }

    @Test
    fun `a lost round still gives the multiplied poignee to the defenders`() {
        val p = previewRound(
            three, "A", Contract.GARDE,
            RoundDetails(bouts = 0, points = 50, partnerName = null, petitAuBout = null,
                doublePoignees = listOf("B"), chelem = Chelem.NONE),
            rules = timesPoignee
        ).playerScores
        // Short by 6 → (25+6)×2 = 62; double poignée 30 × 2 = 60.
        assertEquals(62 + 60, p["B"])
        assertEquals(-2 * 122, p["A"])
    }

    @Test
    fun `applyBonuses defaults to FFT`() {
        val details = RoundDetails(bouts = 3, points = 40, partnerName = null, petitAuBout = "A",
            poignees = listOf("A"), chelem = Chelem.NONE)
        val base = three.associateWith { 0 }
        assertEquals(
            applyBonuses(base, Contract.GARDE, details, "A", true, 2, ScoringRules.FFT),
            applyBonuses(base, Contract.GARDE, details, "A", true, 2)
        )
    }

    // ── ViewModel and persistence ─────────────────────────────────────────────

    @Test
    fun `scoringRules starts at FFT and reflects the stored value`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        assertEquals(ScoringRules.FFT, vm.scoringRules.value)
        // stateIn(WhileSubscribed) only reads the storage while someone collects.
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.scoringRules.collect {} }
        vm.setScoringRules(flatPetit)
        assertEquals(flatPetit, storage.lastSavedScoringRules)
        assertEquals(flatPetit, vm.scoringRules.value)
        job.cancel()
    }

    @Test
    fun `a new game captures the rules and scores with them`() {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(three, inProgressGame = null, scoringRules = flatPetit)
        assertEquals(flatPetit, vm.gameRules)
        vm.recordPlayed(
            "A", Contract.GARDE_SANS,
            RoundDetails(bouts = 2, points = 51, partnerName = null, petitAuBout = "A", chelem = Chelem.NONE)
        )
        assertEquals(-(140 + 10), vm.roundHistory.single().playerScores["B"])
        assertEquals(flatPetit, storage.lastSavedInProgress?.scoringRules)
        vm.endGame()
        assertEquals(flatPetit, storage.lastAddedGame?.scoringRules)
    }

    @Test
    fun `a resumed game keeps its own rules, not the current setting`() {
        val saved = InProgressGame(
            gameId = "g", playerNames = three, currentRound = 2, startingIndex = 0,
            rounds = emptyList(), scoringRules = timesPoignee
        )
        val vm = GameViewModel(Application(), FakeGameStorage())
        vm.initGame(three, saved, scoringRules = flatPetit)
        assertEquals(timesPoignee, vm.gameRules)
    }

    @Test
    fun `games saved before issue 236 decode with the FFT rules`() {
        val json = Json { ignoreUnknownKeys = true }
        val oldInProgress = """{"playerNames":["A","B","C"],"currentRound":1,"startingIndex":0,"rounds":[]}"""
        assertEquals(ScoringRules.FFT, json.decodeFromString<InProgressGame>(oldInProgress).scoringRules)
        val oldSaved = """{"id":"g","datestamp":0,"playerNames":["A"],"rounds":[],"finalScores":{}}"""
        assertEquals(ScoringRules.FFT, json.decodeFromString<SavedGame>(oldSaved).scoringRules)
        // A partial object (a future rule added later) keeps the missing fields at FFT.
        assertEquals(flatPetit, json.decodeFromString<ScoringRules>("""{"petitAuBoutMultiplied":false}"""))
    }
}

package fr.mandarine.tarotcounter

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the dead-player rule (issue #231): at a table of 6 — or of 4 with the
 * option on — the dealer sits out each round and scores 0.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeadPlayerTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val six  = listOf("A", "B", "C", "D", "E", "F")
    private val four = listOf("A", "B", "C", "D")

    private fun details(
        bouts: Int = 2,
        points: Int = 51,
        partner: String? = null,
        petit: String? = null,
        poignees: List<String> = emptyList(),
        chelem: Chelem = Chelem.NONE
    ) = RoundDetails(
        bouts = bouts, points = points, partnerName = partner, petitAuBout = petit,
        poignees = poignees, chelem = chelem
    )

    // ── Table-size rules ──────────────────────────────────────────────────────

    @Test
    fun `table sizes are 3 to 6`() = assertEquals(listOf(3, 4, 5, 6), TABLE_SIZES)

    @Test
    fun `sit-out is mandatory only at 6`() {
        assertTrue(dealerSitsOutMandatory(6))
        listOf(3, 4, 5).forEach { assertFalse(dealerSitsOutMandatory(it)) }
    }

    @Test
    fun `sit-out is optional only at 4`() {
        assertTrue(dealerSitsOutOptional(4))
        listOf(3, 5, 6).forEach { assertFalse(dealerSitsOutOptional(it)) }
    }

    @Test
    fun `resolveDealerSitsOut forces 6, follows the choice at 4, refuses 3 and 5`() {
        assertTrue(resolveDealerSitsOut(6, false))
        assertTrue(resolveDealerSitsOut(6, true))
        assertTrue(resolveDealerSitsOut(4, true))
        assertFalse(resolveDealerSitsOut(4, false))
        assertFalse(resolveDealerSitsOut(3, true))
        assertFalse(resolveDealerSitsOut(5, true))
    }

    @Test
    fun `activePlayers drops the dealer only when they sit out, keeping seat order`() {
        assertEquals(listOf("A", "B", "D", "E", "F"), activePlayers(six, "C", true))
        assertEquals(six, activePlayers(six, "C", false))
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    @Test
    fun `6-player round scores like a 5-player round and gives the dead player 0`() {
        // Garde, 2 bouts, 51 pts → made by 10 → (25+10)×2 = 70.
        val p = previewRound(six, "B", Contract.GARDE, details(partner = "D"), deadPlayer = "A")
        assertTrue(p.won)
        assertEquals(0, p.playerScores["A"])
        assertEquals(140, p.playerScores["B"])  // taker ×2
        assertEquals(70, p.playerScores["D"])   // partner ×1
        assertEquals(-70, p.playerScores["C"])
        assertEquals(-70, p.playerScores["E"])
        assertEquals(-70, p.playerScores["F"])
        assertEquals(6, p.playerScores.size)
        assertEquals(0, p.playerScores.values.sum())
    }

    @Test
    fun `6-player round without a called partner yet scores the taker against the 4 others`() {
        val p = previewRound(six, "B", Contract.PRISE, details(), deadPlayer = "A")
        // (25+10)×1 = 35 from each of the 4 active defenders.
        assertEquals(140, p.playerScores["B"])
        assertEquals(0, p.playerScores["A"])
        assertEquals(0, p.playerScores.values.sum())
    }

    @Test
    fun `4-player table with a dead dealer scores like a 3-player round`() {
        val p = previewRound(four, "B", Contract.GARDE, details(points = 30), deadPlayer = "D")
        // Lost by 11 → (25+11)×2 = 72; taker pays both active defenders.
        assertFalse(p.won)
        assertEquals(-144, p.playerScores["B"])
        assertEquals(72, p.playerScores["A"])
        assertEquals(72, p.playerScores["C"])
        assertEquals(0, p.playerScores["D"])
    }

    @Test
    fun `bonuses are not paid by the dead player`() {
        val p = previewRound(
            four, "B", Contract.PRISE,
            details(petit = "B", poignees = listOf("B"), chelem = Chelem.NOT_ANNOUNCED_REALIZED),
            deadPlayer = "D"
        )
        assertEquals(0, p.playerScores["D"])
        assertEquals(0, p.playerScores.values.sum())
        // Base 35 + petit 10 + poignée 20 + chelem 200 = 265 per active defender.
        assertEquals(-265, p.playerScores["A"])
        assertEquals(530, p.playerScores["B"])
    }

    @Test
    fun `previewRound without a dead player is unchanged`() {
        val p = previewRound(four, "B", Contract.GARDE, details())
        assertEquals(3, p.playerScores.count { it.value == -70 })
        assertEquals(210, p.playerScores["B"])
        assertEquals(4, p.playerScores.size)
    }

    // ── ViewModel ─────────────────────────────────────────────────────────────

    private fun vm(storage: FakeGameStorage = FakeGameStorage()) = GameViewModel(Application(), storage)

    @Test
    fun `a 6-player game always has the dealer sitting out`() {
        val vm = vm()
        vm.initGame(six, inProgressGame = null, startingIndexOverride = 0, dealerSitsOut = false)
        assertTrue(vm.dealerSitsOut)
        assertEquals("A", vm.deadPlayer)
        assertEquals(listOf("B", "C", "D", "E", "F"), vm.activePlayers)
    }

    @Test
    fun `the dead player rotates with the dealer`() {
        val vm = vm()
        vm.initGame(six, inProgressGame = null, startingIndexOverride = 5)
        assertEquals("F", vm.deadPlayer)
        vm.recordSkipped()
        assertEquals("A", vm.deadPlayer)
        assertFalse("A" in vm.activePlayers)
        assertTrue("F" in vm.activePlayers)
    }

    @Test
    fun `nobody sits out at 3 or 5 players, or at 4 without the option`() {
        listOf(listOf("A", "B", "C"), four, listOf("A", "B", "C", "D", "E")).forEach { names ->
            val vm = vm()
            vm.initGame(names, inProgressGame = null, dealerSitsOut = names.size != 4)
            assertFalse(vm.dealerSitsOut)
            assertNull(vm.deadPlayer)
            assertEquals(names, vm.activePlayers)
        }
    }

    @Test
    fun `recordPlayed stores the dead player with a 0 score and persists the setting`() {
        val storage = FakeGameStorage()
        val vm = vm(storage)
        vm.initGame(four, inProgressGame = null, startingIndexOverride = 3, dealerSitsOut = true)
        vm.recordPlayed("A", Contract.GARDE, details())
        val round = vm.roundHistory.single()
        assertEquals("D", round.deadPlayer)
        assertEquals(0, round.playerScores["D"])
        assertEquals(140, round.playerScores["A"])
        assertEquals(true, storage.lastSavedInProgress?.dealerSitsOut)
    }

    @Test
    fun `recordSkipped stores who sat out`() {
        val vm = vm()
        vm.initGame(six, inProgressGame = null, startingIndexOverride = 2)
        vm.recordSkipped()
        assertEquals("C", vm.roundHistory.single().deadPlayer)
    }

    @Test
    fun `endGame saves the sit-out setting with the game`() {
        val storage = FakeGameStorage()
        val vm = vm(storage)
        vm.initGame(six, inProgressGame = null, startingIndexOverride = 0)
        vm.recordSkipped()
        vm.endGame()
        assertEquals(true, storage.lastAddedGame?.dealerSitsOut)
    }

    @Test
    fun `a resumed game keeps its own sit-out setting`() {
        val saved = InProgressGame(
            gameId = "g", playerNames = four, currentRound = 2, startingIndex = 0,
            rounds = emptyList(), dealerSitsOut = true
        )
        val vm = vm()
        vm.initGame(four, saved, dealerSitsOut = false)
        assertTrue(vm.dealerSitsOut)
        assertEquals("B", vm.deadPlayer)   // round 2 → second dealer
    }

    // ── Backward compatibility ────────────────────────────────────────────────

    @Test
    fun `games saved before issue 231 still decode with nobody sitting out`() {
        val json = Json { ignoreUnknownKeys = true }
        val oldInProgress = """{"gameId":"g","playerNames":["A","B","C"],"currentRound":2,""" +
            """"startingIndex":0,"rounds":[{"roundNumber":1,"takerName":"A","contract":null,""" +
            """"details":null,"won":null}]}"""
        val game = json.decodeFromString<InProgressGame>(oldInProgress)
        assertFalse(game.dealerSitsOut)
        assertNull(game.rounds.single().deadPlayer)

        val oldSaved = """{"id":"g","datestamp":0,"playerNames":["A","B","C"],"rounds":[],""" +
            """"finalScores":{"A":0,"B":0,"C":0}}"""
        assertFalse(json.decodeFromString<SavedGame>(oldSaved).dealerSitsOut)
    }
}

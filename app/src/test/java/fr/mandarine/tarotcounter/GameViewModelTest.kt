package fr.mandarine.tarotcounter

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [GameViewModel].
 *
 * [GameStorage] (DataStore) is replaced by [FakeGameStorage] so these tests run on the
 * JVM without an Android device or emulator.
 *
 * Two things make this possible:
 *
 *   1. `testOptions { unitTests.isReturnDefaultValues = true }` in build.gradle.kts
 *      lets us call `Application()` on the JVM.  Android's stub classes normally throw
 *      "Method not mocked" — this flag makes them return default values (null / 0 / false)
 *      instead.  GameViewModel never calls any Application method when storage is injected,
 *      so null internals are safe.
 *
 *   2. `Dispatchers.setMain(UnconfinedTestDispatcher())` replaces the main dispatcher that
 *      `viewModelScope` uses.  UnconfinedTestDispatcher runs coroutines eagerly on the
 *      calling thread, so `viewModelScope.launch { … }` finishes before the next line of
 *      test code runs — no `advanceUntilIdle` or `delay` needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    // UnconfinedTestDispatcher: coroutines start immediately, run synchronously.
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        // Make viewModelScope use our test dispatcher instead of Dispatchers.Main.
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // Restore Dispatchers.Main so other test classes are not affected.
        Dispatchers.resetMain()
    }

    // ── Test data builders ────────────────────────────────────────────────────

    /** Minimal [SavedGame] suitable for use in tests. */
    private fun savedGame(id: String = "id-1") = SavedGame(
        id          = id,
        datestamp   = 0L,
        playerNames = listOf("Alice", "Bob", "Charlie"),
        rounds      = emptyList(),
        finalScores = mapOf("Alice" to 0, "Bob" to 0, "Charlie" to 0)
    )

    /** Minimal [InProgressGame] suitable for use in tests. */
    private fun inProgressGame() = InProgressGame(
        playerNames   = listOf("Alice", "Bob", "Charlie"),
        currentRound  = 2,
        startingIndex = 0,
        rounds        = emptyList()
    )

    // ── saveGame ──────────────────────────────────────────────────────────────

    @Test
    fun `saveGame calls addGame exactly once`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)

        vm.saveGame(savedGame())

        assertEquals(1, storage.addGameCallCount)
    }

    @Test
    fun `saveGame passes the correct SavedGame to storage`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        val game = savedGame("game-42")

        vm.saveGame(game)

        assertEquals(game, storage.lastAddedGame)
    }

    @Test
    fun `saveGame also clears the in-progress game in the same invocation`() = runTest {
        // Spec: saving a completed game must atomically clear the in-progress entry
        // so the same game never appears both as completed and resumable.
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)

        vm.saveGame(savedGame())

        assertEquals("addGame should have been called", 1, storage.addGameCallCount)
        assertEquals("clearInProgressGame should have been called", 1, storage.clearInProgressCallCount)
    }

    @Test
    fun `saveGame twice with the same id does not create a duplicate in pastGames`() = runTest {
        // Regression test for: game saved twice when user ends game, navigates back,
        // and ends it again. The second save must replace the first, not append.
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        val game = savedGame("same-id")

        val collected = mutableListOf<List<SavedGame>>()
        val job = launch(testDispatcher) { vm.pastGames.collect { collected.add(it) } }

        vm.saveGame(game)
        // Save the same game again (same id, possibly different datestamp).
        vm.saveGame(game.copy(datestamp = 99L))

        // The list must contain exactly one entry, not two.
        assertEquals("History should have exactly one entry after saving the same game twice",
            1, collected.last().size)
        // The stored entry should be the second (most-recent) version.
        assertEquals(99L, collected.last().first().datestamp)
        job.cancel()
    }

    // ── saveInProgressGame ────────────────────────────────────────────────────

    @Test
    fun `saveInProgressGame delegates to storage with the correct game`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        val game = inProgressGame()

        vm.saveInProgressGame(game)

        assertEquals(1, storage.saveInProgressCallCount)
        assertEquals(game, storage.lastSavedInProgress)
    }

    // ── clearInProgressGame ───────────────────────────────────────────────────

    @Test
    fun `clearInProgressGame delegates to storage`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)

        vm.clearInProgressGame()

        assertEquals(1, storage.clearInProgressCallCount)
    }

    // ── setLocale ─────────────────────────────────────────────────────────────

    @Test
    fun `setLocale FR delegates to saveLocale with the correct locale`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)

        vm.setLocale(AppLocale.FR)

        assertEquals(1, storage.saveLocaleCallCount)
        assertEquals(AppLocale.FR, storage.lastSavedLocale)
    }

    @Test
    fun `setLocale EN delegates to saveLocale with the correct locale`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)

        vm.setLocale(AppLocale.EN)

        assertEquals(AppLocale.EN, storage.lastSavedLocale)
    }

    // ── StateFlow initial values ──────────────────────────────────────────────
    //
    // StateFlow.value is readable synchronously — no subscription needed.
    // These tests verify the `initialValue` used in each stateIn() call.

    @Test
    fun `pastGames initial value is an empty list`() {
        val vm = GameViewModel(Application(), FakeGameStorage())
        assertEquals(emptyList<SavedGame>(), vm.pastGames.value)
    }

    @Test
    fun `inProgressGame initial value is null`() {
        val vm = GameViewModel(Application(), FakeGameStorage())
        assertNull(vm.inProgressGame.value)
    }

    @Test
    fun `locale initial value is null`() {
        val vm = GameViewModel(Application(), FakeGameStorage())
        assertNull(vm.locale.value)
    }

    // ── StateFlow reflects storage updates ────────────────────────────────────
    //
    // stateIn(WhileSubscribed) only starts collecting the upstream Flow when there
    // is at least one active subscriber.  We subscribe via launch { collect { … } }
    // before asserting — UnconfinedTestDispatcher ensures the upstream (the Fake's
    // MutableStateFlow) has emitted by the time launch returns.

    @Test
    fun `pastGames StateFlow reflects games seeded before ViewModel is created`() = runTest {
        val games = listOf(savedGame("a"), savedGame("b"))
        val storage = FakeGameStorage().also { it.seedGames(games) }
        val vm = GameViewModel(Application(), storage)

        val collected = mutableListOf<List<SavedGame>>()
        val job = launch(testDispatcher) { vm.pastGames.collect { collected.add(it) } }

        assertEquals(games, collected.last())
        job.cancel()
    }

    @Test
    fun `pastGames StateFlow updates after saveGame adds a new game`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        val game = savedGame()

        val collected = mutableListOf<List<SavedGame>>()
        val job = launch(testDispatcher) { vm.pastGames.collect { collected.add(it) } }

        vm.saveGame(game)

        assertEquals(listOf(game), collected.last())
        job.cancel()
    }

    @Test
    fun `inProgressGame StateFlow reflects game seeded before ViewModel is created`() = runTest {
        val game = inProgressGame()
        val storage = FakeGameStorage().also { it.seedInProgressGame(game) }
        val vm = GameViewModel(Application(), storage)

        val collected = mutableListOf<InProgressGame?>()
        val job = launch(testDispatcher) { vm.inProgressGame.collect { collected.add(it) } }

        assertEquals(game, collected.last())
        job.cancel()
    }

    @Test
    fun `inProgressGame StateFlow becomes null after clearInProgressGame`() = runTest {
        val storage = FakeGameStorage().also { it.seedInProgressGame(inProgressGame()) }
        val vm = GameViewModel(Application(), storage)

        val collected = mutableListOf<InProgressGame?>()
        val job = launch(testDispatcher) { vm.inProgressGame.collect { collected.add(it) } }

        vm.clearInProgressGame()

        assertNull("After clearInProgressGame the StateFlow should emit null", collected.last())
        job.cancel()
    }

    @Test
    fun `inProgressGame StateFlow reflects saved in-progress game after saveInProgressGame`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        val game = inProgressGame()

        val collected = mutableListOf<InProgressGame?>()
        val job = launch(testDispatcher) { vm.inProgressGame.collect { collected.add(it) } }

        vm.saveInProgressGame(game)

        assertEquals(game, collected.last())
        job.cancel()
    }

    @Test
    fun `locale StateFlow reflects locale seeded before ViewModel is created`() = runTest {
        val storage = FakeGameStorage().also { it.seedLocale(AppLocale.FR) }
        val vm = GameViewModel(Application(), storage)

        val collected = mutableListOf<AppLocale?>()
        val job = launch(testDispatcher) { vm.locale.collect { collected.add(it) } }

        assertEquals(AppLocale.FR, collected.last())
        job.cancel()
    }

    @Test
    fun `locale StateFlow updates after setLocale is called`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)

        val collected = mutableListOf<AppLocale?>()
        val job = launch(testDispatcher) { vm.locale.collect { collected.add(it) } }

        vm.setLocale(AppLocale.EN)

        assertEquals(AppLocale.EN, collected.last())
        job.cancel()
    }

    // ── setTheme ──────────────────────────────────────────────────────────────

    @Test
    fun `setTheme DARK delegates to saveTheme with the correct theme`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)

        vm.setTheme(AppTheme.DARK)

        assertEquals(1, storage.saveThemeCallCount)
        assertEquals(AppTheme.DARK, storage.lastSavedTheme)
    }

    @Test
    fun `setTheme LIGHT delegates to saveTheme with the correct theme`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)

        vm.setTheme(AppTheme.LIGHT)

        assertEquals(AppTheme.LIGHT, storage.lastSavedTheme)
    }

    @Test
    fun `theme initial value is null`() {
        val vm = GameViewModel(Application(), FakeGameStorage())
        assertNull(vm.theme.value)
    }

    @Test
    fun `theme StateFlow reflects theme seeded before ViewModel is created`() = runTest {
        val storage = FakeGameStorage().also { it.seedTheme(AppTheme.DARK) }
        val vm = GameViewModel(Application(), storage)

        val collected = mutableListOf<AppTheme?>()
        val job = launch(testDispatcher) { vm.theme.collect { collected.add(it) } }

        assertEquals(AppTheme.DARK, collected.last())
        job.cancel()
    }

    @Test
    fun `theme StateFlow updates after setTheme is called`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)

        val collected = mutableListOf<AppTheme?>()
        val job = launch(testDispatcher) { vm.theme.collect { collected.add(it) } }

        vm.setTheme(AppTheme.DARK)

        assertEquals(AppTheme.DARK, collected.last())
        job.cancel()
    }

    // ── initGame ──────────────────────────────────────────────────────────────

    @Test
    fun `initGame fresh game starts at round 1`() {
        val vm = GameViewModel(Application(), FakeGameStorage())

        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        assertEquals(1, vm.currentRound)
    }

    @Test
    fun `initGame stores display names`() {
        val vm = GameViewModel(Application(), FakeGameStorage())
        val names = listOf("Alice", "Bob", "Charlie")

        vm.initGame(names, inProgressGame = null)

        assertEquals(names, vm.displayNames)
    }

    @Test
    fun `initGame restores currentRound from inProgressGame`() {
        val vm = GameViewModel(Application(), FakeGameStorage())
        val saved = InProgressGame(
            gameId        = "g1",
            playerNames   = listOf("Alice", "Bob"),
            currentRound  = 5,
            startingIndex = 0,
            rounds        = emptyList()
        )

        vm.initGame(saved.playerNames, saved)

        assertEquals(5, vm.currentRound)
    }

    @Test
    fun `initGame restores rounds from inProgressGame`() {
        val vm = GameViewModel(Application(), FakeGameStorage())
        val round = RoundResult(1, "Alice", contract = null, details = null, won = null)
        val saved = InProgressGame(
            gameId        = "g1",
            playerNames   = listOf("Alice", "Bob"),
            currentRound  = 2,
            startingIndex = 0,
            rounds        = listOf(round)
        )

        vm.initGame(saved.playerNames, saved)

        assertEquals(1, vm.roundHistory.size)
        assertEquals(round, vm.roundHistory[0])
    }

    @Test
    fun `initGame clears previous session rounds`() {
        val vm = GameViewModel(Application(), FakeGameStorage())
        // First game: one skipped round.
        vm.initGame(listOf("Alice", "Bob"), inProgressGame = null)
        vm.recordSkipped()
        assertEquals(1, vm.roundHistory.size)

        // Second game: fresh start — history must be empty.
        vm.initGame(listOf("Alice", "Bob"), inProgressGame = null)

        assertEquals(0, vm.roundHistory.size)
    }

    // ── recordSkipped ─────────────────────────────────────────────────────────

    @Test
    fun `recordSkipped advances currentRound`() = runTest {
        val vm = GameViewModel(Application(), FakeGameStorage())
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.recordSkipped()

        assertEquals(2, vm.currentRound)
    }

    @Test
    fun `recordSkipped adds a skipped RoundResult to history`() = runTest {
        val vm = GameViewModel(Application(), FakeGameStorage())
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.recordSkipped()

        assertEquals(1, vm.roundHistory.size)
        assertNull(vm.roundHistory[0].contract)
        assertNull(vm.roundHistory[0].won)
    }

    @Test
    fun `recordSkipped calls saveInProgressGame`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.recordSkipped()

        assertEquals(1, storage.saveInProgressCallCount)
    }

    // ── recordPlayed ──────────────────────────────────────────────────────────

    /** Minimal [RoundDetails] suitable for use in recordPlayed tests. */
    private fun basicDetails(bouts: Int = 0, points: Int = 0) = RoundDetails(
        bouts         = bouts,
        points        = points,
        partnerName   = null,
        petitAuBout   = null,
        poignee       = null,
        doublePoignee = null,
        chelem        = Chelem.NONE
    )

    @Test
    fun `recordPlayed advances currentRound`() = runTest {
        val vm = GameViewModel(Application(), FakeGameStorage())
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.recordPlayed(Contract.GARDE, basicDetails())

        assertEquals(2, vm.currentRound)
    }

    @Test
    fun `recordPlayed adds a RoundResult with the correct contract`() = runTest {
        val vm = GameViewModel(Application(), FakeGameStorage())
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.recordPlayed(Contract.GARDE, basicDetails())

        assertEquals(Contract.GARDE, vm.roundHistory[0].contract)
    }

    @Test
    fun `recordPlayed marks result as Lost when points below threshold`() = runTest {
        // 0 bouts → requires 56 pts; 0 < 56 → Lost.
        val vm = GameViewModel(Application(), FakeGameStorage())
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.recordPlayed(Contract.GARDE, basicDetails(bouts = 0, points = 0))

        assertEquals(false, vm.roundHistory[0].won)
    }

    @Test
    fun `recordPlayed marks result as Won when points meet threshold`() = runTest {
        // 3 bouts → requires 36 pts; 91 >= 36 → Won.
        val vm = GameViewModel(Application(), FakeGameStorage())
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.recordPlayed(Contract.GARDE, basicDetails(bouts = 3, points = 91))

        assertEquals(true, vm.roundHistory[0].won)
    }

    @Test
    fun `recordPlayed calls saveInProgressGame`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.recordPlayed(Contract.GARDE, basicDetails())

        assertEquals(1, storage.saveInProgressCallCount)
    }

    @Test
    fun `recordPlayed populates playerScores for all players`() = runTest {
        val players = listOf("Alice", "Bob", "Charlie")
        val vm = GameViewModel(Application(), FakeGameStorage())
        vm.initGame(players, inProgressGame = null)

        vm.recordPlayed(Contract.GARDE, basicDetails(bouts = 0, points = 0))

        // Every player should have a score entry (zero-sum game, so all 3 must be present).
        val scores = vm.roundHistory[0].playerScores
        players.forEach { name ->
            assertTrue("$name should have a score entry", scores.containsKey(name))
        }
        // Tarot is zero-sum: the sum of all player scores for a round is always 0.
        assertEquals(0, scores.values.sum())
    }

    // ── endGame ───────────────────────────────────────────────────────────────

    @Test
    fun `endGame saves a SavedGame when rounds exist`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)
        vm.recordSkipped()  // at least one round needed

        vm.endGame()

        assertEquals(1, storage.addGameCallCount)
    }

    @Test
    fun `endGame does not save when no rounds have been played`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.endGame()

        assertEquals(0, storage.addGameCallCount)
    }

    @Test
    fun `endGame clears in-progress game even when no rounds have been played`() = runTest {
        // Regression test for issue #90: ending a game with zero rounds must cancel
        // the in-progress entry so it does not linger as a resumable game on the landing screen.
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.endGame()

        assertEquals(
            "clearInProgressGame must be called even when no rounds were played",
            1, storage.clearInProgressCallCount
        )
    }

    @Test
    fun `endGame saved game contains correct playerNames`() = runTest {
        val players = listOf("Alice", "Bob", "Charlie")
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(players, inProgressGame = null)
        vm.recordSkipped()

        vm.endGame()

        assertEquals(players, storage.lastAddedGame?.playerNames)
    }

    @Test
    fun `endGame clears the in-progress game`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)
        vm.recordSkipped()

        vm.endGame()

        // saveGame() (called by endGame()) also clears in-progress via the ViewModel.
        assertEquals(1, storage.clearInProgressCallCount)
    }

    @Test
    fun `endGame saved game includes all completed rounds`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)
        vm.recordSkipped()
        vm.recordSkipped()

        vm.endGame()

        assertEquals(2, storage.lastAddedGame?.rounds?.size)
    }

    @Test
    fun `endGame saved game has correct finalScores for every player`() = runTest {
        val players = listOf("Alice", "Bob", "Charlie")
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        val saved = InProgressGame(
            gameId        = "g1",
            playerNames   = players,
            currentRound  = 1,
            startingIndex = 0,
            rounds        = emptyList()
        )
        vm.initGame(players, saved)
        vm.recordSkipped()  // skipped round → all deltas are 0

        vm.endGame()

        val finalScores = storage.lastAddedGame?.finalScores
        // All three players must appear in finalScores (even if all are 0 after a skipped round).
        assertEquals(players.toSet(), finalScores?.keys)
    }

    @Test
    fun `endGame saved game id is non-blank`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)
        vm.recordSkipped()

        vm.endGame()

        assertTrue(
            "Saved game id must not be blank",
            storage.lastAddedGame?.id?.isNotBlank() == true
        )
    }

    // ── currentTaker ──────────────────────────────────────────────────────────

    @Test
    fun `currentTaker returns empty string when displayNames is empty`() {
        val vm = GameViewModel(Application(), FakeGameStorage())
        // initGame has not been called — _displayNames is empty.
        assertEquals("", vm.currentTaker)
    }

    @Test
    fun `currentTaker returns player at startingIndex for round 1`() {
        val players = listOf("Alice", "Bob", "Charlie")
        val vm = GameViewModel(Application(), FakeGameStorage())
        // Restore a game where Bob (index 1) is the first taker.
        vm.initGame(players, InProgressGame(
            gameId        = "g1",
            playerNames   = players,
            currentRound  = 1,
            startingIndex = 1,
            rounds        = emptyList()
        ))
        assertEquals("Bob", vm.currentTaker)
    }

    @Test
    fun `currentTaker advances to the next player on round 2`() {
        val players = listOf("Alice", "Bob", "Charlie")
        val vm = GameViewModel(Application(), FakeGameStorage())
        // Alice started (index 0); now it is round 2 → Bob (index 1).
        vm.initGame(players, InProgressGame(
            gameId        = "g1",
            playerNames   = players,
            currentRound  = 2,
            startingIndex = 0,
            rounds        = emptyList()
        ))
        assertEquals("Bob", vm.currentTaker)
    }

    @Test
    fun `currentTaker wraps back to the first player after a full cycle`() {
        val players = listOf("Alice", "Bob", "Charlie")
        val vm = GameViewModel(Application(), FakeGameStorage())
        // startingIndex=0, 3 players → round 4 wraps to index 0 (Alice again).
        // Formula: (0 + 4 − 1) % 3 = 3 % 3 = 0.
        vm.initGame(players, InProgressGame(
            gameId        = "g1",
            playerNames   = players,
            currentRound  = 4,
            startingIndex = 0,
            rounds        = emptyList()
        ))
        assertEquals("Alice", vm.currentTaker)
    }

    // ── initGame — startingIndex, gameId, displayNames ───────────────────────

    @Test
    fun `initGame restores startingIndex from inProgressGame`() {
        val vm = GameViewModel(Application(), FakeGameStorage())
        vm.initGame(
            listOf("Alice", "Bob", "Charlie"),
            InProgressGame(
                gameId        = "g1",
                playerNames   = listOf("Alice", "Bob", "Charlie"),
                currentRound  = 3,
                startingIndex = 2,
                rounds        = emptyList()
            )
        )
        assertEquals(2, vm.startingIndex)
    }

    @Test
    fun `initGame restores gameId from inProgressGame`() {
        // The saved gameId must be reused so that ending the same resumed game does
        // not create a duplicate entry (upsert logic relies on the id being stable).
        val vm = GameViewModel(Application(), FakeGameStorage())
        vm.initGame(
            listOf("Alice", "Bob"),
            InProgressGame(
                gameId        = "known-game-id",
                playerNames   = listOf("Alice", "Bob"),
                currentRound  = 2,
                startingIndex = 0,
                rounds        = emptyList()
            )
        )
        assertEquals("known-game-id", vm.gameId)
    }

    @Test
    fun `initGame uses playerNames from the inProgressGame argument`() {
        // Passing saved.playerNames to initGame must set displayNames to those names.
        // This test exercises InProgressGame.playerNames getter directly so mutations
        // that replace the getter's return value (emptyList) are caught.
        val players = listOf("Alice", "Bob", "Charlie")
        val saved = InProgressGame(
            gameId        = "g1",
            playerNames   = players,
            currentRound  = 2,
            startingIndex = 0,
            rounds        = emptyList()
        )
        val vm = GameViewModel(Application(), FakeGameStorage())
        vm.initGame(saved.playerNames, saved)
        assertEquals(players, vm.displayNames)
    }

    // ── recordPlayed — takerName stored in history ────────────────────────────

    @Test
    fun `recordPlayed stores the currentTaker as takerName in the round result`() = runTest {
        val players = listOf("Alice", "Bob", "Charlie")
        val vm = GameViewModel(Application(), FakeGameStorage())
        // Charlie (index 2) is the taker for round 1.
        vm.initGame(players, InProgressGame(
            gameId        = "g1",
            playerNames   = players,
            currentRound  = 1,
            startingIndex = 2,
            rounds        = emptyList()
        ))

        vm.recordPlayed(Contract.GARDE, basicDetails())

        assertEquals("Charlie", vm.roundHistory[0].takerName)
    }

    // ── recordPlayed — 5-player game (numDefenders = 3) ──────────────────────

    @Test
    fun `recordPlayed 5-player game distributes scores with 3 defenders`() = runTest {
        val players = listOf("Alice", "Bob", "Charlie", "Dave", "Eve")
        val vm = GameViewModel(Application(), FakeGameStorage())
        // Alice (index 0) takes, Bob is the called partner.
        vm.initGame(players, InProgressGame(
            gameId        = "g1",
            playerNames   = players,
            currentRound  = 1,
            startingIndex = 0,
            rounds        = emptyList()
        ))

        // Garde (×2), 2 bouts (threshold 41), 50 pts scored → taker wins (50 ≥ 41).
        // roundScore = (25 + |50−41|) × 2 = (25 + 9) × 2 = 68.
        // base: Alice = +2×68 = +136, Bob (partner) = +68,
        //       Charlie/Dave/Eve (3 defenders) = −68 each.
        val details = RoundDetails(
            bouts         = 2,
            points        = 50,
            partnerName   = "Bob",
            petitAuBout   = null,
            poignee       = null,
            doublePoignee = null,
            chelem        = Chelem.NONE
        )
        vm.recordPlayed(Contract.GARDE, details)

        val scores = vm.roundHistory[0].playerScores
        assertEquals(+136, scores["Alice"])
        assertEquals(+68,  scores["Bob"])
        assertEquals(-68,  scores["Charlie"])
        assertEquals(-68,  scores["Dave"])
        assertEquals(-68,  scores["Eve"])
        assertEquals(0, scores.values.sum())
    }

    // ── recordPlayed — 3-player game numDef = size−1 = 2 ─────────────────────

    @Test
    fun `recordPlayed 3-player game with petit au bout uses numDefenders of 2`() = runTest {
        // This test catches the mutation "size − 1" → "size + 1" (numDef 2 → 4)
        // and the "negated conditional" on partnerName != null (swaps 3 ↔ size−1 branch).
        //
        // Garde (×2), 2 bouts (threshold 41), 50 pts → taker wins.
        // roundScore = (25 + |50−41|) × 2 = 68.
        // base: Alice = +2×68 = +136, Bob/Charlie = −68 each.
        //
        // Petit au bout by Alice (taker's camp): pabAmount = 10×2 = 20, pabSign = +1.
        // numDef = 3 − 1 = 2 (correct).
        //   Alice delta  = +1 × 20 × 2 = +40  →  136 + 40  = +176
        //   Bob/Charlie  = −1 × 20     = −20  →  −68 − 20  = −88 each
        //
        // If numDef were wrongly 4 (size+1): Alice delta = +80 → +216, others −88 → sum = +216−88−88 = +40 ≠ 0.
        // If numDef were wrongly 3 (from negated conditional): Alice delta = +60 → +196, others −88 → sum = +196−88−88 = +20 ≠ 0.
        val players = listOf("Alice", "Bob", "Charlie")
        val vm = GameViewModel(Application(), FakeGameStorage())
        vm.initGame(players, InProgressGame(
            gameId = "g1", playerNames = players,
            currentRound = 1, startingIndex = 0, rounds = emptyList()
        ))
        vm.recordPlayed(Contract.GARDE, RoundDetails(
            bouts         = 2,
            points        = 50,
            partnerName   = null,
            petitAuBout   = "Alice",   // taker achieved petit au bout
            poignee       = null,
            doublePoignee = null,
            chelem        = Chelem.NONE
        ))
        val scores = vm.roundHistory[0].playerScores
        assertEquals(+176, scores["Alice"])
        assertEquals(-88,  scores["Bob"])
        assertEquals(-88,  scores["Charlie"])
        assertEquals(0, scores.values.sum())
    }

    // ── buildProgressSnapshot — contents verified via storage ─────────────────

    @Test
    fun `recordSkipped snapshot persisted to storage contains the new skipped round`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.recordSkipped()

        // The snapshot saved after recordSkipped must include the skipped round.
        assertEquals(1, storage.lastSavedInProgress?.rounds?.size)
    }

    @Test
    fun `recordSkipped snapshot persisted to storage has the advanced round number`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.recordSkipped()

        // currentRound is incremented before saving, so the snapshot must reflect round 2.
        assertEquals(2, storage.lastSavedInProgress?.currentRound)
    }

    @Test
    fun `recordPlayed snapshot persisted to storage contains the played round`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.recordPlayed(Contract.PRISE, basicDetails())

        assertEquals(1, storage.lastSavedInProgress?.rounds?.size)
    }

    @Test
    fun `recordPlayed snapshot persisted to storage has the advanced round number`() = runTest {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(listOf("Alice", "Bob", "Charlie"), inProgressGame = null)

        vm.recordPlayed(Contract.PRISE, basicDetails())

        assertEquals(2, storage.lastSavedInProgress?.currentRound)
    }
}

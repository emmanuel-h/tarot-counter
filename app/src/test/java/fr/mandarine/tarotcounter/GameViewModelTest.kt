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
}

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for the Play Store review prompt (issue #223):
 *   - [ReviewPromptPolicy]: the pure "should we ask now?" rules.
 *   - [GameViewModel]: counting finished games and exposing `shouldRequestReview`.
 *
 * The real Play launch is covered by the instrumented `PlayReviewLauncherTest`
 * (FakeReviewManager needs an Android Context).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewPromptTest {

    // Runs viewModelScope coroutines eagerly (see GameViewModelTest for details).
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // ── ReviewPromptPolicy ────────────────────────────────────────────────────

    @Test
    fun `threshold is the third finished game`() {
        assertEquals(3, ReviewPromptPolicy.GAMES_BEFORE_REVIEW)
    }

    @Test
    fun `does not ask before the threshold`() {
        assertFalse(ReviewPromptPolicy.shouldAsk(finishedGames = 0, alreadyAsked = false))
        assertFalse(ReviewPromptPolicy.shouldAsk(finishedGames = 2, alreadyAsked = false))
    }

    @Test
    fun `asks exactly at the threshold`() {
        assertTrue(ReviewPromptPolicy.shouldAsk(finishedGames = 3, alreadyAsked = false))
    }

    @Test
    fun `asks after the threshold if never asked`() {
        // e.g. Play Store was unavailable the first time, so the flow never ran.
        assertTrue(ReviewPromptPolicy.shouldAsk(finishedGames = 10, alreadyAsked = false))
    }

    @Test
    fun `never asks again once asked`() {
        assertFalse(ReviewPromptPolicy.shouldAsk(finishedGames = 3, alreadyAsked = true))
        assertFalse(ReviewPromptPolicy.shouldAsk(finishedGames = 50, alreadyAsked = true))
    }

    // ── GameViewModel ─────────────────────────────────────────────────────────

    private val players = listOf("Alice", "Bob", "Charlie")

    /** Starts a fresh game, skips one round (so it counts as played) and ends it. */
    private fun GameViewModel.playAndEndGame() {
        initGame(players, inProgressGame = null, startingIndexOverride = 0)
        recordSkipped()
        endGame()
    }

    @Test
    fun `ending a game with rounds increments the finished-games counter`() {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.playAndEndGame()
        assertEquals(1, storage.finishedGameCount)
    }

    @Test
    fun `ending a game with no rounds does not count it`() {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.initGame(players, inProgressGame = null)
        vm.endGame()
        assertEquals(0, storage.finishedGameCount)
    }

    @Test
    fun `ending the same game twice counts it once`() {
        // Game Over → back to the game → End Game again reuses the same game id.
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        vm.playAndEndGame()
        vm.recordSkipped()
        vm.endGame()
        assertEquals(1, storage.finishedGameCount)
    }

    @Test
    fun `each new game is counted`() {
        val storage = FakeGameStorage()
        val vm = GameViewModel(Application(), storage)
        repeat(3) { vm.playAndEndGame() }
        assertEquals(3, storage.finishedGameCount)
    }

    @Test
    fun `shouldRequestReview turns true on the third finished game`() = runTest {
        val storage = FakeGameStorage()
        storage.seedReviewState(finishedGames = 2)
        val vm = GameViewModel(Application(), storage)
        // A collector keeps the WhileSubscribed StateFlow active.
        val job = launch(testDispatcher) { vm.shouldRequestReview.collect {} }

        assertFalse(vm.shouldRequestReview.value)
        vm.playAndEndGame()
        assertTrue(vm.shouldRequestReview.value)
        job.cancel()
    }

    @Test
    fun `markReviewRequested persists the flag and stops further requests`() = runTest {
        val storage = FakeGameStorage()
        storage.seedReviewState(finishedGames = 3)
        val vm = GameViewModel(Application(), storage)
        val job = launch(testDispatcher) { vm.shouldRequestReview.collect {} }

        assertTrue(vm.shouldRequestReview.value)
        vm.markReviewRequested()
        assertTrue(storage.reviewRequested)
        assertEquals(1, storage.markReviewCallCount)
        assertFalse(vm.shouldRequestReview.value)

        // Further games never bring the prompt back.
        vm.playAndEndGame()
        assertFalse(vm.shouldRequestReview.value)
        job.cancel()
    }

    @Test
    fun `shouldRequestReview stays false when already asked`() = runTest {
        val storage = FakeGameStorage()
        storage.seedReviewState(finishedGames = 5, reviewRequested = true)
        val vm = GameViewModel(Application(), storage)
        val job = launch(testDispatcher) { vm.shouldRequestReview.collect {} }
        assertFalse(vm.shouldRequestReview.value)
        job.cancel()
    }
}

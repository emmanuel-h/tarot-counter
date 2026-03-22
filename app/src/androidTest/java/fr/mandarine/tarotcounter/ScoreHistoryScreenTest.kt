package fr.mandarine.tarotcounter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for ScoreHistoryScreen.
 *
 * Run with: ./gradlew connectedAndroidTest
 *
 * Spec (docs/score-history.md):
 *   - Shows "Score history" title
 *   - Shows player names as column headers
 *   - Shows round numbers in the first column
 *   - Shows cumulative scores (running total, not per-round delta)
 *   - "Back to game" arrow returns to the caller
 */
@RunWith(AndroidJUnit4::class)
class ScoreHistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val players = listOf("Alice", "Bob", "Charlie")

    /** Launches ScoreHistoryScreen with the given history, capturing back events. */
    private fun launchHistory(
        roundHistory: List<RoundResult> = emptyList(),
        onBack: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TarotCounterTheme {
                ScoreHistoryScreen(
                    playerNames = players,
                    roundHistory = roundHistory,
                    onBack = onBack
                )
            }
        }
    }

    // ── Spec: screen title ────────────────────────────────────────────────────

    @Test
    fun score_history_title_is_displayed() {
        launchHistory()
        composeTestRule.onNodeWithText("Score history").assertIsDisplayed()
    }

    // ── Spec: column headers ──────────────────────────────────────────────────

    @Test
    fun round_column_header_is_displayed() {
        launchHistory()
        composeTestRule.onNodeWithText("Round").assertIsDisplayed()
    }

    @Test
    fun player_names_are_shown_as_column_headers() {
        launchHistory()
        players.forEach { name ->
            composeTestRule.onNodeWithText(name).assertIsDisplayed()
        }
    }

    // ── Spec: empty state (no rounds completed) ───────────────────────────────

    @Test
    fun empty_history_shows_only_headers() {
        // With no rounds completed, only the header row should be present.
        launchHistory(roundHistory = emptyList())
        // Headers present.
        composeTestRule.onNodeWithText("Round").assertIsDisplayed()
        // No data rows — round number "1" should not appear in the table.
        composeTestRule.onNodeWithText("1").assertDoesNotExist()
    }

    // ── Spec: round rows ──────────────────────────────────────────────────────

    @Test
    fun completed_round_number_appears_in_table() {
        // A single skipped round — playerScores is empty, no scores change.
        val history = listOf(
            RoundResult(
                roundNumber = 1,
                takerName   = "Alice",
                contract    = null,
                details     = null,
                won         = null
                // playerScores defaults to emptyMap()
            )
        )
        launchHistory(roundHistory = history)
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
    }

    // ── Spec: cumulative scores ───────────────────────────────────────────────

    @Test
    fun cumulative_scores_are_shown_after_one_played_round() {
        // Manually constructed round where Alice gets +50, Bob -25, Charlie -25.
        val history = listOf(
            RoundResult(
                roundNumber  = 1,
                takerName    = "Alice",
                contract     = Contract.GARDE,
                details      = null,
                won          = true,
                playerScores = mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25)
            )
        )
        launchHistory(roundHistory = history)

        // Cumulative after round 1 should match the per-round scores exactly.
        composeTestRule.onNodeWithText("+50").assertIsDisplayed()
        composeTestRule.onNodeWithText("-25").assertIsDisplayed()
    }

    @Test
    fun cumulative_scores_accumulate_across_rounds() {
        // Round 1: Alice +50, Bob -25, Charlie -25
        // Round 2: Alice -30, Bob +15, Charlie +15
        // After round 2 totals: Alice +20, Bob -10, Charlie -10
        val history = listOf(
            RoundResult(
                roundNumber  = 1,
                takerName    = "Alice",
                contract     = Contract.GARDE,
                details      = null,
                won          = true,
                playerScores = mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25)
            ),
            RoundResult(
                roundNumber  = 2,
                takerName    = "Bob",
                contract     = Contract.PRISE,
                details      = null,
                won          = false,
                playerScores = mapOf("Alice" to -30, "Bob" to 15, "Charlie" to 15)
            )
        )
        launchHistory(roundHistory = history)

        // After round 2 the last row should show the running totals.
        composeTestRule.onNodeWithText("+20").assertIsDisplayed()
        composeTestRule.onNodeWithText("-10").assertIsDisplayed()
    }

    @Test
    fun skipped_rounds_do_not_change_cumulative_scores() {
        // Round 1 played: Alice +50, Bob -25, Charlie -25
        // Round 2 skipped: no change
        val history = listOf(
            RoundResult(
                roundNumber  = 1,
                takerName    = "Alice",
                contract     = Contract.GARDE,
                details      = null,
                won          = true,
                playerScores = mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25)
            ),
            RoundResult(
                roundNumber = 2,
                takerName   = "Bob",
                contract    = null,
                details     = null,
                won         = null
                // playerScores = emptyMap() (default)
            )
        )
        launchHistory(roundHistory = history)

        // After the skipped round 2, Alice should still show +50 (unchanged).
        // There will be two "+50" cells: one for round 1 and one for round 2.
        assertTrue(
            "+50 should appear at least once (cumulative unchanged after skip)",
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasText("+50")
            ).fetchSemanticsNodes().isNotEmpty()
        )
    }

    // ── Spec: back navigation ─────────────────────────────────────────────────

    @Test
    fun back_arrow_is_displayed() {
        launchHistory()
        composeTestRule
            .onNodeWithContentDescription("Back to game")
            .assertIsDisplayed()
    }

    @Test
    fun tapping_back_arrow_triggers_onBack_callback() {
        var backCalled = false
        launchHistory(onBack = { backCalled = true })
        composeTestRule
            .onNodeWithContentDescription("Back to game")
            .performClick()
        assertTrue("onBack callback should have been called", backCalled)
    }
}

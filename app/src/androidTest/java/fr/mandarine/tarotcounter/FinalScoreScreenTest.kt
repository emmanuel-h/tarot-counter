package fr.mandarine.tarotcounter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for FinalScoreScreen.
 *
 * Run with: ./gradlew connectedAndroidTest
 *
 * Spec (docs/final-score.md):
 *   - Shows "Game Over" heading
 *   - Shows winner name prominently
 *   - Shows "It's a tie!" when multiple players share the highest score
 *   - Displays the score table with all round data
 *   - Winner's column is highlighted (bold) in the table
 *   - Shows "No rounds played" when the history is empty
 *   - "New Game" button fires the onNewGame callback
 */
@RunWith(AndroidJUnit4::class)
class FinalScoreScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val players = listOf("Alice", "Bob", "Charlie")

    /** Launches FinalScoreScreen with the given history, capturing newGame events. */
    private fun launchFinal(
        roundHistory: List<RoundResult> = emptyList(),
        onNewGame: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TarotCounterTheme {
                FinalScoreScreen(
                    playerNames  = players,
                    roundHistory = roundHistory,
                    onNewGame    = onNewGame
                )
            }
        }
    }

    // ── Spec: heading ─────────────────────────────────────────────────────────

    @Test
    fun game_over_heading_is_displayed() {
        launchFinal()
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
    }

    // ── Spec: single winner ───────────────────────────────────────────────────

    @Test
    fun winner_name_is_shown_when_one_player_has_the_highest_score() {
        // Alice +50, Bob −25, Charlie −25 → Alice wins.
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
        launchFinal(roundHistory = history)
        composeTestRule.onNodeWithText("Alice", substring = true).assertIsDisplayed()
    }

    @Test
    fun winner_label_is_shown_for_single_winner() {
        val history = listOf(
            RoundResult(1, "Alice", Contract.GARDE, null, true,
                mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25))
        )
        launchFinal(roundHistory = history)
        composeTestRule.onNodeWithText("Winner").assertIsDisplayed()
    }

    @Test
    fun winner_final_score_is_shown() {
        // Alice total = +50.
        val history = listOf(
            RoundResult(1, "Alice", Contract.GARDE, null, true,
                mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25))
        )
        launchFinal(roundHistory = history)
        composeTestRule.onNodeWithText("+50 pts", substring = true).assertIsDisplayed()
    }

    // ── Spec: tie ─────────────────────────────────────────────────────────────

    @Test
    fun tie_label_is_shown_when_multiple_players_share_the_highest_score() {
        // Alice and Bob both at 0, Charlie at -20 → Alice and Bob tie.
        val history = listOf(
            RoundResult(
                roundNumber  = 1,
                takerName    = "Alice",
                contract     = Contract.PRISE,
                details      = null,
                won          = false,
                playerScores = mapOf("Alice" to 10, "Bob" to 10, "Charlie" to -20)
            )
        )
        launchFinal(roundHistory = history)
        composeTestRule.onNodeWithText("It's a tie!").assertIsDisplayed()
    }

    // ── Spec: empty state ─────────────────────────────────────────────────────

    @Test
    fun no_rounds_played_message_shown_when_history_is_empty() {
        launchFinal(roundHistory = emptyList())
        composeTestRule.onNodeWithText("No rounds played").assertIsDisplayed()
    }

    @Test
    fun score_table_is_not_shown_when_history_is_empty() {
        // The "Round" column header only appears in the table, not elsewhere.
        launchFinal(roundHistory = emptyList())
        composeTestRule.onNodeWithText("Round").assertDoesNotExist()
    }

    // ── Spec: score table ─────────────────────────────────────────────────────

    @Test
    fun round_column_header_is_shown_when_history_is_not_empty() {
        val history = listOf(
            RoundResult(1, "Alice", null, null, null)  // skipped round
        )
        launchFinal(roundHistory = history)
        composeTestRule.onNodeWithText("Round").assertIsDisplayed()
    }

    @Test
    fun player_names_appear_as_column_headers_in_the_table() {
        val history = listOf(
            RoundResult(1, "Alice", null, null, null)
        )
        launchFinal(roundHistory = history)
        players.forEach { name ->
            // Each name should appear at least once (column header + possibly winner card).
            assertTrue(
                "$name should appear in the screen",
                composeTestRule.onAllNodes(
                    androidx.compose.ui.test.hasText(name)
                ).fetchSemanticsNodes().isNotEmpty()
            )
        }
    }

    @Test
    fun cumulative_scores_are_shown_in_the_table() {
        // After round 1: Alice +50, Bob -25, Charlie -25.
        val history = listOf(
            RoundResult(1, "Alice", Contract.GARDE, null, true,
                mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25))
        )
        launchFinal(roundHistory = history)
        composeTestRule.onNodeWithText("+50").assertIsDisplayed()
        composeTestRule.onNodeWithText("-25").assertIsDisplayed()
    }

    @Test
    fun cumulative_scores_accumulate_correctly_across_rounds() {
        // Round 1: Alice +50, Bob -25, Charlie -25
        // Round 2: Alice -30, Bob +15, Charlie +15
        // After round 2: Alice +20, Bob -10, Charlie -10
        val history = listOf(
            RoundResult(1, "Alice", Contract.GARDE, null, true,
                mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25)),
            RoundResult(2, "Bob", Contract.PRISE, null, false,
                mapOf("Alice" to -30, "Bob" to 15, "Charlie" to 15))
        )
        launchFinal(roundHistory = history)
        composeTestRule.onNodeWithText("+20").assertIsDisplayed()
        composeTestRule.onNodeWithText("-10").assertIsDisplayed()
    }

    // ── Spec: New Game navigation ─────────────────────────────────────────────

    @Test
    fun new_game_button_is_displayed() {
        launchFinal()
        composeTestRule.onNodeWithText("New Game").assertIsDisplayed()
    }

    @Test
    fun tapping_new_game_button_fires_onNewGame_callback() {
        var callbackFired = false
        launchFinal(onNewGame = { callbackFired = true })
        composeTestRule.onNodeWithText("New Game").performClick()
        assertTrue("onNewGame callback should have been called", callbackFired)
    }
}

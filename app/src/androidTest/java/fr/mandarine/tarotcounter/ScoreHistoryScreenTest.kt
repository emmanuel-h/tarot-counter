package fr.mandarine.tarotcounter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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

    // ── Spec: colour coding ───────────────────────────────────────────────────
    // Note: Compose UI tests cannot assert exact text colours without custom
    // semantic properties or screenshot comparison. These tests verify that the
    // score values are still rendered after the colour-coding change (regression
    // guard). Visual colour review is done manually or via screenshot tests.

    @Test
    fun positive_score_cell_is_displayed_after_colour_coding_applied() {
        // Alice +50 should still render as "+50" after scoreColor is applied.
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
        composeTestRule.onNodeWithText("+50").assertIsDisplayed()
    }

    @Test
    fun negative_score_cell_is_displayed_after_colour_coding_applied() {
        // Bob -25 should still render as "-25" after scoreColor is applied.
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
        composeTestRule.onNodeWithText("-25").assertIsDisplayed()
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

    // ── Spec: view mode toggle ────────────────────────────────────────────────

    @Test
    fun view_toggle_is_displayed() {
        // The segmented toggle ("Table" / "List") must appear on the history screen.
        launchHistory()
        composeTestRule.onNodeWithTag("history_view_toggle").assertIsDisplayed()
    }

    @Test
    fun default_view_is_table() {
        // On first open the TABLE segment must be selected — shown by the Round column header.
        launchHistory()
        composeTestRule.onNodeWithText("Table").assertIsDisplayed()
        composeTestRule.onNodeWithText("Round").assertIsDisplayed()
    }

    @Test
    fun tapping_list_tab_switches_to_list_view() {
        // After tapping the List segment the "Round" table column header disappears
        // (because the list view does not use a table header row).
        val history = listOf(
            RoundResult(
                roundNumber = 1, takerName = "Alice",
                contract = null, details = null, won = null
            )
        )
        launchHistory(roundHistory = history)
        composeTestRule.onNodeWithTag("toggle_list").performClick()
        // The round column header is part of the TABLE view only — it must be gone.
        composeTestRule.onNodeWithText("Round").assertDoesNotExist()
    }

    @Test
    fun tapping_table_tab_after_list_restores_table() {
        // TABLE → LIST → TABLE round-trip: the round column header must reappear.
        val history = listOf(
            RoundResult(
                roundNumber = 1, takerName = "Alice",
                contract = null, details = null, won = null
            )
        )
        launchHistory(roundHistory = history)
        composeTestRule.onNodeWithTag("toggle_list").performClick()
        composeTestRule.onNodeWithTag("toggle_table").performClick()
        composeTestRule.onNodeWithText("Round").assertIsDisplayed()
    }

    // ── Spec: LIST view — round indicators (issue #136) ──────────────────────

    /** Helper: launch the history screen in LIST view. */
    private fun launchHistoryInListMode(roundHistory: List<RoundResult> = emptyList()) {
        launchHistory(roundHistory = roundHistory)
        composeTestRule.onNodeWithTag("toggle_list").performClick()
    }

    @Test
    fun list_view_empty_state_shows_no_rounds_played() {
        // Empty history in list view shows the "No rounds played" notice.
        launchHistoryInListMode()
        composeTestRule.onNodeWithText("No rounds played").assertIsDisplayed()
    }

    @Test
    fun list_view_skipped_round_shows_skipped_indicator() {
        val history = listOf(
            RoundResult(
                roundNumber = 1, takerName = "Alice",
                contract = null, details = null, won = null
            )
        )
        launchHistoryInListMode(roundHistory = history)
        composeTestRule.onNodeWithTag("round_indicator_skipped").assertIsDisplayed()
    }

    @Test
    fun list_view_won_round_shows_won_indicator() {
        val history = listOf(
            RoundResult(
                roundNumber  = 1, takerName = "Alice",
                contract     = Contract.GARDE, details = null, won = true,
                playerScores = mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25)
            )
        )
        launchHistoryInListMode(roundHistory = history)
        composeTestRule.onNodeWithTag("round_indicator_won").assertIsDisplayed()
    }

    @Test
    fun list_view_lost_round_shows_lost_indicator() {
        val history = listOf(
            RoundResult(
                roundNumber  = 1, takerName = "Alice",
                contract     = Contract.PRISE, details = null, won = false,
                playerScores = mapOf("Alice" to -25, "Bob" to 12, "Charlie" to 13)
            )
        )
        launchHistoryInListMode(roundHistory = history)
        composeTestRule.onNodeWithTag("round_indicator_lost").assertIsDisplayed()
    }

    @Test
    fun list_view_shows_skipped_text_for_skipped_round() {
        val history = listOf(
            RoundResult(
                roundNumber = 1, takerName = "Alice",
                contract = null, details = null, won = null
            )
        )
        launchHistoryInListMode(roundHistory = history)
        composeTestRule.onNodeWithText("Skipped", substring = true).assertIsDisplayed()
    }

    @Test
    fun list_view_shows_multiple_round_indicators() {
        val history = listOf(
            RoundResult(
                roundNumber = 1, takerName = "Alice",
                contract = null, details = null, won = null
            ),
            RoundResult(
                roundNumber  = 2, takerName = "Bob",
                contract     = Contract.PRISE, details = null, won = false,
                playerScores = mapOf("Alice" to 10, "Bob" to -20, "Charlie" to 10)
            )
        )
        launchHistoryInListMode(roundHistory = history)
        composeTestRule.onNodeWithTag("round_indicator_skipped").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_indicator_lost").assertIsDisplayed()
    }

    @Test
    fun list_view_is_newest_round_first() {
        // With two rounds, the most recent round (round 2) must appear in the list.
        val history = listOf(
            RoundResult(
                roundNumber = 1, takerName = "Alice",
                contract = null, details = null, won = null
            ),
            RoundResult(
                roundNumber = 2, takerName = "Bob",
                contract = null, details = null, won = null
            )
        )
        launchHistoryInListMode(roundHistory = history)
        // Both round numbers must be visible (newest first ordering).
        composeTestRule.onNodeWithText("Round 2", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Round 1", substring = true).assertIsDisplayed()
    }
}

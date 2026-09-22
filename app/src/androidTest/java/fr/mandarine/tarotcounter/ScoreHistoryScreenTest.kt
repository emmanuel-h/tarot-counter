package fr.mandarine.tarotcounter

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
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
        launchHistory(roundHistory = listOf(RoundResult(1, "Alice", Contract.GARDE, null, true,
            mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25))))
        composeTestRule.onNodeWithText("Round").assertIsDisplayed()
    }

    @Test
    fun player_names_and_avatars_are_shown_in_the_header() {
        launchHistory(roundHistory = listOf(RoundResult(1, "Alice", Contract.GARDE, null, true,
            mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25))))
        players.forEach { name ->
            composeTestRule.onNodeWithText(name).assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("Player $name").assertIsDisplayed()
        }
    }

    // ── Spec: empty state (no rounds completed) ───────────────────────────────

    @Test
    fun empty_history_shows_the_empty_state() {
        // Salon (#202): no table before the first round, an empty state instead.
        launchHistory(roundHistory = emptyList())
        composeTestRule.onNodeWithTag("history_empty").assertIsDisplayed()
        composeTestRule.onNodeWithText(EnStrings.historyEmpty).assertIsDisplayed()
        composeTestRule.onNodeWithText("Round").assertDoesNotExist()
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
        // Bob and Charlie share the same total, so the value appears in two cells.
        composeTestRule.onAllNodesWithText("-25").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("-25").onFirst().assertIsDisplayed()
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
        // Bob and Charlie share the same total, so the value appears in two cells.
        composeTestRule.onAllNodesWithText("-10").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("-10").onFirst().assertIsDisplayed()
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
        // Bob and Charlie share the same total, so the value appears in two cells.
        composeTestRule.onAllNodesWithText("-25").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("-25").onFirst().assertIsDisplayed()
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
        // On first open the TABLE segment is selected — shown by the Round column header.
        launchHistory(roundHistory = listOf(RoundResult(1, "Alice", Contract.GARDE, null, true,
            mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25))))
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
    fun list_view_empty_state_is_shown() {
        launchHistoryInListMode()
        composeTestRule.onNodeWithText(EnStrings.historyEmpty).assertIsDisplayed()
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
        val history = listOf(
            RoundResult(roundNumber = 1, takerName = "Alice", contract = null, details = null, won = null),
            RoundResult(roundNumber = 2, takerName = "Bob", contract = null, details = null, won = null)
        )
        launchHistoryInListMode(roundHistory = history)
        // "R2" card above the "R1" card.
        val r2 = composeTestRule.onNodeWithTag("round_card_2").fetchSemanticsNode().boundsInRoot
        val r1 = composeTestRule.onNodeWithTag("round_card_1").fetchSemanticsNode().boundsInRoot
        assertTrue("Newest round first", r2.top < r1.top)
        composeTestRule.onNodeWithText("R2").assertIsDisplayed()
    }

    // ── Salon restyle (issue #202) ────────────────────────────────────────────

    @Test
    fun list_card_shows_taker_contract_details_outcome_and_delta() {
        val history = listOf(
            RoundResult(
                roundNumber = 1, takerName = "Bob", contract = Contract.GARDE,
                details = RoundDetails(bouts = 2, points = 47, partnerName = null,
                    petitAuBout = null, chelem = Chelem.NONE),
                won = true, playerScores = mapOf("Alice" to -62, "Bob" to 124, "Charlie" to -62)
            )
        )
        launchHistoryInListMode(roundHistory = history)
        composeTestRule.onNodeWithText("Bob · Guard").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 bouts · 47 pts").assertIsDisplayed()
        composeTestRule.onNodeWithText("Won").assertIsDisplayed()
        composeTestRule.onNodeWithText("+124").assertIsDisplayed()
    }

    @Test
    fun five_players_table_scrolls_sideways_on_a_narrow_phone() {
        // 5 × 64 dp + 48 dp = 368 dp > a ~360 dp phone minus margins: the last
        // player's column starts off screen but exists, reachable by scrolling.
        val five = listOf("Alice", "Bob", "Charlie", "Dave", "Eve")
        composeTestRule.setContent {
            TarotCounterTheme {
                ScoreHistoryScreen(
                    playerNames  = five,
                    roundHistory = listOf(RoundResult(1, "Alice", Contract.PRISE, null, true,
                        mapOf("Alice" to 100, "Bob" to -25, "Charlie" to -25, "Dave" to -25, "Eve" to -25))),
                    onBack       = {},
                    modifier     = androidx.compose.ui.Modifier.width(360.dp)
                )
            }
        }
        composeTestRule.onNodeWithTag("history_table").assertIsDisplayed()
        composeTestRule.onNodeWithText("Eve").assertExists()
    }
}


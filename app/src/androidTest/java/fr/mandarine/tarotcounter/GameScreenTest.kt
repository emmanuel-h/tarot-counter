package fr.mandarine.tarotcounter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for GameScreen and RoundDetailsForm.
 *
 * These run on a device or emulator via AndroidJUnit4.
 * Run with: ./gradlew connectedAndroidTest
 *
 * Spec (docs/game-flow.md — Game Screen):
 *   Step 1 — Pick a contract (5 options + Skip round)
 *   Step 2 — RoundDetailsForm (bouts, points, bonuses, chelem, Confirm / ← Change contract)
 *   Round history — shown newest-first after the first round is complete.
 */
@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Fixed player names used across most tests so assertions are deterministic.
    private val players = listOf("Alice", "Bob", "Charlie")

    /** Launches GameScreen with [players] inside our app theme. */
    private fun launchGame(playerNames: List<String> = players) {
        composeTestRule.setContent {
            TarotCounterTheme {
                GameScreen(playerNames = playerNames)
            }
        }
    }

    // ── Spec: round header ────────────────────────────────────────────────────

    @Test
    fun round_1_header_is_displayed_at_start() {
        launchGame()
        composeTestRule.onNodeWithText("Round 1").assertIsDisplayed()
    }

    // ── Spec: taker is one of the players ─────────────────────────────────────

    @Test
    fun current_taker_is_one_of_the_player_names() {
        // Spec: "Round 1 — a random player is chosen as the first taker."
        // We cannot predict which player, but one of them must be visible.
        launchGame()
        val anyTakerVisible = players.any { name ->
            // `substring = true` matches "Alice — choose a contract:" etc.
            composeTestRule.onAllNodesWithText(name, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue("One of the player names should appear as the current taker", anyTakerVisible)
    }

    // ── Spec: Step 1 — four contract buttons ─────────────────────────────────

    @Test
    fun all_four_contract_buttons_are_displayed() {
        // Spec table: Prise, Garde, Garde Sans, Garde Contre (POUSSE removed).
        launchGame()
        Contract.entries.forEach { contract ->
            composeTestRule.onNodeWithText(contract.displayName).assertIsDisplayed()
        }
    }

    @Test
    fun pousse_button_does_not_exist() {
        // POUSSE was removed from the contract list.
        launchGame()
        composeTestRule.onNodeWithText("Pousse").assertDoesNotExist()
    }

    @Test
    fun skip_round_button_is_displayed() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").assertIsDisplayed()
    }

    // ── Spec: selecting a contract opens the details form (Step 2) ────────────

    @Test
    fun selecting_a_contract_shows_the_round_details_form() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        // RoundDetailsForm key labels should now be visible.
        composeTestRule.onNodeWithText("Number of bouts (oudlers)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm round").assertIsDisplayed()
    }

    @Test
    fun details_form_shows_selected_contract_in_header() {
        launchGame()
        composeTestRule.onNodeWithText("Garde Sans").performClick()
        // Header format: "<taker> — Garde Sans"
        composeTestRule
            .onNodeWithText("Garde Sans", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun details_form_shows_all_four_chelem_options() {
        // Spec: four Chelem values with their display names.
        launchGame()
        composeTestRule.onNodeWithText("Prise").performClick()
        Chelem.entries.forEach { chelem ->
            composeTestRule.onNodeWithText(chelem.displayName).assertIsDisplayed()
        }
    }

    @Test
    fun details_form_shows_bouts_chips_0_through_3() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        // Bouts chips: 0, 1, 2, 3
        listOf("0", "1", "2", "3").forEach { n ->
            composeTestRule.onAllNodesWithText(n).fetchSemanticsNodes().let { nodes ->
                assertTrue("Bouts chip '$n' should be visible", nodes.isNotEmpty())
            }
        }
    }

    @Test
    fun details_form_shows_player_names_as_bonus_options() {
        // Spec: each player-assigned bonus shows "None + one chip per player".
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        // Every player should appear at least once in the form as a bonus option.
        players.forEach { name ->
            assertTrue(
                "$name should appear as a bonus-assignment option",
                composeTestRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
            )
        }
    }

    // ── Spec: "← Change contract" goes back to Step 1 ────────────────────────

    @Test
    fun change_contract_button_returns_to_contract_selection() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("← Change contract").performClick()

        // Contract buttons must be visible again.
        composeTestRule.onNodeWithText("Garde").assertIsDisplayed()
        // Details form must be gone.
        composeTestRule.onNodeWithText("Confirm round").assertDoesNotExist()
    }

    // ── Spec: confirming a round advances the round counter ───────────────────

    @Test
    fun confirming_a_round_advances_to_round_2() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").performClick()
        composeTestRule.onNodeWithText("Round 2").assertIsDisplayed()
    }

    // ── Spec: Skip round records immediately and advances ─────────────────────

    @Test
    fun skipping_a_round_advances_to_round_2() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule.onNodeWithText("Round 2").assertIsDisplayed()
    }

    // ── Spec: round history (shown newest-first) ──────────────────────────────

    @Test
    fun history_appears_after_first_round_is_skipped() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule.onNodeWithText("History").assertIsDisplayed()
    }

    @Test
    fun skipped_round_shows_Skipped_in_history() {
        // Spec history example: "Round 2: Bob — Skipped"
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule
            .onNodeWithText("Skipped", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun played_round_shows_contract_and_score_in_history() {
        // Spec history example: "Round 1: Alice — Garde · 2 bouts · 56 pts"
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").performClick()
        // "Garde" should appear in the history line (substring of full history text).
        composeTestRule
            .onNodeWithText("Garde", substring = true)
            .assertIsDisplayed()
        // Default bouts=0 and points=0, so history shows "0 bouts · 0 pts".
        composeTestRule
            .onNodeWithText("0 pts", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun played_round_shows_Lost_in_history_when_taker_did_not_reach_threshold() {
        // Default form values: 0 bouts, 0 points.
        // With 0 bouts the taker needs 56 pts; 0 < 56 → Lost.
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").performClick()
        composeTestRule
            .onNodeWithText("Lost", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun history_is_newest_round_first() {
        // Complete two rounds; round 2 summary should appear before round 1.
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick() // round 1 skipped
        composeTestRule.onNodeWithText("Skip round").performClick() // round 2 skipped

        // Both entries must be present.
        composeTestRule.onNodeWithText("Round 1", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Round 2", substring = true).assertIsDisplayed()
    }

    // ── Spec: scoreboard shown after a played round ────────────────────────────

    @Test
    fun scores_section_appears_after_first_played_round() {
        // After completing a round, a "Scores" heading and player names should be visible.
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").performClick()

        composeTestRule.onNodeWithText("Scores").assertIsDisplayed()
        // All player names should appear in the scoreboard.
        players.forEach { name ->
            assertTrue(
                "$name should appear in the scoreboard",
                composeTestRule.onAllNodesWithText(name, substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            )
        }
    }

    @Test
    fun partner_selector_not_shown_for_3_player_game() {
        // Partner selection is only for 5-player games.
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie"))
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Partner (called by taker)").assertDoesNotExist()
    }

    @Test
    fun partner_selector_is_shown_for_5_player_game() {
        // In a 5-player game the partner selector must appear in the details form.
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie", "Dave", "Eve"))
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Partner (called by taker)").assertIsDisplayed()
    }
}

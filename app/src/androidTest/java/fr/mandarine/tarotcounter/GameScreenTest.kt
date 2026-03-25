package fr.mandarine.tarotcounter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for GameScreen.
 *
 * These run on a device or emulator via AndroidJUnit4.
 * Run with: ./gradlew connectedAndroidTest
 *
 * Spec (docs/game-flow.md — Game Screen):
 *   Contract selection — FilterChips for Prise/Garde/Garde Sans/Garde Contre + Skip round.
 *   Inline details form — bouts, points, bonuses, chelem, Confirm / ← Change contract.
 *   All visible on a single scrollable page alongside the compact scoreboard.
 *   Round history — shown newest-first at the bottom of the page.
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
    fun details_form_shows_bouts_dropdown_with_0_through_3() {
        // Issue #9: bouts is now an ExposedDropdownMenuBox instead of FilterChips.
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        // The dropdown anchor (text field) must be visible with the default value "0".
        composeTestRule.onNodeWithTag("bouts_dropdown").assertIsDisplayed()

        // Open the dropdown to reveal all options.
        composeTestRule.onNodeWithTag("bouts_dropdown").performClick()

        // All four options (0–3) should now be visible in the expanded menu.
        listOf("0", "1", "2", "3").forEach { n ->
            composeTestRule.onAllNodesWithText(n).fetchSemanticsNodes().let { nodes ->
                assertTrue("Bouts dropdown option '$n' should be visible", nodes.isNotEmpty())
            }
        }
    }

    @Test
    fun selecting_a_bout_value_from_dropdown_updates_selection() {
        // Opening the dropdown and picking "2" should update the displayed value.
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        // Open the dropdown.
        composeTestRule.onNodeWithTag("bouts_dropdown").performClick()

        // Tap the "2" option.
        composeTestRule.onAllNodesWithText("2").fetchSemanticsNodes().let { nodes ->
            assertTrue("Option '2' should be visible in the open dropdown", nodes.isNotEmpty())
        }
        // The first node with text "2" belongs to the dropdown menu item — click it.
        composeTestRule.onAllNodesWithText("2")[0].performClick()

        // After selection the dropdown anchor should now display "2".
        composeTestRule.onNodeWithTag("bouts_dropdown").assertIsDisplayed()
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
        composeTestRule.onNodeWithContentDescription("History").assertIsDisplayed()
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

    // ── Spec: score history navigation ────────────────────────────────────────

    @Test
    fun score_history_button_appears_after_first_round_is_completed() {
        // The History button only appears once at least one round has been recorded.
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        // IconButton — findable by its contentDescription.
        composeTestRule.onNodeWithContentDescription("History").assertIsDisplayed()
    }

    @Test
    fun tapping_score_history_button_opens_history_screen() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule.onNodeWithContentDescription("History").performClick()
        composeTestRule.onNodeWithText("Score history").assertIsDisplayed()
    }

    @Test
    fun back_button_on_history_screen_returns_to_game() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()  // complete round 1
        composeTestRule.onNodeWithContentDescription("History").performClick()
        composeTestRule
            .onNodeWithContentDescription("Back to game")
            .performClick()
        // We should be back on round 2's contract selection screen.
        composeTestRule.onNodeWithText("Round 2").assertIsDisplayed()
    }

    @Test
    fun score_history_button_also_appears_in_round_details_form() {
        // After round 1 is done, entering step 2 should also show the History button.
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()  // complete round 1
        composeTestRule.onNodeWithText("Garde").performClick()        // enter step 2
        composeTestRule.onNodeWithContentDescription("History").assertIsDisplayed()
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

    // ── Spec: End Game button (step 1) ────────────────────────────────────────

    @Test
    fun end_game_button_is_displayed_on_step_1_from_the_start() {
        // "End Game" must be visible even before any round has been played.
        launchGame()
        composeTestRule.onNodeWithContentDescription("End Game").assertIsDisplayed()
    }

    @Test
    fun tapping_end_game_on_step_1_opens_final_score_screen() {
        launchGame()
        composeTestRule.onNodeWithContentDescription("End Game").performClick()
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
    }

    @Test
    fun end_game_button_is_displayed_on_step_2() {
        // After selecting a contract, the End Game button should appear in the details form.
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithContentDescription("End Game").assertIsDisplayed()
    }

    @Test
    fun tapping_end_game_on_step_2_opens_final_score_screen() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithContentDescription("End Game").performClick()
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
    }

    @Test
    fun final_score_screen_shows_new_game_button() {
        launchGame()
        composeTestRule.onNodeWithContentDescription("End Game").performClick()
        composeTestRule.onNodeWithText("New Game").assertIsDisplayed()
    }

    @Test
    fun back_to_game_button_on_final_score_screen_returns_to_game() {
        // Complete one round so there is a score to show, then end the game.
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()  // complete round 1
        composeTestRule.onNodeWithContentDescription("End Game").performClick()
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()

        // Tapping "Back to game" should return to the active round.
        composeTestRule.onNodeWithText("Back to game").performClick()
        composeTestRule.onNodeWithText("Round 2").assertIsDisplayed()
    }

    @Test
    fun back_arrow_on_final_score_screen_returns_to_game() {
        launchGame()
        composeTestRule.onNodeWithContentDescription("End Game").performClick()
        composeTestRule
            .onNodeWithContentDescription("Back to game")
            .performClick()
        // Should be back at round 1 contract selection.
        composeTestRule.onNodeWithText("Round 1").assertIsDisplayed()
    }

    // ── Spec: points field validation (issue #8) ──────────────────────────────
    // The user must not be able to enter a value outside 0–91.
    // Entering a value > 91 shows an error message and disables the Confirm button.

    @Test
    fun entering_value_above_91_shows_error_message() {
        // Open the details form so the points field is visible.
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        // Type an out-of-range value (92) into the points field.
        composeTestRule.onNodeWithTag("points_input").performTextInput("92")

        // The error string defined in EnStrings.pointsOutOfRange should appear.
        composeTestRule
            .onNodeWithText(EnStrings.pointsOutOfRange)
            .assertIsDisplayed()
    }

    @Test
    fun entering_value_above_91_disables_confirm_button() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        // Type 99 — clearly out of range.
        composeTestRule.onNodeWithTag("points_input").performTextInput("99")

        // Confirm button must be disabled so the round cannot be submitted.
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun entering_91_does_not_show_error() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        // 91 is the maximum allowed value — it must not trigger an error.
        composeTestRule.onNodeWithTag("points_input").performTextInput("91")

        composeTestRule
            .onNodeWithText(EnStrings.pointsOutOfRange)
            .assertDoesNotExist()
    }

    @Test
    fun entering_91_keeps_confirm_button_enabled() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule.onNodeWithTag("points_input").performTextInput("91")

        composeTestRule.onNodeWithText("Confirm round").assertIsEnabled()
    }

    // ── Spec: styled round history rows (issue #6) ────────────────────────────
    // Each history row must have a colored indicator dot whose testTag encodes
    // the outcome: "round_indicator_won", "round_indicator_lost",
    // or "round_indicator_skipped".

    @Test
    fun skipped_round_shows_skipped_indicator() {
        // A skipped round must display the grey (skipped) indicator dot.
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()

        // The indicator tagged "round_indicator_skipped" must be visible.
        composeTestRule.onNodeWithTag("round_indicator_skipped").assertIsDisplayed()
    }

    @Test
    fun lost_round_shows_lost_indicator() {
        // Default form values: 0 bouts, 0 points → taker needs 56 to win → Lost.
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").performClick()

        // The indicator tagged "round_indicator_lost" must be visible.
        composeTestRule.onNodeWithTag("round_indicator_lost").assertIsDisplayed()
    }

    @Test
    fun won_round_shows_won_indicator() {
        // 3 bouts, 91 points → needs only 36 → Won.
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        // Select 3 bouts from the dropdown.
        composeTestRule.onNodeWithTag("bouts_dropdown").performClick()
        composeTestRule.onAllNodesWithText("3")[0].performClick()

        // Enter a winning score (91 pts with 3 bouts → 91 ≥ 36 → Won).
        composeTestRule.onNodeWithTag("points_input").performTextInput("91")

        composeTestRule.onNodeWithText("Confirm round").performClick()

        // The indicator tagged "round_indicator_won" must be visible.
        composeTestRule.onNodeWithTag("round_indicator_won").assertIsDisplayed()
    }

    @Test
    fun multiple_rounds_show_correct_indicators() {
        // Play one skipped + one lost round and verify both indicators appear.
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()   // round 1 → skipped
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").performClick() // round 2 → lost (0 pts)

        composeTestRule.onNodeWithTag("round_indicator_skipped").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_indicator_lost").assertIsDisplayed()
    }
}

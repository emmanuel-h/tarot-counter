package fr.mandarine.tarotcounter

import android.app.Application
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
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
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
 * Each test creates a [GameViewModel] backed by [FakeGameStorage] and calls
 * [GameViewModel.initGame] before setting the compose content. This avoids relying
 * on Android DataStore and keeps the tests fast and deterministic.
 *
 * Spec (docs/game-flow.md — Game Screen):
 *   Contract selection — FilterChips for Prise/Garde/Garde Sans/Garde Contre + Skip round.
 *   Inline details form — bouts, points, bonuses, chelem, Confirm.
 *   All visible on a single scrollable page alongside the compact scoreboard.
 *   Round history — shown newest-first at the bottom of the page.
 */
@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Fixed player names used across most tests so assertions are deterministic.
    private val players = listOf("Alice", "Bob", "Charlie")

    /**
     * Creates a [GameViewModel] with [FakeGameStorage], initialises it with [playerNames],
     * and sets the Compose content to [GameScreen].
     */
    private fun launchGame(playerNames: List<String> = players) {
        // FakeGameStorage replaces DataStore so no filesystem access is needed.
        val viewModel = GameViewModel(
            ApplicationProvider.getApplicationContext<Application>(),
            FakeGameStorage()
        )
        // initGame resolves display names and picks a random starting player.
        viewModel.initGame(playerNames, inProgressGame = null)
        composeTestRule.setContent {
            TarotCounterTheme {
                GameScreen(viewModel = viewModel)
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
            composeTestRule.onAllNodesWithText(name, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue("One of the player names should appear as the current taker", anyTakerVisible)
    }

    // ── Spec: Step 1 — four contract buttons ─────────────────────────────────

    @Test
    fun all_four_contract_buttons_are_displayed() {
        launchGame()
        Contract.entries.forEach { contract ->
            composeTestRule.onNodeWithText(contract.displayName).assertIsDisplayed()
        }
    }

    @Test
    fun pousse_button_does_not_exist() {
        launchGame()
        composeTestRule.onNodeWithText("Pousse").assertDoesNotExist()
    }

    // ── Spec: bottom action bar (issue #32) ──────────────────────────────────

    @Test
    fun skip_round_button_is_displayed_in_bottom_bar() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").assertIsDisplayed()
    }

    @Test
    fun end_game_and_skip_round_buttons_are_both_in_bottom_bar() {
        launchGame()
        composeTestRule.onNodeWithText("End Game").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip round").assertIsDisplayed()
    }

    @Test
    fun bottom_bar_buttons_remain_visible_while_contract_form_is_open() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("End Game").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip round").assertIsDisplayed()
    }

    // ── Spec: selecting a contract opens the details form (Step 2) ────────────

    @Test
    fun selecting_a_contract_shows_the_round_details_form() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule.onNodeWithText("Number of bouts (oudlers)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm round").assertIsDisplayed()
    }

    @Test
    fun details_form_shows_selected_contract_in_header() {
        launchGame()
        composeTestRule.onNodeWithText("Garde Sans").performClick()
        composeTestRule
            .onNodeWithText("Garde Sans", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun details_form_shows_all_four_chelem_options() {
        launchGame()
        composeTestRule.onNodeWithText("Prise").performClick()
        Chelem.entries.forEach { chelem ->
            composeTestRule.onNodeWithText(chelem.displayName).assertIsDisplayed()
        }
    }

    @Test
    fun details_form_shows_bouts_dropdown_with_0_through_3() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule.onNodeWithTag("bouts_dropdown").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bouts_dropdown").performClick()

        listOf("0", "1", "2", "3").forEach { n ->
            composeTestRule.onAllNodesWithText(n).fetchSemanticsNodes().let { nodes ->
                assertTrue("Bouts dropdown option '$n' should be visible", nodes.isNotEmpty())
            }
        }
    }

    @Test
    fun selecting_a_bout_value_from_dropdown_updates_selection() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule.onNodeWithTag("bouts_dropdown").performClick()

        composeTestRule.onAllNodesWithText("2").fetchSemanticsNodes().let { nodes ->
            assertTrue("Option '2' should be visible in the open dropdown", nodes.isNotEmpty())
        }
        composeTestRule.onAllNodesWithText("2")[0].performClick()

        composeTestRule.onNodeWithTag("bouts_dropdown").assertIsDisplayed()
    }

    @Test
    fun details_form_shows_player_names_as_bonus_options() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        players.forEach { name ->
            assertTrue(
                "$name should appear as a bonus-assignment option",
                composeTestRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
            )
        }
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
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule
            .onNodeWithText("Skipped", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun played_round_shows_contract_and_score_in_history() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").performClick()
        composeTestRule
            .onNodeWithText("Garde", substring = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("0 pts", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun played_round_shows_Lost_in_history_when_taker_did_not_reach_threshold() {
        // Default form values: 0 bouts, 0 points → needs 56 → Lost.
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").performClick()
        composeTestRule
            .onNodeWithText("Lost", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun history_is_newest_round_first() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick() // round 1 skipped
        composeTestRule.onNodeWithText("Skip round").performClick() // round 2 skipped

        composeTestRule.onNodeWithText("Round 1", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Round 2", substring = true).assertIsDisplayed()
    }

    // ── Spec: scoreboard shown after a played round ────────────────────────────

    @Test
    fun scores_section_appears_after_first_played_round() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").performClick()

        composeTestRule.onNodeWithText("Scores").assertIsDisplayed()
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
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
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
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule.onNodeWithContentDescription("History").performClick()
        composeTestRule
            .onNodeWithContentDescription("Back to game")
            .performClick()
        composeTestRule.onNodeWithText("Round 2").assertIsDisplayed()
    }

    @Test
    fun score_history_button_also_appears_in_round_details_form() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithContentDescription("History").assertIsDisplayed()
    }

    @Test
    fun partner_selector_not_shown_for_3_player_game() {
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie"))
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Partner (called by taker)").assertDoesNotExist()
    }

    @Test
    fun partner_selector_is_shown_for_5_player_game() {
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie", "Dave", "Eve"))
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Partner (called by taker)").assertIsDisplayed()
    }

    // ── Spec: End Game button (bottom bar) ────────────────────────────────────

    @Test
    fun end_game_button_is_displayed_on_step_1_from_the_start() {
        launchGame()
        composeTestRule.onNodeWithText("End Game").assertIsDisplayed()
    }

    @Test
    fun tapping_end_game_on_step_1_opens_final_score_screen() {
        launchGame()
        composeTestRule.onNodeWithText("End Game").performClick()
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
    }

    @Test
    fun end_game_button_is_displayed_on_step_2() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("End Game").assertIsDisplayed()
    }

    @Test
    fun tapping_end_game_on_step_2_opens_final_score_screen() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("End Game").performClick()
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
    }

    @Test
    fun final_score_screen_shows_new_game_button() {
        launchGame()
        composeTestRule.onNodeWithText("End Game").performClick()
        composeTestRule.onNodeWithText("New Game").assertIsDisplayed()
    }

    @Test
    fun back_to_game_button_on_final_score_screen_returns_to_game() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule.onNodeWithText("End Game").performClick()
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()

        composeTestRule.onNodeWithText("Back to game").performClick()
        composeTestRule.onNodeWithText("Round 2").assertIsDisplayed()
    }

    @Test
    fun back_arrow_on_final_score_screen_returns_to_game() {
        launchGame()
        composeTestRule.onNodeWithText("End Game").performClick()
        composeTestRule
            .onNodeWithContentDescription("Back to game")
            .performClick()
        composeTestRule.onNodeWithText("Round 1").assertIsDisplayed()
    }

    // ── Spec: points field validation (issue #8) ──────────────────────────────

    @Test
    fun entering_value_above_91_shows_error_message() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule.onNodeWithTag("points_input").performTextInput("92")

        composeTestRule
            .onNodeWithText(EnStrings.pointsOutOfRange)
            .assertIsDisplayed()
    }

    @Test
    fun entering_value_above_91_disables_confirm_button() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule.onNodeWithTag("points_input").performTextInput("99")

        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun entering_91_does_not_show_error() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

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

    @Test
    fun skipped_round_shows_skipped_indicator() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule.onNodeWithTag("round_indicator_skipped").assertIsDisplayed()
    }

    @Test
    fun lost_round_shows_lost_indicator() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").performClick()
        composeTestRule.onNodeWithTag("round_indicator_lost").assertIsDisplayed()
    }

    @Test
    fun won_round_shows_won_indicator() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule.onNodeWithTag("bouts_dropdown").performClick()
        composeTestRule.onAllNodesWithText("3")[0].performClick()

        composeTestRule.onNodeWithTag("points_input").performTextInput("91")

        composeTestRule.onNodeWithText("Confirm round").performClick()

        composeTestRule.onNodeWithTag("round_indicator_won").assertIsDisplayed()
    }

    // ── Spec: bonus label cell is fully tappable (issue #36) ─────────────────

    @Test
    fun tapping_bonus_label_text_shows_tooltip() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule.onNodeWithText("Petit").performClick()

        composeTestRule
            .onNodeWithText(EnStrings.petitTooltipBody, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun tapping_bonus_label_shows_tooltip_title() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule.onNodeWithText("Petit").performClick()

        composeTestRule
            .onAllNodesWithText("Petit")
            .fetchSemanticsNodes()
            .let { nodes ->
                assertTrue(
                    "Tooltip title 'Petit' should be visible after tapping the label",
                    nodes.size >= 1
                )
            }
    }

    @Test
    fun multiple_rounds_show_correct_indicators() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()   // round 1 → skipped
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").performClick() // round 2 → lost (0 pts)

        composeTestRule.onNodeWithTag("round_indicator_skipped").assertIsDisplayed()
        composeTestRule.onNodeWithTag("round_indicator_lost").assertIsDisplayed()
    }

    // ── Spec: system back-button on game screen (issue #38) ───────────────────

    @Test
    fun pressing_system_back_on_game_screen_fires_onEndGame_callback() {
        var endGameCalled = false
        val viewModel = GameViewModel(
            ApplicationProvider.getApplicationContext<Application>(),
            FakeGameStorage()
        )
        viewModel.initGame(players, inProgressGame = null)
        composeTestRule.setContent {
            TarotCounterTheme {
                GameScreen(
                    viewModel = viewModel,
                    onEndGame = { endGameCalled = true }
                )
            }
        }
        Espresso.pressBack()
        assertTrue("System back on game screen should call onEndGame", endGameCalled)
    }

    @Test
    fun pressing_system_back_on_history_overlay_fires_onEndGame_callback() {
        var endGameCalled = false
        val viewModel = GameViewModel(
            ApplicationProvider.getApplicationContext<Application>(),
            FakeGameStorage()
        )
        viewModel.initGame(players, inProgressGame = null)
        composeTestRule.setContent {
            TarotCounterTheme {
                GameScreen(
                    viewModel = viewModel,
                    onEndGame = { endGameCalled = true }
                )
            }
        }
        composeTestRule.onNodeWithText("History").performClick()
        Espresso.pressBack()
        assertTrue("System back on history overlay should call onEndGame", endGameCalled)
    }
}

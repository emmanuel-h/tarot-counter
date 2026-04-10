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

    /**
     * Selects a contract by its test tag then types [score] into the points field so the Confirm
     * button becomes enabled. Required in every test that submits a round, because
     * Confirm is now disabled until both a contract and a non-empty score are set.
     *
     * [contractTag] is the testTag on the SegmentedButton, e.g. "contract_GARDE".
     * Using tags instead of display text makes tests locale-independent and robust against
     * the multiplier suffix now shown in each button label (e.g. "Guard ×2").
     */
    private fun selectContractAndEnterScore(contractTag: String = "contract_GARDE", score: String = "45") {
        composeTestRule.onNodeWithTag(contractTag).performClick()
        composeTestRule.onNodeWithTag("points_input").performTextInput(score)
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
        // Each button now shows "<localized name> ×<multiplier>" (e.g. "Guard ×2" in EN).
        // We assert via testTag (locale-independent) and also verify the multiplier suffix
        // is visible as text so the feature is covered end-to-end.
        Contract.entries.forEach { contract ->
            composeTestRule.onNodeWithTag("contract_${contract.name}").assertIsDisplayed()
        }
    }

    // ── Spec: contract multiplier is visible in the button label (issue #104) ───

    @Test
    fun contract_buttons_display_multiplier_factor() {
        // Spec (#104): the multiplier (×1, ×2, ×4, ×6) must be visible in the
        // contract selection row so players understand the score impact at a glance.
        launchGame()
        listOf("×1", "×2", "×4", "×6").forEach { multiplier ->
            composeTestRule
                .onNodeWithText(multiplier, substring = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun pousse_button_does_not_exist() {
        launchGame()
        composeTestRule.onNodeWithText("Pousse").assertDoesNotExist()
    }

    // ── Spec: bottom action bar (issues #32, #89) ────────────────────────────
    // All three action buttons must sit on a single horizontal row at the bottom.

    @Test
    fun skip_round_button_is_displayed_in_bottom_bar() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").assertIsDisplayed()
    }

    @Test
    fun all_three_bottom_bar_buttons_are_displayed_from_the_start() {
        // Spec (#89): End Game, Skip Round, and Confirm round must all be visible
        // on the same row from the moment the game starts.
        launchGame()
        composeTestRule.onNodeWithText("End Game").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip round").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm round").assertIsDisplayed()
    }

    @Test
    fun confirm_button_is_disabled_when_no_contract_is_selected() {
        // Spec (#89): Confirm is always visible but disabled until a contract is chosen.
        launchGame()
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun confirm_button_remains_disabled_when_contract_selected_but_no_score_entered() {
        // Spec: contract alone is not enough — a score value must also be typed.
        launchGame()
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun confirm_button_becomes_enabled_when_contract_selected_and_score_entered() {
        launchGame()
        selectContractAndEnterScore()
        composeTestRule.onNodeWithText("Confirm round").assertIsEnabled()
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
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()
        composeTestRule.onNodeWithText("End Game").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip round").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm round").assertIsDisplayed()
    }

    // ── Spec: selecting a contract opens the details form (Step 2) ────────────

    @Test
    fun selecting_a_contract_shows_the_round_details_form() {
        launchGame()
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()

        // The bouts field appears in the scrollable form content.
        composeTestRule.onNodeWithText("Number of bouts (oudlers)").assertIsDisplayed()
        // Confirm is visible in the bottom bar but still disabled until a score is entered.
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun details_form_shows_selected_contract_in_header() {
        launchGame()
        composeTestRule.onNodeWithTag("contract_GARDE_SANS").performClick()
        // In EN locale the label is "Guard Without ×4"; substring = true so the
        // assertion survives future multiplier-format tweaks.
        composeTestRule
            .onNodeWithText("Guard Without", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun details_form_shows_all_four_chelem_options() {
        launchGame()
        composeTestRule.onNodeWithTag("contract_PRISE").performClick()
        Chelem.entries.forEach { chelem ->
            composeTestRule.onNodeWithText(chelem.displayName).assertIsDisplayed()
        }
    }

    @Test
    fun details_form_shows_bouts_dropdown_with_0_through_3() {
        launchGame()
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()

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
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()

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
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()
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
        selectContractAndEnterScore()
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
        // Enter 0 explicitly so the history row shows "0 pts".
        selectContractAndEnterScore(score = "0")
        composeTestRule.onNodeWithText("Confirm round").performClick()
        // Round history shows the localized contract name without multiplier.
        // In EN locale, Contract.GARDE localizes to "Guard".
        composeTestRule
            .onNodeWithText("Guard", substring = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("0 pts", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun played_round_shows_Lost_in_history_when_taker_did_not_reach_threshold() {
        // 0 bouts, 10 points → needs 56 to win → Lost.
        launchGame()
        selectContractAndEnterScore(score = "10")
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
        selectContractAndEnterScore()
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
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()
        composeTestRule.onNodeWithContentDescription("History").assertIsDisplayed()
    }

    @Test
    fun partner_selector_not_shown_for_3_player_game() {
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie"))
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()
        composeTestRule.onNodeWithText("Partner (called by taker)").assertDoesNotExist()
    }

    @Test
    fun partner_selector_is_shown_for_5_player_game() {
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie", "Dave", "Eve"))
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()
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
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()
        composeTestRule.onNodeWithText("End Game").assertIsDisplayed()
    }

    @Test
    fun tapping_end_game_on_step_2_opens_final_score_screen() {
        launchGame()
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()
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
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()

        composeTestRule.onNodeWithTag("points_input").performTextInput("92")

        composeTestRule
            .onNodeWithText(EnStrings.pointsOutOfRange)
            .assertIsDisplayed()
    }

    @Test
    fun entering_value_above_91_disables_confirm_button() {
        launchGame()
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()

        composeTestRule.onNodeWithTag("points_input").performTextInput("99")

        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun entering_91_does_not_show_error() {
        launchGame()
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()

        composeTestRule.onNodeWithTag("points_input").performTextInput("91")

        composeTestRule
            .onNodeWithText(EnStrings.pointsOutOfRange)
            .assertDoesNotExist()
    }

    @Test
    fun entering_91_keeps_confirm_button_enabled() {
        launchGame()
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()

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
        selectContractAndEnterScore(score = "10") // 10 pts, 0 bouts → Lost
        composeTestRule.onNodeWithText("Confirm round").performClick()
        composeTestRule.onNodeWithTag("round_indicator_lost").assertIsDisplayed()
    }

    @Test
    fun won_round_shows_won_indicator() {
        launchGame()
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()

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
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()

        composeTestRule.onNodeWithText("Petit").performClick()

        composeTestRule
            .onNodeWithText(EnStrings.petitTooltipBody, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun tapping_bonus_label_shows_tooltip_title() {
        launchGame()
        composeTestRule.onNodeWithTag("contract_GARDE").performClick()

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
        composeTestRule.onNodeWithText("Skip round").performClick()  // round 1 → skipped
        selectContractAndEnterScore(score = "10")                    // round 2 → lost
        composeTestRule.onNodeWithText("Confirm round").performClick()

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

    // ── Spec: end game with zero rounds cancels instead of recording (issue #90) ─

    @Test
    fun end_game_with_no_rounds_fires_onEndGame_immediately_without_showing_final_score() {
        // Spec (issue #90): clicking "End Game" before any round is confirmed must
        // cancel the game and navigate away — it must NOT show the Final Score screen.
        var endGameCalled = false
        val storage = FakeGameStorage()
        val viewModel = GameViewModel(
            ApplicationProvider.getApplicationContext<Application>(),
            storage
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

        // Click "End Game" without confirming any rounds.
        composeTestRule.onNodeWithText("End Game").performClick()

        // onEndGame callback must have fired (user navigates away).
        assertTrue("End Game with zero rounds must fire onEndGame", endGameCalled)
        // The Final Score screen must not appear — "Game Over" heading should be absent.
        composeTestRule.onNodeWithText("Game Over").assertDoesNotExist()
    }

    @Test
    fun end_game_with_no_rounds_clears_in_progress_game_in_storage() {
        // Spec (issue #90): cancelling a game with zero rounds must remove the
        // in-progress entry so it does not show up as resumable on the landing screen.
        val storage = FakeGameStorage()
        val viewModel = GameViewModel(
            ApplicationProvider.getApplicationContext<Application>(),
            storage
        )
        viewModel.initGame(players, inProgressGame = null)
        composeTestRule.setContent {
            TarotCounterTheme {
                GameScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("End Game").performClick()

        assertTrue(
            "In-progress game must be cleared when game is cancelled with zero rounds",
            storage.clearInProgressCallCount >= 1
        )
    }

    @Test
    fun end_game_with_rounds_still_shows_final_score_screen() {
        // Guard: the fix for issue #90 must not break the normal "End Game" flow
        // when at least one round has been played.
        val viewModel = GameViewModel(
            ApplicationProvider.getApplicationContext<Application>(),
            FakeGameStorage()
        )
        viewModel.initGame(players, inProgressGame = null)
        composeTestRule.setContent {
            TarotCounterTheme {
                GameScreen(viewModel = viewModel)
            }
        }

        // Confirm one round so roundHistory is non-empty.
        selectContractAndEnterScore()
        composeTestRule.onNodeWithText("Confirm").performClick()

        // Now end the game.
        composeTestRule.onNodeWithText("End Game").performClick()

        // Final Score screen must be shown.
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
    }
}

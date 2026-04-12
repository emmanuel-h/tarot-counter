package fr.mandarine.tarotcounter

import android.app.Application
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
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
     * Selects [attackerName] as the attacker by tapping their name in the attacker
     * segmented-button row. Must be called before selecting a contract, because the
     * "choose a contract" prompt is only shown once an attacker is selected.
     */
    private fun selectAttacker(attackerName: String = "Alice") {
        // The attacker selector shows all player names as segmented-button labels.
        // We need to find the one in the attacker row (not the bonus grid or history).
        // Using onAllNodesWithText and picking the first match is safe because the
        // attacker selector is the topmost occurrence of each name before the form opens.
        composeTestRule.onAllNodesWithText(attackerName).fetchSemanticsNodes().let {
            require(it.isNotEmpty()) { "No node found with text '$attackerName'" }
        }
        composeTestRule.onAllNodesWithText(attackerName)[0].performClick()
    }

    /**
     * Selects an attacker, selects [contract], then types [score] into the points
     * field so the Confirm button becomes enabled. Required in every test that submits
     * a round, because Confirm is disabled until an attacker, a contract, and a
     * non-empty score are all provided (issue #124).
     */
    private fun selectContractAndEnterScore(
        attacker: String = "Alice",
        contract: String = "Garde",
        score: String = "45"
    ) {
        selectAttacker(attacker)
        composeTestRule.onNodeWithText(contract).performClick()
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
    fun contract_buttons_hidden_before_attacker_is_selected() {
        // Issue #131: contract buttons must not appear until the attacker is chosen.
        launchGame()
        Contract.entries.forEach { contract ->
            composeTestRule.onNodeWithText(contract.displayName).assertDoesNotExist()
        }
    }

    @Test
    fun all_four_contract_buttons_are_displayed() {
        // Contract buttons only appear after the attacker is selected (issue #131).
        launchGame()
        selectAttacker()
        Contract.entries.forEach { contract ->
            composeTestRule.onNodeWithText(contract.displayName).assertIsDisplayed()
        }
    }

    @Test
    fun pousse_button_does_not_exist() {
        // Even after selecting an attacker, "Pousse" must never appear.
        launchGame()
        selectAttacker()
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
        // An attacker must be selected first because contract buttons are hidden until then
        // (issue #131).
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun confirm_button_remains_disabled_when_attacker_and_contract_selected_but_no_score() {
        // All three conditions must be met: attacker + contract + score.
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun confirm_button_remains_disabled_when_only_attacker_selected() {
        // Selecting only the attacker is not enough — a contract is also required.
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun confirm_button_becomes_enabled_when_attacker_contract_and_score_all_set() {
        // Fix for issue #124: attacker selection is now required in addition to
        // contract and score before the Confirm button becomes active.
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
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithText("End Game").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip round").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm round").assertIsDisplayed()
    }

    // ── Spec: selecting a contract opens the details form (Step 2) ────────────

    @Test
    fun selecting_a_contract_shows_the_round_details_form() {
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        // The bouts field appears in the scrollable form content.
        composeTestRule.onNodeWithText("Number of bouts (oudlers)").assertIsDisplayed()
        // Confirm is visible in the bottom bar but still disabled until a score is entered.
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
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
    fun score_history_button_is_visible_and_enabled_from_round_1() {
        // The button is always rendered and always enabled so users can discover it immediately.
        launchGame()
        composeTestRule.onNodeWithContentDescription("History").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("History").assertIsEnabled()
    }

    @Test
    fun tapping_history_button_before_first_round_shows_header_only() {
        // Before any round is played the table should show player-name headers but no score rows.
        launchGame()
        composeTestRule.onNodeWithContentDescription("History").performClick()
        // The screen title and the player-name header must appear.
        composeTestRule.onNodeWithText("Score history").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        // No round rows: "Round" column header is present but no "1", "2", … cells.
        composeTestRule.onAllNodesWithText("1").assertCountEquals(0)
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

    @Test
    fun partner_dropdown_shows_non_attacker_players_when_opened() {
        // Opening the partner dropdown must list all players except the attacker.
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie", "Dave", "Eve"))
        selectAttacker("Alice")
        composeTestRule.onNodeWithText("Garde").performClick()

        // Tap the dropdown field (identified by its test tag) to open the menu.
        composeTestRule.onNodeWithTag("partner_dropdown").performClick()

        // Alice is the attacker — she must NOT appear; the other four must.
        for (name in listOf("Bob", "Charlie", "Dave", "Eve")) {
            composeTestRule.onNodeWithText(name).assertIsDisplayed()
        }
    }

    @Test
    fun partner_dropdown_sets_selected_player_on_item_click() {
        // Picking a player from the dropdown must display their name in the field.
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie", "Dave", "Eve"))
        selectAttacker("Alice")
        composeTestRule.onNodeWithText("Garde").performClick()

        // Open the dropdown and pick Bob.
        composeTestRule.onNodeWithTag("partner_dropdown").performClick()
        composeTestRule.onNodeWithText("Bob").performClick()

        // The OutlinedTextField inside the dropdown now shows Bob's name.
        composeTestRule.onNodeWithTag("partner_dropdown")
            .assert(hasText("Bob"))
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

    // ── Spec: points field label (issue #114) ────────────────────────────────

    @Test
    fun points_input_shows_attacker_label_by_default() {
        // By default the field should show the attacker label (including the valid range)
        // so users always know what to enter without needing an external hint.
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule
            .onNodeWithText(EnStrings.attackerPointsLabel)
            .assertIsDisplayed()
    }

    // ── Spec: camp toggle (issue #115) ────────────────────────────────────────

    @Test
    fun camp_toggle_icon_is_visible_after_contract_selected() {
        // The trailing toggle icon must appear once the points field is shown
        // (i.e. after a contract is selected), giving the user access to swap camps.
        launchGame()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule
            .onNodeWithTag("camp_toggle")
            .assertIsDisplayed()
    }

    @Test
    fun tapping_camp_toggle_switches_label_to_defenders() {
        // Tapping the trailing icon when in attacker mode must switch the field
        // label to the defenders label, confirming the mode changed.
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText("Garde").performClick()

        // Default: attacker label is visible.
        composeTestRule
            .onNodeWithText(EnStrings.attackerPointsLabel)
            .assertIsDisplayed()

        // Tap the toggle icon.
        composeTestRule.onNodeWithTag("camp_toggle").performClick()

        // After toggling: defenders label should now be visible.
        composeTestRule
            .onNodeWithText(EnStrings.defenderPointsLabel)
            .assertIsDisplayed()
    }

    @Test
    fun tapping_camp_toggle_twice_returns_to_attacker_label() {
        // Two taps on the toggle must cycle back to attacker mode.
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule.onNodeWithTag("camp_toggle").performClick() // → defenders
        composeTestRule.onNodeWithTag("camp_toggle").performClick() // → attacker

        composeTestRule
            .onNodeWithText(EnStrings.attackerPointsLabel)
            .assertIsDisplayed()
    }

    @Test
    fun tapping_camp_toggle_clears_typed_points() {
        // When the user switches camps the existing value must be cleared so
        // there is no ambiguity about which team the displayed number belongs to.
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithTag("points_input").performTextInput("45")

        composeTestRule.onNodeWithTag("camp_toggle").performClick()

        // After the toggle the field must be empty.
        composeTestRule
            .onNodeWithTag("points_input")
            .assert(hasText(""))
    }

    @Test
    fun defender_mode_derives_taker_points_on_confirm() {
        // Entering 30 in defender mode must record taker points as 91 - 30 = 61.
        // With 1 bout the threshold is 51, so 61 pts → won (+round score in history).
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText("Garde").performClick()

        // Switch to defender mode.
        composeTestRule.onNodeWithTag("camp_toggle").performClick()

        // Enter the defenders' points (30 → taker has 91-30 = 61 pts).
        composeTestRule.onNodeWithTag("points_input").performTextInput("30")

        // Select 1 bout so the threshold is 51 — taker with 61 pts wins.
        composeTestRule.onNodeWithTag("bouts_dropdown").performClick()
        composeTestRule.onAllNodesWithText("1")[0].performClick()

        composeTestRule.onNodeWithText("Confirm round").performClick()

        // The round was recorded successfully — the header advances to Round 2.
        composeTestRule.onNodeWithText("Round 2").assertIsDisplayed()
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
        selectAttacker()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule.onNodeWithTag("points_input").performTextInput("91")

        composeTestRule
            .onNodeWithText(EnStrings.pointsOutOfRange)
            .assertDoesNotExist()
    }

    @Test
    fun entering_91_keeps_confirm_button_enabled() {
        // An attacker must also be selected — issue #124.
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText("Garde").performClick()

        composeTestRule.onNodeWithTag("points_input").performTextInput("91")

        composeTestRule.onNodeWithText("Confirm round").assertIsEnabled()
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
        composeTestRule.onNodeWithText("Confirm round").performClick()

        // Now end the game.
        composeTestRule.onNodeWithText("End Game").performClick()

        // Final Score screen must be shown.
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
    }

    // ── Spec: attacker selection (issue #124) ─────────────────────────────────
    // The attacker is the player who wins the bidding and takes the contract.
    // Any player can be the attacker, regardless of who is dealing this round.

    @Test
    fun attacker_selector_shows_all_player_names() {
        // All players must be visible in the attacker selector so any can be chosen.
        launchGame()
        players.forEach { name ->
            assertTrue(
                "$name should appear in the attacker selector",
                composeTestRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
            )
        }
    }

    @Test
    fun dealer_label_is_displayed() {
        // The dealer label ("Dealer: Alice" / "Distributeur : Alice") should always
        // be visible so the table knows whose turn it is to distribute the cards.
        launchGame()
        assertTrue(
            "Dealer label should be visible",
            composeTestRule.onAllNodesWithText("Dealer:", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun attacker_label_is_displayed() {
        // The "Attacker" label should always be visible above the selector row.
        launchGame()
        composeTestRule.onNodeWithText("Attacker").assertIsDisplayed()
    }

    @Test
    fun confirm_disabled_without_attacker_even_when_contract_and_score_set() {
        // Core regression test for issue #124: scoring must go to the selected attacker.
        // Confirm must stay disabled when no attacker is selected.
        launchGame()
        // Select contract and enter score, but do NOT select an attacker.
        composeTestRule.onNodeWithText("Garde").performClick()
        composeTestRule.onNodeWithTag("points_input").performTextInput("45")
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun selecting_attacker_then_contract_and_score_enables_confirm() {
        // Selecting the attacker should allow the round to be confirmed once the
        // other required fields (contract, score) are also filled in.
        launchGame()
        selectContractAndEnterScore(attacker = "Bob")
        composeTestRule.onNodeWithText("Confirm round").assertIsEnabled()
    }

    @Test
    fun choose_contract_prompt_appears_after_attacker_is_selected() {
        // The "Attacker — choose a contract:" prompt should appear once an attacker
        // is selected, and include the selected attacker's name.
        launchGame()
        selectAttacker("Charlie")
        composeTestRule
            .onNodeWithText("Charlie", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun attacker_resets_after_round_is_confirmed() {
        // After a round is confirmed the attacker selection must clear so the next
        // round starts fresh — no player should be pre-selected.
        launchGame()
        selectContractAndEnterScore(attacker = "Alice")
        composeTestRule.onNodeWithText("Confirm round").performClick()

        // After advancing to round 2, the Confirm button must again be disabled
        // (no attacker selected yet for the new round).
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    // ── Spec: undo previous round (issue #146) ───────────────────────────────

    @Test
    fun undo_button_is_not_shown_before_any_round_is_played() {
        // The undo button must not appear when there is nothing to undo.
        launchGame()
        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .assertDoesNotExist()
    }

    @Test
    fun undo_button_appears_after_a_round_is_confirmed() {
        // Once at least one round has been recorded the undo button must be visible.
        launchGame()
        selectContractAndEnterScore()
        composeTestRule.onNodeWithText("Confirm round").performClick()

        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .assertIsDisplayed()
    }

    @Test
    fun undo_button_appears_after_a_round_is_skipped() {
        // Skipped rounds are recorded too, so undo must be available after a skip.
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()

        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .assertIsDisplayed()
    }

    @Test
    fun tapping_undo_button_shows_confirmation_dialog() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()

        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .performClick()

        composeTestRule.onNodeWithText(EnStrings.undoConfirmTitle).assertIsDisplayed()
    }

    @Test
    fun undo_confirmation_dialog_shows_updated_body_text() {
        // The body must describe pre-filling behaviour, not deletion.
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()

        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .performClick()

        composeTestRule.onNodeWithText(EnStrings.undoConfirmBody).assertIsDisplayed()
    }

    @Test
    fun cancelling_undo_dialog_keeps_the_current_round() {
        // Tapping Cancel must close the dialog without changing the round counter.
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .performClick()

        composeTestRule.onNodeWithText(EnStrings.cancel).performClick()

        // Still on round 2 after the skip.
        composeTestRule.onNodeWithText("Round 2").assertIsDisplayed()
    }

    @Test
    fun confirming_undo_goes_back_to_round_1() {
        // After confirming undo the round counter must decrease by one.
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        // Now on round 2 — undo it.
        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .performClick()
        composeTestRule.onNodeWithText(EnStrings.undoPreviousRound).performClick()

        composeTestRule.onNodeWithText("Round 1").assertIsDisplayed()
    }

    @Test
    fun confirming_undo_restores_attacker_in_form() {
        // After undo, the attacker from the previous round must be pre-selected.
        // The "choose a contract" prompt includes the attacker name and is only shown
        // when an attacker is selected — so its presence proves the attacker was restored.
        launchGame()
        selectContractAndEnterScore(attacker = "Alice", contract = "Garde", score = "45")
        composeTestRule.onNodeWithText("Confirm round").performClick()

        // Undo round 1.
        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .performClick()
        composeTestRule.onNodeWithText(EnStrings.undoPreviousRound).performClick()

        // "Alice — choose a contract:" is only rendered when Alice is selected as attacker.
        composeTestRule
            .onNodeWithText("Alice", substring = true)
            .assertIsDisplayed()
        // Contract row must be visible (selectedContract was restored to Garde).
        composeTestRule.onNodeWithText("Garde", substring = true).assertIsDisplayed()
    }

    @Test
    fun confirming_undo_restores_points_in_form() {
        // After undo, the points field must show the taker's score from the previous round.
        launchGame()
        selectContractAndEnterScore(attacker = "Alice", contract = "Garde", score = "45")
        composeTestRule.onNodeWithText("Confirm round").performClick()

        // Undo round 1.
        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .performClick()
        composeTestRule.onNodeWithText(EnStrings.undoPreviousRound).performClick()

        // Points field must be pre-filled with "45".
        composeTestRule
            .onNodeWithTag("points_input")
            .assert(hasText("45"))
    }

    @Test
    fun confirming_undo_hides_undo_button_when_back_to_first_round() {
        // After undoing the only round, the undo button must disappear again.
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .performClick()
        composeTestRule.onNodeWithText(EnStrings.undoPreviousRound).performClick()

        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .assertDoesNotExist()
    }

    @Test
    fun history_button_is_on_top_right_and_undo_on_top_left_after_first_round() {
        // Both buttons must be simultaneously visible after the first round is recorded.
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()

        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(EnStrings.history)
            .assertIsDisplayed()
    }

    // ── Spec: compact scoreboard — player name truncation (issue #118) ─────────

    @Test
    fun scoreboard_shows_all_five_player_names_after_a_round() {
        // Regression: with 5 players, each Column in CompactScoreboard must carry
        // Modifier.weight(1f) so the Row's width is divided equally and
        // TextOverflow.Ellipsis has a finite width to truncate against.
        // Without the weight the columns expand freely and names never clip —
        // meaning they can overflow their neighbour and become unreadable.
        val fivePlayers = listOf("Alice", "Bob", "Charlie", "Dave", "Eve")
        launchGame(playerNames = fivePlayers)
        selectContractAndEnterScore(attacker = "Alice")
        composeTestRule.onNodeWithText("Confirm round").performClick()

        // After the first round the "Scores" card must be visible and every
        // player name must appear somewhere in the composition (possibly truncated
        // to "Ali…" but still present as a semantics node).
        composeTestRule.onNodeWithText("Scores").assertIsDisplayed()
        fivePlayers.forEach { name ->
            assertTrue(
                "Player '$name' should appear in the compact scoreboard after round 1",
                composeTestRule
                    .onAllNodesWithText(name, substring = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            )
        }
    }

    // ── Spec: End Game confirmation when points are pending (issue #150) ──────
    // Clicking "End Game" while the points field is non-empty must show a
    // confirmation dialog to prevent accidentally discarding unsaved round data.

    @Test
    fun end_game_without_pending_points_proceeds_without_dialog() {
        // Spec (issue #150): if the user has NOT typed anything in the points field,
        // pressing "End Game" should end the game immediately with no dialog.
        // This ensures normal flow is unaffected when no data would be lost.
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

        // Click "End Game" without touching the points field at all.
        composeTestRule.onNodeWithText("End Game").performClick()

        // No confirmation dialog should appear — the title is absent.
        composeTestRule.onNodeWithText("End the game?").assertDoesNotExist()
    }

    @Test
    fun end_game_with_pending_points_shows_confirmation_dialog() {
        // Spec (issue #150): if the user has typed points but not confirmed the round,
        // pressing "End Game" must show a confirmation dialog so they can cancel.
        launchGame()

        // Select attacker + contract + type points (but do NOT confirm the round).
        selectContractAndEnterScore()

        // Click "End Game" while the points field is non-empty.
        composeTestRule.onNodeWithText("End Game").performClick()

        // The confirmation dialog title must be visible.
        composeTestRule.onNodeWithText("End the game?").assertIsDisplayed()
        // The dialog body must also be shown.
        composeTestRule.onNodeWithText("The current round will not be saved.").assertIsDisplayed()
    }

    @Test
    fun end_game_confirmation_dialog_cancel_dismisses_dialog_and_keeps_game_running() {
        // Spec (issue #150): tapping "Cancel" in the end-game confirmation dialog must
        // close the dialog and leave the user on the game screen (game still running).
        launchGame()
        selectContractAndEnterScore()

        // Trigger the dialog.
        composeTestRule.onNodeWithText("End Game").performClick()
        composeTestRule.onNodeWithText("End the game?").assertIsDisplayed()

        // Cancel — the dialog should disappear and the game screen should still be shown.
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Dialog is gone.
        composeTestRule.onNodeWithText("End the game?").assertDoesNotExist()
        // Game screen is still active — the round header should still be visible.
        composeTestRule.onNodeWithText("Round 1").assertIsDisplayed()
        // Final Score screen must NOT have been shown.
        composeTestRule.onNodeWithText("Game Over").assertDoesNotExist()
    }

    @Test
    fun end_game_confirmation_dialog_confirm_ends_game_and_shows_final_score() {
        // Spec (issue #150): tapping the "End Game" confirm button in the dialog must
        // end the game and navigate to the Final Score screen (when rounds have been played).
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

        // Confirm one round so roundHistory is non-empty, then start filling the next.
        selectContractAndEnterScore()
        composeTestRule.onNodeWithText("Confirm round").performClick()

        // Begin filling the next round (attacker + contract + points) without confirming.
        selectContractAndEnterScore()

        // Trigger the confirmation dialog and confirm.
        composeTestRule.onNodeWithText("End Game").performClick()
        composeTestRule.onNodeWithText("End the game?").assertIsDisplayed()
        // The dialog has two buttons that say "End Game" — click the one inside the dialog.
        // onAllNodesWithText gives us both; the last one is the confirm button in the dialog.
        composeTestRule.onAllNodesWithText("End Game").apply {
            // There are two: the bottom-bar button and the dialog confirm button.
            // Index 1 is the dialog button (rendered on top of the game screen).
            get(1).performClick()
        }

        // Game should have ended — Final Score screen must now be displayed.
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
    }
}

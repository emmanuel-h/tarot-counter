package fr.mandarine.tarotcounter

import android.app.Application
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
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

    // Contract labels as the screen shows them in English (issue #112 translated them:
    // "Garde" is displayed as "Guard", etc.). Reading them from the same function the
    // UI uses keeps the tests in sync if a translation changes.
    private fun label(contract: Contract) = contract.localizedName(AppLocale.EN)
    private val guard = label(Contract.GARDE)

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
     * Selects [attackerName] as the taker by tapping their tile in the "Who took?"
     * grid (issue #198). This opens the round-entry view (contract, bouts, points…).
     */
    private fun selectAttacker(attackerName: String = "Alice") {
        composeTestRule.onNodeWithTag("taker_tile_$attackerName").performClick()
    }

    /**
     * Selects an attacker, selects [contract], then types [score] into the points
     * field so the Confirm button becomes enabled. Required in every test that submits
     * a round, because Confirm is disabled until an attacker, a contract, and a
     * non-empty score are all provided (issue #124).
     */
    private fun selectContractAndEnterScore(
        attacker: String = "Alice",
        contract: String = guard,
        score: String = "45"
    ) {
        selectAttacker(attacker)
        composeTestRule.onNodeWithText(contract).performClick()
        composeTestRule.onNodeWithTag("points_input").performTextInput(score)
        // Typing opens the soft keyboard, which can cover the bottom action bar and
        // swallow the next tap on End Game / Confirm. Close it so tests are stable.
        Espresso.closeSoftKeyboard()
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
            composeTestRule.onNodeWithText(label(contract)).assertDoesNotExist()
        }
    }

    @Test
    fun all_four_contract_buttons_are_displayed() {
        // Contract buttons only appear after the attacker is selected (issue #131).
        launchGame()
        selectAttacker()
        Contract.entries.forEach { contract ->
            composeTestRule.onNodeWithText(label(contract)).assertIsDisplayed()
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
    fun between_rounds_bottom_bar_shows_end_game_and_skip_round() {
        // Salon (#198): between rounds the bar holds End game (text) and Skip round
        // (outlined). Confirm only appears once a taker opens the round entry.
        launchGame()
        composeTestRule.onNodeWithText("End Game").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip round").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm round").assertDoesNotExist()
    }

    @Test
    fun confirm_button_is_disabled_when_no_contract_is_selected() {
        // Confirm is visible in the round entry but disabled until a contract is chosen.
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun confirm_button_remains_disabled_when_contract_selected_but_no_score_entered() {
        // Spec: contract alone is not enough — a score value must also be typed.
        // An attacker must be selected first because contract buttons are hidden until then
        // (issue #131).
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun confirm_button_remains_disabled_when_attacker_and_contract_selected_but_no_score() {
        // All three conditions must be met: attacker + contract + score.
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()
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
    fun round_entry_bottom_bar_shows_end_game_and_confirm() {
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()
        composeTestRule.onNodeWithText("End Game").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm round").assertIsDisplayed()
        // Skipping only makes sense before a taker is chosen.
        composeTestRule.onNodeWithText("Skip round").assertDoesNotExist()
    }

    // ── Spec: selecting a contract opens the details form (Step 2) ────────────

    @Test
    fun selecting_a_contract_shows_the_round_details_form() {
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()

        // The bouts field appears in the scrollable form content.
        composeTestRule.onNodeWithText("Number of bouts (oudlers)").assertIsDisplayed()
        // Confirm is visible in the bottom bar but still disabled until a score is entered.
        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun details_form_shows_selected_contract_in_header() {
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(label(Contract.GARDE_SANS)).performClick()
        // The chosen contract stays highlighted while the form is open.
        composeTestRule
            .onNodeWithText(label(Contract.GARDE_SANS))
            .assertIsSelected()
    }

    @Test
    fun details_form_shows_all_four_chelem_options() {
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(label(Contract.PRISE)).performClick()
        // The chelem outcomes live in a dropdown menu: open it, then look inside the popup.
        composeTestRule.onNodeWithTag("chelem_dropdown").performClick()
        Chelem.entries.forEach { chelem ->
            composeTestRule
                .onNode(hasText(chelem.localizedName(AppLocale.EN)) and hasAnyAncestor(isPopup()))
                .assertIsDisplayed()
        }
    }

    @Test
    fun details_form_shows_bouts_dropdown_with_0_through_3() {
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()

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
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()

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
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()
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
    fun standings_card_appears_after_first_played_round() {
        launchGame()
        composeTestRule.onNodeWithTag("standings_card").assertDoesNotExist()
        selectContractAndEnterScore()
        composeTestRule.onNodeWithText("Confirm round").performClick()

        composeTestRule.onNodeWithTag("standings_card").assertIsDisplayed()
        players.forEach { name ->
            composeTestRule.onNodeWithTag("standing_$name").assertExists()
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
    fun round_entry_shows_taker_title_and_back_arrow() {
        launchGame()
        selectAttacker("Alice")
        composeTestRule.onNodeWithText("Alice takes").assertIsDisplayed()
        // The back arrow returns to "Who took?" and keeps the chosen taker selected.
        composeTestRule.onNodeWithContentDescription("Change taker").performClick()
        composeTestRule.onNodeWithText("Who took?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("taker_tile_Alice").assertIsSelected()
    }

    @Test
    fun partner_selector_not_shown_for_3_player_game() {
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie"))
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()
        composeTestRule.onNodeWithText("Partner (called by taker)").assertDoesNotExist()
    }

    @Test
    fun partner_selector_is_shown_for_5_player_game() {
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie", "Dave", "Eve"))
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()
        composeTestRule.onNodeWithText("Partner (called by taker)").assertIsDisplayed()
    }

    @Test
    fun partner_dropdown_shows_non_attacker_players_when_opened() {
        // Opening the partner dropdown must list all players except the attacker.
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie", "Dave", "Eve"))
        selectAttacker("Alice")
        composeTestRule.onNodeWithText(guard).performClick()

        // Tap the dropdown field (identified by its test tag) to open the menu.
        composeTestRule.onNodeWithTag("partner_dropdown").performClick()

        // Alice is the attacker — she must NOT appear; the other four must.
        // Names also appear on the attacker buttons, so only look inside the popup menu.
        for (name in listOf("Bob", "Charlie", "Dave", "Eve")) {
            composeTestRule.onNode(hasText(name) and hasAnyAncestor(isPopup())).assertIsDisplayed()
        }
        composeTestRule.onNode(hasText("Alice") and hasAnyAncestor(isPopup())).assertDoesNotExist()
    }

    @Test
    fun partner_dropdown_sets_selected_player_on_item_click() {
        // Picking a player from the dropdown must display their name in the field.
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie", "Dave", "Eve"))
        selectAttacker("Alice")
        composeTestRule.onNodeWithText(guard).performClick()

        // Open the dropdown and pick Bob.
        composeTestRule.onNodeWithTag("partner_dropdown").performClick()
        // Pick the menu item (the attacker buttons also show "Bob").
        composeTestRule.onNode(hasText("Bob") and hasAnyAncestor(isPopup())).performClick()

        // The OutlinedTextField inside the dropdown now shows Bob's name.
        // The tag sits on the dropdown box; the selected name is in its text field child.
        composeTestRule.onNodeWithTag("partner_dropdown")
            .assert(hasAnyDescendant(hasText("Bob")))
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
        // With zero rounds, End Game leaves the game instead: skip one round first.
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule.onNodeWithText("End Game").performClick()
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
    }

    @Test
    fun end_game_button_is_displayed_on_step_2() {
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()
        composeTestRule.onNodeWithText("End Game").assertIsDisplayed()
    }

    @Test
    fun tapping_end_game_on_step_2_opens_final_score_screen() {
        launchGame()
        // With zero rounds, End Game leaves the game instead: skip one round first.
        composeTestRule.onNodeWithText("Skip round").performClick()
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()
        composeTestRule.onNodeWithText("End Game").performClick()
        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
    }

    @Test
    fun final_score_screen_shows_new_game_button() {
        launchGame()
        // With zero rounds, End Game leaves the game instead: skip one round first.
        composeTestRule.onNodeWithText("Skip round").performClick()
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
        // With zero rounds, End Game leaves the game instead: skip one round first.
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule.onNodeWithText("End Game").performClick()
        composeTestRule
            .onNodeWithContentDescription("Back to game")
            .performClick()
        composeTestRule.onNodeWithText("Round 2").assertIsDisplayed()
    }

    // ── Spec: points field label (issue #114) ────────────────────────────────

    @Test
    fun points_input_shows_attacker_label_by_default() {
        // By default the field should show the attacker label (including the valid range)
        // so users always know what to enter without needing an external hint.
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()

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
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()

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
        composeTestRule.onNodeWithText(guard).performClick()

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
        composeTestRule.onNodeWithText(guard).performClick()

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
        composeTestRule.onNodeWithText(guard).performClick()
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
        composeTestRule.onNodeWithText(guard).performClick()

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
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()

        composeTestRule.onNodeWithTag("points_input").performTextInput("92")

        composeTestRule
            .onNodeWithText(EnStrings.pointsOutOfRange)
            .assertIsDisplayed()
    }

    @Test
    fun entering_value_above_91_disables_confirm_button() {
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()

        composeTestRule.onNodeWithTag("points_input").performTextInput("99")

        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun entering_91_does_not_show_error() {
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()

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
        composeTestRule.onNodeWithText(guard).performClick()

        composeTestRule.onNodeWithTag("points_input").performTextInput("91")

        composeTestRule.onNodeWithText("Confirm round").assertIsEnabled()
    }

    // ── Spec: atout count validation (issue #149) ────────────────────────────

    @Test
    fun declaring_too_many_atouts_shows_error_message() {
        // 3-player game: thresholds are simple=13, double=15, triple=18.
        // Selecting triple (18) + simple (13) = 31 > 22 → error must appear.
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie"))
        selectAttacker("Alice")
        composeTestRule.onNodeWithText(guard).performClick()

        // The bonus grid has 4 rows × 3 players = 12 toggleable checkboxes.
        // Row order in traversal: Petit (0-2), Poignée (3-5), Double (6-8), Triple (9-11).
        // Tick Alice's triple-poignée box (index 9).
        val boxes = composeTestRule.onAllNodes(
            androidx.compose.ui.test.hasClickAction() and
            androidx.compose.ui.test.isToggleable()
        )
        boxes[9].performClick()  // Alice — triple
        boxes[3].performClick()  // Alice — simple  (13 + 18 = 31 > 22)

        // Error message must be visible.
        composeTestRule
            .onNodeWithTag("atout_count_error")
            .assertIsDisplayed()
    }

    @Test
    fun declaring_too_many_atouts_disables_confirm_button() {
        // 3-player game: triple (18) + simple (13) = 31 > 22 → Confirm disabled.
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie"))
        selectAttacker("Alice")
        composeTestRule.onNodeWithText(guard).performClick()

        val boxes = composeTestRule.onAllNodes(
            androidx.compose.ui.test.hasClickAction() and
            androidx.compose.ui.test.isToggleable()
        )
        boxes[9].performClick()  // Alice — triple (18)
        boxes[3].performClick()  // Alice — simple (13)  → total 31

        // Type the points last: the soft keyboard it opens would otherwise cover
        // the checkboxes and swallow the taps above.
        composeTestRule.onNodeWithTag("points_input").performTextInput("50")

        composeTestRule.onNodeWithText("Confirm round").assertIsNotEnabled()
    }

    @Test
    fun valid_multi_player_atout_count_does_not_show_error() {
        // 4-player game: Alice simple (10) + Bob simple (10) = 20 ≤ 22 → no error.
        launchGame()   // uses default 3-player list "Alice", "Bob", "Charlie"
        selectAttacker("Alice")
        composeTestRule.onNodeWithText(guard).performClick()

        // 3-player row layout: rows × 3 columns.
        // Poignée row starts at index 3. Alice = index 3, Bob = index 4.
        val boxes = composeTestRule.onAllNodes(
            androidx.compose.ui.test.hasClickAction() and
            androidx.compose.ui.test.isToggleable()
        )
        boxes[3].performClick()  // Alice — simple (3-player threshold = 13)
        // Total = 13 ≤ 22 — no error.

        composeTestRule
            .onNodeWithTag("atout_count_error")
            .assertDoesNotExist()
    }

    // ── Spec: bonus label cell is fully tappable (issue #36) ─────────────────

    @Test
    fun tapping_bonus_label_text_shows_tooltip() {
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()

        composeTestRule.onNodeWithText("Petit").performClick()

        composeTestRule
            .onNodeWithText(EnStrings.petitTooltipBody, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun tapping_bonus_label_shows_tooltip_title() {
        launchGame()
        selectAttacker()
        composeTestRule.onNodeWithText(guard).performClick()

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
        composeTestRule.onNodeWithContentDescription("History").performClick()
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
    fun who_took_heading_and_one_tile_per_player_are_displayed() {
        launchGame()
        composeTestRule.onNodeWithText("Who took?").assertIsDisplayed()
        players.forEach { composeTestRule.onNodeWithTag("taker_tile_$it").assertIsDisplayed() }
    }

    @Test
    fun changing_the_taker_clears_the_contract_and_disables_confirm() {
        // Core regression for issue #124: scoring must go to the selected taker.
        // Going back and picking another taker resets the form, so Confirm is
        // disabled until the new taker's contract and points are entered.
        launchGame()
        selectContractAndEnterScore(attacker = "Alice")
        composeTestRule.onNodeWithContentDescription("Change taker").performClick()
        selectAttacker("Bob")
        composeTestRule.onNodeWithText("Bob takes").assertIsDisplayed()
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
            .onNodeWithText(EnStrings.chooseContract("Charlie"))
            .assertIsDisplayed()
    }

    @Test
    fun attacker_resets_after_round_is_confirmed() {
        // After a round is confirmed the view returns to "Who took?" with no tile
        // selected, so the next round starts fresh.
        launchGame()
        selectContractAndEnterScore(attacker = "Alice")
        composeTestRule.onNodeWithText("Confirm round").performClick()

        composeTestRule.onNodeWithText("Who took?").assertIsDisplayed()
        players.forEach { composeTestRule.onNodeWithTag("taker_tile_$it").assertIsNotSelected() }
        composeTestRule.onNodeWithText("Confirm round").assertDoesNotExist()
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
        selectContractAndEnterScore(attacker = "Alice", score = "45")
        composeTestRule.onNodeWithText("Confirm round").performClick()

        // Undo round 1.
        composeTestRule
            .onNodeWithContentDescription(EnStrings.undoPreviousRound)
            .performClick()
        composeTestRule.onNodeWithText(EnStrings.undoPreviousRound).performClick()

        // "Alice — choose a contract:" is only rendered when Alice is selected as attacker.
        composeTestRule
            .onNodeWithText(EnStrings.chooseContract("Alice"))
            .assertIsDisplayed()
        // Contract row must be visible (selectedContract was restored to Garde).
        composeTestRule.onNodeWithText(guard).assertIsSelected()
    }

    @Test
    fun confirming_undo_restores_points_in_form() {
        // After undo, the points field must show the taker's score from the previous round.
        launchGame()
        selectContractAndEnterScore(attacker = "Alice", score = "45")
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
    fun standings_show_all_five_players_after_a_round() {
        val fivePlayers = listOf("Alice", "Bob", "Charlie", "Dave", "Eve")
        launchGame(playerNames = fivePlayers)
        selectContractAndEnterScore(attacker = "Alice")
        composeTestRule.onNodeWithText("Confirm round").performClick()

        composeTestRule.onNodeWithTag("standings_card").assertIsDisplayed()
        fivePlayers.forEach { composeTestRule.onNodeWithTag("standing_$it").assertExists() }
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
        // Game screen is still active — the round entry is still open.
        composeTestRule.onNodeWithText("Alice takes").assertIsDisplayed()
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

    @Test
    fun end_game_confirmation_dialog_confirm_with_zero_rounds_cancels_game_and_navigates_away() {
        // Spec (issue #150): if the user typed points but never confirmed a round,
        // confirming the end-game dialog must cancel the game (not show Final Score).
        // This covers the zero-rounds path inside the dialog's confirm handler.
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

        // Enter points (and select attacker + contract) without confirming the round.
        selectContractAndEnterScore()

        // Trigger the dialog and confirm.
        composeTestRule.onNodeWithText("End Game").performClick()
        composeTestRule.onNodeWithText("End the game?").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("End Game").get(1).performClick()

        // onEndGame callback must have fired — user navigates away.
        assertTrue("Confirming end-game dialog with zero rounds must fire onEndGame", endGameCalled)
        // Final Score screen must NOT appear.
        composeTestRule.onNodeWithText("Game Over").assertDoesNotExist()
        // In-progress game entry must have been cleared from storage.
        assertTrue(
            "In-progress game must be cleared when game is cancelled via dialog",
            storage.clearInProgressCallCount >= 1
        )
    }

    // ── Salon game screen (issue #198) ────────────────────────────────────────

    @Test
    fun dealer_chip_is_shown_between_rounds() {
        launchGame()
        composeTestRule.onNodeWithTag("dealer_chip").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dealer:", substring = true).assertIsDisplayed()
    }

    @Test
    fun last_rounds_log_lists_skipped_and_played_rounds_newest_first() {
        launchGame()
        composeTestRule.onNodeWithText("Last rounds").assertDoesNotExist()
        composeTestRule.onNodeWithText("Skip round").performClick()
        selectContractAndEnterScore(attacker = "Alice")
        composeTestRule.onNodeWithText("Confirm round").performClick()

        composeTestRule.onNodeWithText("Last rounds").assertIsDisplayed()
        composeTestRule.onNodeWithTag("last_round_1").assertExists()
        composeTestRule.onNodeWithTag("last_round_2").assertExists()
        composeTestRule.onNodeWithText("Skipped").assertExists()
        composeTestRule.onNodeWithText("Alice · $guard", substring = true).assertExists()
        // Newest first: round 2 sits above round 1.
        val r2 = composeTestRule.onNodeWithTag("last_round_2").fetchSemanticsNode().boundsInRoot
        val r1 = composeTestRule.onNodeWithTag("last_round_1").fetchSemanticsNode().boundsInRoot
        assertTrue("R2 must be above R1", r2.top < r1.top)
    }

    @Test
    fun see_all_opens_the_score_history() {
        launchGame()
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule.onNodeWithText("See all").performClick()
        composeTestRule.onNodeWithText("Score history").assertIsDisplayed()
    }

    @Test
    fun leader_row_shows_leading_label_after_a_won_round() {
        launchGame()
        // With 0 bouts the taker needs 56 points: 60 wins, so Alice leads.
        selectContractAndEnterScore(attacker = "Alice", score = "60")
        composeTestRule.onNodeWithText("Confirm round").performClick()
        composeTestRule.onNodeWithText("LEADING").assertIsDisplayed()
    }

    @Test
    fun top_bar_has_history_and_undo_icon_buttons() {
        launchGame()
        composeTestRule.onNodeWithContentDescription("History").assertIsDisplayed()
        // Nothing to undo before the first round.
        composeTestRule.onNodeWithContentDescription("Undo previous round").assertDoesNotExist()
        composeTestRule.onNodeWithText("Skip round").performClick()
        composeTestRule.onNodeWithContentDescription("Undo previous round").assertIsDisplayed()
    }

    @Test
    fun four_players_get_a_two_by_two_taker_grid() {
        launchGame(playerNames = listOf("Alice", "Bob", "Charlie", "Dave"))
        val a = composeTestRule.onNodeWithTag("taker_tile_Alice").fetchSemanticsNode().boundsInRoot
        val b = composeTestRule.onNodeWithTag("taker_tile_Bob").fetchSemanticsNode().boundsInRoot
        val c = composeTestRule.onNodeWithTag("taker_tile_Charlie").fetchSemanticsNode().boundsInRoot
        assertTrue("Alice and Bob share the first row", a.top == b.top)
        assertTrue("Charlie starts the second row", c.top > a.top && c.left == a.left)
    }

    @Test
    fun system_back_in_round_entry_returns_to_who_took() {
        var endedGame = false
        val viewModel = GameViewModel(
            ApplicationProvider.getApplicationContext<Application>(), FakeGameStorage()
        )
        viewModel.initGame(players, inProgressGame = null)
        composeTestRule.setContent {
            TarotCounterTheme { GameScreen(viewModel = viewModel, onEndGame = { endedGame = true }) }
        }
        selectAttacker("Alice")
        Espresso.pressBack()
        composeTestRule.onNodeWithText("Who took?").assertIsDisplayed()
        assertTrue("Back from the round entry must not leave the game", !endedGame)
    }
}


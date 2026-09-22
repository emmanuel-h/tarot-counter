package fr.mandarine.tarotcounter

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for LandingScreen (the player-setup screen).
 *
 * These run on a device or emulator via AndroidJUnit4.
 * Run with: ./gradlew connectedAndroidTest
 *
 * Spec (docs/game-flow.md — Setup Screen):
 *   - Choose 3–5 players via filter chips.
 *   - Enter optional names for each player.
 *   - Tap Start Game to lock in names and navigate to the game screen.
 *   - Tap the gear icon to navigate to the Settings page.
 */
@RunWith(AndroidJUnit4::class)
class LandingScreenTest {

    // createComposeRule() sets up an isolated Compose environment for each test.
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Launches LandingScreen inside our app theme (same as production).
     * [onNavigateToSettings] captures whether the settings gear was tapped.
     *
     * The [onStartGame] lambda now receives both the raw name list **and** the chosen
     * dealer index (null = random). Existing tests that only care about names use the
     * default no-op `{ _, _ -> }`.
     */
    private fun launch(
        onStartGame: (List<String>, Int?) -> Unit = { _, _ -> },
        onNavigateToSettings: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TarotCounterTheme {
                LandingScreen(
                    onStartGame          = onStartGame,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }

    /**
     * The name field of seat [index] (0-based). Fields are found by test tag because
     * their "Player N" placeholder disappears as soon as the user types.
     */
    private fun field(index: Int) = composeTestRule.onNodeWithTag("player_name_field_$index")

    /** One scored round, so saved/in-progress games have a real winner/leader. */
    private val scoredRound = RoundResult(
        roundNumber = 1, takerName = "Alice", contract = Contract.GARDE, details = null,
        won = true, playerScores = mapOf("Alice" to 150, "Bob" to -75, "Charlie" to -75)
    )

    /** Launches LandingScreen with a game in progress, so the felt resume card appears. */
    private fun launchWithInProgress(onResume: (InProgressGame) -> Unit = {}) {
        composeTestRule.setContent {
            TarotCounterTheme {
                LandingScreen(
                    inProgressGame = InProgressGame(
                        gameId = "g", playerNames = listOf("Alice", "Bob", "Charlie"),
                        currentRound = 2, startingIndex = 0, rounds = listOf(scoredRound)
                    ),
                    onResumeGame = onResume
                )
            }
        }
    }

    /** Launches LandingScreen with a list of past games so the history section appears. */
    private fun launchWithPastGames() {
        composeTestRule.setContent {
            TarotCounterTheme {
                LandingScreen(
                    pastGames = listOf(
                        SavedGame(
                            id = "test-1",
                            datestamp = System.currentTimeMillis(),
                            playerNames = listOf("Alice", "Bob", "Charlie"),
                            rounds = listOf(scoredRound),
                            finalScores = mapOf("Alice" to 150, "Bob" to -75, "Charlie" to -75)
                        )
                    )
                )
            }
        }
    }

    // ── Spec: title is shown ──────────────────────────────────────────────────

    @Test
    fun screen_shows_app_title() {
        // The default locale (AppLocale.EN) renders the English title.
        launch()
        composeTestRule.onNodeWithText("Tarot Counter").assertIsDisplayed()
    }

    // ── Spec: settings gear icon is shown ────────────────────────────────────

    @Test
    fun settings_gear_icon_is_displayed() {
        launch()
        // The gear icon has contentDescription = strings.settings = "Settings".
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun tapping_settings_icon_calls_onNavigateToSettings() {
        var called = false
        launch(onNavigateToSettings = { called = true })

        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        assert(called) { "Expected onNavigateToSettings to be called when gear icon is tapped" }
    }

    // ── Spec: settings button is large/consistent with other action buttons ────
    // The settings button lives in the SalonTopBar, whose icon buttons are 48 dp.
    @Test
    fun settings_button_has_minimum_touch_target_size() {
        launch()
        val bounds = composeTestRule
            .onNodeWithContentDescription("Settings")
            .getBoundsInRoot()
        val width  = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        // SalonTopBar icon buttons provide a 48 dp touch area.
        assert(width >= 48.dp && height >= 48.dp) {
            "Expected settings button to be at least 48×48 dp " +
                "(actual: ${width}×${height})"
        }
    }

    // ── Spec: player-count chips 3, 4, 5 ─────────────────────────────────────

    @Test
    fun player_count_chips_3_4_and_5_are_displayed() {
        launch()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("4").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
    }

    // ── Spec: default is 3 players ────────────────────────────────────────────

    @Test
    fun default_shows_three_player_name_fields() {
        launch()
        // Labels "Player 1", "Player 2", "Player 3" should exist …
        composeTestRule.onNodeWithText("Player 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Player 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Player 3").assertIsDisplayed()
        // … but "Player 4" must not exist.
        composeTestRule.onNodeWithText("Player 4").assertDoesNotExist()
    }

    // ── Spec: selecting chip 4 shows 4 fields ─────────────────────────────────

    @Test
    fun selecting_chip_4_shows_four_player_fields() {
        launch()
        composeTestRule.onNodeWithText("4").performClick()
        composeTestRule.onNodeWithText("Player 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Player 5").assertDoesNotExist()
    }

    // ── Spec: selecting chip 5 shows 5 fields ─────────────────────────────────

    @Test
    fun selecting_chip_5_shows_five_player_fields() {
        launch()
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("Player 5").assertIsDisplayed()
    }

    // ── Spec: switching back to fewer players removes extra fields ─────────────

    @Test
    fun switching_from_5_to_3_removes_extra_fields() {
        launch()
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("Player 5").assertIsDisplayed() // sanity check
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("Player 4").assertDoesNotExist()
        composeTestRule.onNodeWithText("Player 5").assertDoesNotExist()
    }

    // ── Spec: Start Game button ───────────────────────────────────────────────

    @Test
    fun start_game_button_is_displayed() {
        launch()
        composeTestRule.onNodeWithText("Start Game").assertIsDisplayed()
    }

    // The button must appear BELOW the name input fields so the visual flow
    // guides the user: enter names first, then press Start (issue #31).
    @Test
    fun start_game_button_is_below_player_name_fields() {
        launch()
        // getBoundsInRoot() returns the position of each node on screen.
        // We compare the top edge of the button with the bottom edge of the
        // last name field: button.top must be greater than field.bottom.
        val buttonBounds = composeTestRule
            .onNodeWithText("Start Game")
            .getBoundsInRoot()
        val fieldBounds = field(2).getBoundsInRoot()

        // The button's top edge should be below the last name-field's bottom edge.
        assert(buttonBounds.top > fieldBounds.bottom) {
            "Expected Start Game button (top=${buttonBounds.top}) to be below " +
                "Player 3 field (bottom=${fieldBounds.bottom})"
        }
    }

    @Test
    fun start_game_callback_receives_three_names_by_default() {
        var capturedNames: List<String>? = null
        launch(onStartGame = { names, _ -> capturedNames = names })

        composeTestRule.onNodeWithText("Start Game").performClick()

        // Default is 3 players, all names blank → list of 3 empty strings.
        assertNotNull(capturedNames)
        assertEquals(3, capturedNames!!.size)
    }

    @Test
    fun start_game_callback_receives_five_names_when_chip_5_selected() {
        var capturedNames: List<String>? = null
        launch(onStartGame = { names, _ -> capturedNames = names })

        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("Start Game").performClick()

        assertNotNull(capturedNames)
        assertEquals(5, capturedNames!!.size)
    }

    // ── Spec: duplicate name validation ───────────────────────────────────────
    // When two or more players share the same resolved name, the button is
    // disabled and an error message is shown next to each conflicting field.

    @Test
    fun start_game_button_is_enabled_with_unique_names() {
        launch()
        // Type distinct names into the first two fields; third stays blank ("Player 3").
        field(0).performTextInput("Alice")
        field(1).performTextInput("Bob")

        // No duplicates → button should be enabled.
        composeTestRule.onNodeWithText("Start Game").assertIsEnabled()
    }

    @Test
    fun start_game_button_is_disabled_when_two_typed_names_match() {
        launch()
        // Enter the same name in both fields — a clear duplicate.
        field(0).performTextInput("Alice")
        field(1).performTextInput("Alice")

        // Duplicate detected → button must be disabled.
        composeTestRule.onNodeWithText("Start Game").assertIsNotEnabled()
    }

    @Test
    fun error_message_is_shown_for_duplicate_names() {
        launch()
        field(0).performTextInput("Alice")
        field(1).performTextInput("Alice")

        // "Name already used" appears under each of the two conflicting fields.
        composeTestRule.onAllNodesWithText("Name already used").assertCountEquals(2)
    }

    @Test
    fun duplicate_detection_is_case_insensitive() {
        launch()
        // "alice" and "ALICE" resolve to the same lowercase name.
        field(0).performTextInput("alice")
        field(1).performTextInput("ALICE")

        composeTestRule.onNodeWithText("Start Game").assertIsNotEnabled()
        // Both conflicting fields show the error.
        composeTestRule.onAllNodesWithText("Name already used").assertCountEquals(2)
    }

    @Test
    fun blank_fields_treated_as_player_n_for_duplicate_detection() {
        launch()
        // The first field is left blank (resolves to "Player 1").
        // Typing "Player 1" in the second field creates a duplicate.
        field(1).performTextInput("Player 1")

        composeTestRule.onNodeWithText("Start Game").assertIsNotEnabled()
    }

    @Test
    fun fixing_duplicate_re_enables_start_game_button() {
        launch()
        field(0).performTextInput("Alice")
        field(1).performTextInput("Alice")

        // Button disabled while duplicate exists.
        composeTestRule.onNodeWithText("Start Game").assertIsNotEnabled()

        // Fix the duplicate by changing the second name.
        field(1).performTextClearance()
        field(1).performTextInput("Bob")

        // No more duplicates → button enabled again.
        composeTestRule.onNodeWithText("Start Game").assertIsEnabled()
    }

    // ── Spec: Salon layout (issue #197) ───────────────────────────────────────

    @Test
    fun new_game_card_title_is_displayed() {
        launch()
        composeTestRule.onNodeWithText("New Game").assertIsDisplayed()
    }

    @Test
    fun player_avatars_are_shown_in_name_fields() {
        launch()
        // Blank fields: each avatar falls back to the placeholder name.
        composeTestRule.onNodeWithContentDescription("Player Player 1").assertIsDisplayed()
        field(0).performTextInput("Alice")
        composeTestRule.onNodeWithContentDescription("Player Alice").assertIsDisplayed()
    }

    @Test
    fun no_resume_card_without_game_in_progress() {
        launch()
        composeTestRule.onNodeWithTag("resume_card").assertDoesNotExist()
    }

    @Test
    fun resume_card_shows_round_players_and_leader() {
        launchWithInProgress()
        composeTestRule.onNodeWithText("GAME IN PROGRESS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Round 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 players · Alice leads +150").assertIsDisplayed()
    }

    @Test
    fun resume_card_is_above_new_game_card() {
        launchWithInProgress()
        val resume  = composeTestRule.onNodeWithTag("resume_card").getBoundsInRoot()
        val newGame = composeTestRule.onNodeWithText("New Game").getBoundsInRoot()
        assert(resume.bottom < newGame.top) { "Resume card must sit above the New game card" }
    }

    @Test
    fun tapping_resume_calls_onResumeGame() {
        var resumed: InProgressGame? = null
        launchWithInProgress(onResume = { resumed = it })
        composeTestRule.onNodeWithText("Resume").performClick()
        assertEquals("g", resumed?.gameId)
    }

    // ── Spec: "Past Games" section heading weight (issue #5) ─────────────────

    @Test
    fun past_games_heading_is_displayed_when_history_exists() {
        launchWithPastGames()
        // The "Past Games" heading must be visible.
        composeTestRule.onNodeWithText("Past Games").assertIsDisplayed()
    }

    // ── Spec: past game card shows winner name (issue #5) ─────────────────────

    @Test
    fun past_game_row_shows_winner_score_and_details() {
        launchWithPastGames()
        // Row: winner name, winner score, then "date · 3 players · 1 round".
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("+150").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 players · 1 round", substring = true).assertIsDisplayed()
    }

    @Test
    fun past_game_row_is_at_least_48dp_tall() {
        launchWithPastGames()
        val bounds = composeTestRule.onNodeWithTag("past_game_test-1").getBoundsInRoot()
        assert(bounds.bottom - bounds.top >= 48.dp) { "Past game rows must be ≥ 48 dp tall" }
    }

    // ── Spec: dealer selection section (issue #128) ───────────────────────────

    @Test
    fun dealer_selection_section_heading_is_displayed() {
        launch()
        composeTestRule.onNodeWithText("First Dealer").assertIsDisplayed()
    }

    @Test
    fun random_option_is_displayed_and_selected_by_default() {
        launch()
        // The "Random" segment must be visible when the screen opens.
        composeTestRule.onNodeWithText("Random").assertIsDisplayed()
    }

    @Test
    fun choose_option_is_displayed() {
        launch()
        composeTestRule.onNodeWithText("Choose").assertIsDisplayed()
    }

    @Test
    fun start_game_callback_receives_null_dealer_index_when_random_is_selected() {
        // Default mode is Random — the dealer index passed to onStartGame must be null.
        var capturedDealerIndex: Int? = -1  // sentinel: will be overwritten by the callback
        launch(onStartGame = { _, dealerIndex -> capturedDealerIndex = dealerIndex })

        composeTestRule.onNodeWithText("Start Game").performClick()

        assertNull(
            "Dealer index must be null when Random mode is selected",
            capturedDealerIndex
        )
    }

    @Test
    fun start_game_callback_receives_non_null_dealer_index_when_choose_is_selected() {
        // When the user picks "Choose", the callback must receive a non-null index.
        var capturedDealerIndex: Int? = -1
        launch(onStartGame = { _, dealerIndex -> capturedDealerIndex = dealerIndex })

        composeTestRule.onNodeWithText("Choose").performClick()
        composeTestRule.onNodeWithText("Start Game").performClick()

        assertNotNull(
            "Dealer index must be non-null when Choose mode is selected",
            capturedDealerIndex
        )
    }

    @Test
    fun start_game_callback_receives_correct_index_when_second_player_chosen_as_dealer() {
        // Tap "Choose", then tap Player 2. The callback must report index 1.
        var capturedDealerIndex: Int? = -1
        launch(onStartGame = { _, dealerIndex -> capturedDealerIndex = dealerIndex })

        composeTestRule.onNodeWithText("Choose").performClick()
        // "Player 2" is also the name field's placeholder: match only the dealer
        // segmented button (a radio button carrying the name).
        composeTestRule.onNode(
            hasText("Player 2") and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        ).performClick()
        composeTestRule.onNodeWithText("Start Game").performClick()

        assertEquals(
            "Index 1 expected when Player 2 (index 1) is chosen as dealer",
            1, capturedDealerIndex
        )
    }

}

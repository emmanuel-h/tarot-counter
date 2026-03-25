package fr.mandarine.tarotcounter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.getBoundsInRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
 */
@RunWith(AndroidJUnit4::class)
class LandingScreenTest {

    // createComposeRule() sets up an isolated Compose environment for each test.
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Launches LandingScreen inside our app theme (same as production).
     * [onThemeChange] captures the AppTheme passed to the callback when a chip is tapped.
     */
    private fun launch(
        onStartGame: (List<String>) -> Unit = {},
        onThemeChange: (AppTheme) -> Unit = {}
    ) {
        composeTestRule.setContent {
            TarotCounterTheme {
                LandingScreen(
                    onStartGame   = onStartGame,
                    onThemeChange = onThemeChange
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
                            rounds = emptyList(),
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

    // ── Spec: language switcher is shown ──────────────────────────────────────

    @Test
    fun language_switcher_shows_both_flags() {
        launch()
        // Both flag emoji chips must be present on the landing screen.
        composeTestRule.onNodeWithText("🇬🇧").assertIsDisplayed()
        composeTestRule.onNodeWithText("🇫🇷").assertIsDisplayed()
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

    // The button must appear ABOVE the name input fields so it stays visible
    // when the on-screen keyboard is open (issue #19).
    @Test
    fun start_game_button_is_above_player_name_fields() {
        launch()
        // getBoundsInRoot() returns the position of each node on screen.
        // We compare the bottom edge of the button with the top edge of the
        // first name field: button.bottom must be less than field.top.
        val buttonBounds = composeTestRule
            .onNodeWithText("Start Game")
            .getBoundsInRoot()
        val fieldBounds = composeTestRule
            .onNodeWithText("Player 1")
            .getBoundsInRoot()

        // The button's bottom edge should be above the first name-field's top edge.
        assert(buttonBounds.bottom < fieldBounds.top) {
            "Expected Start Game button (bottom=${buttonBounds.bottom}) to be above " +
                "Player 1 field (top=${fieldBounds.top})"
        }
    }

    @Test
    fun start_game_callback_receives_three_names_by_default() {
        var capturedNames: List<String>? = null
        launch(onStartGame = { capturedNames = it })

        composeTestRule.onNodeWithText("Start Game").performClick()

        // Default is 3 players, all names blank → list of 3 empty strings.
        assertNotNull(capturedNames)
        assertEquals(3, capturedNames!!.size)
    }

    @Test
    fun start_game_callback_receives_five_names_when_chip_5_selected() {
        var capturedNames: List<String>? = null
        launch(onStartGame = { capturedNames = it })

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
        composeTestRule.onNodeWithText("Player 1").performTextInput("Alice")
        composeTestRule.onNodeWithText("Player 2").performTextInput("Bob")

        // No duplicates → button should be enabled.
        composeTestRule.onNodeWithText("Start Game").assertIsEnabled()
    }

    @Test
    fun start_game_button_is_disabled_when_two_typed_names_match() {
        launch()
        // Enter the same name in both fields — a clear duplicate.
        composeTestRule.onNodeWithText("Player 1").performTextInput("Alice")
        composeTestRule.onNodeWithText("Player 2").performTextInput("Alice")

        // Duplicate detected → button must be disabled.
        composeTestRule.onNodeWithText("Start Game").assertIsNotEnabled()
    }

    @Test
    fun error_message_is_shown_for_duplicate_names() {
        launch()
        composeTestRule.onNodeWithText("Player 1").performTextInput("Alice")
        composeTestRule.onNodeWithText("Player 2").performTextInput("Alice")

        // "Name already used" appears under each conflicting field.
        // There are two duplicate slots, so the matcher finds the first occurrence by default.
        composeTestRule.onNodeWithText("Name already used").assertIsDisplayed()
    }

    @Test
    fun duplicate_detection_is_case_insensitive() {
        launch()
        // "alice" and "ALICE" resolve to the same lowercase name.
        composeTestRule.onNodeWithText("Player 1").performTextInput("alice")
        composeTestRule.onNodeWithText("Player 2").performTextInput("ALICE")

        composeTestRule.onNodeWithText("Start Game").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Name already used").assertIsDisplayed()
    }

    @Test
    fun blank_fields_treated_as_player_n_for_duplicate_detection() {
        launch()
        // The first field is left blank (resolves to "Player 1").
        // Typing "Player 1" in the second field creates a duplicate.
        composeTestRule.onNodeWithText("Player 2").performTextInput("Player 1")

        composeTestRule.onNodeWithText("Start Game").assertIsNotEnabled()
    }

    @Test
    fun fixing_duplicate_re_enables_start_game_button() {
        launch()
        composeTestRule.onNodeWithText("Player 1").performTextInput("Alice")
        composeTestRule.onNodeWithText("Player 2").performTextInput("Alice")

        // Button disabled while duplicate exists.
        composeTestRule.onNodeWithText("Start Game").assertIsNotEnabled()

        // Fix the duplicate by changing the second name.
        composeTestRule.onNodeWithText("Player 2").performTextClearance()
        composeTestRule.onNodeWithText("Player 2").performTextInput("Bob")

        // No more duplicates → button enabled again.
        composeTestRule.onNodeWithText("Start Game").assertIsEnabled()
    }

    // ── Spec: decorative card-suit header (issue #5) ──────────────────────────

    @Test
    fun card_suit_symbols_are_shown_above_title() {
        launch()
        // The four French tarot suit symbols should appear on screen.
        composeTestRule.onNodeWithText("♠  ♥  ♦  ♣").assertIsDisplayed()
    }

    @Test
    fun card_suit_symbols_are_above_app_title() {
        launch()
        // The suit symbols must appear above the title — same spatial check used for
        // the "Start Game" button position test.
        val suitBounds = composeTestRule
            .onNodeWithText("♠  ♥  ♦  ♣")
            .getBoundsInRoot()
        val titleBounds = composeTestRule
            .onNodeWithText("Tarot Counter")
            .getBoundsInRoot()

        assert(suitBounds.bottom <= titleBounds.top) {
            "Expected suit symbols (bottom=${suitBounds.bottom}) to be above " +
                "app title (top=${titleBounds.top})"
        }
    }

    // ── Spec: "Past Games" section heading weight (issue #5) ─────────────────

    @Test
    fun past_games_heading_is_displayed_when_history_exists() {
        launchWithPastGames()
        // The "Past Games" heading must be visible.
        composeTestRule.onNodeWithText("Past Games").assertIsDisplayed()
    }

    // ── Spec: theme toggle chips are shown ────────────────────────────────────

    @Test
    fun theme_toggle_shows_both_sun_and_moon_chips() {
        launch()
        // Both emoji chips must be present in the header row.
        composeTestRule.onNodeWithText("☀️").assertIsDisplayed()
        composeTestRule.onNodeWithText("🌙").assertIsDisplayed()
    }

    @Test
    fun tapping_moon_chip_calls_onThemeChange_with_DARK() {
        var capturedTheme: AppTheme? = null
        launch(onThemeChange = { capturedTheme = it })

        composeTestRule.onNodeWithText("🌙").performClick()

        assertEquals(AppTheme.DARK, capturedTheme)
    }

    @Test
    fun tapping_sun_chip_calls_onThemeChange_with_LIGHT() {
        var capturedTheme: AppTheme? = null
        launch(onThemeChange = { capturedTheme = it })

        composeTestRule.onNodeWithText("☀️").performClick()

        assertEquals(AppTheme.LIGHT, capturedTheme)
    }

    // ── Spec: past game card shows winner name (issue #5) ─────────────────────

    @Test
    fun past_game_card_shows_winner_line() {
        launchWithPastGames()
        // The winner result text should appear (e.g. "Alice +150") inside the card.
        // We use a substring check via containsText to stay locale-agnostic.
        composeTestRule.onNodeWithText("Alice", substring = true).assertIsDisplayed()
    }
}

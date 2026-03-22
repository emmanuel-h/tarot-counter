package fr.mandarine.tarotcounter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    /** Launches LandingScreen inside our app theme (same as production). */
    private fun launch(onStartGame: (List<String>) -> Unit = {}) {
        composeTestRule.setContent {
            TarotCounterTheme {
                LandingScreen(onStartGame = onStartGame)
            }
        }
    }

    // ── Spec: title is shown ──────────────────────────────────────────────────

    @Test
    fun screen_shows_app_title() {
        launch()
        composeTestRule.onNodeWithText("Compteur de points").assertIsDisplayed()
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
}

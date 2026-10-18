package fr.mandarine.tarotcounter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for StatisticsScreen (issue #229, docs/statistics.md).
 *
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class StatisticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val names = listOf("Alice", "Bob", "Chloé")

    // Alice takes a Garde and makes it: she wins the only game.
    private val oneGame = listOf(
        SavedGame(
            id = "1", datestamp = 0L, playerNames = names,
            rounds = listOf(
                RoundResult(1, "Alice", Contract.GARDE, null, true,
                    mapOf("Alice" to 120, "Bob" to -60, "Chloé" to -60))
            ),
            finalScores = mapOf("Alice" to 120, "Bob" to -60, "Chloé" to -60)
        )
    )

    private fun launch(games: List<SavedGame>, onBack: () -> Unit = {}) {
        composeTestRule.setContent {
            TarotCounterTheme { StatisticsScreen(games = games, onBack = onBack) }
        }
    }

    @Test
    fun empty_state_is_shown_without_games() {
        launch(emptyList())
        composeTestRule.onNodeWithTag("stats_empty").assertIsDisplayed()
    }

    @Test
    fun one_card_per_player_with_key_figures() {
        launch(oneGame)
        composeTestRule.onNodeWithText("Based on 1 saved game").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stats_alice").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stats_bob").assertExists()
        // Alice made her only Garde.
        composeTestRule
            .onNode(hasText("1/1 made") and hasAnyAncestor(hasTestTag("stats_alice")), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun back_arrow_calls_onBack() {
        var back = false
        launch(oneGame, onBack = { back = true })
        composeTestRule.onNodeWithContentDescription("Main Menu").performClick()
        assertTrue(back)
    }
}

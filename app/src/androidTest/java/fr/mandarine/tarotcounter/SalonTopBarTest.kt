package fr.mandarine.tarotcounter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [SalonTopBar] (UiComponents.kt), which replaced ScreenHeader in issue #196.
 *
 * Spec:
 *   - The title is visible.
 *   - With `onBack`, a back arrow labelled with the localized "back to game" text is shown,
 *     and tapping it calls `onBack` exactly once. Without `onBack`, there is no arrow.
 *   - Action icons are shown with their content descriptions and fire their callbacks.
 *   - Every button has a 48 dp touch target.
 *
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SalonTopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launch(
        title: String = "History",
        locale: AppLocale = AppLocale.EN,
        onBack: (() -> Unit)? = {},
        actions: List<TopBarAction> = emptyList()
    ) {
        composeTestRule.setContent {
            TarotCounterTheme {
                CompositionLocalProvider(LocalAppLocale provides locale) {
                    SalonTopBar(title = title, onBack = onBack, actions = actions)
                }
            }
        }
    }

    @Test
    fun title_is_displayed() {
        launch(title = "Score History")
        composeTestRule.onNodeWithText("Score History").assertIsDisplayed()
    }

    @Test
    fun back_arrow_is_displayed_in_english() {
        launch(locale = AppLocale.EN)
        composeTestRule
            .onNodeWithContentDescription(appStrings(AppLocale.EN).backToGame)
            .assertIsDisplayed()
    }

    @Test
    fun back_arrow_is_displayed_in_french() {
        launch(locale = AppLocale.FR)
        composeTestRule
            .onNodeWithContentDescription(appStrings(AppLocale.FR).backToGame)
            .assertIsDisplayed()
    }

    @Test
    fun no_back_arrow_without_onBack() {
        launch(onBack = null)
        composeTestRule
            .onNodeWithContentDescription(appStrings(AppLocale.EN).backToGame)
            .assertDoesNotExist()
    }

    @Test
    fun tapping_back_arrow_calls_onBack_exactly_once() {
        var backCount = 0
        launch(onBack = { backCount++ })
        composeTestRule
            .onNodeWithContentDescription(appStrings(AppLocale.EN).backToGame)
            .performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun back_arrow_has_48dp_touch_target() {
        launch()
        composeTestRule
            .onNodeWithContentDescription(appStrings(AppLocale.EN).backToGame)
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun actions_are_displayed_and_clickable() {
        var settingsClicks = 0
        var historyClicks = 0
        launch(
            actions = listOf(
                TopBarAction(Icons.Default.Settings, "History") { historyClicks++ },
                TopBarAction(Icons.Default.Settings, "Settings") { settingsClicks++ }
            ),
            title = "Round 5"
        )
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .performClick()
        composeTestRule.onNodeWithContentDescription("History").performClick()
        assertEquals(1, settingsClicks)
        assertEquals(1, historyClicks)
    }

    @Test
    fun en_and_fr_back_descriptions_differ() {
        assertNotEquals(appStrings(AppLocale.EN).backToGame, appStrings(AppLocale.FR).backToGame)
    }
}

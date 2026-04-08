package fr.mandarine.tarotcounter

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for ScreenHeader.kt.
 *
 * ScreenHeader is used by FinalScoreScreen and ScoreHistoryScreen to render a
 * consistent navigation row: back-arrow icon on the left, screen title to its right.
 *
 * Spec:
 *   - The title string is visible.
 *   - A back-arrow button with the localized "back to game" content description is visible.
 *   - Tapping the back arrow invokes the onBack callback exactly once.
 *   - Both EN and FR locale variants work correctly.
 *
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ScreenHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Renders [ScreenHeader] inside [TarotCounterTheme] with the given [locale],
     * providing the [LocalAppLocale] composition local so [appStrings] resolves
     * the correct bundle — the same pattern used in every other composable.
     */
    private fun launch(
        title: String,
        locale: AppLocale = AppLocale.EN,
        onBack: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TarotCounterTheme {
                CompositionLocalProvider(LocalAppLocale provides locale) {
                    ScreenHeader(title = title, onBack = onBack)
                }
            }
        }
    }

    // ── Title display ─────────────────────────────────────────────────────────

    @Test
    fun title_is_displayed() {
        launch(title = "Score History")
        composeTestRule.onNodeWithText("Score History").assertIsDisplayed()
    }

    @Test
    fun title_can_be_any_string() {
        launch(title = "Final Score")
        composeTestRule.onNodeWithText("Final Score").assertIsDisplayed()
    }

    // ── Back arrow ────────────────────────────────────────────────────────────

    @Test
    fun back_arrow_is_displayed_in_english() {
        // The EN back-arrow content description is "Back to game" (from EnStrings).
        val strings = appStrings(AppLocale.EN)
        launch(title = "History", locale = AppLocale.EN)
        composeTestRule
            .onNodeWithContentDescription(strings.backToGame)
            .assertIsDisplayed()
    }

    @Test
    fun back_arrow_is_displayed_in_french() {
        // The FR content description differs from EN — both must resolve correctly.
        val strings = appStrings(AppLocale.FR)
        launch(title = "Historique", locale = AppLocale.FR)
        composeTestRule
            .onNodeWithContentDescription(strings.backToGame)
            .assertIsDisplayed()
    }

    // ── Back callback ─────────────────────────────────────────────────────────

    @Test
    fun tapping_back_arrow_calls_onBack() {
        var backCalled = false
        val strings = appStrings(AppLocale.EN)
        launch(title = "History", locale = AppLocale.EN, onBack = { backCalled = true })

        composeTestRule
            .onNodeWithContentDescription(strings.backToGame)
            .performClick()

        assertTrue("onBack should be called when the arrow is tapped", backCalled)
    }

    @Test
    fun onBack_is_called_exactly_once_per_tap() {
        var backCallCount = 0
        val strings = appStrings(AppLocale.EN)
        launch(title = "History", locale = AppLocale.EN, onBack = { backCallCount++ })

        composeTestRule
            .onNodeWithContentDescription(strings.backToGame)
            .performClick()

        assert(backCallCount == 1) { "Expected exactly 1 onBack call, got $backCallCount" }
    }

    // ── Locale correctness ────────────────────────────────────────────────────

    @Test
    fun en_and_fr_content_descriptions_are_different() {
        // If they were the same this test would be redundant with other tests —
        // catching the divergence early prevents silent i18n regressions.
        val enDesc = appStrings(AppLocale.EN).backToGame
        val frDesc = appStrings(AppLocale.FR).backToGame
        assert(enDesc != frDesc) {
            "EN and FR back-arrow content descriptions should differ (both are '$enDesc')"
        }
    }
}

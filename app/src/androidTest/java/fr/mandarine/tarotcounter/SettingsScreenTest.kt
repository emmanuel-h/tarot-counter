package fr.mandarine.tarotcounter

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for SettingsScreen.
 *
 * These run on a device or emulator via AndroidJUnit4.
 * Run with: ./gradlew connectedAndroidTest
 *
 * Spec (docs/settings.md):
 *   - Shows a back arrow that calls onBack when tapped.
 *   - Shows the screen title "Settings".
 *   - Shows theme toggle (☀️ / 🌙) and calls onThemeChange when tapped.
 *   - Shows language toggle (🇬🇧 / 🇫🇷) and calls onLocaleChange when tapped.
 *   - Shows the developer feedback button.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Launches SettingsScreen wrapped in the app theme and optionally a custom locale/theme.
     * The [CompositionLocalProvider] mirrors what MainActivity does so the screen
     * reads the current locale and theme the same way as in production.
     */
    private fun launch(
        locale: AppLocale = AppLocale.EN,
        theme: AppTheme = AppTheme.LIGHT,
        onThemeChange: (AppTheme) -> Unit = {},
        onLocaleChange: (AppLocale) -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            TarotCounterTheme(darkTheme = theme == AppTheme.DARK) {
                // Provide locale and theme so SettingsScreen can read them via
                // LocalAppLocale.current and LocalAppTheme.current.
                CompositionLocalProvider(
                    LocalAppLocale provides locale,
                    LocalAppTheme  provides theme
                ) {
                    SettingsScreen(
                        onThemeChange  = onThemeChange,
                        onLocaleChange = onLocaleChange,
                        onBack         = onBack
                    )
                }
            }
        }
    }

    // ── Spec: screen title ────────────────────────────────────────────────────

    @Test
    fun settings_title_is_displayed() {
        launch()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun settings_title_is_displayed_in_french() {
        launch(locale = AppLocale.FR)
        composeTestRule.onNodeWithText("Paramètres").assertIsDisplayed()
    }

    // ── Spec: back navigation ─────────────────────────────────────────────────

    @Test
    fun tapping_back_arrow_calls_onBack() {
        var called = false
        launch(onBack = { called = true })

        // ScreenHeader renders a back arrow with contentDescription = strings.backToGame.
        composeTestRule.onNodeWithContentDescription("Back to game").performClick()

        assert(called) { "Expected onBack to be called when the back arrow is tapped" }
    }

    // ── Spec: theme toggle ────────────────────────────────────────────────────

    @Test
    fun theme_toggle_shows_both_sun_and_moon() {
        launch()
        composeTestRule.onNodeWithText("☀️").assertIsDisplayed()
        composeTestRule.onNodeWithText("🌙").assertIsDisplayed()
    }

    @Test
    fun tapping_moon_calls_onThemeChange_with_DARK() {
        var captured: AppTheme? = null
        launch(onThemeChange = { captured = it })

        composeTestRule.onNodeWithText("🌙").performClick()

        assertEquals(AppTheme.DARK, captured)
    }

    @Test
    fun tapping_sun_calls_onThemeChange_with_LIGHT() {
        var captured: AppTheme? = null
        launch(onThemeChange = { captured = it })

        composeTestRule.onNodeWithText("☀️").performClick()

        assertEquals(AppTheme.LIGHT, captured)
    }

    // ── Spec: language toggle ─────────────────────────────────────────────────

    @Test
    fun language_toggle_shows_both_flags() {
        launch()
        composeTestRule.onNodeWithText("🇬🇧").assertIsDisplayed()
        composeTestRule.onNodeWithText("🇫🇷").assertIsDisplayed()
    }

    @Test
    fun tapping_french_flag_calls_onLocaleChange_with_FR() {
        var captured: AppLocale? = null
        launch(onLocaleChange = { captured = it })

        composeTestRule.onNodeWithText("🇫🇷").performClick()

        assertEquals(AppLocale.FR, captured)
    }

    @Test
    fun tapping_english_flag_calls_onLocaleChange_with_EN() {
        var captured: AppLocale? = null
        launch(onLocaleChange = { captured = it })

        composeTestRule.onNodeWithText("🇬🇧").performClick()

        assertEquals(AppLocale.EN, captured)
    }

    // ── Spec: feedback button ─────────────────────────────────────────────────

    @Test
    fun feedback_button_is_displayed() {
        launch()
        composeTestRule.onNodeWithText("Send Feedback").assertIsDisplayed()
    }

    @Test
    fun feedback_button_is_displayed_in_french() {
        launch(locale = AppLocale.FR)
        composeTestRule.onNodeWithText("Contacter le développeur").assertIsDisplayed()
    }

    // ── Spec: section labels ──────────────────────────────────────────────────

    @Test
    fun theme_section_label_is_displayed() {
        launch()
        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
    }

    @Test
    fun language_section_label_is_displayed() {
        launch()
        composeTestRule.onNodeWithText("Language").assertIsDisplayed()
    }
}

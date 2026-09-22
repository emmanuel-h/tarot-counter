package fr.mandarine.tarotcounter

import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
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

        // SalonTopBar renders a back arrow with contentDescription = strings.backToGame.
        composeTestRule.onNodeWithContentDescription("Back to game").performClick()

        assert(called) { "Expected onBack to be called when the back arrow is tapped" }
    }

    // ── Spec: theme toggle ────────────────────────────────────────────────────

    @Test
    fun theme_toggle_shows_both_sun_and_moon() {
        launch()
        composeTestRule.onNodeWithText("Light").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun tapping_moon_calls_onThemeChange_with_DARK() {
        var captured: AppTheme? = null
        launch(onThemeChange = { captured = it })

        composeTestRule.onNodeWithText("Dark").performClick()

        assertEquals(AppTheme.DARK, captured)
    }

    @Test
    fun tapping_sun_calls_onThemeChange_with_LIGHT() {
        var captured: AppTheme? = null
        launch(onThemeChange = { captured = it })

        composeTestRule.onNodeWithText("Light").performClick()

        assertEquals(AppTheme.LIGHT, captured)
    }

    // ── Spec: language toggle ─────────────────────────────────────────────────

    @Test
    fun language_toggle_shows_both_flags() {
        launch()
        composeTestRule.onNodeWithText("English").assertIsDisplayed()
        composeTestRule.onNodeWithText("Français").assertIsDisplayed()
    }

    @Test
    fun tapping_french_flag_calls_onLocaleChange_with_FR() {
        var captured: AppLocale? = null
        launch(onLocaleChange = { captured = it })

        composeTestRule.onNodeWithText("Français").performClick()

        assertEquals(AppLocale.FR, captured)
    }

    @Test
    fun tapping_english_flag_calls_onLocaleChange_with_EN() {
        var captured: AppLocale? = null
        launch(onLocaleChange = { captured = it })

        composeTestRule.onNodeWithText("English").performClick()

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
        // The language group heading is shown in upper case (Salon settings, #203).
        composeTestRule.onNodeWithText("LANGUAGE").assertIsDisplayed()
    }

    // ── Spec: rules button ────────────────────────────────────────────────────

    @Test
    fun rules_button_is_displayed_in_english() {
        launch()
        composeTestRule.onNodeWithText("Rules").assertIsDisplayed()
    }

    @Test
    fun rules_button_is_displayed_in_french() {
        launch(locale = AppLocale.FR)
        composeTestRule.onNodeWithText("Règles").assertIsDisplayed()
    }

    // ── Spec: rules dialog ────────────────────────────────────────────────────

    @Test
    fun tapping_rules_button_opens_rules_page_with_title() {
        launch()

        composeTestRule.onNodeWithText("Rules").performClick()

        // The dialog title should now be visible.
        composeTestRule.onNodeWithText("Game Rules").assertIsDisplayed()
    }

    @Test
    fun rules_page_shows_all_section_headings() {
        launch()
        composeTestRule.onNodeWithText("Rules").performClick()

        // The dialog scrolls: bring each heading into view before checking it.
        composeTestRule.onNodeWithText("Objective").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Contracts").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Score Formula").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Score Distribution").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Bonuses").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun rules_page_shows_section_headings_in_french() {
        launch(locale = AppLocale.FR)
        composeTestRule.onNodeWithText("Règles").performClick()

        composeTestRule.onNodeWithText("Objectif").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Contrats").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Calcul du score").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Répartition des scores").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Bonus").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tapping_close_closes_rules_page() {
        launch()

        // Open the dialog.
        composeTestRule.onNodeWithText("Rules").performClick()
        composeTestRule.onNodeWithText("Game Rules").assertIsDisplayed()

        // The rules page's back arrow is labelled "Close".
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        // The dialog title should no longer be visible.
        composeTestRule.onNodeWithText("Game Rules").assertDoesNotExist()
    }

    // ── Salon settings + rules (issue #203) ───────────────────────────────────

    @Test
    fun groups_are_shown_with_upper_case_headings() {
        launch()
        listOf("APPEARANCE", "LANGUAGE", "HELP", "ABOUT").forEach {
            composeTestRule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun about_shows_the_version_name() {
        composeTestRule.setContent {
            TarotCounterTheme { SettingsScreen(versionName = "9.9.9") }
        }
        composeTestRule.onNodeWithText("Version").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("version_name").assert(hasText("9.9.9"))
    }

    @Test
    fun rules_page_shows_the_bouts_and_contracts_tables() {
        launch()
        composeTestRule.onNodeWithText("Rules").performClick()
        composeTestRule.onNodeWithTag("rules_bouts_table").performScrollTo()
        composeTestRule.onNodeWithText("Points needed").assertIsDisplayed()
        composeTestRule.onNodeWithText("56").assertExists()
        composeTestRule.onNodeWithText("36").assertExists()
        composeTestRule.onNodeWithTag("rules_contracts_table").performScrollTo()
        composeTestRule.onNodeWithText("×6").assertExists()
    }

    @Test
    fun system_back_closes_the_rules_page() {
        var left = false
        launch(onBack = { left = true })
        composeTestRule.onNodeWithText("Rules").performClick()
        composeTestRule.onNodeWithTag("rules_screen").assertExists()
        androidx.test.espresso.Espresso.pressBack()
        composeTestRule.onNodeWithTag("rules_screen").assertDoesNotExist()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        assert(!left) { "Back from the rules page must stay in Settings" }
    }
}


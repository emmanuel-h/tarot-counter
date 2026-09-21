package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.tarotcounter.ui.theme.CormorantGaramond
import fr.mandarine.tarotcounter.ui.theme.DarkColorScheme
import fr.mandarine.tarotcounter.ui.theme.DarkTarotColors
import fr.mandarine.tarotcounter.ui.theme.Figtree
import fr.mandarine.tarotcounter.ui.theme.LightColorScheme
import fr.mandarine.tarotcounter.ui.theme.LightTarotColors
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import fr.mandarine.tarotcounter.ui.theme.tarotColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the Salon theme wiring (issue #195).
 *
 * The palette values themselves are checked by SalonPaletteTest (JVM). Here we
 * check what only a running composition can tell: that [TarotCounterTheme]
 * hands the right tokens to composables, and that [scoreColor] reads them.
 *
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SalonThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── scoreColor ────────────────────────────────────────────────────────────

    /** Renders scoreColor(+10), scoreColor(0) and scoreColor(-10) in the given theme. */
    private fun captureScoreColors(dark: Boolean): Triple<Color, Color, Color> {
        var colors = Triple(Color.Unspecified, Color.Unspecified, Color.Unspecified)
        composeTestRule.setContent {
            TarotCounterTheme(darkTheme = dark) {
                colors = Triple(scoreColor(10), scoreColor(0), scoreColor(-10))
            }
        }
        composeTestRule.waitForIdle()
        return colors
    }

    @Test
    fun scoreColor_light_uses_salon_positive_and_negative() {
        val (plus, zero, minus) = captureScoreColors(dark = false)
        assertEquals(LightTarotColors.positive, plus)
        // Zero counts as "not losing": it uses the positive colour.
        assertEquals(LightTarotColors.positive, zero)
        assertEquals(LightTarotColors.negative, minus)
    }

    @Test
    fun scoreColor_dark_uses_night_positive_and_negative() {
        val (plus, zero, minus) = captureScoreColors(dark = true)
        assertEquals(DarkTarotColors.positive, plus)
        assertEquals(DarkTarotColors.positive, zero)
        assertEquals(DarkTarotColors.negative, minus)
    }

    // ── Theme wiring ──────────────────────────────────────────────────────────

    @Test
    fun theme_provides_light_tokens_by_default() {
        var background = Color.Unspecified
        var brass = Color.Unspecified
        composeTestRule.setContent {
            TarotCounterTheme {
                background = MaterialTheme.colorScheme.background
                brass = MaterialTheme.tarotColors.brass
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(LightColorScheme.background, background)
        assertEquals(LightTarotColors.brass, brass)
    }

    @Test
    fun theme_provides_dark_tokens_when_dark() {
        var background = Color.Unspecified
        var brass = Color.Unspecified
        composeTestRule.setContent {
            TarotCounterTheme(darkTheme = true) {
                background = MaterialTheme.colorScheme.background
                brass = MaterialTheme.tarotColors.brass
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(DarkColorScheme.background, background)
        assertEquals(DarkTarotColors.brass, brass)
    }

    @Test
    fun typography_uses_cormorant_for_titles_and_figtree_for_ui_text() {
        var headlineFamily: Any? = null
        var bodyFamily: Any? = null
        var bodyFeatures: String? = null
        composeTestRule.setContent {
            TarotCounterTheme {
                headlineFamily = MaterialTheme.typography.headlineMedium.fontFamily
                bodyFamily = MaterialTheme.typography.bodyLarge.fontFamily
                bodyFeatures = MaterialTheme.typography.bodyLarge.fontFeatureSettings
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(CormorantGaramond, headlineFamily)
        assertEquals(Figtree, bodyFamily)
        // Tabular figures so score columns line up.
        assertEquals("tnum", bodyFeatures)
    }

    @Test
    fun shapes_use_16dp_cards() {
        var medium: Any? = null
        composeTestRule.setContent {
            TarotCounterTheme { medium = MaterialTheme.shapes.medium }
        }
        composeTestRule.waitForIdle()
        assertEquals(RoundedCornerShape(16.dp), medium)
    }

    @Test
    fun bundled_fonts_render_text() {
        // Smoke test: the TTF resources load and text in both families displays.
        composeTestRule.setContent {
            TarotCounterTheme {
                Column {
                    Text(
                        "Round 5", style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "+312", style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Round 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("+312").assertIsDisplayed()
    }
}

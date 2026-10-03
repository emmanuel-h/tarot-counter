package fr.mandarine.tarotcounter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the shared building blocks in UiComponents.kt.
 *
 * Covers:
 *   - AppButton / AppOutlinedButton / AppTextButton — label display and click behavior
 *   - ScoreTableRow — cell content rendered for header and data rows
 *   - Salon components — card, avatars, divider, header, score, top bar, text fields
 *
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class UiComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─────────────────────────────────────────────────────────────────────────
    // AppButton
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun appButton_shows_label() {
        composeTestRule.setContent {
            TarotCounterTheme { AppButton(text = "Confirm", onClick = {}) }
        }
        composeTestRule.onNodeWithText("Confirm").assertIsDisplayed()
    }

    @Test
    fun appButton_click_fires_callback() {
        var clicked = false
        composeTestRule.setContent {
            TarotCounterTheme { AppButton(text = "Go", onClick = { clicked = true }) }
        }
        composeTestRule.onNodeWithText("Go").performClick()
        assertTrue("AppButton click should fire onClick", clicked)
    }

    @Test
    fun appButton_disabled_does_not_fire_callback() {
        var clicked = false
        composeTestRule.setContent {
            TarotCounterTheme {
                AppButton(text = "Disabled", onClick = { clicked = true }, enabled = false)
            }
        }
        // The button is in the tree but should not react to taps.
        composeTestRule.onNodeWithText("Disabled").assertIsNotEnabled()
        assertFalse("Disabled AppButton should not fire onClick", clicked)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AppOutlinedButton
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun appOutlinedButton_shows_label() {
        composeTestRule.setContent {
            TarotCounterTheme { AppOutlinedButton(text = "Cancel", onClick = {}) }
        }
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun appOutlinedButton_click_fires_callback() {
        var clicked = false
        composeTestRule.setContent {
            TarotCounterTheme { AppOutlinedButton(text = "Back", onClick = { clicked = true }) }
        }
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue("AppOutlinedButton click should fire onClick", clicked)
    }

    @Test
    fun appOutlinedButton_disabled_is_not_enabled() {
        composeTestRule.setContent {
            TarotCounterTheme {
                AppOutlinedButton(text = "Locked", onClick = {}, enabled = false)
            }
        }
        composeTestRule.onNodeWithText("Locked").assertIsNotEnabled()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AppTextButton
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun appTextButton_shows_label() {
        composeTestRule.setContent {
            TarotCounterTheme { AppTextButton(text = "Skip", onClick = {}) }
        }
        composeTestRule.onNodeWithText("Skip").assertIsDisplayed()
    }

    @Test
    fun appTextButton_click_fires_callback() {
        var clicked = false
        composeTestRule.setContent {
            TarotCounterTheme { AppTextButton(text = "Skip", onClick = { clicked = true }) }
        }
        composeTestRule.onNodeWithText("Skip").performClick()
        assertTrue("AppTextButton click should fire onClick", clicked)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ScoreTableRow
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun scoreTableRow_shows_all_cell_texts() {
        composeTestRule.setContent {
            TarotCounterTheme {
                ScoreTableRow(
                    cells     = listOf("1", "Alice", "Bob"),
                    isHeader  = false
                )
            }
        }
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun scoreTableRow_header_shows_all_column_titles() {
        composeTestRule.setContent {
            TarotCounterTheme {
                ScoreTableRow(
                    cells    = listOf("Round", "Alice", "Bob"),
                    isHeader = true
                )
            }
        }
        composeTestRule.onNodeWithText("Round").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun scoreTableRow_renders_score_values_as_text() {
        composeTestRule.setContent {
            TarotCounterTheme {
                ScoreTableRow(
                    cells       = listOf("2", "+84", "-42"),
                    isHeader    = false,
                    scoreValues = listOf(null, 84, -42)
                )
            }
        }
        composeTestRule.onNodeWithText("+84").assertIsDisplayed()
        composeTestRule.onNodeWithText("-42").assertIsDisplayed()
    }

    @Test
    fun scoreTableRow_with_winner_column_shows_winner_text() {
        // The winner column should still show the score text even when highlighted.
        composeTestRule.setContent {
            TarotCounterTheme {
                ScoreTableRow(
                    cells               = listOf("3", "+120", "+60"),
                    isHeader            = false,
                    scoreValues         = listOf(null, 120, 60),
                    winnerColumnIndices = setOf(1)          // Alice's column
                )
            }
        }
        composeTestRule.onNodeWithText("+120").assertIsDisplayed()
        composeTestRule.onNodeWithText("+60").assertIsDisplayed()
    }

    // ── Responsive table (issue #129) ─────────────────────────────────────────

    @Test
    fun scoreTableRow_five_players_all_headers_visible_without_scroll() {
        // Verifies that a 5-player header row (the worst case for width overflow)
        // renders all cells as displayed nodes — i.e. no horizontal scrolling is
        // required to see any column.
        val players = listOf("Alice", "Bob", "Charlie", "Diana", "Eve")
        composeTestRule.setContent {
            TarotCounterTheme {
                // fillMaxWidth() gives the Row the full screen width, which is
                // the same constraint it gets inside the real ScoreHistoryScreen.
                ScoreTableRow(
                    cells    = listOf("Round") + players,
                    isHeader = true
                )
            }
        }
        // Every header label must be reachable without scrolling.
        composeTestRule.onNodeWithText("Round").assertIsDisplayed()
        players.forEach { name ->
            composeTestRule.onNodeWithText(name).assertIsDisplayed()
        }
    }

    @Test
    fun scoreTableRow_five_players_data_row_all_scores_visible() {
        // Data row equivalent of the header test — scores for all 5 players must
        // be simultaneously visible so the user can compare results at a glance.
        composeTestRule.setContent {
            TarotCounterTheme {
                ScoreTableRow(
                    cells       = listOf("1", "+100", "-25", "+50", "-75", "-50"),
                    isHeader    = false,
                    scoreValues = listOf(null, 100, -25, 50, -75, -50)
                )
            }
        }
        composeTestRule.onNodeWithText("+100").assertIsDisplayed()
        composeTestRule.onNodeWithText("-25").assertIsDisplayed()
        composeTestRule.onNodeWithText("+50").assertIsDisplayed()
        composeTestRule.onNodeWithText("-75").assertIsDisplayed()
        composeTestRule.onNodeWithText("-50").assertIsDisplayed()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Salon components (issue #196)
    // ─────────────────────────────────────────────────────────────────────────

    // Renders content inside the theme and a locale, like every real screen.
    private fun setSalonContent(
        locale: AppLocale = AppLocale.EN,
        content: @androidx.compose.runtime.Composable () -> Unit
    ) {
        composeTestRule.setContent {
            TarotCounterTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAppLocale provides locale
                ) { content() }
            }
        }
    }

    @Test
    fun salonCard_shows_title_and_content() {
        setSalonContent {
            SalonCard(title = "New game") { androidx.compose.material3.Text("Body") }
        }
        composeTestRule.onNodeWithText("New game").assertIsDisplayed()
        composeTestRule.onNodeWithText("Body").assertIsDisplayed()
    }

    @Test
    fun playerAvatar_has_localized_description_in_english() {
        setSalonContent { PlayerAvatar(name = "alice", seatIndex = 0) }
        composeTestRule.onNodeWithContentDescription("Player alice").assertIsDisplayed()
    }

    @Test
    fun playerAvatar_has_localized_description_in_french() {
        setSalonContent(AppLocale.FR) { PlayerAvatar(name = "Chloé", seatIndex = 2) }
        composeTestRule.onNodeWithContentDescription("Joueur Chloé").assertIsDisplayed()
    }

    @Test
    fun playerAvatar_sizes_match_the_design() {
        setSalonContent {
            androidx.compose.foundation.layout.Column {
                PlayerAvatar(name = "S", seatIndex = 0, size = AvatarSize.S)
                PlayerAvatar(name = "M", seatIndex = 1, size = AvatarSize.M)
                PlayerAvatar(name = "L", seatIndex = 2, size = AvatarSize.L)
            }
        }
        composeTestRule.onNodeWithContentDescription("Player S").assertWidthIsEqualTo(24.dp)
        composeTestRule.onNodeWithContentDescription("Player M").assertWidthIsEqualTo(36.dp)
        composeTestRule.onNodeWithContentDescription("Player L").assertWidthIsEqualTo(56.dp)
    }

    @Test
    fun playerAvatar_initial_text_is_merged_into_description() {
        // The raw initial "A" must not appear as a separate node for screen readers.
        setSalonContent { PlayerAvatar(name = "Alice", seatIndex = 0) }
        composeTestRule.onNodeWithText("A").assertDoesNotExist()
    }

    @Test
    fun avatarStack_shows_every_player() {
        val names = listOf("Alice", "Bruno", "Chloé", "David", "Émile")
        setSalonContent { AvatarStack(names = names) }
        for (name in names) {
            composeTestRule.onNodeWithContentDescription("Player $name").assertIsDisplayed()
        }
    }

    @Test
    fun sectionHeader_shows_title_and_trailing_action() {
        var clicked = false
        setSalonContent {
            SectionHeader(title = "Last rounds") {
                AppTextButton(text = "See all", onClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithText("Last rounds").assertIsDisplayed()
        composeTestRule.onNodeWithText("See all").performClick()
        assertTrue(clicked)
    }

    @Test
    fun scoreText_shows_signed_values() {
        setSalonContent {
            androidx.compose.foundation.layout.Column {
                ScoreText(score = 312, size = ScoreSize.XL)
                ScoreText(score = -48)
                ScoreText(score = 0, size = ScoreSize.S)
            }
        }
        composeTestRule.onNodeWithText("+312").assertIsDisplayed()
        composeTestRule.onNodeWithText("-48").assertIsDisplayed()
        composeTestRule.onNodeWithText("+0").assertIsDisplayed()
    }

    @Test
    fun appButton_is_56dp_tall() {
        setSalonContent { AppButton(text = "Start game", onClick = {}) }
        composeTestRule.onNodeWithText("Start game", useUnmergedTree = false)
            .assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun appOutlinedButton_is_at_least_48dp_tall() {
        setSalonContent { AppOutlinedButton(text = "Skip round", onClick = {}) }
        composeTestRule.onNodeWithText("Skip round").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun playerNameField_accepts_input_and_shows_avatar() {
        var value by androidx.compose.runtime.mutableStateOf("")
        setSalonContent {
            PlayerNameField(
                value         = value,
                onValueChange = { value = it },
                seatIndex     = 0,
                placeholder   = "Player 1"
            )
        }
        // Empty field: the avatar falls back to the placeholder name.
        composeTestRule.onNodeWithContentDescription("Player Player 1").assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Alice")
        assertEquals("Alice", value)
        composeTestRule.onNodeWithContentDescription("Player Alice").assertIsDisplayed()
    }

    @Test
    fun playerNameField_shows_supporting_text_on_error() {
        setSalonContent {
            PlayerNameField(
                value = "Alice", onValueChange = {}, seatIndex = 1, placeholder = "Player 2",
                isError = true, supportingText = "Name already used"
            )
        }
        composeTestRule.onNodeWithText("Name already used").assertIsDisplayed()
    }

    @Test
    fun feltCard_shows_content() {
        setSalonContent { FeltCard { androidx.compose.material3.Text("Round 5") } }
        composeTestRule.onNodeWithText("Round 5").assertIsDisplayed()
    }

    @Test
    fun playerAvatar_label_replaces_initial_but_keeps_description() {
        setSalonContent {
            PlayerAvatar(name = "Player 3", seatIndex = 2, label = "3")
        }
        // The label is drawn but merged away for screen readers, like the initial.
        composeTestRule.onNodeWithContentDescription("Player Player 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("P").assertDoesNotExist()
    }
}

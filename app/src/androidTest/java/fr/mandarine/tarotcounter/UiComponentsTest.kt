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
 *   - CompactBonusGrid — player header, bonus labels, and checkbox toggle logic
 *   - PlayerChipSelector — "None" chip, player chips, selection and deselection
 *   - ScoreTableRow — cell content rendered for header and data rows
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
    // CompactBonusGrid
    // ─────────────────────────────────────────────────────────────────────────

    private val bonusPlayers = listOf("Alice", "Bob")
    private val bonusLabels  = listOf("Petit au bout", "Poignée", "Double poignée", "Triple poignée")
    private val bonusTips    = listOf("tip1", "tip2", "tip3", "tip4")

    /** Launches CompactBonusGrid with mutable state that tests can inspect. */
    private fun launchBonusGrid(
        petitAuBout: String?     = null, onPetit: (String?) -> Unit      = {},
        poignee: String?         = null, onPoignee: (String?) -> Unit     = {},
        doublePoignee: String?   = null, onDoublePoignee: (String?) -> Unit = {},
        triplePoignee: String?   = null, onTriplePoignee: (String?) -> Unit = {}
    ) {
        composeTestRule.setContent {
            TarotCounterTheme {
                CompactBonusGrid(
                    playerNames     = bonusPlayers,
                    bonusLabels     = bonusLabels,
                    bonusTooltips   = bonusTips,
                    petitAuBout     = petitAuBout,   onPetit          = onPetit,
                    poignee         = poignee,        onPoignee        = onPoignee,
                    doublePoignee   = doublePoignee,  onDoublePoignee  = onDoublePoignee,
                    triplePoignee   = triplePoignee,  onTriplePoignee  = onTriplePoignee
                )
            }
        }
    }

    @Test
    fun bonusGrid_shows_player_names_in_header() {
        launchBonusGrid()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun bonusGrid_shows_all_four_bonus_labels() {
        launchBonusGrid()
        for (label in bonusLabels) {
            composeTestRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun bonusGrid_ticking_unchecked_box_calls_onSelect_with_player_name() {
        // The first checkbox in the "Petit au bout" row corresponds to "Alice".
        // Ticking it should call onPetit("Alice").
        var selected: String? = "UNCHANGED"
        launchBonusGrid(petitAuBout = null, onPetit = { selected = it })

        // Click the first checkbox in the grid (Alice's cell in the petit au bout row).
        // We use onAllNodes to find the right checkbox by traversal order.
        composeTestRule
            .onAllNodes(androidx.compose.ui.test.hasClickAction())[0]
            // Compact bonus grids use Checkboxes; the header row has no checkboxes so
            // the first clickable with a checked state is Alice's petit-au-bout cell.
        composeTestRule
            // Use the semantic selector for a Checkbox node that is NOT checked
            // (because petitAuBout starts as null, meaning unchecked).
            .onAllNodes(
                androidx.compose.ui.test.hasClickAction() and
                androidx.compose.ui.test.isToggleable()
            )[0]
            .performClick()

        assertEquals("Ticking Alice's checkbox should pass her name", "Alice", selected)
    }

    @Test
    fun bonusGrid_ticking_already_checked_box_calls_onSelect_with_null() {
        // When Alice is already assigned to "petit au bout", ticking her checkbox again
        // should clear the selection (call onPetit(null)).
        var selected: String? = "UNCHANGED"
        launchBonusGrid(petitAuBout = "Alice", onPetit = { selected = it })

        // Alice's checkbox is now checked — ticking it should deselect.
        composeTestRule
            .onAllNodes(
                androidx.compose.ui.test.hasClickAction() and
                androidx.compose.ui.test.isToggleable()
            )[0]
            .performClick()

        assertEquals(
            "Ticking an already-checked checkbox should pass null (clear assignment)",
            null, selected
        )
    }

    @Test
    fun bonusGrid_five_players_all_names_appear_in_header() {
        // Regression for issue #118: with 5 players the label column consumed
        // 0.42 f of the width, leaving each player column only ~11 % — too narrow
        // for most names. Reducing labelWeight to 0.36 f and adding textAlign =
        // TextAlign.Center ensures each name is visible (at least as a truncated node).
        val fivePlayers = listOf("Alice", "Bob", "Charlie", "Dave", "Eve")
        composeTestRule.setContent {
            TarotCounterTheme {
                CompactBonusGrid(
                    playerNames     = fivePlayers,
                    bonusLabels     = bonusLabels,
                    bonusTooltips   = bonusTips,
                    petitAuBout     = null, onPetit          = {},
                    poignee         = null, onPoignee        = {},
                    doublePoignee   = null, onDoublePoignee  = {},
                    triplePoignee   = null, onTriplePoignee  = {}
                )
            }
        }
        // All five names must be present somewhere in the composition tree
        // (the Text composable emits a semantics node even when the text is clipped
        // by the available width — substring = true catches partial matches too).
        fivePlayers.forEach { name ->
            assertTrue(
                "Player '$name' should appear in the bonus-grid header with 5 players",
                composeTestRule
                    .onAllNodesWithText(name, substring = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PlayerChipSelector
    // ─────────────────────────────────────────────────────────────────────────

    private val chipPlayers = listOf("Alice", "Bob", "Charlie")

    private fun launchChipSelector(
        selected: String? = null,
        onSelect: (String?) -> Unit = {}
    ) {
        composeTestRule.setContent {
            TarotCounterTheme {
                PlayerChipSelector(
                    label          = "Chelem winner",
                    noneLabel      = "None",
                    selectedPlayer = selected,
                    playerNames    = chipPlayers,
                    onSelect       = onSelect
                )
            }
        }
    }

    @Test
    fun chipSelector_shows_section_label() {
        launchChipSelector()
        composeTestRule.onNodeWithText("Chelem winner").assertIsDisplayed()
    }

    @Test
    fun chipSelector_shows_none_chip() {
        launchChipSelector()
        composeTestRule.onNodeWithText("None").assertIsDisplayed()
    }

    @Test
    fun chipSelector_shows_all_player_chips() {
        launchChipSelector()
        for (name in chipPlayers) {
            composeTestRule.onNodeWithText(name).assertIsDisplayed()
        }
    }

    @Test
    fun chipSelector_none_chip_is_selected_when_no_player_selected() {
        launchChipSelector(selected = null)
        // FilterChip with selected=true has the Selected role.
        composeTestRule.onNodeWithText("None").assertIsSelected()
    }

    @Test
    fun chipSelector_player_chip_is_selected_when_that_player_is_set() {
        launchChipSelector(selected = "Bob")
        composeTestRule.onNodeWithText("Bob").assertIsSelected()
    }

    @Test
    fun chipSelector_other_chips_are_not_selected_when_one_player_chosen() {
        launchChipSelector(selected = "Bob")
        composeTestRule.onNodeWithText("None").assertIsNotSelected()
        composeTestRule.onNodeWithText("Alice").assertIsNotSelected()
    }

    @Test
    fun chipSelector_tapping_unselected_player_calls_onSelect_with_name() {
        var result: String? = "UNCHANGED"
        launchChipSelector(selected = null, onSelect = { result = it })

        composeTestRule.onNodeWithText("Alice").performClick()

        assertEquals("Tapping Alice chip should call onSelect(\"Alice\")", "Alice", result)
    }

    @Test
    fun chipSelector_tapping_already_selected_player_calls_onSelect_with_null() {
        var result: String? = "UNCHANGED"
        launchChipSelector(selected = "Alice", onSelect = { result = it })

        // Alice is currently selected — tapping again should deselect (pass null).
        composeTestRule.onNodeWithText("Alice").performClick()

        assertEquals(
            "Tapping the already-selected player chip should call onSelect(null)",
            null, result
        )
    }

    @Test
    fun chipSelector_tapping_none_chip_calls_onSelect_with_null() {
        var result: String? = "UNCHANGED"
        launchChipSelector(selected = "Bob", onSelect = { result = it })

        composeTestRule.onNodeWithText("None").performClick()

        assertEquals("Tapping None chip should call onSelect(null)", null, result)
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
}

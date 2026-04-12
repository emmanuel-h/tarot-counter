package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

// ── View mode enum ────────────────────────────────────────────────────────────

// Represents the two display modes available on the history screen:
//   TABLE — the existing cumulative score table (one row per round, one column per player)
//   LIST  — a round-by-round detail list (previously shown at the bottom of GameScreen)
enum class HistoryViewMode { TABLE, LIST }

// Returns the localized label for each view mode.
// Kept private to this file — the enum is shared but the label mapping is a UI detail.
private fun HistoryViewMode.label(strings: AppStrings) = when (this) {
    HistoryViewMode.TABLE -> strings.historyViewTable
    HistoryViewMode.LIST  -> strings.historyViewList
}

/**
 * ScoreHistoryScreen displays the score history in one of two views, switchable via a
 * segmented toggle at the top of the content area:
 *
 *   TABLE (default) — cumulative score table, one row per completed round.
 *   LIST            — round-by-round detail list that was previously shown at the
 *                     bottom of the Game screen.
 *
 * Layout:
 *   - Header: back arrow + "Score history" title (localized)
 *   - View toggle: segmented button to switch between TABLE and LIST
 *   - Content: either the score table or the round-detail list
 *
 * @param playerNames  Ordered list of player display names (fallbacks already resolved).
 * @param roundHistory Completed rounds in chronological order, oldest first.
 * @param onBack       Callback fired when the user taps the back arrow.
 * @param modifier     Passed from the parent (e.g. Scaffold inner padding).
 */
@Composable
fun ScoreHistoryScreen(
    playerNames: List<String>,
    roundHistory: List<RoundResult>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Read the active locale and resolve all strings once at the top of the composable.
    val locale  = LocalAppLocale.current
    val strings = appStrings(locale)

    // Which view is currently active. Defaults to TABLE and resets each time the
    // screen enters the composition (i.e. every time the user opens the history overlay).
    var viewMode by remember { mutableStateOf(HistoryViewMode.TABLE) }

    // Box centers the content Column horizontally on wide screens (tablets in landscape).
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(
        modifier = Modifier
            .widthIn(max = MAX_CONTENT_WIDTH)
            .fillMaxWidth()
            // verticalScroll scrolls the entire page (header + toggle + content) as one unit.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // ── Screen header: back arrow + title ─────────────────────────────────
        ScreenHeader(title = strings.scoreHistory, onBack = onBack)

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // ── View mode toggle ──────────────────────────────────────────────────
        // SingleChoiceSegmentedButtonRow is the Material 3 standard for mutually
        // exclusive options. The selected segment is visually filled; no checkmark
        // icon is shown (icon = {}) because the fill already communicates selection.
        //
        // rememberSharedAutoSizeState ensures both labels shrink to the same font
        // size if either label overflows its half-width slot (e.g. on a small screen
        // with a long translation). Keyed on locale so a language change resets sizing.
        val modes     = HistoryViewMode.entries
        val labelSize = rememberSharedAutoSizeState(locale)

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_view_toggle")
        ) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    shape    = SegmentedButtonDefaults.itemShape(index, modes.size),
                    selected = viewMode == mode,
                    onClick  = { viewMode = mode },
                    // Suppress the default checkmark — the filled segment already shows selection.
                    icon     = {},
                    modifier = Modifier.testTag("toggle_${mode.name.lowercase()}")
                ) {
                    AutoSizeText(
                        text            = mode.label(strings),
                        modifier        = Modifier.padding(horizontal = 1.dp),
                        sharedSizeState = labelSize
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Content area — switches based on the selected view mode ───────────
        when (viewMode) {
            HistoryViewMode.TABLE -> ScoreTableView(
                playerNames  = playerNames,
                roundHistory = roundHistory,
                strings      = strings
            )
            HistoryViewMode.LIST -> RoundListView(
                roundHistory = roundHistory,
                locale       = locale,
                strings      = strings
            )
        }
    }   // end Column
    }   // end (centering) Box
}

// ── TABLE view ────────────────────────────────────────────────────────────────

// Renders the cumulative score table: one row per completed round, one column per player.
// The table uses weighted columns (ScoreTableRow) so it always fills the available width
// without horizontal scrolling, regardless of player count (issue #129).
@Composable
private fun ScoreTableView(
    playerNames: List<String>,
    roundHistory: List<RoundResult>,
    strings: AppStrings
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row: "Round" label + one column per player name.
        ScoreTableRow(
            cells    = listOf(strings.roundColumn) + playerNames,
            isHeader = true
        )
        HorizontalDivider()

        // buildScoreTableData() (GameModels.kt) accumulates running totals and formats
        // each cell — shared with FinalScoreScreen (issue #75).
        for (row in buildScoreTableData(playerNames, roundHistory)) {
            ScoreTableRow(
                cells       = row.cells,
                isHeader    = false,
                scoreValues = row.scoreValues
            )
        }
    }
}

// ── LIST view ─────────────────────────────────────────────────────────────────

// Renders the round-by-round detail list, newest round first.
// Each row uses RoundHistoryRow (GameScreen.kt) — the same composable that was
// previously shown at the bottom of the Game screen (removed in issue #136).
@Composable
private fun RoundListView(
    roundHistory: List<RoundResult>,
    locale: AppLocale,
    strings: AppStrings
) {
    if (roundHistory.isEmpty()) {
        // No rounds played yet — the list would be empty, so show a brief notice
        // consistent with how FinalScoreScreen handles the same edge case.
        androidx.compose.material3.Text(
            text  = strings.noRoundsPlayed,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    // Show newest round first so the most recent result is immediately visible
    // without scrolling — mirrors the ordering that was used in GameScreen.
    val reversedHistory = roundHistory.reversed()
    Column(modifier = Modifier.fillMaxWidth()) {
        reversedHistory.forEachIndexed { index, round ->
            RoundHistoryRow(round = round, locale = locale, strings = strings)
            // Thin divider between rows; omitted after the last one.
            if (index < reversedHistory.lastIndex) {
                HorizontalDivider(
                    modifier  = Modifier.padding(vertical = 4.dp),
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

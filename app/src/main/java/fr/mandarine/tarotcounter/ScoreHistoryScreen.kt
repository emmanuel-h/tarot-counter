package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ScoreHistoryScreen displays the score evolution as a table.
 *
 * Layout:
 *   - Header: back arrow + "Score history" title (localized)
 *   - Table: one row per completed round, one column per player
 *     - Each cell shows the player's **cumulative** total after that round,
 *       matching exactly what the scoreboard shows on the game screen.
 *
 * Example (3 players, 2 completed rounds):
 *
 *   | Round | Alice | Bob  | Charlie |
 *   |-------|-------|------|---------|
 *   |   1   |  +50  | -25  |  -25   |
 *   |   2   |  +20  | -10  |  -10   |
 *
 * The table uses weighted columns so all players fit on screen at once (issue #129).
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
    val strings = appStrings(LocalAppLocale.current)

    // Box centers the content Column horizontally on wide screens (tablets in landscape).
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(
        modifier = Modifier
            .widthIn(max = MAX_CONTENT_WIDTH)
            .fillMaxWidth()
            // verticalScroll on the outer Column scrolls the entire page (header + table)
            // vertically, so long game histories never clip off-screen.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // ── Screen header: back arrow + title ─────────────────────────────────
        // ScreenHeader is the shared composable defined in ScreenHeader.kt —
        // it renders a back arrow + title in a Row, consistent with FinalScoreScreen.
        ScreenHeader(title = strings.scoreHistory, onBack = onBack)

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // ── Score table ───────────────────────────────────────────────────────
        // The table uses weighted columns (ScoreTableRow) so it always fills the
        // available screen width without horizontal scrolling, regardless of how
        // many players are in the game (issue #129).
        // Vertical scrolling is already handled by the outer Column above.
        Column(modifier = Modifier.fillMaxWidth()) {
            // Column headers: localized "Round" header, then one header per player name.
            // No score values for the header row — labels use the default colour.
            ScoreTableRow(
                cells    = listOf(strings.roundColumn) + playerNames,
                isHeader = true
            )
            HorizontalDivider()

            // buildScoreTableData() (GameModels.kt) accumulates the running totals and
            // formats each cell — the loop that was previously duplicated here is now
            // shared with FinalScoreScreen (issue #75).
            for (row in buildScoreTableData(playerNames, roundHistory)) {
                ScoreTableRow(
                    cells       = row.cells,
                    isHeader    = false,
                    scoreValues = row.scoreValues
                )
            }
        }   // end table Column
    }   // end Column
    }   // end (centering) Box
}


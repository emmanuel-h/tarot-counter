package fr.mandarine.tarotcounter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Width for the "Round" / "Manche" column — short because round numbers only go up to ~99.
private val ROUND_COL_WIDTH: Dp = 64.dp

// Width for each player column — wide enough for score strings like "+1000"
// and player names up to about 8 characters.
private val PLAYER_COL_WIDTH: Dp = 80.dp

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
 * The table scrolls horizontally (for 5 players) and vertically (for many rounds).
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
        // Two scroll directions: left↔right for many players, up↓down for many rounds.
        // We use a plain Column (not LazyColumn) inside two nested scroll modifiers,
        // which is fine for Tarot games (typical: tens of rounds, 3–5 players).
        //
        // The Box wrapper lets us overlay a scroll-hint arrow when the table extends
        // beyond the right edge of the screen. The Icon sits at Alignment.TopEnd so
        // it is visible when the user first opens the screen and naturally disappears
        // as they scroll down — exactly when the hint is no longer needed.
        val hScrollState = rememberScrollState()
        Box {
            Column(
                // Only horizontal scrolling here — vertical scrolling is handled
                // by the outer Column so the two scroll directions don't conflict.
                modifier = Modifier.horizontalScroll(hScrollState)
            ) {
                // Column headers: localized "Round" header, then one header per player name.
                // No score values for the header row — labels use the default colour.
                ScoreTableRow(
                    cells = listOf(strings.roundColumn) + playerNames,
                    isHeader = true
                )
                HorizontalDivider()

            // `runningTotals` accumulates each player's score as we iterate rounds.
            // We start everyone at 0 and add their per-round delta on each iteration.
            val runningTotals = playerNames.associateWith { 0 }.toMutableMap()

            for (round in roundHistory) {
                // Add this round's scores to each player's running total.
                // `getOrDefault(name, 0)` returns 0 for skipped rounds, which have an
                // empty `playerScores` map — so skipped rounds don't change totals.
                for (name in playerNames) {
                    runningTotals[name] =
                        (runningTotals[name] ?: 0) + round.playerScores.getOrDefault(name, 0)
                }

                // Build the cell list: round number first, then cumulative score per player.
                val cells = buildList {
                    add(round.roundNumber.toString())
                    for (name in playerNames) {
                        val total = runningTotals[name] ?: 0
                        // Prefix "+" for non-negative values so the sign is always visible.
                        val sign = if (total >= 0) "+" else ""
                        add("$sign$total")
                    }
                }

                // Build a parallel list of integer score values for colour coding.
                // Index 0 is null (round-number column has no semantic colour);
                // indices 1+ hold the running total so ScoreTableRow can call scoreColor().
                val scoreValues: List<Int?> = buildList {
                    add(null) // round-number column — no colour
                    for (name in playerNames) {
                        add(runningTotals[name] ?: 0)
                    }
                }

                ScoreTableRow(
                    cells = cells,
                    isHeader = false,
                    scoreValues = scoreValues
                )
            }
        }

            // Arrow hint: floats at the top-right corner of the Box.
            // Visible when there is more content to the right (i.e. ≥4–5 players).
            // Automatically disappears when the user scrolls right (canScrollForward = false).
            if (hScrollState.canScrollForward) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
        }   // end (inner table) Box
    }   // end Column
    }   // end (centering) Box
}

/**
 * A single horizontal row in the score table.
 *
 * The first cell (index 0) uses [ROUND_COL_WIDTH]; all other cells use [PLAYER_COL_WIDTH].
 * This keeps the "Round" column compact while giving player names room to breathe.
 *
 * @param cells       Text content for each cell in left-to-right order.
 * @param isHeader    If true, renders the text in bold (used for the header row).
 * @param scoreValues Optional parallel list of raw score integers used for colour coding.
 *                    A null entry (or a null list) means "use the default text colour".
 *                    Non-null entries are passed to [scoreColor] so positive values appear
 *                    green and negative values appear red.
 */
@Composable
private fun ScoreTableRow(
    cells: List<String>,
    isHeader: Boolean,
    scoreValues: List<Int?>? = null
) {
    Row {
        cells.forEachIndexed { index, text ->
            // First column ("Round") is narrower; player columns are wider.
            val cellWidth = if (index == 0) ROUND_COL_WIDTH else PLAYER_COL_WIDTH

            // Determine the text colour: semantic colour for score cells, default otherwise.
            // `Color.Unspecified` tells Compose to inherit from the parent (i.e. default colour).
            val textColor = if (!isHeader && scoreValues != null) {
                val value = scoreValues.getOrNull(index)
                if (value != null) scoreColor(value) else Color.Unspecified
            } else {
                Color.Unspecified
            }

            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = if (isHeader) {
                        // Bold weight for the header row so column labels stand out.
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = textColor,
                    textAlign = TextAlign.Center,
                    // Prevent very long names from wrapping and making rows uneven.
                    maxLines = 1
                )
            }
        }
    }
}

package fr.mandarine.tarotcounter

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Width of the "Round" column — round numbers rarely exceed two digits.
private val FINAL_ROUND_COL_WIDTH: Dp = 64.dp

// Width of each player column — must fit score strings like "+1000" and names
// up to about 8 characters.
private val FINAL_PLAYER_COL_WIDTH: Dp = 80.dp

/**
 * FinalScoreScreen shows the game results when the player ends the game early
 * or when a natural end is declared.
 *
 * Layout:
 *   - Trophy icon + "Game Over" heading
 *   - Winner card (highlighted with primaryContainer) showing name and final score
 *     (or "Tie!" with all co-winner names in case of a draw)
 *   - Full round-by-round score table, with winner column(s) highlighted
 *   - "New Game" button that returns to the setup screen
 *
 * The winner is the player with the highest cumulative total after all rounds.
 * If multiple players share the highest score, all are shown as co-winners.
 *
 * @param playerNames  Ordered list of player display names (fallbacks already resolved).
 * @param roundHistory All completed rounds in chronological order, oldest first.
 * @param onNewGame    Callback fired when the user taps "New Game" (navigates back to setup).
 * @param modifier     Passed from the parent (e.g. Scaffold inner padding).
 */
@Composable
fun FinalScoreScreen(
    playerNames: List<String>,
    roundHistory: List<RoundResult>,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    // `computeFinalTotals` sums each player's per-round scores across all rounds.
    // It lives in GameModels so it can be unit-tested without Compose.
    val totals = computeFinalTotals(playerNames, roundHistory)

    // `findWinners` returns a list to handle ties: normally one name, multiple on a draw.
    val winners = findWinners(totals)

    // Build a set of column indices (1-based, because index 0 is the "Round" column)
    // that correspond to winner(s). Used to highlight those columns in the table.
    // Set<Int> gives O(1) membership checks inside the row composable.
    val winnerColumnIndices: Set<Int> = playerNames
        .mapIndexedNotNull { i, name -> if (name in winners) i + 1 else null }
        .toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Title ─────────────────────────────────────────────────────────────
        // The icon is decorative — the heading already conveys "game over".
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Game Over",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Winner card ───────────────────────────────────────────────────────
        // `primaryContainer` is the Material 3 colour meant for prominent, coloured
        // containers — it stands out clearly without being too loud.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (winners.size == 1) {
                    // Single winner ─ show "Winner", the name, and their final score.
                    Text(
                        text = "Winner",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = winners.first(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    val score = totals[winners.first()] ?: 0
                    val sign = if (score >= 0) "+" else ""
                    Text(
                        text = "$sign$score pts",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (winners.isNotEmpty()) {
                    // Tie ─ list all co-winners.
                    Text(
                        text = "It's a tie!",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = winners.joinToString(" & "),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                // `winners.isEmpty()` can only happen with no players — impossible in practice.
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // ── Score table ───────────────────────────────────────────────────────
        // Scrolls horizontally (5 players) and vertically (many rounds).
        // Winner column(s) receive a `secondaryContainer` tint so they stand out
        // without competing visually with the winner card above.
        if (roundHistory.isEmpty()) {
            // No rounds were played — show a simple notice instead of an empty table.
            Text(
                text = "No rounds played",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                // Header row: "Round" label + one header per player name.
                FinalScoreTableRow(
                    cells = listOf("Round") + playerNames,
                    isHeader = true,
                    winnerColumnIndices = winnerColumnIndices
                )
                HorizontalDivider()

                // Accumulate running totals the same way ScoreHistoryScreen does.
                // We start every player at 0 and add their per-round delta on each iteration.
                val runningTotals = playerNames.associateWith { 0 }.toMutableMap()

                for (round in roundHistory) {
                    // Add this round's contribution to each player's running total.
                    // Skipped rounds have an empty `playerScores` map, so they add 0.
                    for (name in playerNames) {
                        runningTotals[name] =
                            (runningTotals[name] ?: 0) + round.playerScores.getOrDefault(name, 0)
                    }

                    // Build the cell list: round number first, then cumulative score per player.
                    val cells = buildList {
                        add(round.roundNumber.toString())
                        for (name in playerNames) {
                            val total = runningTotals[name] ?: 0
                            // Always show the sign so positive/negative is immediately clear.
                            val sign = if (total >= 0) "+" else ""
                            add("$sign$total")
                        }
                    }

                    FinalScoreTableRow(
                        cells = cells,
                        isHeader = false,
                        winnerColumnIndices = winnerColumnIndices
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── New Game button ───────────────────────────────────────────────────
        // This navigates back to the setup screen so a fresh game can be started.
        Button(
            onClick = onNewGame,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("New Game")
        }
    }
}

/**
 * A single horizontal row in the final score table.
 *
 * Columns listed in [winnerColumnIndices] receive a `secondaryContainer` background tint,
 * and their text is rendered bold, to highlight the winner(s) throughout the table.
 *
 * The first column (index 0) is the "Round" column and uses [FINAL_ROUND_COL_WIDTH];
 * all other columns use [FINAL_PLAYER_COL_WIDTH].
 *
 * @param cells               Text content for each cell, left-to-right.
 * @param isHeader            True for the column-header row (all cells rendered bold).
 * @param winnerColumnIndices Zero-based column indices to highlight as winner column(s).
 */
@Composable
private fun FinalScoreTableRow(
    cells: List<String>,
    isHeader: Boolean,
    winnerColumnIndices: Set<Int>
) {
    Row {
        cells.forEachIndexed { index, text ->
            val cellWidth = if (index == 0) FINAL_ROUND_COL_WIDTH else FINAL_PLAYER_COL_WIDTH
            val isWinnerColumn = index in winnerColumnIndices

            Box(
                modifier = Modifier
                    .width(cellWidth)
                    // Tint the winner's column cells with `secondaryContainer` so they
                    // are visually linked to the winner card above.
                    .then(
                        if (isWinnerColumn) {
                            Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            Modifier
                        }
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    // Bold for the header row and for every cell in the winner's column.
                    style = if (isHeader || isWinnerColumn) {
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    textAlign = TextAlign.Center,
                    // Prevent long names from wrapping and making rows uneven.
                    maxLines = 1
                )
            }
        }
    }
}

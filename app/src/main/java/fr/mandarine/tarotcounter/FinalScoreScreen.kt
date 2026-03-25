package fr.mandarine.tarotcounter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import fr.mandarine.tarotcounter.ui.theme.GoldWinnerDark
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
 *   - Winner card (gold/amber secondaryContainer) showing name and final score
 *     (or "Tie!" with all co-winner names in case of a draw)
 *   - Full round-by-round score table, with winner column(s) highlighted
 *   - "New Game" button that returns to the setup screen
 *
 * The winner is the player with the highest cumulative total after all rounds.
 * If multiple players share the highest score, all are shown as co-winners.
 *
 * @param playerNames  Ordered list of player display names (fallbacks already resolved).
 * @param roundHistory All completed rounds in chronological order, oldest first.
 * @param onBack       Callback fired when the user taps the back arrow (returns to the game).
 * @param onNewGame    Callback fired when the user taps "New Game" (navigates back to setup).
 * @param modifier     Passed from the parent (e.g. Scaffold inner padding).
 */
@Composable
fun FinalScoreScreen(
    playerNames: List<String>,
    roundHistory: List<RoundResult>,
    onBack: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Read the active locale and resolve all strings once at the top of the composable.
    val strings = appStrings(LocalAppLocale.current)

    // ── System back-button handling ───────────────────────────────────────────
    // Controls whether the leave-confirmation dialog is visible.
    // The dialog is triggered by the system back button (or gesture), not by the
    // in-screen back arrow — the arrow stays wired to onBack (return to game).
    var showLeaveConfirm by remember { mutableStateOf(false) }

    // BackHandler intercepts the Android system back button while this composable
    // is in the composition. Because FinalScoreScreen is placed *after* the
    // GameScreen-level BackHandler in the composition tree, this one takes priority
    // and GameScreen's handler is effectively shadowed.
    BackHandler { showLeaveConfirm = true }

    // Confirmation dialog — only rendered when showLeaveConfirm is true.
    // AlertDialog is a Material 3 modal that blocks interaction with the rest of
    // the screen until the user picks "Leave" or "Cancel".
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text(strings.backConfirmTitle) },
            text  = { Text(strings.backConfirmBody) },
            confirmButton = {
                // "Leave" navigates to the landing page (same as "New Game" button).
                TextButton(onClick = onNewGame) {
                    Text(strings.backConfirmLeave)
                }
            },
            dismissButton = {
                // "Cancel" closes the dialog and returns to the Final Score screen.
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

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
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Screen header: back arrow + title ─────────────────────────────────
        // ScreenHeader is a shared composable (ScreenHeader.kt) that renders the
        // back arrow and screen title in a Row — the same pattern as ScoreHistoryScreen,
        // now unified into one place so both screens look identical at the top.
        ScreenHeader(title = strings.gameOver, onBack = onBack)

        // ── Decorative trophy icon ─────────────────────────────────────────────
        // Enlarged to 72dp and tinted gold (secondary) to make the game-ending moment
        // feel more dramatic. The icon is purely decorative — the title conveys meaning.
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.secondary  // gold/amber accent
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Winner card ───────────────────────────────────────────────────────
        // `secondaryContainer` is the gold/amber tinted container — aligns with the
        // trophy icon above and the winner-column highlight in the table.
        //
        // The card uses a scale-in + fade-in entry animation so it "pops" into view
        // when the screen first appears, giving the winner announcement more drama.
        // `visible` starts false and is set to true in a LaunchedEffect so the
        // animation fires exactly once on composition.
        var cardVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { cardVisible = true }

        AnimatedVisibility(
            visible = cardVisible,
            // scaleIn grows the card from 80% → 100%; fadeIn prevents a hard pop.
            enter = scaleIn(initialScale = 0.8f) + fadeIn()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (winners.size == 1) {
                        // Single winner ─ show "Winner", the name (with star medal), and score.
                        Text(
                            text = strings.winner,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Row so the star icon sits inline with the winner's name.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Small decorative star medal — contentDescription null because
                            // the winner's name text already conveys the meaning.
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = winners.first(),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        val score = totals[winners.first()] ?: 0
                        val sign = if (score >= 0) "+" else ""
                        Text(
                            text = strings.scoreDisplay(sign, score),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else if (winners.isNotEmpty()) {
                        // Tie ─ list all co-winners. No star icon — no single champion.
                        Text(
                            text = strings.itsATie,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = winners.joinToString(" & "),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    // `winners.isEmpty()` can only happen with no players — impossible in practice.
                }
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
                text = strings.noRoundsPlayed,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Box lets us overlay a scroll-hint arrow when the table is wider than
            // the screen. The Icon anchors to Alignment.TopEnd so it is immediately
            // visible when the user arrives at this screen and disappears once they
            // scroll the table to the right.
            val hScrollState = rememberScrollState()
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(hScrollState)
                ) {
                // Header row: localized "Round" label + one header per player name.
                // No score values for the header row — labels use the default colour.
                FinalScoreTableRow(
                    cells = listOf(strings.roundColumn) + playerNames,
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

                    // Parallel list of raw score integers for colour coding.
                    // Index 0 is null (round-number column); indices 1+ hold running totals.
                    val scoreValues: List<Int?> = buildList {
                        add(null) // round-number column — no colour
                        for (name in playerNames) {
                            add(runningTotals[name] ?: 0)
                        }
                    }

                    FinalScoreTableRow(
                        cells = cells,
                        isHeader = false,
                        winnerColumnIndices = winnerColumnIndices,
                        scoreValues = scoreValues
                    )
                }
            }   // end Column

                // Arrow hint: visible when the table extends beyond the right screen edge.
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
            }   // end Box
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── New Game button ───────────────────────────────────────────────────
        // Full-width with titleMedium text so it reads as the dominant call-to-action
        // after the winner is announced. `fillMaxWidth` is already set; the larger
        // text size (`titleMedium` vs the default `labelLarge`) adds visual weight.
        Button(
            onClick = onNewGame,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = strings.newGame,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Back to game button ───────────────────────────────────────────────
        // Lets the user resume the current game if they ended it by mistake.
        // OutlinedButton has a lower visual weight than the filled "New Game" button,
        // signalling that resuming is the secondary action.
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.backToGame)
        }
    }
}

/**
 * A single horizontal row in the final score table.
 *
 * Winner columns (listed in [winnerColumnIndices]) receive a gold/amber `secondary`
 * background so they stand out throughout the table — changed from the softer
 * `secondaryContainer` to the saturated amber token as requested in issue #4.
 * Their text is rendered bold for extra emphasis.
 *
 * Score cells (data rows, non-round-number columns) apply semantic colour coding
 * via [scoreColor]: green for positive/zero, red for negative.
 *
 * The first column (index 0) is the "Round" column and uses [FINAL_ROUND_COL_WIDTH];
 * all other columns use [FINAL_PLAYER_COL_WIDTH].
 *
 * @param cells               Text content for each cell, left-to-right.
 * @param isHeader            True for the column-header row (all cells rendered bold).
 * @param winnerColumnIndices Zero-based column indices to highlight as winner column(s).
 * @param scoreValues         Optional parallel list of raw score integers.
 *                            A null entry means "use default colour"; a non-null entry
 *                            triggers [scoreColor] for green/red coding.
 */
@Composable
private fun FinalScoreTableRow(
    cells: List<String>,
    isHeader: Boolean,
    winnerColumnIndices: Set<Int>,
    scoreValues: List<Int?>? = null
) {
    Row {
        cells.forEachIndexed { index, text ->
            val cellWidth = if (index == 0) FINAL_ROUND_COL_WIDTH else FINAL_PLAYER_COL_WIDTH
            val isWinnerColumn = index in winnerColumnIndices

            // Winner columns get a gold/amber background.
            // Light mode: saturated `secondary` (rich amber) — vivid enough on parchment.
            // Dark mode: `GoldWinnerDark` (muted dark gold) — `secondary` (GoldLight) is
            //            too flashy on the dark felt surface, so we use a deeper tone.
            val winnerBg = if (isSystemInDarkTheme()) GoldWinnerDark
                           else MaterialTheme.colorScheme.secondary
            val bgModifier = if (isWinnerColumn) Modifier.background(winnerBg) else Modifier

            // Semantic text colour for score cells: green (positive) or red (negative).
            // Header row and round-number column always use the default colour.
            // On winner columns the text is drawn on `secondary` (amber); the score
            // colour (primary = green, error = red) still provides enough contrast.
            val textColor = if (!isHeader && scoreValues != null) {
                val value = scoreValues.getOrNull(index)
                if (value != null) scoreColor(value) else Color.Unspecified
            } else {
                Color.Unspecified
            }

            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .then(bgModifier)
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
                    color = textColor,
                    textAlign = TextAlign.Center,
                    // Prevent long names from wrapping and making rows uneven.
                    maxLines = 1
                )
            }
        }
    }
}

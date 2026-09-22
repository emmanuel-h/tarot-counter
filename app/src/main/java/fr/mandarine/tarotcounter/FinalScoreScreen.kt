package fr.mandarine.tarotcounter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mandarine.tarotcounter.ui.theme.Dimens
import fr.mandarine.tarotcounter.ui.theme.tarotColors

/**
 * The game-over screen (Salon redesign, issue #201):
 *
 * ```
 * ┌──────────────────────────────┐
 * │ ←  Game Over                 │
 * │ ┌── felt card ─────────────┐ │
 * │ │        [trophy]          │ │
 * │ │ WINNER                   │ │   "IT'S A TIE!" + every co-winner on a tie
 * │ │        Alice             │ │
 * │ │        +428              │ │
 * │ │  8 rounds · 4 players    │ │
 * │ └──────────────────────────┘ │
 * │ ┌──────────────────────────┐ │   ranking (competition ranks, brass leaders)
 * │ │ 1 (A) Alice        +428  │ │
 * │ │ 2 (C) Chloé         +96  │ │
 * │ └──────────────────────────┘ │
 * │ See all rounds             › │   → round-by-round table (score history)
 * │ Score over time              │
 * │  ╱‾‾╲__╱‾   one line/player  │   Canvas chart, zero baseline, round ticks
 * │ [         New Game         ] │
 * │ [         Main Menu        ] │
 * │           Back to game       │
 * └──────────────────────────────┘
 * ```
 *
 * The system back button asks for confirmation first (the results would be lost).
 *
 * @param playerNames  Ordered list of player display names (fallbacks already resolved).
 * @param roundHistory All completed rounds in chronological order, oldest first.
 * @param onBack       "Back to game": return to the active game.
 * @param onNewGame    "New Game": navigate back to setup.
 * @param onMainMenu   "Main Menu": navigate to the landing screen.
 */
@Composable
fun FinalScoreScreen(
    playerNames: List<String>,
    roundHistory: List<RoundResult>,
    onBack: () -> Unit,
    onNewGame: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = appStrings(LocalAppLocale.current)

    // "See all rounds" opens the round-by-round table on top of this screen;
    // its back arrow returns here (not to the game).
    var showAllRounds by remember { mutableStateOf(false) }
    if (showAllRounds) {
        ScoreHistoryScreen(
            playerNames  = playerNames,
            roundHistory = roundHistory,
            onBack       = { showAllRounds = false },
            modifier     = modifier
        )
        // While the table is open, system back closes it too.
        BackHandler { showAllRounds = false }
        return
    }

    // ── System back-button handling ───────────────────────────────────────────
    // System back asks for confirmation; the in-screen back arrow returns to the game.
    var showLeaveConfirm by remember { mutableStateOf(false) }
    BackHandler { showLeaveConfirm = true }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text(strings.backConfirmTitle) },
            text  = { Text(strings.backConfirmBody) },
            confirmButton = {
                AppTextButton(text = strings.backConfirmLeave, onClick = onNewGame)
            },
            dismissButton = {
                AppTextButton(text = strings.cancel, onClick = { showLeaveConfirm = false })
            }
        )
    }

    // Pure helpers (GameModels.kt / Standings.kt), unit-tested on the JVM.
    val totals  = computeFinalTotals(playerNames, roundHistory)
    val winners = findWinners(totals)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenMargin)
                .padding(bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL)
        ) {
            SalonTopBar(title = strings.gameOver, onBack = onBack)

            // ── Winner card ───────────────────────────────────────────────────
            // Pops in once (scale + fade) to give the result a sense of occasion.
            var cardVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { cardVisible = true }
            AnimatedVisibility(
                visible = cardVisible,
                enter   = scaleIn(initialScale = 0.8f) + fadeIn()
            ) {
                WinnerCard(
                    winners     = winners,
                    score       = winners.firstOrNull()?.let { totals.getValue(it) } ?: 0,
                    detail      = listOf(
                        strings.roundCount(roundHistory.size),
                        strings.playerCount(playerNames.size)
                    ).joinToString(" · "),
                    strings     = strings
                )
            }

            if (roundHistory.isEmpty()) {
                // Nothing to rank or chart.
                Text(
                    text     = strings.noRoundsPlayed,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                // ── Ranking ───────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                    RankingCard(computeStandings(playerNames, roundHistory))
                    // The full round-by-round table now lives behind this link.
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        AppTextButton(text = strings.seeAllRounds, onClick = { showAllRounds = true })
                    }
                }

                // ── Score over time ───────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(title = strings.scoreOverTime)
                    ScoreChart(
                        playerNames = playerNames,
                        rounds      = roundHistory,
                        description = strings.scoreChartDescription(roundHistory.size)
                    )
                    ChartLegend(playerNames)
                }
            }

            // ── Actions, most to least important ──────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                AppButton(
                    text      = strings.newGame,
                    onClick   = onNewGame,
                    modifier  = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleMedium
                )
                AppOutlinedButton(
                    text     = strings.mainMenu,
                    onClick  = onMainMenu,
                    modifier = Modifier.fillMaxWidth()
                )
                AppTextButton(
                    text     = strings.backToGame,
                    onClick  = onBack,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

// The felt-green winner card: trophy, "WINNER" (or "IT'S A TIE!"), the name(s)
// in Cormorant, the winning score in brass, and "8 rounds · 4 players".
@Composable
private fun WinnerCard(winners: List<String>, score: Int, detail: String, strings: AppStrings) {
    FeltCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("winner_card")
            // Read as one announcement: "Winner, Alice, +428, 8 rounds · 4 players".
            .semantics(mergeDescendants = true) {}
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
        ) {
            Icon(
                imageVector        = Icons.Default.EmojiEvents,
                contentDescription = null, // decorative: the text announces the winner
                tint               = MaterialTheme.tarotColors.brassOnFelt,
                modifier           = Modifier.size(40.dp)
            )
            Text(
                text  = (if (winners.size > 1) strings.itsATie else strings.winner).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.tarotColors.brassOnFelt
            )
            Text(
                text      = winners.joinToString(" & "),
                style     = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            // On the felt the score is brass rather than green/red: it is always
            // the best score of the game, and brass stays readable on green.
            Text(
                text  = score.withSign(),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.tarotColors.brassOnFelt
            )
            Text(text = detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Final ranking: rank, avatar, name, score. Leaders (every co-winner) in brass.
@Composable
private fun RankingCard(standings: List<Standing>) {
    SalonCard(
        modifier       = Modifier.fillMaxWidth().testTag("ranking_card"),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column {
            standings.forEachIndexed { index, standing ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .then(
                            if (standing.isLeader) {
                                Modifier.background(MaterialTheme.tarotColors.winnerHighlight.copy(alpha = 0.5f))
                            } else Modifier
                        )
                        .padding(horizontal = Dimens.SpaceM)
                        .testTag("rank_${standing.name}"),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text     = standing.rank.toString(),
                        style    = MaterialTheme.typography.labelLarge,
                        color    = if (standing.isLeader) MaterialTheme.tarotColors.brassText
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(20.dp)
                    )
                    PlayerAvatar(name = standing.name, seatIndex = standing.seatIndex, size = AvatarSize.M)
                    Text(
                        text     = standing.name,
                        style    = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    ScoreText(score = standing.total, size = ScoreSize.M)
                }
                if (index < standings.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

// Line chart of every player's cumulative score, drawn on a Compose Canvas:
//   - one line per player, in that player's tone (same colour as their avatar),
//   - a dashed zero baseline,
//   - a tick under the baseline for every round, with a few round numbers,
//   - the highest and lowest values printed on the left.
@Composable
private fun ScoreChart(playerNames: List<String>, rounds: List<RoundResult>, description: String) {
    val series   = cumulativeSeries(playerNames, rounds)
    val (low, high) = chartBounds(series)
    val labels   = xAxisLabels(rounds.size)
    val tarot    = MaterialTheme.tarotColors
    val scheme   = MaterialTheme.colorScheme
    // A TextMeasurer lays out text so drawText() can paint it on the canvas.
    val measurer = rememberTextMeasurer()
    // labelMedium without letter spacing: labelSmall's wide tracking is for overlines.
    val labelStyle = MaterialTheme.typography.labelMedium.copy(
        color         = scheme.onSurfaceVariant,
        letterSpacing = 0.sp
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .testTag("score_chart")
            // The canvas is a picture: describe it for screen readers.
            .semantics { contentDescription = description }
    ) {
        val left   = 40.dp.toPx()   // room for the y labels
        val bottom = 20.dp.toPx()   // room for the x labels
        val top    = 8.dp.toPx()
        val plotW  = size.width - left - 4.dp.toPx()
        val plotH  = size.height - bottom - top

        // Maps a round index / score to canvas coordinates.
        fun x(i: Int): Float = left + plotW * i / rounds.size.coerceAtLeast(1)
        fun y(v: Int): Float = top + plotH * (high - v) / (high - low).toFloat()

        // Zero baseline (dashed hairline).
        drawLine(
            color       = scheme.outline,
            start       = Offset(left, y(0)),
            end         = Offset(left + plotW, y(0)),
            strokeWidth = 1.dp.toPx(),
            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )
        // Round ticks along the bottom, with a few round numbers.
        val axisY = top + plotH
        for (i in 1..rounds.size) {
            drawLine(scheme.outline, Offset(x(i), axisY), Offset(x(i), axisY + 4.dp.toPx()), 1.dp.toPx())
        }
        for (r in labels) {
            val text = measurer.measure(r.toString(), labelStyle)
            drawText(text, topLeft = Offset(x(r) - text.size.width / 2f, axisY + 5.dp.toPx()))
        }
        // Y labels: highest, zero, lowest (skipping duplicates of zero).
        for (v in listOf(high, 0, low).distinct()) {
            val text = measurer.measure(v.withSign(), labelStyle)
            drawText(text, topLeft = Offset(0f, y(v) - text.size.height / 2f))
        }
        // One line per player, drawn segment by segment.
        series.forEachIndexed { seat, points ->
            val color = tarot.playerTone(seat).container
            for (i in 1 until points.size) {
                drawLine(
                    color       = color,
                    start       = Offset(x(i - 1), y(points[i - 1])),
                    end         = Offset(x(i), y(points[i])),
                    strokeWidth = 2.5.dp.toPx(),
                    cap         = StrokeCap.Round
                )
            }
        }
    }
}

// Colour key under the chart: a dot in each player's tone, then their name.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartLegend(playerNames: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        verticalArrangement   = Arrangement.spacedBy(Dimens.SpaceXs)
    ) {
        playerNames.forEachIndexed { seat, name ->
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MaterialTheme.tarotColors.playerTone(seat).container, CircleShape)
                )
                Text(text = name, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

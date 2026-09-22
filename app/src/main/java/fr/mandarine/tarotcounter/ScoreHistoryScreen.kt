package fr.mandarine.tarotcounter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.mandarine.tarotcounter.ui.theme.Dimens
import fr.mandarine.tarotcounter.ui.theme.tarotColors

// ── View mode enum ────────────────────────────────────────────────────────────

// The two display modes of the history screen:
//   TABLE — cumulative totals, one row per round, one column per player
//   LIST  — one card per round with its details, newest first
enum class HistoryViewMode { TABLE, LIST }

// Localized label of each mode (a UI detail, so kept private to this file).
private fun HistoryViewMode.label(strings: AppStrings) = when (this) {
    HistoryViewMode.TABLE -> strings.historyViewTable
    HistoryViewMode.LIST  -> strings.historyViewList
}

/**
 * The Score history screen (Salon restyle, issue #202):
 *
 * ```
 * ┌──────────────────────────────┐
 * │ ←  Score history             │  SalonTopBar
 * │ (   Table    |    List     ) │  segmented toggle
 * │ ┌──────────────────────────┐ │  TABLE view
 * │ │ Round (A)   (B)   (C)    │ │  sticky header with avatars
 * │ │       Alice Bob  Chloé   │ │  leader column tinted brass
 * │ │  1    +50   -25   -25    │ │  hairline rows, tabular figures
 * │ │  2    +20   -10   -10    │ │  5 players on a narrow phone → scrolls sideways
 * │ └──────────────────────────┘ │
 * │                              │  LIST view: one card per round, newest first
 * │ ┌──────────────────────────┐ │
 * │ │ (R2) (B) Bob · Small      │ │
 * │ │      1 bouts · 38 pts [Lost] -26 │
 * │ └──────────────────────────┘ │
 * └──────────────────────────────┘
 * ```
 *
 * Both views show an empty state before the first round.
 *
 * @param playerNames  Player display names in seat order.
 * @param roundHistory Completed rounds, oldest first.
 * @param onBack       Called when the user taps the back arrow.
 */
@Composable
fun ScoreHistoryScreen(
    playerNames: List<String>,
    roundHistory: List<RoundResult>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale  = LocalAppLocale.current
    val strings = appStrings(locale)

    // The active view; TABLE each time the screen opens.
    var viewMode by remember { mutableStateOf(HistoryViewMode.TABLE) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Not scrollable itself: the table and the list each scroll inside the space
        // left under the top bar and the toggle, so the table header can stay pinned.
        Column(
            modifier = Modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxWidth()
                .fillMaxSize()
                .padding(horizontal = Dimens.ScreenMargin),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
        ) {
            SalonTopBar(title = strings.scoreHistory, onBack = onBack)

            // ── View toggle ───────────────────────────────────────────────────
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
                        icon     = {},
                        colors   = salonSegmentedButtonColors(),
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

            // ── Content ───────────────────────────────────────────────────────
            when {
                roundHistory.isEmpty() -> HistoryEmptyState(strings.historyEmpty)
                viewMode == HistoryViewMode.TABLE -> ScoreTableView(
                    playerNames  = playerNames,
                    roundHistory = roundHistory,
                    strings      = strings,
                    // weight(fill = false): take at most the remaining height, so a
                    // short table stays compact and a long one scrolls inside it.
                    modifier     = Modifier.weight(1f, fill = false)
                )
                else -> RoundListView(
                    playerNames  = playerNames,
                    roundHistory = roundHistory,
                    locale       = locale,
                    strings      = strings,
                    modifier     = Modifier.weight(1f)
                )
            }
        }
    }
}

// Shown in both views before the first round: a muted chart icon and one sentence.
@Composable
private fun HistoryEmptyState(message: String) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceXl)
            .testTag("history_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
    ) {
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.ShowChart,
            contentDescription = null, // decorative
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(40.dp)
        )
        Text(
            text      = message,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── TABLE view ────────────────────────────────────────────────────────────────

// Cumulative totals in a paper card. The header row (avatars + names) is sticky:
// it stays pinned while the rounds scroll underneath. When the player columns
// would be narrower than 64 dp (5 players on a small phone), every column keeps
// 64 dp and the whole table scrolls sideways instead.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScoreTableView(
    playerNames: List<String>,
    roundHistory: List<RoundResult>,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val rows    = buildScoreTableData(playerNames, roundHistory)
    val leaders = leaderColumns(playerNames, roundHistory)

    SalonCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        // BoxWithConstraints tells us how wide the card is, to decide on scrolling.
        BoxWithConstraints(modifier = Modifier.testTag("history_table")) {
            val scrolls = historyTableScrolls(maxWidth.value, playerNames.size)
            // null = share the width equally; a value = fixed width + sideways scroll.
            val playerColumn: Dp? = if (scrolls) HISTORY_MIN_PLAYER_COLUMN_DP.dp else null
            val tableWidth = if (scrolls) {
                (HISTORY_ROUND_COLUMN_DP + playerNames.size * HISTORY_MIN_PLAYER_COLUMN_DP).dp
            } else maxWidth

            Box(
                modifier = if (scrolls) Modifier.horizontalScroll(rememberScrollState()) else Modifier
            ) {
                // LazyColumn only composes the visible rows; stickyHeader pins row 0.
                LazyColumn(modifier = Modifier.width(tableWidth)) {
                    stickyHeader {
                        HistoryHeaderRow(playerNames, leaders, playerColumn, strings.roundColumn)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                    itemsIndexed(rows) { index, row ->
                        HistoryDataRow(playerNames, leaders, playerColumn, row)
                        if (index < rows.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

// Background of a leader's column: brass at low opacity (replaces the old orange).
@Composable
private fun leaderTint(isLeader: Boolean): Color =
    if (isLeader) MaterialTheme.tarotColors.winnerHighlight.copy(alpha = 0.6f) else Color.Transparent

@Composable
private fun HistoryHeaderRow(
    playerNames: List<String>,
    leaders: Set<String>,
    playerColumn: Dp?,
    roundLabel: String
) {
    // The paper background keeps the pinned header opaque over scrolled rows.
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(HISTORY_ROUND_COLUMN_DP.dp), contentAlignment = Alignment.Center) {
            AutoSizeText(
                text  = roundLabel,
                style = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center)
            )
        }
        // Player columns: a fixed width when the table scrolls sideways, else an equal share.
        playerNames.forEachIndexed { seat, name ->
            Column(
                modifier            = (if (playerColumn != null) Modifier.width(playerColumn) else Modifier.weight(1f))
                    .background(leaderTint(name in leaders))
                    .padding(vertical = Dimens.SpaceS, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
            ) {
                PlayerAvatar(name = name, seatIndex = seat, size = AvatarSize.S)
                AutoSizeText(
                    text  = name,
                    style = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center)
                )
            }
        }
    }
}

@Composable
private fun HistoryDataRow(
    playerNames: List<String>,
    leaders: Set<String>,
    playerColumn: Dp?,
    row: ScoreRowData
) {
    Row(
        modifier          = Modifier.fillMaxWidth().heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // cells[0] is the round number; cells[1..] are the players' totals.
        Text(
            text      = row.cells.first(),
            style     = MaterialTheme.typography.labelMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier  = Modifier.width(HISTORY_ROUND_COLUMN_DP.dp)
        )
        playerNames.forEachIndexed { i, name ->
            val value = row.scoreValues[i + 1] ?: 0
            Box(
                modifier         = (if (playerColumn != null) Modifier.width(playerColumn) else Modifier.weight(1f))
                    .heightIn(min = 40.dp)
                    .background(leaderTint(name in leaders)),
                contentAlignment = Alignment.Center
            ) {
                // scoreColor: green ≥ 0, red < 0; the theme's tabular figures align digits.
                Text(
                    text  = row.cells[i + 1],
                    style = MaterialTheme.typography.bodyMedium,
                    color = scoreColor(value)
                )
            }
        }
    }
}

// ── LIST view ─────────────────────────────────────────────────────────────────

// One card per round, newest first.
@Composable
private fun RoundListView(
    playerNames: List<String>,
    roundHistory: List<RoundResult>,
    locale: AppLocale,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val newestFirst = roundHistory.asReversed()
    LazyColumn(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        contentPadding      = PaddingValues(bottom = Dimens.SpaceL)
    ) {
        items(newestFirst.size) { i ->
            RoundCard(newestFirst[i], playerNames, locale, strings)
        }
    }
}

// A round card:
//   (R4)  (B) Bob · Guard                    +96
//         2 bouts · 47 pts   [Won]
// Skipped rounds are muted: "(R3)  Skipped".
@Composable
private fun RoundCard(
    round: RoundResult,
    playerNames: List<String>,
    locale: AppLocale,
    strings: AppStrings
) {
    val contract = round.contract
    val muted    = MaterialTheme.colorScheme.onSurfaceVariant
    SalonCard(
        modifier       = Modifier.fillMaxWidth().testTag("round_card_${round.roundNumber}"),
        contentPadding = PaddingValues(horizontal = Dimens.SpaceM, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoundBadge(strings.roundBadge(round.roundNumber))
            if (contract == null) {
                Text(
                    text     = strings.skipped,
                    style    = MaterialTheme.typography.bodyLarge,
                    color    = muted,
                    modifier = Modifier.weight(1f).testTag("round_indicator_skipped")
                )
            } else {
                PlayerAvatar(
                    name      = round.takerName,
                    seatIndex = playerNames.indexOf(round.takerName),
                    size      = AvatarSize.M
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text     = "${round.takerName} · ${contract.localizedName(locale)}",
                        style    = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)
                    ) {
                        round.details?.let { d ->
                            Text(
                                text  = strings.boutsPoints(d.bouts, d.points),
                                style = MaterialTheme.typography.bodySmall,
                                color = muted
                            )
                        }
                        OutcomeChip(won = round.won == true, strings = strings)
                    }
                }
                round.playerScores[round.takerName]?.let { ScoreText(score = it, size = ScoreSize.M) }
            }
        }
    }
}

// "R4" in a small pill on the track colour.
@Composable
private fun RoundBadge(text: String) {
    Box(
        modifier         = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// "Won" on a felt tint or "Lost" on a red tint.
@Composable
private fun OutcomeChip(won: Boolean, strings: AppStrings) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape        = CircleShape,
        color        = if (won) scheme.primaryContainer else scheme.errorContainer,
        contentColor = if (won) scheme.onPrimaryContainer else scheme.onErrorContainer,
        modifier     = Modifier.testTag(if (won) "round_indicator_won" else "round_indicator_lost")
    ) {
        Text(
            text     = if (won) strings.wonShort else strings.lostShort,
            style    = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

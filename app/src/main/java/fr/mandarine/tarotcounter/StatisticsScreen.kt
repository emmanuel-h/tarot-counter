package fr.mandarine.tarotcounter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import fr.mandarine.tarotcounter.ui.theme.Dimens
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme

// StatisticsScreen (issue #229): one paper card per player, computed from every
// saved game by computePlayerStats() (PlayerStats.kt).
//
//   ┌──────────────────────────────────┐
//   │ ←  Statistics                    │  SalonTopBar
//   │ Based on 7 saved games           │
//   │ ┌──────────────────────────────┐ │
//   │ │ (A) Alice                    │ │  avatar + name
//   │ │ GAMES  WINS   TAKES  AVG SCORE│ │  four key figures
//   │ │   7      3     24 %    +45   │ │
//   │ │ ──────────────────────────── │ │
//   │ │ Success by contract          │ │
//   │ │ Prise        3/4 made   75 % │ │
//   │ │ Garde        1/2 made   50 % │ │
//   │ │ Garde Sans             –     │ │
//   │ └──────────────────────────────┘ │
//   │ ┌── Bob … ─────────────────────┐ │
//   └──────────────────────────────────┘
//
// games:  every saved game (newest first, as stored).
// onBack: back arrow and system back → home screen.
@Composable
fun StatisticsScreen(
    games: List<SavedGame>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale  = LocalAppLocale.current
    val strings = appStrings(locale)

    // The system back button behaves like the back arrow.
    BackHandler(onBack = onBack)

    // Pure, unit-tested computation (PlayerStats.kt).
    val stats   = computePlayerStats(games)
    val counted = countedGames(games)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenMargin)
                .padding(bottom = Dimens.SpaceXl),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL)
        ) {
            SalonTopBar(
                title                  = strings.statistics,
                onBack                 = onBack,
                backContentDescription = strings.mainMenu
            )

            if (stats.isEmpty()) {
                // Empty state: nothing has been saved yet.
                Text(
                    text      = strings.statsEmpty,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().testTag("stats_empty")
                )
            } else {
                Text(
                    text  = strings.statsBasedOn(counted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // The avatar colour follows the card's position in the list.
                stats.forEachIndexed { index, player ->
                    PlayerStatsCard(player = player, seatIndex = index, strings = strings, locale = locale)
                }
            }
        }
    }
}

// One player's card: header, four key figures, then success by contract.
@Composable
private fun PlayerStatsCard(
    player: PlayerStats,
    seatIndex: Int,
    strings: AppStrings,
    locale: AppLocale
) {
    // `?.let { … } ?: fallback` formats the number when there is one, else shows "–".
    fun percent(value: Int?): String = value?.let { "$it %" } ?: strings.statNone

    SalonCard(modifier = Modifier.fillMaxWidth().testTag("stats_${playerKey(player.name)}")) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
        ) {
            PlayerAvatar(name = player.name, seatIndex = seatIndex, size = AvatarSize.M)
            Text(
                text     = player.name,
                style    = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Four equal columns: label on top, value below.
        Row(modifier = Modifier.fillMaxWidth()) {
            KeyFigure(strings.statGames, player.gamesPlayed.toString(), Modifier.weight(1f))
            KeyFigure(strings.statWins, player.wins.toString(), Modifier.weight(1f))
            KeyFigure(strings.statTakeRate, percent(player.takePercent), Modifier.weight(1f))
            Column(
                modifier            = Modifier.weight(1f).semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FigureLabel(strings.statAverage)
                ScoreText(score = player.averageScore, size = ScoreSize.M)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(text = strings.statSuccessByContract, style = MaterialTheme.typography.titleSmall)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
            for ((contract, c) in player.contracts) {
                Row(
                    modifier          = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = contract.localizedName(locale),
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    // Never-taken contracts show only "–" (no "0/0 made").
                    if (c.taken > 0) {
                        Text(
                            text  = strings.statMadeOutOf(c.won, c.taken),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text      = percent(c.successPercent),
                        style     = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.End,
                        modifier  = Modifier.widthIn(min = Dimens.SpaceXl * 2)
                    )
                }
            }
        }
    }
}

// A key figure: small muted label above a bold value, read as one item by TalkBack.
@Composable
private fun KeyFigure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FigureLabel(label)
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

// Upper-case muted overline used above each key figure.
@Composable
private fun FigureLabel(label: String) {
    Text(
        text     = label.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewStatsGames = listOf(
    SavedGame(
        id = "1", datestamp = 0L,
        playerNames = listOf("Alice", "Bob", "Chloé"),
        rounds = listOf(
            RoundResult(1, "Alice", Contract.GARDE, null, true,
                mapOf("Alice" to 120, "Bob" to -60, "Chloé" to -60)),
            RoundResult(2, "Bob", Contract.PRISE, null, false,
                mapOf("Alice" to 30, "Bob" to -60, "Chloé" to 30))
        ),
        finalScores = mapOf("Alice" to 150, "Bob" to -120, "Chloé" to -30)
    )
)

@Preview(heightDp = 1200)
@Composable
private fun StatisticsScreenPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    TarotCounterTheme(darkTheme = dark) {
        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) {
            StatisticsScreen(games = previewStatsGames, onBack = {})
        }
    }
}

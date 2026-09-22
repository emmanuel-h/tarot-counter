package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import fr.mandarine.tarotcounter.ui.theme.Dimens
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import fr.mandarine.tarotcounter.ui.theme.tarotColors

// LandingScreen is the home screen (Salon redesign, issue #197). From top to bottom:
//
//   ┌──────────────────────────────┐
//   │ Tarot Counter            [⚙] │  SalonTopBar: wordmark + settings
//   │ ┌── felt card ─────────────┐ │  only when a game is in progress
//   │ │ GAME IN PROGRESS   (A)(B)│ │
//   │ │ Round 5                  │ │
//   │ │ 4 players · Alice leads… │ │
//   │ │ [        Resume        ] │ │
//   │ └──────────────────────────┘ │
//   │ ═════════ ♠ ♥ ♦ ♣ ═════════  │  SuitDivider
//   │ ┌── New game ──────────────┐ │  SalonCard
//   │ │ Players       (3 | 4 | 5)│ │
//   │ │ (A) Alice                │ │  PlayerNameField per seat
//   │ │ First dealer (Random|Choose)│
//   │ │ [      Start game      ] │ │
//   │ └──────────────────────────┘ │
//   │ Past games                   │  SectionHeader + one row per game
//   └──────────────────────────────┘
//
// onStartGame:          called when the user presses "Start game".
//                         names      : raw names entered by the user (blank = use fallback).
//                         dealerIndex: index of the chosen first dealer in `names`, or null
//                                      to let the app pick a random dealer.
// onResumeGame:         called when the user taps "Resume" — passes the saved state back
//                       to MainActivity so GameScreen can be initialized from it.
// onNavigateToSettings: called when the user taps the gear icon.
// inProgressGame:       a game that was interrupted mid-session, or null if there is none.
// pastGames:            list of completed games; defaults to empty for the @Preview below.
@Composable
fun LandingScreen(
    modifier: Modifier = Modifier,
    inProgressGame: InProgressGame? = null,
    pastGames: List<SavedGame> = emptyList(),
    onStartGame: (names: List<String>, dealerIndex: Int?) -> Unit = { _, _ -> },
    onResumeGame: (InProgressGame) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    // Read the active locale from the composition tree.
    val locale  = LocalAppLocale.current
    val strings = appStrings(locale)

    // Box fills the whole screen and centres the content column horizontally, so on a
    // 10" tablet in landscape the column stays 600 dp wide instead of stretching.
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // `imePadding()` shrinks the column by the keyboard height when it is open, and
        // `verticalScroll` lets the user scroll to any name field above the keyboard.
        Column(
            modifier = Modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenMargin)
                .padding(bottom = Dimens.SpaceXl),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL)
        ) {
            // ── Top bar: wordmark on the left, settings on the right ──────────
            SalonTopBar(
                title      = strings.appTitle,
                titleStyle = MaterialTheme.typography.headlineLarge,
                actions    = listOf(
                    TopBarAction(Icons.Default.Settings, strings.settings, onNavigateToSettings)
                )
            )

            // ── Resume card — first thing on screen when a game is in progress ──
            if (inProgressGame != null) {
                ResumeGameCard(
                    game     = inProgressGame,
                    strings  = strings,
                    onResume = { onResumeGame(inProgressGame) }
                )
                SuitDivider()
            }

            // ── New game ──────────────────────────────────────────────────────
            NewGameCard(strings = strings, locale = locale, onStartGame = onStartGame)

            // ── Past games ────────────────────────────────────────────────────
            if (pastGames.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                    SectionHeader(title = strings.pastGames)
                    for (game in pastGames) {
                        PastGameRow(game = game, strings = strings, locale = locale)
                    }
                }
            }
        }
    }
}

// The felt-green card shown when a game was interrupted. It answers three questions
// at a glance: which round, who is playing (avatar stack), and who is leading.
@Composable
private fun ResumeGameCard(
    game: InProgressGame,
    strings: AppStrings,
    onResume: () -> Unit
) {
    // Who leads right now — null before the first scored round.
    val leaders = currentLeaders(game.playerNames, game.rounds)
    val detail  = buildList {
        add(strings.playerCount(game.playerNames.size))
        add(
            if (leaders != null) strings.leaderLine(leaders.names, leaders.score.withSign())
            else strings.noRoundsPlayed
        )
    }.joinToString(" · ")

    FeltCard(modifier = Modifier.fillMaxWidth().testTag("resume_card")) {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)
            ) {
                // Small brass overline, upper case with wide letter spacing (labelSmall).
                Text(
                    text  = strings.gameInProgress.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.tarotColors.brassOnFelt
                )
                Text(
                    text  = strings.roundHeader(game.currentRound),
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text  = detail,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // The ring colour matches the felt, so each avatar looks cut out of the card.
            AvatarStack(
                names     = game.playerNames,
                size      = AvatarSize.M,
                ringColor = MaterialTheme.tarotColors.felt
            )
        }
        // An ivory pill on the felt: the inverse of the usual felt-green button.
        AppButton(
            text     = strings.resume,
            onClick  = onResume,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.tarotColors.onFelt,
                contentColor   = MaterialTheme.tarotColors.felt
            )
        )
    }
}

// The "New game" card: player count, one name field per seat, first dealer, Start.
// All the setup state lives here, so recomposing the rest of the screen never
// resets what the user typed.
@Composable
private fun NewGameCard(
    strings: AppStrings,
    locale: AppLocale,
    onStartGame: (names: List<String>, dealerIndex: Int?) -> Unit
) {
    // `remember` keeps a value alive across recompositions (UI redraws).
    // `mutableIntStateOf` creates an integer that, when changed, triggers a redraw.
    var selectedPlayers by remember { mutableIntStateOf(3) }

    // `mutableStateListOf` creates an observable list: any change triggers a redraw.
    // Starts with 3 empty strings, matching the default player count.
    val playerNames = remember { mutableStateListOf("", "", "") }

    // `useRandomDealer` toggles between random (true, default) and manual (false).
    // `selectedDealerIndex` is the chosen dealer's seat when in manual mode.
    var useRandomDealer by remember { mutableStateOf(true) }
    var selectedDealerIndex by remember { mutableIntStateOf(0) }

    // Blank fields fall back to "Player N", so two blank fields never clash and a
    // typed "Player 1" does clash with a blank first field.
    val resolvedNames = playerNames.mapIndexed { i, name ->
        name.ifBlank { strings.playerFallback(i + 1) }
    }
    // Duplicates are detected case-insensitively ("alice" == "ALICE").
    val lowerNames     = resolvedNames.map { it.lowercase() }
    val duplicateFlags = lowerNames.map { name -> lowerNames.count { it == name } > 1 }
    val hasDuplicates  = duplicateFlags.any { it }

    SalonCard(modifier = Modifier.fillMaxWidth(), title = strings.newGame) {

        // ── Player count: label on the left, 3 | 4 | 5 on the right ───────────
        LabeledRow(label = strings.playersLabel) {
            val playerCountOptions = listOf(3, 4, 5)
            val playerLabelSize    = rememberSharedAutoSizeState()
            SingleChoiceSegmentedButtonRow(modifier = Modifier.width(SETUP_TOGGLE_WIDTH)) {
                playerCountOptions.forEachIndexed { index, n ->
                    SegmentedButton(
                        shape    = SegmentedButtonDefaults.itemShape(index, playerCountOptions.size),
                        selected = selectedPlayers == n,
                        onClick  = {
                            selectedPlayers = n
                            // Grow or shrink the name list to the new player count.
                            while (playerNames.size < n) playerNames.add("")
                            while (playerNames.size > n) playerNames.removeAt(playerNames.lastIndex)
                            // A chosen dealer index could now be out of range: reset it.
                            selectedDealerIndex = 0
                        },
                        icon     = {},
                        colors   = salonSegmentedButtonColors()
                    ) {
                        AutoSizeText(
                            text            = n.toString(),
                            modifier        = Modifier.padding(horizontal = 1.dp),
                            sharedSizeState = playerLabelSize
                        )
                    }
                }
            }
        }

        // ── One name field per seat, with that seat's avatar ─────────────────
        // The duplicate-name error is shown inline, right under the clashing field.
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            for (i in playerNames.indices) {
                PlayerNameField(
                    value          = playerNames[i],
                    onValueChange  = { playerNames[i] = it },
                    seatIndex      = i,
                    placeholder    = strings.playerFallback(i + 1),
                    isError        = duplicateFlags[i],
                    supportingText = if (duplicateFlags[i]) strings.nameAlreadyUsed else null,
                    modifier       = Modifier
                        .fillMaxWidth()
                        .testTag("player_name_field_$i")
                )
            }
        }

        // ── First dealer: Random | Choose ─────────────────────────────────────
        LabeledRow(label = strings.dealerSelectionLabel) {
            // Keyed on the locale so both labels are re-measured after a language change.
            val dealerModeSize = rememberSharedAutoSizeState(locale)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.width(SETUP_TOGGLE_WIDTH)) {
                listOf(true to strings.randomDealer, false to strings.chooseDealer)
                    .forEachIndexed { index, (random, label) ->
                        SegmentedButton(
                            shape    = SegmentedButtonDefaults.itemShape(index, 2),
                            selected = useRandomDealer == random,
                            onClick  = { useRandomDealer = random },
                            icon     = {},
                            colors   = salonSegmentedButtonColors()
                        ) {
                            AutoSizeText(
                                text            = label,
                                modifier        = Modifier.padding(horizontal = 1.dp),
                                sharedSizeState = dealerModeSize
                            )
                        }
                    }
            }
        }

        // In "Choose" mode, one segment per player lets the user tap the first dealer.
        if (!useRandomDealer) {
            val dealerNameSize = rememberSharedAutoSizeState(locale)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                resolvedNames.forEachIndexed { index, name ->
                    SegmentedButton(
                        shape    = SegmentedButtonDefaults.itemShape(index, resolvedNames.size),
                        selected = selectedDealerIndex == index,
                        onClick  = { selectedDealerIndex = index },
                        icon     = {},
                        colors   = salonSegmentedButtonColors()
                    ) {
                        AutoSizeText(
                            text            = name,
                            modifier        = Modifier.padding(horizontal = 1.dp),
                            sharedSizeState = dealerNameSize
                        )
                    }
                }
            }
        }

        // ── Start ─────────────────────────────────────────────────────────────
        // Disabled while two names clash; the inline errors above explain why.
        AppButton(
            text      = strings.startGame,
            onClick   = {
                val dealerIndex = if (useRandomDealer) null else selectedDealerIndex
                onStartGame(playerNames.toList(), dealerIndex)
            },
            enabled   = !hasDuplicates,
            modifier  = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.titleMedium
        )
    }
}

// Width of the compact toggles (player count, dealer mode) at the right of a row.
private val SETUP_TOGGLE_WIDTH = 176.dp

// A setting row inside the New game card: a muted label on the left and a
// control on the right. `content` is a slot — any composable can be passed in.
@Composable
private fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelLarge,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        content()
    }
}

// One completed game in the "Past games" list:
//   [trophy]  Alice                        +540
//             Sat 13 Sep · 5 players · 8 rounds
// A tie shows "Tie: Alice & Bob"; a game with no rounds shows no score.
@Composable
private fun PastGameRow(game: SavedGame, strings: AppStrings, locale: AppLocale) {
    // `findWinners` returns several names on a tie, none when nobody scored.
    val winners = findWinners(game.finalScores)
    val title = when {
        game.rounds.isEmpty() || winners.isEmpty() -> strings.noRoundsPlayed
        winners.size == 1                           -> winners.first()
        else                                        -> strings.tieResult(winners.joinToString(" & "))
    }
    val subtitle = listOf(
        formatGameDate(game.datestamp, locale),
        strings.playerCount(game.playerNames.size),
        strings.roundCount(game.rounds.size)
    ).joinToString(" · ")
    // The winning score, shown only when there really is a winner.
    val winningScore = if (game.rounds.isNotEmpty() && winners.isNotEmpty()) {
        game.finalScores[winners.first()]
    } else null

    SalonCard(
        modifier       = Modifier.fillMaxWidth().testTag("past_game_${game.id}"),
        contentPadding = PaddingValues(horizontal = Dimens.SpaceM, vertical = 12.dp)
    ) {
        Row(
            // 48 dp minimum height: comfortable to scan and to tap in a future detail view.
            modifier              = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.EmojiEvents,
                contentDescription = null, // decorative: the winner's name says it all
                tint               = MaterialTheme.tarotColors.brass,
                modifier           = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = title,
                    style    = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (winningScore != null) {
                ScoreText(score = winningScore, size = ScoreSize.M)
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewInProgress = InProgressGame(
    gameId        = "p",
    playerNames   = listOf("Alice", "Bruno", "Chloé", "David"),
    currentRound  = 5,
    startingIndex = 0,
    rounds        = listOf(
        RoundResult(1, "Alice", Contract.GARDE, null, true,
            mapOf("Alice" to 312, "Bruno" to -104, "Chloé" to -104, "David" to -104))
    )
)

private val previewPastGames = listOf(
    SavedGame(
        id = "1", datestamp = System.currentTimeMillis(),
        playerNames = listOf("Alice", "Bob", "Charlie"),
        rounds = previewInProgress.rounds,
        finalScores = mapOf("Alice" to 150, "Bob" to -75, "Charlie" to -75)
    ),
    SavedGame(
        id = "2", datestamp = System.currentTimeMillis() - 86_400_000,
        playerNames = listOf("Alice", "Bob", "Charlie", "Dave"),
        rounds = previewInProgress.rounds,
        finalScores = mapOf("Alice" to 50, "Bob" to 50, "Charlie" to -50, "Dave" to -50)
    )
)

@Preview(heightDp = 1400)
@Composable
private fun LandingScreenPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    TarotCounterTheme(darkTheme = dark) {
        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) {
            LandingScreen(inProgressGame = previewInProgress, pastGames = previewPastGames)
        }
    }
}

// Tablet landscape: the content stays centred at 600 dp instead of stretching.
@Preview(showBackground = true, device = Devices.PIXEL_TABLET, widthDp = 1032, heightDp = 800)
@Composable
private fun LandingScreenTabletLandscapePreview() {
    TarotCounterTheme {
        LandingScreen(inProgressGame = previewInProgress, pastGames = previewPastGames)
    }
}

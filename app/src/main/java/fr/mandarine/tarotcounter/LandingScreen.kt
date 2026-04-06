package fr.mandarine.tarotcounter

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale as JavaLocale

// LandingScreen lets the user configure how many players there are and enter their names.
// It also shows:
//   - a theme toggle (☀️ / 🌙) in the top-left corner
//   - a language switcher (🇬🇧 / 🇫🇷) in the top-right corner
//   - a "Resume Game" card (if there is an unfinished game saved from a previous session)
//   - a "Past Games" list at the bottom (if any games have been completed)
//
// onStartGame:     lambda called when the user presses "Start Game" with a list of names.
// onResumeGame:    lambda called when the user taps "Resume" — passes the saved state back
//                  to MainActivity so GameScreen can be initialized from it.
// onLocaleChange:  lambda called when the user taps a flag to switch language.
// onThemeChange:   lambda called when the user taps ☀️ or 🌙 to switch the theme.
// inProgressGame:  a game that was interrupted mid-session, or null if there is none.
// pastGames:       list of completed games; defaults to empty for the @Preview below.
@Composable
fun LandingScreen(
    modifier: Modifier = Modifier,
    inProgressGame: InProgressGame? = null,
    pastGames: List<SavedGame> = emptyList(),
    onStartGame: (List<String>) -> Unit = {},
    onResumeGame: (InProgressGame) -> Unit = {},
    onLocaleChange: (AppLocale) -> Unit = {},
    onThemeChange: (AppTheme) -> Unit = {}
) {
    // Read the active locale and theme from the composition tree.
    val locale  = LocalAppLocale.current
    val theme   = LocalAppTheme.current
    val strings = appStrings(locale)

    // Context is needed to fire an Android Intent (e.g. open the email client).
    // LocalContext.current gives us the nearest Activity/Context in the Compose tree.
    val context = LocalContext.current

    // `remember` keeps a value alive across recompositions (UI redraws).
    // `mutableIntStateOf` creates an integer that, when changed, triggers a redraw.
    var selectedPlayers by remember { mutableIntStateOf(3) }

    // `mutableStateListOf` creates an observable list: any change triggers a UI redraw.
    // Initialized with 3 empty strings matching the default player count.
    val playerNames = remember { mutableStateListOf("", "", "") }

    // Column stacks children vertically. `verticalScroll` makes it scrollable
    // in case the content (name fields + button) doesn't fit on smaller screens.
    // `imePadding()` shrinks this Column by the keyboard height when the IME is open.
    // `Arrangement.Top` is the correct choice for scrollable columns: centering fights
    // with overflow and can clip content when the keyboard reduces the available height.
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        // ── Header row: theme toggle (left) + language switcher (right) ──────────
        // Both sets of chips use Material3 FilterChip — the selected chip gets a
        // filled background; the unselected one has only an outlined border.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // push groups to each edge
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // ── Theme chips (left) ────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = theme == AppTheme.LIGHT,
                    onClick  = { onThemeChange(AppTheme.LIGHT) },
                    label    = { Text("☀️") }
                )
                FilterChip(
                    selected = theme == AppTheme.DARK,
                    onClick  = { onThemeChange(AppTheme.DARK) },
                    label    = { Text("🌙") }
                )
            }

            // ── Language chips (right) ────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = locale == AppLocale.EN,
                    onClick  = { onLocaleChange(AppLocale.EN) },
                    label    = { Text("🇬🇧") }
                )
                FilterChip(
                    selected = locale == AppLocale.FR,
                    onClick  = { onLocaleChange(AppLocale.FR) },
                    label    = { Text("🇫🇷") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Decorative card-suit row ───────────────────────────────────────────
        // The four French tarot suit symbols serve as a thematic header above the
        // app title, giving the screen a card-game identity at a glance.
        // `displaySmall` is a large, airy text style — perfect for decorative glyphs.
        Text(
            text = "♠  ♥  ♦  ♣",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Text displays a string. MaterialTheme.typography gives us pre-defined
        // text styles that match Material Design (headlineLarge is a big bold title).
        Text(
            text = strings.appTitle,
            style = MaterialTheme.typography.headlineLarge
        )

        // ── Resume card ───────────────────────────────────────────────────────
        // Shown prominently at the top when the user closed the app mid-game.
        if (inProgressGame != null) {
            Spacer(modifier = Modifier.height(24.dp))
            ResumeGameCard(
                game = inProgressGame,
                strings = strings,
                onResume = { onResumeGame(inProgressGame) }
            )
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = strings.numberOfPlayers,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Row places its children side by side horizontally.
        // `spacedBy(8.dp)` adds 8dp of space between each chip.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Loop from 3 to 5 (inclusive) to create one chip per player count.
            for (n in 3..5) {
                // FilterChip is a selectable chip from Material Design 3.
                // `selected` controls whether this chip appears highlighted.
                FilterChip(
                    selected = selectedPlayers == n,
                    onClick = {
                        selectedPlayers = n
                        // Resize the name list to match the new player count.
                        // If the new count is larger, pad with empty strings.
                        // If smaller, drop the extra entries from the end.
                        while (playerNames.size < n) playerNames.add("")
                        while (playerNames.size > n) playerNames.removeAt(playerNames.lastIndex)
                    },
                    label = { Text(n.toString()) } // chip label: "3", "4", or "5"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Resolve display names: blank fields fall back to the localized "Player N" equivalent.
        // This ensures that leaving two fields blank is treated as a duplicate.
        val resolvedNames = playerNames.mapIndexed { i, name ->
            name.ifBlank { strings.playerFallback(i + 1) }
        }

        // Build a set of lower-cased names to detect duplicates case-insensitively.
        val lowerNames = resolvedNames.map { it.lowercase() }

        // `duplicateFlags[i]` is true when the same resolved name appears more than once.
        val duplicateFlags = lowerNames.map { name -> lowerNames.count { it == name } > 1 }

        // The button is disabled and a warning is shown whenever any duplicate exists.
        val hasDuplicates = duplicateFlags.any { it }

        Text(
            text = strings.playerNamesLabel,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Loop over each player slot and render a text field for their name.
        for (i in playerNames.indices) {
            // OutlinedTextField is a Material Design text input with a visible border.
            // `isError` turns the border red when this slot's name conflicts with another.
            // `supportingText` shows a small hint below the field (only when there is an error).
            OutlinedTextField(
                value = playerNames[i],
                onValueChange = { playerNames[i] = it }, // `it` is the new string the user typed
                label = { Text(strings.playerFallback(i + 1)) },
                singleLine = true,                        // prevent multi-line input
                isError = duplicateFlags[i],
                supportingText = if (duplicateFlags[i]) {
                    { Text(strings.nameAlreadyUsed) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth(0.8f)                   // 80% of screen width
                    .padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // "Start Game" button placed BELOW the name fields so the visual flow naturally
        // guides the user: enter names first, then press Start.
        // `enabled = !hasDuplicates` prevents starting a game when names clash.
        AppButton(
            text     = strings.startGame,
            onClick  = { onStartGame(playerNames.toList()) },
            enabled  = !hasDuplicates,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        // ── Past Games ────────────────────────────────────────────────────────
        // Only shown when there is at least one saved game on the device.
        if (pastGames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(40.dp))
            // A divider with generous vertical padding clearly separates the
            // setup section (above) from the historical games list (below).
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Spacer(modifier = Modifier.height(16.dp))

            // titleLarge gives the section heading more visual weight than titleMedium,
            // improving the hierarchy between the section label and the cards below it.
            Text(
                text = strings.pastGames,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            for (game in pastGames) {
                PastGameCard(game = game, strings = strings)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ── Feedback button ───────────────────────────────────────────────────
        // Always at the very bottom of the scrollable column, below Past Games.
        // Right-aligned so it doesn't compete with the centred "Start Game" CTA.
        // TextButton (no fill, no border) keeps it subtle; the envelope icon makes
        // its purpose instantly recognisable without reading the label.
        // We use TextButton directly here (rather than AppTextButton) because we need
        // to place an Icon alongside AutoSizeText inside the button content slot.
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End // push the button to the right edge
        ) {
            TextButton(
                onClick = {
                    // ACTION_SENDTO + "mailto:" URI opens the default email app.
                    // Using SENDTO (rather than ACTION_SEND) ensures only email clients
                    // respond to this intent — messaging apps are excluded.
                    val intent = Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("mailto:mandarinetech.dev@gmail.com")
                    )
                    context.startActivity(Intent.createChooser(intent, null))
                }
            ) {
                // Icon on the left, label on the right, with 4 dp gap between them.
                Icon(
                    imageVector        = Icons.Default.Email,
                    contentDescription = null, // label next to it already describes the action
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                AutoSizeText(text = strings.feedbackButton)
            }
        }
    }
}

// ResumeGameCard is shown when there is an unfinished game saved from a previous session.
//
// It uses `primaryContainer` as its background to stand out from the "Past Games" cards
// below, signalling that this is an active action rather than passive history.
//
// Tapping "Resume" calls onResume, which navigates straight into GameScreen with
// the saved state (player names, round history, starting index).
@Composable
private fun ResumeGameCard(
    game: InProgressGame,
    strings: AppStrings,
    onResume: () -> Unit
) {
    val roundsPlayed = game.rounds.size
    // Builds e.g. "Round 3 · 2 rounds played" using the localized templates.
    val roundLabel = strings.roundsPlayed(roundsPlayed)

    // Capture primary color here so it can be used inside the Box below.
    // Composable functions can only be called from within a @Composable scope,
    // so we read MaterialTheme values before we enter the Card content lambda.
    val accentColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        // Elevated shadow makes the Resume card stand out from the Past Games list below
        // and signals that it represents an active, time-sensitive action.
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        // Box layers its children on top of each other.
        // The first child (the colored strip) sits at the start edge;
        // the second child (the content) fills the remaining space.
        Row(modifier = Modifier.fillMaxWidth()) {
            // A narrow vertical strip in the primary color acts as an accent border
            // on the left side of the card, giving it extra visual emphasis.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(120.dp)           // tall enough to cover typical card content
                    .background(accentColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)               // fill remaining horizontal space after the strip
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // titleMedium (vs the previous labelLarge) gives the card a clear heading
                // that's immediately readable at a glance — matching the visual weight
                // of a section title rather than a small chip label.
                Text(
                    text = strings.resumeGameTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    // Show which players are in the game.
                    text = game.playerNames.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    // Show how far the game has progressed, e.g. "Round 4 · 3 rounds played".
                    text = strings.resumeRoundDetail(game.currentRound, roundLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppButton(
                    text     = strings.resume,
                    onClick  = onResume,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }   // end Row (accent strip + content)
    }
}

// PastGameCard displays a summary of one completed game.
//
// It shows:
//   - the player names separated by commas
//   - the winner's name and final score (or "Tie" if there were multiple winners)
//   - how many rounds were played
//   - the date the game was saved
@Composable
private fun PastGameCard(game: SavedGame, strings: AppStrings) {
    // Compute the winner(s) from the final scores that were saved with the game.
    // `findWinners` returns a list to handle the case where two players are tied.
    val winners = findWinners(game.finalScores)

    // Build a human-readable winner line using the localized string templates.
    val winnerText = when {
        winners.isEmpty() -> strings.noRoundsPlayed
        winners.size == 1 -> {
            val score = game.finalScores[winners.first()] ?: 0
            // Prepend "+" for positive scores so the sign is always explicit.
            val sign = if (score >= 0) "+" else ""
            strings.winnerResult(winners.first(), sign, score)
        }
        else -> strings.tieResult(winners.joinToString(" & "))
    }

    // Format the timestamp as a readable date (e.g. "23/03/2026").
    // `JavaLocale.getDefault()` ensures the format follows the user's regional settings.
    val dateStr = SimpleDateFormat("dd/MM/yyyy", JavaLocale.getDefault())
        .format(Date(game.datestamp))

    val roundCount = game.rounds.size

    // Card draws a rounded, elevated surface — a good visual container for a list item.
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Player names on the first line.
            Text(
                text = game.playerNames.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            // Winner (or tie) on the second line, with a small trophy icon on the left.
            // Row arranges the icon and text side by side, vertically centered.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Only show the trophy when there is an actual winner (not a tie or no rounds).
                // winners.size == 1 means a single player came out on top.
                if (winners.size == 1) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null, // decorative icon — screen readers skip it
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = winnerText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // Round count and date on the third line, separated by a dot.
            Text(
                text = "${strings.roundCount(roundCount)} · $dateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// @Preview lets Android Studio render this composable in the IDE without running the app.
@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
    TarotCounterTheme {
        LandingScreen()
    }
}

// Preview with sample past games so we can see the "Past Games" section in the IDE.
@Preview(showBackground = true)
@Composable
fun LandingScreenWithHistoryPreview() {
    TarotCounterTheme {
        LandingScreen(
            pastGames = listOf(
                SavedGame(
                    id = "1",
                    datestamp = System.currentTimeMillis(),
                    playerNames = listOf("Alice", "Bob", "Charlie"),
                    rounds = emptyList(),
                    finalScores = mapOf("Alice" to 150, "Bob" to -75, "Charlie" to -75)
                ),
                SavedGame(
                    id = "2",
                    datestamp = System.currentTimeMillis() - 86_400_000, // yesterday
                    playerNames = listOf("Alice", "Bob", "Charlie", "Dave"),
                    rounds = emptyList(),
                    finalScores = mapOf("Alice" to 50, "Bob" to 50, "Charlie" to -50, "Dave" to -50)
                )
            )
        )
    }
}

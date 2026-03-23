package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// LandingScreen lets the user configure how many players there are and enter their names.
// It also shows:
//   - a "Resume Game" card (if there is an unfinished game saved from a previous session)
//   - a "Past Games" list at the bottom (if any games have been completed)
//
// onStartGame:     lambda called when the user presses "Start Game" with a list of names.
// onResumeGame:    lambda called when the user taps "Resume" — passes the saved state back
//                  to MainActivity so GameScreen can be initialized from it.
// inProgressGame:  a game that was interrupted mid-session, or null if there is none.
// pastGames:       list of completed games; defaults to empty for the @Preview below.
@Composable
fun LandingScreen(
    modifier: Modifier = Modifier,
    inProgressGame: InProgressGame? = null,
    pastGames: List<SavedGame> = emptyList(),
    onStartGame: (List<String>) -> Unit = {},
    onResumeGame: (InProgressGame) -> Unit = {}
) {
    // `remember` keeps a value alive across recompositions (UI redraws).
    // `mutableIntStateOf` creates an integer that, when changed, triggers a redraw.
    var selectedPlayers by remember { mutableIntStateOf(3) }

    // `mutableStateListOf` creates an observable list: any change triggers a UI redraw.
    // Initialized with 3 empty strings matching the default player count.
    val playerNames = remember { mutableStateListOf("", "", "") }

    // Column stacks children vertically. `verticalScroll` makes it scrollable
    // in case the content (name fields + button) doesn't fit on smaller screens.
    // `imePadding()` shrinks this Column by the keyboard height when the IME is open.
    // Combined with `verticalScroll`, this means all text fields remain reachable by
    // scrolling even while the keyboard is visible.
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
        // Text displays a string. MaterialTheme.typography gives us pre-defined
        // text styles that match Material Design (headlineLarge is a big bold title).
        Text(
            text = "Compteur de points",
            style = MaterialTheme.typography.headlineLarge
        )

        // ── Resume card ───────────────────────────────────────────────────────
        // Shown prominently at the top when the user closed the app mid-game.
        if (inProgressGame != null) {
            Spacer(modifier = Modifier.height(24.dp))
            ResumeGameCard(
                game = inProgressGame,
                onResume = { onResumeGame(inProgressGame) }
            )
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Number of players",
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

        Text(
            text = "Player names",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Loop over each player slot and render a text field for their name.
        // `playerNames.indices` gives us 0, 1, 2 (or up to 4 for 5 players).
        for (i in playerNames.indices) {
            // OutlinedTextField is a Material Design text input with a visible border.
            // `value` is the current text; `onValueChange` updates it when the user typed.
            OutlinedTextField(
                value = playerNames[i],
                onValueChange = { playerNames[i] = it }, // `it` is the new string the user typed
                label = { Text("Player ${i + 1}") },
                singleLine = true,                        // prevent multi-line input
                modifier = Modifier
                    .fillMaxWidth(0.8f)                   // 80% of screen width
                    .padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // "Start Game" button. Tapping it calls `onStartGame` with a snapshot of the
        // current names. `toList()` converts the mutable state list to a regular
        // immutable List<String> so it's safe to pass to another screen.
        Button(
            onClick = { onStartGame(playerNames.toList()) },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Start Game")
        }

        // ── Past Games ────────────────────────────────────────────────────────
        // Only shown when there is at least one saved game on the device.
        if (pastGames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(40.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Past Games",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            for (game in pastGames) {
                PastGameCard(game = game)
                Spacer(modifier = Modifier.height(8.dp))
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
private fun ResumeGameCard(game: InProgressGame, onResume: () -> Unit) {
    val roundsPlayed = game.rounds.size
    val roundLabel = if (roundsPlayed == 1) "1 round played" else "$roundsPlayed rounds played"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Resume Game",
                style = MaterialTheme.typography.labelLarge,
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
                text = "Round ${game.currentRound} · $roundLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onResume,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Resume")
            }
        }
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
private fun PastGameCard(game: SavedGame) {
    // Compute the winner(s) from the final scores that were saved with the game.
    // `findWinners` returns a list to handle the case where two players are tied.
    val winners = findWinners(game.finalScores)

    // Build a human-readable winner line.
    val winnerText = when {
        winners.isEmpty() -> "No rounds played"
        winners.size == 1 -> {
            val score = game.finalScores[winners.first()] ?: 0
            // Prepend "+" for positive scores so the sign is always explicit.
            val sign = if (score >= 0) "+" else ""
            "Winner: ${winners.first()} ($sign$score)"
        }
        else -> "Tie: ${winners.joinToString(" & ")}"
    }

    // Format the timestamp as a readable date (e.g. "23/03/2026").
    // `SimpleDateFormat` is the standard Java date formatter available on all API levels.
    // `Locale.getDefault()` ensures the format follows the user's regional settings.
    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        .format(Date(game.datestamp))

    val roundCount = game.rounds.size
    val roundLabel = if (roundCount == 1) "1 round" else "$roundCount rounds"

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
            // Winner (or tie) on the second line.
            Text(
                text = winnerText,
                style = MaterialTheme.typography.bodyMedium
            )
            // Round count and date on the third line, separated by a dot.
            Text(
                text = "$roundLabel · $dateStr",
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

package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// GameScreen handles the full round-by-round flow of a Tarot game.
//
// playerNames: the list of players set up on the previous screen.
// modifier: passed in from the parent (e.g. Scaffold padding).
@Composable
fun GameScreen(
    playerNames: List<String>,
    modifier: Modifier = Modifier
) {
    // Current round number — increases by 1 each time a round is completed.
    var currentRound by remember { mutableIntStateOf(1) }

    // Pick a random starting player index once when the game begins.
    // `remember { ... }` without a key only runs its block on the very first composition,
    // so this value stays fixed for the entire game session.
    val startingIndex = remember { playerNames.indices.random() }

    // Observable list of completed rounds — adding to it triggers a UI redraw.
    val roundHistory = remember { mutableStateListOf<RoundResult>() }

    // Returns the display name for a player: their typed name, or "Player N" if blank.
    // `ifBlank` returns the fallback string when the value is empty or whitespace-only.
    fun displayName(index: Int): String =
        playerNames[index].ifBlank { "Player ${index + 1}" }

    // Derive the current taker from the starting index and the round number.
    // `%` (modulo) wraps the index back to 0 once we've cycled through all players.
    // Example with 3 players starting at index 1 (Bob):
    //   Round 1 → (1 + 0) % 3 = 1 → Bob
    //   Round 2 → (1 + 1) % 3 = 2 → Charlie
    //   Round 3 → (1 + 2) % 3 = 0 → Alice
    //   Round 4 → (1 + 3) % 3 = 1 → Bob  (cycle restarts)
    val currentTakerIndex = (startingIndex + currentRound - 1) % playerNames.size
    val currentTaker = displayName(currentTakerIndex)

    // Records the outcome of the current round and advances to the next.
    fun recordRound(contract: Contract?) {
        roundHistory.add(RoundResult(currentRound, currentTaker, contract))
        currentRound++
    }

    // verticalScroll allows the screen to scroll if the content (especially history) overflows.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Round $currentRound",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Pick a contract ─────────────────────────────────────────────────────
        // The taker is already known (auto-assigned), so we go straight to contract selection.
        Text(
            text = "$currentTaker — choose a contract:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        // One button per contract, from weakest (Petite) to strongest (Garde Contre).
        // `Contract.entries` is the idiomatic Kotlin 1.9+ way to iterate all enum values.
        for (contract in Contract.entries) {
            Button(
                onClick = { recordRound(contract) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(contract.displayName)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Skip: records the round without a contract.
        // OutlinedButton has a less prominent style to visually distinguish it.
        OutlinedButton(
            onClick = { recordRound(null) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip round")
        }

        // ── Round history ───────────────────────────────────────────────────────
        // Only shown once at least one round is complete.
        if (roundHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()  // a thin horizontal line to separate sections
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "History",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Show rounds newest-first so the latest result is always at the top.
            for (round in roundHistory.reversed()) {
                val contractText = round.contract?.displayName ?: "Skipped"
                Text(
                    text = "Round ${round.roundNumber}: ${round.takerName} — $contractText",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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

    // The player selected as taker for this round.
    // null means we're still on step 1 (player selection).
    var selectedTaker by remember { mutableStateOf<String?>(null) }

    // Observable list of completed rounds — adding to it triggers a UI redraw.
    val roundHistory = remember { mutableStateListOf<RoundResult>() }

    // Returns the display name for a player: their typed name, or "Player N" if blank.
    // `ifBlank` returns the fallback string when the value is empty or whitespace-only.
    fun displayName(index: Int): String =
        playerNames[index].ifBlank { "Player ${index + 1}" }

    // Records the outcome of the current round and resets state for the next one.
    fun recordRound(takerName: String, contract: Contract?) {
        roundHistory.add(RoundResult(currentRound, takerName, contract))
        currentRound++
        selectedTaker = null  // reset so step 1 is shown again for the next round
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

        // The UI has two steps, toggled by whether `selectedTaker` is null or not.
        if (selectedTaker == null) {

            // ── Step 1: Pick the taker ──────────────────────────────────────────
            Text(
                text = "Who is taking?",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // One button per player. Tapping it advances to step 2.
            for (i in playerNames.indices) {
                Button(
                    onClick = { selectedTaker = displayName(i) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(displayName(i))
                }
            }

        } else {

            // ── Step 2: Pick a contract (or skip) ──────────────────────────────
            //
            // We capture `selectedTaker` into a local `taker` val here.
            // This is the idiomatic Kotlin way to handle a nullable that we know
            // is non-null inside an `else` branch — no `!!` (the crash operator) needed.
            // `?: return@Column` means "if somehow null, exit the Column lambda early".
            val taker = selectedTaker ?: return@Column

            Text(
                text = "$taker — choose a contract:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // One button per contract, from weakest (Petite) to strongest (Garde Contre).
            // `Contract.entries` is the idiomatic Kotlin 1.9+ way to iterate all enum values.
            for (contract in Contract.entries) {
                Button(
                    onClick = { recordRound(taker, contract) },
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
                onClick = { recordRound(taker, null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip round")
            }

            // TextButton is the least prominent style — used for secondary actions.
            // This lets the user go back and pick a different taker.
            TextButton(onClick = { selectedTaker = null }) {
                Text("← Change player")
            }
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

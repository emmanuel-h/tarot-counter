package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import java.util.UUID

// GameScreen handles the full round-by-round flow of a Tarot game.
//
// playerNames:      the list of players set up on the previous screen.
// inProgressGame:   if non-null, the screen restores from this saved state instead of
//                   starting fresh (used when the user taps "Resume" on the setup screen).
// onSaveProgress:   called after every completed or skipped round with the current state.
//                   The caller (MainActivity) forwards this to GameViewModel which persists it.
// onSaveGame:       called with the completed game data when the user taps "End Game".
//                   Saving happens at that moment — not when "New Game" is later pressed —
//                   so the game is persisted even if the app is closed on the Final Score screen.
// onEndGame:        called when the user presses "New Game"; navigates back to the setup screen.
// modifier:         passed in from the parent (e.g. Scaffold padding).
@Composable
fun GameScreen(
    playerNames: List<String>,
    inProgressGame: InProgressGame? = null,
    onSaveProgress: (InProgressGame) -> Unit = {},
    onSaveGame: (SavedGame) -> Unit = {},
    onEndGame: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Current round number — restored from the saved state when resuming, otherwise 1.
    var currentRound by remember { mutableIntStateOf(inProgressGame?.currentRound ?: 1) }

    // The index of the player who takes first. Restored when resuming so the rotation
    // continues seamlessly; chosen randomly for a fresh game.
    // `remember { ... }` without a key runs only on the first composition, so this
    // value stays fixed for the entire game session regardless of recompositions.
    val startingIndex = remember { inProgressGame?.startingIndex ?: playerNames.indices.random() }

    // Observable list of completed rounds — populated from the saved state when resuming.
    // `apply { }` runs the block on the new list and returns it, letting us fill it
    // inline inside the `remember` block.
    val roundHistory = remember {
        mutableStateListOf<RoundResult>().apply {
            inProgressGame?.rounds?.let { addAll(it) }
        }
    }

    // The contract chosen in step 1, waiting for details to be filled in step 2.
    // null = we are on step 1 (contract selection).
    // non-null = we are on step 2 (details form).
    var selectedContract by remember { mutableStateOf<Contract?>(null) }

    // Controls whether the score history table is shown instead of the game screen.
    // Toggled by the bar-chart icon button in the scoreboard section.
    var showScoreHistory by remember { mutableStateOf(false) }

    // Controls whether the final score screen is shown.
    // Set to true when the user taps "End Game" from any step.
    var showFinalScore by remember { mutableStateOf(false) }

    // Returns the display name for a player: their typed name, or "Player N" if blank.
    fun displayName(index: Int): String =
        playerNames[index].ifBlank { "Player ${index + 1}" }

    // Derive the current taker from the starting index and the round number.
    // `%` (modulo) wraps the index back to 0 once we've cycled through all players.
    // Example with 3 players starting at index 1 (Bob):
    //   Round 1 → (1 + 0) % 3 = 1 → Bob
    //   Round 2 → (1 + 1) % 3 = 2 → Charlie
    //   Round 3 → (1 + 2) % 3 = 0 → Alice  (cycle restarts)
    val currentTakerIndex = (startingIndex + currentRound - 1) % playerNames.size
    val currentTaker = displayName(currentTakerIndex)

    // Resolve the display names once so both steps use the same list.
    // `map` transforms each index into its display name.
    val displayNames = playerNames.indices.map { displayName(it) }

    // Records a played round (contract + details) and advances to the next round.
    fun recordPlayed(contract: Contract, details: RoundDetails) {
        // Check if the taker scored enough points for their bout count.
        val won = takerWon(details.bouts, details.points)
        // Compute the base score for this round (before player distribution).
        val roundScore = calculateRoundScore(contract, details.bouts, details.points)
        // Distribute the base score: taker pays/collects from all defenders;
        // in a 5-player game the partner shares with the taker.
        val baseScores = computePlayerScores(
            allPlayers  = displayNames,
            takerName   = currentTaker,
            partnerName = details.partnerName,
            won         = won,
            roundScore  = roundScore
        )
        // numDefenders is used by all three bonus calculations inside applyBonuses.
        // It is 3 in a 5-player game, (n−1) otherwise.
        val numDefenders = if (details.partnerName != null) 3 else displayNames.size - 1
        // Apply petit-au-bout, poignée, and chelem bonuses on top of the base scores.
        // All the bonus logic lives in GameModels so it can be unit-tested without Compose.
        val scores = applyBonuses(baseScores, contract, details, currentTaker, won, numDefenders)

        roundHistory.add(RoundResult(currentRound, currentTaker, contract, details, won, scores))
        currentRound++
        selectedContract = null  // return to step 1 for the next round

        // Persist the current game state so it can be resumed if the app is closed.
        // We save after incrementing currentRound so the restored state points to the
        // correct next taker (the formula uses currentRound to derive the taker index).
        onSaveProgress(InProgressGame(
            playerNames   = displayNames,
            currentRound  = currentRound,
            startingIndex = startingIndex,
            rounds        = roundHistory.toList()
        ))
    }

    // Records a skipped round (no contract, no details) and advances.
    // playerScores is empty — no points change on a skipped round.
    fun recordSkipped() {
        roundHistory.add(RoundResult(currentRound, currentTaker, contract = null, details = null, won = null))
        currentRound++
        selectedContract = null

        // Save progress after a skip just like after a played round.
        onSaveProgress(InProgressGame(
            playerNames   = displayNames,
            currentRound  = currentRound,
            startingIndex = startingIndex,
            rounds        = roundHistory.toList()
        ))
    }

    // Saves the completed game and shows the Final Score screen.
    //
    // Called by both End Game buttons (step 1 and step 2). Saving here — rather than
    // waiting for the user to press "New Game" — means the game is recorded even if
    // the app is closed while the Final Score screen is visible.
    //
    // `onSaveGame` (implemented in GameViewModel) also clears the in-progress entry, so
    // once the game is ended the resume card will not reappear unless new rounds are played.
    //
    // No-op save guard: if no rounds have been played yet there is nothing to record.
    fun endGame() {
        if (roundHistory.isNotEmpty()) {
            // UUID.randomUUID() gives every saved game a unique identifier so entries from
            // the same day or same players are still distinguishable in storage.
            val savedGame = SavedGame(
                id          = UUID.randomUUID().toString(),
                datestamp   = System.currentTimeMillis(),
                playerNames = displayNames,
                rounds      = roundHistory.toList(),
                finalScores = computeFinalTotals(displayNames, roundHistory)
            )
            onSaveGame(savedGame)
        }
        showFinalScore = true
    }

    // ── Step routing ─────────────────────────────────────────────────────────
    // Priority order:
    //   1. Final score screen  (user tapped "End Game")
    //   2. Score history table (user tapped the bar-chart icon)
    //   3. Round details form  (user selected a contract in step 1)
    //   4. Contract selection  (default step 1 view)

    // 1. Final score screen — shown when the user explicitly ends the game.
    if (showFinalScore) {
        FinalScoreScreen(
            playerNames = displayNames,
            roundHistory = roundHistory,
            // "Back to game" dismisses the final score screen and returns to the active round.
            onBack    = { showFinalScore = false },
            // "New Game" just navigates away — the game was already saved when the user
            // pressed "End Game" (see endGame() above).
            onNewGame = { onEndGame() },
            modifier  = modifier
        )
        return  // stop here — don't render anything below
    }

    // 2. Score history table.
    if (showScoreHistory) {
        ScoreHistoryScreen(
            playerNames = displayNames,
            roundHistory = roundHistory,
            onBack = { showScoreHistory = false },
            modifier = modifier
        )
        return  // stop here — don't render anything below
    }

    // 3. Details form (step 2).
    val contract = selectedContract
    if (contract != null) {
        // Step 2: fill in bouts, points, and bonuses.
        // We pass `modifier` so the Scaffold padding still applies.
        // `onShowHistory` is non-null when at least one round is recorded,
        // so the History button appears inside the form's header.
        RoundDetailsForm(
            takerName     = currentTaker,
            contract      = contract,
            playerNames   = displayNames,
            onConfirm     = { details -> recordPlayed(contract, details) },
            onBack        = { selectedContract = null },
            onShowHistory = if (roundHistory.isNotEmpty()) ({ showScoreHistory = true }) else null,
            onEndGame     = { endGame() },
            modifier      = modifier
        )
        return  // stop here — don't render the column below
    }

    // Step 1: contract selection.
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

        // Button row pinned below the round header.
        // - History: only shown once at least one round is recorded (nothing to see otherwise).
        // - End Game: always shown so the user can leave at any point during the game.
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (roundHistory.isNotEmpty()) {
                HistoryButton(onClick = { showScoreHistory = true })
                Spacer(modifier = Modifier.width(8.dp))
            }
            EndGameButton(onClick = { endGame() })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Pick a contract ───────────────────────────────────────────────────
        // The taker is already known (auto-assigned), so we go straight to contract selection.
        Text(
            text = "$currentTaker — choose a contract:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        // One button per contract, from weakest (Petite) to strongest (Garde Contre).
        // `Contract.entries` is the idiomatic Kotlin 1.9+ way to iterate all enum values.
        for (c in Contract.entries) {
            Button(
                // Selecting a contract moves to step 2 — details are collected there.
                onClick = { selectedContract = c },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(c.displayName)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Skip: records the round immediately without entering any details.
        // OutlinedButton has a less prominent style to visually distinguish it.
        OutlinedButton(
            onClick = { recordSkipped() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip round")
        }

        // ── Scoreboard & Round history ────────────────────────────────────────
        // Only shown once at least one round is complete.
        if (roundHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // ── Scoreboard ────────────────────────────────────────────────────
            // Cumulative score per player: sum of all their per-round scores.
            // A positive total means they are ahead; negative means they owe points.
            Text(
                text = "Scores",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            for (name in displayNames) {
                // `sumOf` adds up each round's score for this player (0 for skipped rounds).
                val total = roundHistory.sumOf { it.playerScores[name] ?: 0 }
                // Format the total with an explicit + or − sign for clarity.
                val sign = if (total >= 0) "+" else ""
                Text(
                    text = "$name: $sign$total",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // ── Round history ─────────────────────────────────────────────────
            Text(
                text = "History",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Show rounds newest-first so the latest result is always at the top.
            for (round in roundHistory.reversed()) {
                val contractText = round.contract?.displayName ?: "Skipped"
                // If the round was played, show bouts, points, and whether the taker won.
                val detailsText = round.details?.let { " · ${it.bouts} bouts · ${it.points} pts" } ?: ""
                // `won` is null for skipped rounds; otherwise show the outcome and taker's score.
                val takerScore = round.playerScores[round.takerName]
                val outcomeText = when (round.won) {
                    true  -> { val s = if (takerScore != null) " (+$takerScore)" else ""; " — Won$s" }
                    false -> { val s = if (takerScore != null) " ($takerScore)" else ""; " — Lost$s" }
                    null  -> ""
                }
                Text(
                    text = "Round ${round.roundNumber}: ${round.takerName} — $contractText$detailsText$outcomeText",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

// A tonal button with a bar-chart icon and "History" label.
//
// Using `FilledTonalButton` (Material 3) gives it a coloured background,
// rounded corners, and a ripple — making it unmistakably tappable compared
// to a plain `IconButton`. The icon has no content description because the
// adjacent "History" text already conveys the action to accessibility tools.
//
// This composable is used in both GameScreen (step 1 top bar)
// and RoundDetailsForm (step 2 header), so it lives here at package level
// and is accessible from all files in `fr.mandarine.tarotcounter`.
@Composable
fun HistoryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.BarChart,
            contentDescription = null,                    // "History" text label is sufficient
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text("History")
    }
}

// A tonal button with a flag icon and "End Game" label.
//
// Shown at all times in both step 1 (contract selection) and step 2 (RoundDetailsForm)
// so the user can stop the game at any point and review the final scores.
// Uses `FilledTonalButton` so it has the same visual weight as the History button.
@Composable
fun EndGameButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.Flag,
            contentDescription = null,                    // "End Game" text label is sufficient
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text("End Game")
    }
}

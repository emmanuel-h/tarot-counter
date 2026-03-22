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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

    // The contract chosen in step 1, waiting for details to be filled in step 2.
    // null = we are on step 1 (contract selection).
    // non-null = we are on step 2 (details form).
    var selectedContract by remember { mutableStateOf<Contract?>(null) }

    // Controls whether the score history table is shown instead of the game screen.
    // Toggled by the bar-chart icon button in the scoreboard section.
    var showScoreHistory by remember { mutableStateOf(false) }

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
        // Distribute the score: taker gets/pays for all defenders; partner shares in 5-player.
        val baseScores = computePlayerScores(
            allPlayers  = displayNames,
            takerName   = currentTaker,
            partnerName = details.partnerName,
            won         = won,
            roundScore  = roundScore
        )

        // numDefenders is used by all bonus calculations below.
        // It is 3 in a 5-player game, (n−1) otherwise.
        val numDefenders = if (details.partnerName != null) 3 else displayNames.size - 1

        // Apply the petit-au-bout bonus.
        // The bonus goes to whichever camp captured the Petit on the last trick,
        // regardless of who won the round.
        // Taker's camp = taker + partner; defenders' camp = everyone else.
        val pabAmount = if (details.petitAuBout != null) petitAuBoutBonus(contract) else 0
        // +1 if the achiever is in the taker's camp, -1 if they are a defender.
        val pabSign = when (details.petitAuBout) {
            null                -> 0
            currentTaker,
            details.partnerName -> +1  // taker's camp achieved it
            else                -> -1  // defenders' camp achieved it
        }
        val scoresAfterPab = if (pabAmount == 0) baseScores else {
            baseScores.mapValues { (player, score) ->
                when (player) {
                    currentTaker        -> score + pabSign * pabAmount * numDefenders
                    details.partnerName -> score  // partner unaffected
                    else                -> score - pabSign * pabAmount
                }
            }
        }

        // Apply the poignée (trump show) flat bonus.
        // The bonus always goes to the winning camp, regardless of who declared it:
        //   taker won  → taker collects pBonus from each defender
        //   taker lost → each defender collects pBonus from the taker
        // The partner (5-player) is not involved in the poignée bonus.
        val pBonus = poigneeBonus(details.poignee, details.doublePoignee, details.triplePoignee)
        val pSign  = if (won) 1 else -1  // positive = taker benefits, negative = defenders benefit
        val scoresAfterPoignee = if (pBonus == 0) scoresAfterPab else {
            scoresAfterPab.mapValues { (player, score) ->
                when (player) {
                    currentTaker        -> score + pSign * pBonus * numDefenders
                    details.partnerName -> score  // partner unaffected
                    else                -> score - pSign * pBonus
                }
            }
        }

        // Apply the chelem (grand slam) flat bonus on top of the previous result.
        // The bonus is paid individually between the taker and each defender.
        // The partner (5-player only) is not affected by the chelem bonus.
        val cBonus = chelemBonus(details.chelem)
        val scores = if (cBonus == 0) scoresAfterPoignee else {
            scoresAfterPoignee.mapValues { (player, score) ->
                when (player) {
                    currentTaker        -> score + cBonus * numDefenders // taker collects/pays all
                    details.partnerName -> score                         // partner unaffected
                    else                -> score - cBonus                // each defender pays/receives
                }
            }
        }

        roundHistory.add(RoundResult(currentRound, currentTaker, contract, details, won, scores))
        currentRound++
        selectedContract = null  // return to step 1 for the next round
    }

    // Records a skipped round (no contract, no details) and advances.
    // playerScores is empty — no points change on a skipped round.
    fun recordSkipped() {
        roundHistory.add(RoundResult(currentRound, currentTaker, contract = null, details = null, won = null))
        currentRound++
        selectedContract = null
    }

    // ── Step routing ─────────────────────────────────────────────────────────
    // Priority order:
    //   1. Score history table (user tapped the bar-chart icon)
    //   2. Round details form  (user selected a contract in step 1)
    //   3. Contract selection  (default step 1 view)

    // 1. Score history table.
    if (showScoreHistory) {
        ScoreHistoryScreen(
            playerNames = displayNames,
            roundHistory = roundHistory,
            onBack = { showScoreHistory = false },
            modifier = modifier
        )
        return  // stop here — don't render anything below
    }

    // 2. Details form (step 2).
    val contract = selectedContract
    if (contract != null) {
        // Step 2: fill in bouts, points, and bonuses.
        // We pass `modifier` so the Scaffold padding still applies.
        RoundDetailsForm(
            takerName   = currentTaker,
            contract    = contract,
            playerNames = displayNames,
            onConfirm   = { details -> recordPlayed(contract, details) },
            onBack      = { selectedContract = null },
            modifier    = modifier
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

        Spacer(modifier = Modifier.height(24.dp))

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
            //
            // The "Scores" label and the bar-chart icon are on the same line.
            // `SpaceBetween` pushes the icon to the far right of the row.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scores",
                    style = MaterialTheme.typography.titleSmall
                )
                // Tapping this icon opens the full score-history table screen.
                IconButton(onClick = { showScoreHistory = true }) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "View score history table"
                    )
                }
            }
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

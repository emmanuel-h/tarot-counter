package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.UUID

// GameScreen handles the full round-by-round flow of a Tarot game on a single scrollable page.
//
// All information is presented together: the compact scoreboard, the contract selection,
// and — once a contract is chosen — the scoring details form. There is no longer a separate
// step 2 screen; everything lives in one scrollable column.
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
@OptIn(ExperimentalLayoutApi::class)
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
    val startingIndex = remember { inProgressGame?.startingIndex ?: playerNames.indices.random() }

    // Observable list of completed rounds — populated from the saved state when resuming.
    val roundHistory = remember {
        mutableStateListOf<RoundResult>().apply {
            inProgressGame?.rounds?.let { addAll(it) }
        }
    }

    // The contract selected by tapping one of the contract chips.
    // null = no contract selected yet (details form is hidden).
    // non-null = a contract chip is active and the details form is shown below it.
    var selectedContract by remember { mutableStateOf<Contract?>(null) }

    // Controls whether the score history table overlay is shown.
    var showScoreHistory by remember { mutableStateOf(false) }

    // Controls whether the final score screen overlay is shown.
    var showFinalScore by remember { mutableStateOf(false) }

    // Returns the display name for a player: their typed name, or "Player N" if blank.
    fun displayName(index: Int): String =
        playerNames[index].ifBlank { "Player ${index + 1}" }

    // Derive the current taker from the starting index and the round number.
    val currentTakerIndex = (startingIndex + currentRound - 1) % playerNames.size
    val currentTaker = displayName(currentTakerIndex)

    // Resolve all display names once so both the scoreboard and form use the same list.
    val displayNames = playerNames.indices.map { displayName(it) }

    // Builds an InProgressGame snapshot from the current game state.
    fun progressSnapshot() = InProgressGame(
        playerNames   = displayNames,
        currentRound  = currentRound,
        startingIndex = startingIndex,
        rounds        = roundHistory.toList()
    )

    // Records a played round (contract + details) and advances to the next round.
    fun recordPlayed(contract: Contract, details: RoundDetails) {
        val won = takerWon(details.bouts, details.points)
        val roundScore = calculateRoundScore(contract, details.bouts, details.points)
        val baseScores = computePlayerScores(
            allPlayers  = displayNames,
            takerName   = currentTaker,
            partnerName = details.partnerName,
            won         = won,
            roundScore  = roundScore
        )
        val numDefenders = if (details.partnerName != null) 3 else displayNames.size - 1
        val scores = applyBonuses(baseScores, contract, details, currentTaker, won, numDefenders)
        roundHistory.add(RoundResult(currentRound, currentTaker, contract, details, won, scores))
        currentRound++
        selectedContract = null   // collapse the details form for the next round
        onSaveProgress(progressSnapshot())
    }

    // Records a skipped round (no contract, no details) and advances.
    fun recordSkipped() {
        roundHistory.add(RoundResult(currentRound, currentTaker, contract = null, details = null, won = null))
        currentRound++
        selectedContract = null
        onSaveProgress(progressSnapshot())
    }

    // Saves the completed game and shows the Final Score screen.
    fun endGame() {
        if (roundHistory.isNotEmpty()) {
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

    // ── Overlay screens ───────────────────────────────────────────────────────
    // These replace the whole content when active. The main game column is not rendered.

    if (showFinalScore) {
        FinalScoreScreen(
            playerNames  = displayNames,
            roundHistory = roundHistory,
            onBack       = { showFinalScore = false },
            onNewGame    = { onEndGame() },
            modifier     = modifier
        )
        return
    }

    if (showScoreHistory) {
        ScoreHistoryScreen(
            playerNames  = displayNames,
            roundHistory = roundHistory,
            onBack       = { showScoreHistory = false },
            modifier     = modifier
        )
        return
    }

    // ── Single-page game layout ───────────────────────────────────────────────
    // Everything lives in one scrollable column so the user always sees the scoreboard,
    // the contract chips, and — when a contract is selected — the details form, all
    // without navigating away.
    //
    // imePadding() shrinks the scrollable area when the keyboard opens (used for the
    // points text field), preventing the keyboard from covering content.
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Header: round number + action buttons ─────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Round $currentRound",
                style = MaterialTheme.typography.headlineMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (roundHistory.isNotEmpty()) {
                    HistoryButton(onClick = { showScoreHistory = true })
                    Spacer(Modifier.width(8.dp))
                }
                EndGameButton(onClick = { endGame() })
            }
        }

        // ── Compact scoreboard ────────────────────────────────────────────────
        // Shown after the first round so the user always has the current standings
        // in view without leaving the page.
        if (roundHistory.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            CompactScoreboard(displayNames = displayNames, roundHistory = roundHistory)
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // ── Contract selection ────────────────────────────────────────────────
        // FilterChips replace the old full-width buttons. Tapping a chip selects
        // that contract (and expands the details form below). Tapping the already-
        // selected chip deselects it and collapses the form.
        Text(
            text = "$currentTaker — choose a contract:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(8.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (c in Contract.entries) {
                FilterChip(
                    selected = selectedContract == c,
                    onClick  = { selectedContract = if (selectedContract == c) null else c },
                    label    = { Text(c.displayName) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Outlined (less prominent) so it is visually secondary to the contract chips.
        OutlinedButton(
            onClick  = { recordSkipped() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip round")
        }

        // ── Inline round details ──────────────────────────────────────────────
        // key(selectedContract) is a Compose primitive that discards and recreates
        // everything inside it whenever selectedContract changes. This automatically
        // resets bouts, points, and all the bonus state to their defaults whenever
        // the user picks a different contract — no manual reset needed.
        key(selectedContract) {
            val contract = selectedContract
            if (contract != null) {

                // ── Form state ────────────────────────────────────────────────
                // Declared inside key() so they are reset when the contract changes.
                var bouts         by remember { mutableIntStateOf(0) }
                var pointsText    by remember { mutableStateOf("") }
                var selectedPartner  by remember { mutableStateOf<String?>(null) }
                var petitAuBout   by remember { mutableStateOf<String?>(null) }
                var misere        by remember { mutableStateOf<String?>(null) }
                var doubleMisere  by remember { mutableStateOf<String?>(null) }
                var poignee       by remember { mutableStateOf<String?>(null) }
                var doublePoignee by remember { mutableStateOf<String?>(null) }
                var triplePoignee by remember { mutableStateOf<String?>(null) }
                var chelem        by remember { mutableStateOf(Chelem.NONE) }

                // Used to hide the software keyboard when the user taps "Done" on
                // the numeric keyboard after entering the points value.
                val keyboardController = LocalSoftwareKeyboardController.current

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // ── Bouts (oudlers) ───────────────────────────────────────────
                FormLabel("Number of bouts (oudlers)")
                Spacer(Modifier.height(8.dp))
                // FlowRow wraps chips to the next line if the row is too narrow.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (n in 0..3) {
                        FilterChip(
                            selected = bouts == n,
                            onClick  = { bouts = n },
                            label    = { Text(n.toString()) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Points ────────────────────────────────────────────────────
                FormLabel("Points scored by taker (0–91)")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { input ->
                        // Only accept up to two digits (max score is 91).
                        if (input.all { it.isDigit() } && input.length <= 2) {
                            pointsText = input
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction    = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    placeholder = { Text("0") },
                    singleLine  = true,
                    // fillMaxWidth(0.4f) gives a compact field — points are at most two digits.
                    modifier = Modifier.fillMaxWidth(0.4f)
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // ── Partner selection (5-player only) ─────────────────────────
                // In a 5-player game the taker calls a silent partner before the round.
                // The partner's identity affects how the round score is distributed.
                if (displayNames.size == 5) {
                    val partnerOptions = displayNames.filter { it != currentTaker }
                    PlayerChipSelector(
                        label          = "Partner (called by taker)",
                        selectedPlayer = selectedPartner,
                        playerNames    = partnerOptions,
                        onSelect       = { selectedPartner = it }
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                }

                // ── Player-assigned bonuses ────────────────────────────────────
                // Each bonus belongs to a specific player (or nobody).
                PlayerChipSelector("Petit au bout",   petitAuBout,   displayNames) { petitAuBout   = it }
                Spacer(Modifier.height(12.dp))
                PlayerChipSelector("Misère",          misere,        displayNames) { misere        = it }
                Spacer(Modifier.height(12.dp))
                PlayerChipSelector("Double misère",   doubleMisere,  displayNames) { doubleMisere  = it }
                Spacer(Modifier.height(12.dp))
                PlayerChipSelector("Poignée",         poignee,       displayNames) { poignee       = it }
                Spacer(Modifier.height(12.dp))
                PlayerChipSelector("Double poignée",  doublePoignee, displayNames) { doublePoignee = it }
                Spacer(Modifier.height(12.dp))
                PlayerChipSelector("Triple poignée",  triplePoignee, displayNames) { triplePoignee = it }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // ── Chelem (grand slam) ────────────────────────────────────────
                FormLabel("Chelem (grand slam)")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in Chelem.entries) {
                        FilterChip(
                            selected = chelem == c,
                            onClick  = { chelem = c },
                            label    = { Text(c.displayName) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Confirm / back ─────────────────────────────────────────────
                Button(
                    onClick = {
                        // Parse the typed points; default to 0 if empty, clamp to 0–91.
                        val points = pointsText.toIntOrNull()?.coerceIn(0, 91) ?: 0
                        recordPlayed(
                            contract,
                            RoundDetails(
                                bouts         = bouts,
                                points        = points,
                                // partnerName is only meaningful in 5-player games.
                                partnerName   = if (displayNames.size == 5) selectedPartner else null,
                                petitAuBout   = petitAuBout,
                                misere        = misere,
                                doubleMisere  = doubleMisere,
                                poignee       = poignee,
                                doublePoignee = doublePoignee,
                                triplePoignee = triplePoignee,
                                chelem        = chelem
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Confirm round")
                }

                // Secondary action: deselect the contract and collapse the form.
                TextButton(onClick = { selectedContract = null }) {
                    Text("← Change contract")
                }
            }
        }

        // ── Round history ─────────────────────────────────────────────────────
        // Displayed below the form so the full game log is always a single scroll away.
        // Shown newest-first so the most recent result is at the top of this section.
        if (roundHistory.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            for (round in roundHistory.reversed()) {
                val contractText = round.contract?.displayName ?: "Skipped"
                val detailsText  = round.details?.let { " · ${it.bouts} bouts · ${it.points} pts" } ?: ""
                val takerScore   = round.playerScores[round.takerName]
                val outcomeText  = when (round.won) {
                    true  -> { val s = if (takerScore != null) " (+$takerScore)" else ""; " — Won$s" }
                    false -> { val s = if (takerScore != null) " ($takerScore)" else ""; " — Lost$s" }
                    null  -> ""
                }
                Text(
                    text  = "Round ${round.roundNumber}: ${round.takerName} — $contractText$detailsText$outcomeText",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ── Compact scoreboard ────────────────────────────────────────────────────────

// Displays all players and their cumulative scores in a compact horizontal card.
// Each player gets a column: their name on top and their current total below.
// This is always visible at the top of the game page — no separate History screen needed.
@Composable
private fun CompactScoreboard(
    displayNames: List<String>,
    roundHistory: List<RoundResult>
) {
    // "Scores" label — required by the GameScreen spec and checked by tests.
    // fillMaxWidth() makes the text span the card width, so it aligns left naturally.
    Text(
        text  = "Scores",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(4.dp))

    // Card gives the scoreboard a visible background and subtle elevation.
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (name in displayNames) {
                // Sum every round's score for this player (0 for skipped rounds).
                val total = roundHistory.sumOf { it.playerScores[name] ?: 0 }
                val sign  = if (total >= 0) "+" else ""

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text     = name,
                        style    = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        // Ellipsize long names so the row stays on one line.
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text  = "$sign$total",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

// A tonal button with a bar-chart icon and "History" label.
// Used in the game screen header to open the full score history overlay.
@Composable
fun HistoryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector        = Icons.Default.BarChart,
            contentDescription = null,
            modifier           = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text("History")
    }
}

// A tonal button with a flag icon and "End Game" label.
// Always shown so the user can stop the game at any point.
@Composable
fun EndGameButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector        = Icons.Default.Flag,
            contentDescription = null,
            modifier           = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text("End Game")
    }
}

// ── Form helpers ──────────────────────────────────────────────────────────────

// A small bold label placed above a form section inside the details area.
// `private` limits its scope to this file.
@Composable
private fun FormLabel(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth()
    )
}

// Shows a "None" chip followed by one chip per player.
// Tapping a player assigns that player to the bonus; tapping the selected
// player again (or "None") clears the assignment.
//
// selectedPlayer: the currently assigned player name, or null if nobody is assigned.
// onSelect:       called with the new player name, or null to clear.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerChipSelector(
    label: String,
    selectedPlayer: String?,
    playerNames: List<String>,
    onSelect: (String?) -> Unit
) {
    FormLabel(label)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedPlayer == null,
            onClick  = { onSelect(null) },
            label    = { Text("None") }
        )
        for (name in playerNames) {
            // Tapping the already-selected player deselects them (null = nobody).
            FilterChip(
                selected = selectedPlayer == name,
                onClick  = { onSelect(if (selectedPlayer == name) null else name) },
                label    = { Text(name) }
            )
        }
    }
}

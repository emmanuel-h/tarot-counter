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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// RoundDetailsForm collects all scoring information for a played round.
//
// takerName:     display name of the player who took the hand this round.
// contract:      the contract they announced (already chosen on the previous screen).
// playerNames:   all player display names, used to build the player chip selectors.
// onConfirm:     called with the completed RoundDetails when the user taps "Confirm round".
// onBack:        called when the user taps "← Change contract" to go back.
// onShowHistory: when non-null, shows a History button in the header that calls this lambda.
//                Pass null when there is no history yet (i.e. this is the first round).
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoundDetailsForm(
    takerName: String,
    contract: Contract,
    playerNames: List<String>,
    onConfirm: (RoundDetails) -> Unit,
    onBack: () -> Unit,
    onShowHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // ── Form state ────────────────────────────────────────────────────────────
    // Each piece of state is independent — changing one doesn't reset the others.

    // Number of bouts (oudlers) in the taker's tricks: 0, 1, 2, or 3.
    var bouts by remember { mutableIntStateOf(0) }

    // Points as a text field so the user can type digits freely.
    // We parse and clamp it to 0–91 when the round is confirmed.
    var pointsText by remember { mutableStateOf("") }

    // Partner selection — only used in 5-player games.
    // The taker silently calls one other player as their partner for scoring purposes.
    // In 3- and 4-player games this stays null and no selector is shown.
    var selectedPartner by remember { mutableStateOf<String?>(null) }

    // Player-assigned bonuses — String? means either a player's name or null (nobody).
    var petitAuBout   by remember { mutableStateOf<String?>(null) }
    var misere        by remember { mutableStateOf<String?>(null) }
    var doubleMisere  by remember { mutableStateOf<String?>(null) }
    var poignee       by remember { mutableStateOf<String?>(null) }
    var doublePoignee by remember { mutableStateOf<String?>(null) }
    var triplePoignee by remember { mutableStateOf<String?>(null) }

    // Chelem outcome — defaults to NONE (no grand slam).
    var chelem by remember { mutableStateOf(Chelem.NONE) }

    // ── Layout ────────────────────────────────────────────────────────────────
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: shows who is taking and with which contract.
        Text(
            text = "$takerName — ${contract.displayName}",
            style = MaterialTheme.typography.headlineSmall
        )

        // History button — shown when at least one round has been recorded.
        // It sits right-aligned below the header, mirroring its placement in step 1.
        // When null, we keep the same total spacing as the original Spacer(24.dp).
        if (onShowHistory != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                HistoryButton(onClick = onShowHistory)
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Bouts (oudlers) ───────────────────────────────────────────────────
        SectionLabel("Number of bouts (oudlers)")
        Spacer(modifier = Modifier.height(8.dp))
        // FlowRow lays chips side by side and wraps to the next line if needed.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (n in 0..3) {
                FilterChip(
                    selected = bouts == n,
                    onClick = { bouts = n },
                    label = { Text(n.toString()) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // ── Points ────────────────────────────────────────────────────────────
        SectionLabel("Points scored by taker (0–91)")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pointsText,
            onValueChange = { input ->
                // Accept only digits, and at most 2 characters (91 is the max).
                if (input.all { it.isDigit() } && input.length <= 2) {
                    pointsText = input
                }
            },
            // keyboardType = Number shows a numeric keyboard on the phone.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("0") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.4f)
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // ── Partner (5-player only) ────────────────────────────────────────────
        // In a 5-player game the taker calls a silent partner before the round starts.
        // The partner's identity affects score distribution at the end of the round.
        // This section is hidden for 3- and 4-player games.
        if (playerNames.size == 5) {
            // Show all players except the taker as possible partners.
            val partnerOptions = playerNames.filter { it != takerName }
            PlayerChipSelector(
                label = "Partner (called by taker)",
                selectedPlayer = selectedPartner,
                playerNames = partnerOptions,
                onSelect = { selectedPartner = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Player-assigned bonuses ───────────────────────────────────────────
        // Each bonus can belong to any player, or to nobody (null).

        PlayerChipSelector(
            label = "Petit au bout",
            selectedPlayer = petitAuBout,
            playerNames = playerNames,
            onSelect = { petitAuBout = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlayerChipSelector(
            label = "Misère",
            selectedPlayer = misere,
            playerNames = playerNames,
            onSelect = { misere = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlayerChipSelector(
            label = "Double misère",
            selectedPlayer = doubleMisere,
            playerNames = playerNames,
            onSelect = { doubleMisere = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlayerChipSelector(
            label = "Poignée",
            selectedPlayer = poignee,
            playerNames = playerNames,
            onSelect = { poignee = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlayerChipSelector(
            label = "Double poignée",
            selectedPlayer = doublePoignee,
            playerNames = playerNames,
            onSelect = { doublePoignee = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlayerChipSelector(
            label = "Triple poignée",
            selectedPlayer = triplePoignee,
            playerNames = playerNames,
            onSelect = { triplePoignee = it }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // ── Chelem ────────────────────────────────────────────────────────────
        SectionLabel("Chelem (grand slam)")
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (c in Chelem.entries) {
                FilterChip(
                    selected = chelem == c,
                    onClick = { chelem = c },
                    label = { Text(c.displayName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Confirm button ────────────────────────────────────────────────────
        Button(
            onClick = {
                // Parse the points text; fall back to 0 if empty, clamp to 0–91.
                val points = pointsText.toIntOrNull()?.coerceIn(0, 91) ?: 0
                onConfirm(
                    RoundDetails(
                        bouts         = bouts,
                        points        = points,
                        // partnerName is only set in 5-player games; null otherwise.
                        partnerName   = if (playerNames.size == 5) selectedPartner else null,
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

        // Secondary action: go back and pick a different contract.
        TextButton(onClick = onBack) {
            Text("← Change contract")
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

// A small bold label placed above a form section.
// `private` means it can only be used inside this file.
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall
    )
}

// Shows a "None" chip followed by one chip per player.
// Tapping a player's chip assigns that player to the bonus.
// Tapping the already-selected player (or "None") clears the assignment.
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
    SectionLabel(label)
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // "None" chip — selected when no player is assigned.
        FilterChip(
            selected = selectedPlayer == null,
            onClick = { onSelect(null) },
            label = { Text("None") }
        )
        for (name in playerNames) {
            // Tapping a selected player deselects them (sets back to null).
            FilterChip(
                selected = selectedPlayer == name,
                onClick = { onSelect(if (selectedPlayer == name) null else name) },
                label = { Text(name) }
            )
        }
    }
}

package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlinx.coroutines.launch

// GameScreen handles the full round-by-round flow of a Tarot game on a single scrollable page.
//
// All information is presented together: the compact scoreboard, the contract selection,
// and — once a contract is chosen — the scoring details form. There is no separate
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
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    playerNames: List<String>,
    inProgressGame: InProgressGame? = null,
    onSaveProgress: (InProgressGame) -> Unit = {},
    onSaveGame: (SavedGame) -> Unit = {},
    onEndGame: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Read the active locale from the composition tree and resolve all strings once.
    val locale = LocalAppLocale.current
    val strings = appStrings(locale)

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

    // Returns the display name for a player: their typed name, or the localized
    // fallback (e.g. "Player 1" in English, "Joueur 1" in French) if blank.
    fun displayName(index: Int): String =
        playerNames[index].ifBlank { strings.playerFallback(index + 1) }

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
            onNewGame    = onEndGame,
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

    // Outer non-scrolling column: owns imePadding() so the entire layout
    // (scrollable content + bottom bar) shifts above the keyboard as a unit.
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Inner scrollable column: weight(1f) takes all vertical space above
        // the bottom action bar. Never use fillMaxSize() with weight().
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Header: history button | centred round number ────────────────────
            // Box lets us layer two Rows: one for the side buttons (SpaceBetween)
            // and one for the centered title, so the title is truly centered
            // regardless of the buttons' widths.
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Centered round label — always in the middle of the full width.
                Text(
                    text = strings.roundHeader(currentRound),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                // Side buttons sit in a Row that spans the full width.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // History button — only shown once at least one round has been recorded.
                    if (roundHistory.isNotEmpty()) {
                        HistoryButton(onClick = { showScoreHistory = true })
                    } else {
                        // Invisible placeholder keeps the round number centered even
                        // when the history button is not yet visible.
                        Spacer(Modifier.size(48.dp))
                    }
                    // Right-side placeholder mirrors the history button so the
                    // round number stays centred; End Game is in the bottom bar.
                    Spacer(Modifier.size(48.dp))
                }
            }

            // ── Compact scoreboard ────────────────────────────────────────────────
            // Shown after the first round so the user always has the current standings
            // in view without leaving the page.
            if (roundHistory.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                CompactScoreboard(
                    displayNames = displayNames,
                    roundHistory = roundHistory,
                    scoresLabel  = strings.scores
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // ── Contract selection ────────────────────────────────────────────────
            // FilterChips replace the old full-width buttons. Tapping a chip selects
            // that contract (and expands the details form below). Tapping the already-
            // selected chip deselects it and collapses the form.
            Text(
                text = strings.chooseContract(currentTaker),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in Contract.entries) {
                    FilterChip(
                        selected = selectedContract == c,
                        onClick  = { selectedContract = if (selectedContract == c) null else c },
                        label    = { Text(c.localizedName(locale)) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))


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
                    // When true the user enters the defenders' points instead of the taker's.
                    // The taker's points are derived on submit: takerPoints = 91 − defenderPoints.
                    var defenderMode  by remember { mutableStateOf(false) }
                    var selectedPartner  by remember { mutableStateOf<String?>(null) }
                    var petitAuBout   by remember { mutableStateOf<String?>(null) }
                    var poignee       by remember { mutableStateOf<String?>(null) }
                    var doublePoignee by remember { mutableStateOf<String?>(null) }
                    var triplePoignee by remember { mutableStateOf<String?>(null) }
                    var chelem        by remember { mutableStateOf(Chelem.NONE) }
                    // The player who called/achieved the chelem. Reset to null whenever chelem
                    // changes back to NONE (no chelem in this round).
                    var chelemPlayer  by remember { mutableStateOf<String?>(null) }

                    // Derived error flag — recomputed on every recomposition when pointsText changes.
                    // True only when the typed value parses to an integer that exceeds 91.
                    // An empty field is not an error (it defaults to 0 on Confirm).
                    // Declared here (not inside the Column) so both the TextField and the
                    // Confirm button can read the same value.
                    val pointsError = pointsText.toIntOrNull()?.let { it > 91 } == true

                    // Used to hide the software keyboard when the user taps "Done" on
                    // the numeric keyboard after entering the points value.
                    val keyboardController = LocalSoftwareKeyboardController.current

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // ── Bouts + Points side by side ───────────────────────────────
                    // Placing these in a Row cuts one section of vertical space compared
                    // to stacking them, helping everything fit on one screen.
                    // Alignment.Bottom ensures both halves share the same bottom edge,
                    // so the dropdown and the text field line up visually (issue #23).
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left half: bouts dropdown
                        // Replaced from FilterChips to an ExposedDropdownMenuBox to save
                        // screen space and improve UX (issue #9).
                        Column(modifier = Modifier.weight(1f)) {
                            FormLabel(strings.numberOfBouts)
                            Spacer(Modifier.height(8.dp))

                            // Tracks whether the dropdown menu is currently open.
                            var boutsExpanded by remember { mutableStateOf(false) }

                            // ExposedDropdownMenuBox is a Material3 combo box:
                            // - The text field shows the current selection and a trailing arrow.
                            // - Tapping it opens a menu with the four options (0–3).
                            ExposedDropdownMenuBox(
                                expanded         = boutsExpanded,
                                onExpandedChange = { boutsExpanded = !boutsExpanded },
                                modifier         = Modifier.testTag("bouts_dropdown")
                            ) {
                                OutlinedTextField(
                                    value          = bouts.toString(),
                                    onValueChange  = {},
                                    readOnly       = true,
                                    // The trailing chevron icon flips when the menu opens.
                                    trailingIcon   = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = boutsExpanded)
                                    },
                                    colors         = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    singleLine     = true,
                                    // menuAnchor() links the text field to its popup menu.
                                    modifier       = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded         = boutsExpanded,
                                    onDismissRequest = { boutsExpanded = false }
                                ) {
                                    // One menu item per valid bout count (0 through 3).
                                    for (n in 0..3) {
                                        DropdownMenuItem(
                                            text             = { Text(n.toString()) },
                                            onClick          = {
                                                bouts         = n
                                                boutsExpanded = false
                                            },
                                            contentPadding   = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }
                        }
                        // Right half: points entry — segmented toggle stacked above text field
                        Column(modifier = Modifier.weight(1f)) {
                            // ── Camp toggle ────────────────────────────────────────
                            // The two segments let the user pick which camp's points to type.
                            // Selecting "Defenders" is a convenience — the taker's points are
                            // derived on confirm as: takerPoints = 91 − defenderPoints.
                            // `fillMaxWidth` makes the toggle use the same width as the field below.
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = !defenderMode,
                                    onClick  = {
                                        defenderMode = false
                                        pointsText   = ""  // clear field when switching camps
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) {
                                    // maxLines = 1 and softWrap = false together prevent the label from
                                    // ever breaking onto a second line, regardless of screen width.
                                    // This fixes the French "Défenseurs" wrapping bug on narrow screens.
                                    Text(strings.attackerMode, maxLines = 1, softWrap = false)
                                }
                                SegmentedButton(
                                    selected = defenderMode,
                                    onClick  = {
                                        defenderMode = true
                                        pointsText   = ""  // clear field when switching camps
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Text(strings.defenderMode, maxLines = 1, softWrap = false)
                                }
                            }
                            OutlinedTextField(
                                value = pointsText,
                                onValueChange = { input ->
                                    // Accept only digit characters and at most two of them
                                    // (the highest valid value, 91, has two digits).
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
                                // Show the valid range directly in the placeholder so the
                                // user knows what values are accepted without wasting vertical
                                // space on a separate supporting-text hint.
                                placeholder     = { Text("0-91") },
                                // When the value is out of range, mark the field red and
                                // show a descriptive error message below it.
                                isError         = pointsError,
                                supportingText  = if (pointsError) ({
                                    Text(
                                        text  = strings.pointsOutOfRange,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }) else null,
                                singleLine      = true,
                                // testTag lets UI tests identify and interact with this field.
                                modifier        = Modifier.fillMaxWidth().testTag("points_input")
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // ── Partner selection (5-player only) ─────────────────────────
                    // In a 5-player game the taker calls a silent partner before the round.
                    // The partner's identity affects how the round score is distributed.
                    if (displayNames.size == 5) {
                        val partnerOptions = displayNames.filter { it != currentTaker }
                        PlayerChipSelector(
                            label          = strings.partnerCalledByTaker,
                            noneLabel      = strings.noneOption,
                            selectedPlayer = selectedPartner,
                            playerNames    = partnerOptions,
                            onSelect       = { selectedPartner = it }
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))
                    }

                    // ── Player-assigned bonuses (compact grid) ─────────────────────
                    // Each row has a label + ⓘ info icon that shows a tooltip explaining
                    // the bonus and its point value.
                    CompactBonusGrid(
                        playerNames     = displayNames,
                        bonusLabels     = listOf(
                            strings.petit,
                            strings.poignee,
                            strings.doublePoignee,
                            strings.triplePoignee
                        ),
                        bonusTooltips   = listOf(
                            strings.petitTooltipBody,
                            strings.poigneeTooltipBody,
                            strings.doublePoigneeTooltipBody,
                            strings.triplePoigneeTooltipBody
                        ),
                        petitAuBout     = petitAuBout,    onPetit         = { petitAuBout   = it },
                        poignee         = poignee,         onPoignee       = { poignee       = it },
                        doublePoignee   = doublePoignee,   onDoublePoignee = { doublePoignee = it },
                        triplePoignee   = triplePoignee,   onTriplePoignee = { triplePoignee = it }
                    )

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // ── Chelem (grand slam) ────────────────────────────────────────
                    // The dropdown is self-labelled: it shows "Chelem" when nothing is
                    // selected (Chelem.NONE) and the chosen outcome's name otherwise.
                    // This removes the need for a separate section header above the field.
                    // The ⓘ tooltip icon is placed immediately to the right of the dropdown.

                    // Tracks whether the chelem dropdown menu is open.
                    var chelemExpanded by remember { mutableStateOf(false) }

                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        // ExposedDropdownMenuBox is the Material 3 combo-box pattern (same as bouts).
                        // weight(1f) lets it fill the row minus the ⓘ icon space.
                        ExposedDropdownMenuBox(
                            expanded         = chelemExpanded,
                            onExpandedChange = { chelemExpanded = !chelemExpanded },
                            modifier         = Modifier
                                .weight(1f)
                                .testTag("chelem_dropdown")
                        ) {
                            OutlinedTextField(
                                // Show the placeholder "Chelem" when no outcome is selected,
                                // or the chosen outcome's name when one is active.
                                value         = if (chelem == Chelem.NONE)
                                                    strings.chelemPlaceholder
                                                else
                                                    chelem.localizedName(locale),
                                onValueChange = {},
                                readOnly      = true,
                                trailingIcon  = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = chelemExpanded)
                                },
                                colors        = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                singleLine    = true,
                                modifier      = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded         = chelemExpanded,
                                onDismissRequest = { chelemExpanded = false }
                            ) {
                                for (c in Chelem.entries) {
                                    DropdownMenuItem(
                                        text           = { Text(c.localizedName(locale)) },
                                        onClick        = {
                                            // When the user picks a new chelem option, reset the
                                            // associated player — the previous selection is no longer valid.
                                            if (chelem != c) chelemPlayer = null
                                            chelem         = c
                                            chelemExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                        // ⓘ icon explains the chelem bonus amounts.
                        BonusInfoIcon(
                            title = strings.chelemLabel,
                            body  = strings.chelemTooltipBody
                        )
                    }

                    // ── Chelem player selector ─────────────────────────────────────
                    // Only shown when a non-NONE chelem outcome is selected. The user picks
                    // which player called or achieved the chelem — this player leads the first
                    // trick of the round, overriding the usual turn order.
                    //
                    // Available choices: taker + partner (5-player only). In 3/4-player games
                    // only the taker can call chelem, so the selector is still shown to make the
                    // association explicit, but the partner option is omitted.
                    if (chelem != Chelem.NONE) {
                        Spacer(Modifier.height(8.dp))
                        // The eligible players are the taker and — in 5-player — the partner.
                        val chelemCandidates = buildList {
                            add(currentTaker)
                            // In a 5-player game the partner (if chosen) can also call chelem.
                            // Using ?.let avoids a force-unwrap while preserving the same logic.
                            if (displayNames.size == 5) selectedPartner?.let { add(it) }
                        }
                        PlayerChipSelector(
                            label          = strings.chelemPlayerLabel,
                            noneLabel      = strings.noneOption,
                            selectedPlayer = chelemPlayer,
                            playerNames    = chelemCandidates,
                            onSelect       = { chelemPlayer = it }
                        )
                        // Informational note: the chelem caller plays first this round.
                        // This reminder is shown only when a specific player has been selected.
                        if (chelemPlayer != null &&
                            (chelem == Chelem.ANNOUNCED_REALIZED || chelem == Chelem.ANNOUNCED_NOT_REALIZED)) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text  = strings.chelemPlaysFirst(chelemPlayer!!),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Confirm / back ─────────────────────────────────────────────
                    Button(
                        // Disabled while the points field shows an error so the user cannot
                        // submit an out-of-range value.
                        enabled = !pointsError,
                        onClick = {
                            // Parse the typed points; default to 0 if empty, clamp to 0–91.
                            val enteredPoints = pointsText.toIntOrNull()?.coerceIn(0, 91) ?: 0
                            // When the user counted the defenders' points, convert to taker's points.
                            // The total points in a round always sum to 91:
                            //   takerPoints = 91 − defenderPoints
                            val points = if (defenderMode) 91 - enteredPoints else enteredPoints
                            recordPlayed(
                                contract,
                                RoundDetails(
                                    bouts         = bouts,
                                    points        = points,
                                    // partnerName is only meaningful in 5-player games.
                                    partnerName   = if (displayNames.size == 5) selectedPartner else null,
                                    petitAuBout   = petitAuBout,
                                    poignee       = poignee,
                                    doublePoignee = doublePoignee,
                                    triplePoignee = triplePoignee,
                                    chelem        = chelem,
                                    // chelemPlayer is null when chelem == NONE.
                                    chelemPlayer  = if (chelem == Chelem.NONE) null else chelemPlayer
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(strings.confirmRound)
                    }

                    // Secondary action: deselect the contract and collapse the form.
                    TextButton(onClick = { selectedContract = null }) {
                        Text(strings.changeContract)
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

                // reversed() so the latest round appears first (newest-first order).
                val reversedHistory = roundHistory.reversed()
                reversedHistory.forEachIndexed { index, round ->
                    RoundHistoryRow(round = round, locale = locale, strings = strings)
                    // Draw a thin divider between rows (but not after the last one).
                    if (index < reversedHistory.lastIndex) {
                        HorizontalDivider(
                            modifier  = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color     = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }  // end inner scrollable Column

        // ── Bottom action bar ─────────────────────────────────────────────────────
        // Persistent split row pinned below the scroll area. weight(1f) on each
        // button gives them exactly equal widths.
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left half: end the game and navigate to the Final Score screen.
            OutlinedButton(
                onClick  = { endGame() },
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.endGame)
            }
            // Right half: record a skipped round and advance to the next.
            Button(
                onClick  = { recordSkipped() },
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.skipRound)
            }
        }
    }  // end outer Column
}

// ── Compact scoreboard ────────────────────────────────────────────────────────

// Displays all players and their cumulative scores in a compact horizontal card.
// Each player gets a column: their name on top and their current total below.
// This is always visible at the top of the game page — no separate History screen needed.
@Composable
private fun CompactScoreboard(
    displayNames: List<String>,
    roundHistory: List<RoundResult>,
    scoresLabel: String
) {
    // The label is required by the spec and checked by tests.
    // fillMaxWidth() makes the text span the card width, so it aligns left naturally.
    Text(
        text  = scoresLabel,
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
                        style = MaterialTheme.typography.titleMedium,
                        // Green for positive/zero scores, red for negative — makes
                        // standings readable at a glance without reading the numbers.
                        color = scoreColor(total)
                    )
                }
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

// An icon-only button with a bar-chart icon for opening the score history overlay.
// Using IconButton (no text) keeps the header compact; the contentDescription
// ensures screen readers still announce the purpose of the button.
@Composable
fun HistoryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val strings = appStrings(LocalAppLocale.current)
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector        = Icons.Default.BarChart,
            // Accessible label read by TalkBack — same text that was previously the button label.
            contentDescription = strings.history
        )
    }
}

// ── Round history row ─────────────────────────────────────────────────────────

// A single styled row in the round history list.
//
// Layout:  [●]  Round N: Taker — Contract · details — Outcome
//
// The leading dot (●) is colored by outcome so the user can scan results at a
// glance without reading the text:
//   Won     → MaterialTheme.colorScheme.primary   (green)
//   Lost    → MaterialTheme.colorScheme.error     (red)
//   Skipped → MaterialTheme.colorScheme.onSurfaceVariant (muted grey)
//
// All colors come from Material theme tokens — never hardcoded hex values —
// so they automatically adapt to light vs. dark mode.
//
// The `testTag` on the indicator dot lets UI tests assert the correct color
// semantic without reading color values directly.
@Composable
private fun RoundHistoryRow(
    round:   RoundResult,
    locale:  AppLocale,
    strings: AppStrings
) {
    // ── Build text segments ────────────────────────────────────────────────
    val contractText = round.contract?.localizedName(locale) ?: strings.skipped
    val detailsText  = round.details?.let {
        strings.boutsPointsDetail(it.bouts, it.points)
    } ?: ""
    val takerScore   = round.playerScores[round.takerName]
    val outcomeText  = when (round.won) {
        true  -> {
            val s = if (takerScore != null) " (+$takerScore)" else ""
            strings.wonOutcome(s)
        }
        false -> {
            val s = if (takerScore != null) " ($takerScore)" else ""
            strings.lostOutcome(s)
        }
        null  -> ""
    }

    // ── Choose indicator color and test tag by outcome ─────────────────────
    // `Pair<Color, String>` groups the color token with the test tag so
    // the when expression is compact and both values stay in sync.
    val (indicatorColor, indicatorTag) = when (round.won) {
        true  -> MaterialTheme.colorScheme.primary         to "round_indicator_won"
        false -> MaterialTheme.colorScheme.error           to "round_indicator_lost"
        null  -> MaterialTheme.colorScheme.onSurfaceVariant to "round_indicator_skipped"
    }

    // ── Row: indicator dot + history text ──────────────────────────────────
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Colored ● indicator — the key visual cue for won/lost/skipped.
        // `bodyMedium` matches the text size so the dot aligns with the
        // first line of the history text.
        Text(
            text     = "●  ",
            style    = MaterialTheme.typography.bodyMedium,
            color    = indicatorColor,
            modifier = Modifier.testTag(indicatorTag)
        )

        // Main history text — same content as before, now on the right of the dot.
        // `weight(1f)` lets the text wrap naturally without pushing the dot away.
        Text(
            text     = strings.roundHistoryPrefix(round.roundNumber, round.takerName) +
                       contractText + detailsText + outcomeText,
            style    = MaterialTheme.typography.bodyMedium,
            // Prevent very long player names or bonus lists from causing layout overflow.
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
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

// Holds the display data and state callbacks for one row of the bonus grid.
// Declared at file scope (not inside the composable) so it is not recreated on
// every recomposition of CompactBonusGrid.
private data class BonusRow(
    val label: String,
    val tooltip: String,
    val value: String?,
    val onSelect: (String?) -> Unit
)

// Compact bonus grid: shows four player-assigned bonuses as a table.
//
// Layout:
//   Row 0 (header):  empty label | Player1 | Player2 | …
//   Row 1–4 (data):  label + ⓘ  | ☑/☐    | ☑/☐    | …
//
// Each player cell holds a Checkbox. Ticking it assigns that player;
// ticking the already-checked player clears the assignment (sets to null).
// The ⓘ icon sits immediately to the right of each label text and opens a
// RichTooltip explaining the bonus and its point value.
//
// `bonusLabels`   : four localized label strings (parallel to the state params).
// `bonusTooltips` : four tooltip body strings shown when the ⓘ is tapped.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactBonusGrid(
    playerNames: List<String>,
    bonusLabels: List<String>,
    bonusTooltips: List<String>,
    petitAuBout: String?,     onPetit: (String?) -> Unit,
    poignee: String?,         onPoignee: (String?) -> Unit,
    doublePoignee: String?,   onDoublePoignee: (String?) -> Unit,
    triplePoignee: String?,   onTriplePoignee: (String?) -> Unit
) {
    // Zip labels + tooltips + state pairs into one list for the grid loop.
    val bonuses = listOf(
        BonusRow(bonusLabels[0], bonusTooltips[0], petitAuBout,   onPetit),
        BonusRow(bonusLabels[1], bonusTooltips[1], poignee,        onPoignee),
        BonusRow(bonusLabels[2], bonusTooltips[2], doublePoignee,  onDoublePoignee),
        BonusRow(bonusLabels[3], bonusTooltips[3], triplePoignee,  onTriplePoignee)
    )

    // Number of selectable options = one per player (nobody column removed).
    val colCount    = playerNames.size
    // Slightly wider label column to accommodate the label text + ⓘ icon.
    val labelWeight = 0.42f
    // Each option column gets an equal share of the remaining width.
    val colWeight   = (1f - labelWeight) / colCount

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Header row: column titles ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty space over the label column.
            Spacer(Modifier.weight(labelWeight))
            // One header per player (nobody/— column removed).
            for (name in playerNames) {
                Text(
                    text      = name,
                    style     = MaterialTheme.typography.labelSmall,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.weight(colWeight)
                )
            }
        }

        // ── One row per bonus ─────────────────────────────────────────────────
        for (row in bonuses) {
            Row(
                // heightIn(min = 48.dp) enforces Material's recommended minimum touch-target
                // height, making the radio buttons easier to tap on small screens.
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label cell: the whole row (text + ⓘ icon) is now tappable, giving a
                // much larger touch target than the previous icon-only approach.
                // Modifier.weight() is a RowScope extension — it must be applied here
                // inside the enclosing Row, not inside BonusLabelCell itself.
                BonusLabelCell(
                    label    = row.label,
                    body     = row.tooltip,
                    modifier = Modifier.weight(labelWeight)
                )

                // One Checkbox per player.
                // Ticking an unchecked box assigns that player; ticking an already-checked
                // box clears the assignment (sets value back to null).
                for (name in playerNames) {
                    Checkbox(
                        checked         = row.value == name,
                        onCheckedChange = { checked ->
                            row.onSelect(if (checked) name else null)
                        },
                        modifier = Modifier.weight(colWeight)
                    )
                }
            }
        }
    }
}

// Shows a "None" chip followed by one chip per player.
// Tapping a player assigns that player to the bonus; tapping the selected
// player again (or "None") clears the assignment.
//
// label:          localized section header text.
// noneLabel:      localized label for the "nobody" option chip.
// selectedPlayer: the currently assigned player name, or null if nobody is assigned.
// onSelect:       called with the new player name, or null to clear.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerChipSelector(
    label: String,
    noneLabel: String,
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
            label    = { Text(noneLabel) }
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

// ── Bonus label cell (label + ⓘ icon, fully tappable) ────────────────────────
//
// Wraps the entire label cell — bonus name text and the decorative ⓘ icon — in a
// single TooltipBox so tapping anywhere on the row opens the description tooltip.
// This gives a much larger touch target than a standalone icon button.
//
// label    : localized bonus name; shown as text and as the tooltip title.
// body     : multi-line tooltip body (rules + point value).
// modifier : passed through to TooltipBox — callers use this to apply Modifier.weight()
//            from within the enclosing RowScope (weight is a RowScope extension and cannot
//            be called here where no RowScope is active).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BonusLabelCell(label: String, body: String, modifier: Modifier = Modifier) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope        = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            // RichTooltip supports a title and a multi-line body text.
            RichTooltip(title = { Text(label) }) {
                Text(body)
            }
        },
        state    = tooltipState,
        modifier = modifier
    ) {
        // Outer Row fills the full column width so the grid alignment (checkboxes)
        // is preserved. It is NOT itself clickable — only the inner Row is.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Only the text + icon portion is tappable, keeping the empty space
            // at the end of the label column non-interactive.
            Row(
                modifier = Modifier
                    .clickable { scope.launch { tooltipState.show() } },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = label,
                    style    = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Decorative info icon — the clickable Row above handles all input.
                Icon(
                    imageVector        = Icons.Default.Info,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier
                        .padding(start = 2.dp)
                        .size(14.dp)
                )
            }
        }
    }
}

// ── Bonus tooltip icon (Chelem section) ───────────────────────────────────────
//
// A small ⓘ IconButton that shows a Material3 RichTooltip on tap.
// Used next to the Chelem dropdown header.
//
// title : the bonus name shown as the tooltip heading.
// body  : multi-line explanation text (rules + point value) shown below the title.
//
// The tooltip is set as `isPersistent = true` so it stays open until the user
// dismisses it — important on mobile where there is no hover event to close it.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BonusInfoIcon(title: String, body: String) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope        = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            // RichTooltip supports a title and a multi-line body text.
            RichTooltip(title = { Text(title) }) {
                Text(body)
            }
        },
        state = tooltipState
    ) {
        // Small icon button — 20 dp keeps it compact inside the label column.
        IconButton(
            onClick  = { scope.launch { tooltipState.show() } },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Info,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(14.dp)
            )
        }
    }
}

package fr.mandarine.tarotcounter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

// GameScreen handles the full round-by-round flow of a Tarot game on a single scrollable page.
//
// All information is presented together: the compact scoreboard, the contract selection,
// and — once a contract is chosen — the scoring details form.
//
// The screen observes game state from [GameViewModel] (currentRound, roundHistory, etc.)
// and delegates all state-mutating actions to the ViewModel (recordPlayed, recordSkipped,
// endGame). This separation means the game logic can be unit-tested without Compose.
//
// viewModel : holds the mutable game session state and all game-logic helpers.
// onEndGame : called when the user presses "New Game" on the Final Score screen.
//             The caller (MainActivity) uses this to navigate back to the setup screen.
// modifier  : passed in from the parent (e.g. Scaffold padding).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onEndGame: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Read the active locale from the composition tree and resolve all strings once.
    val locale  = LocalAppLocale.current
    val strings = appStrings(locale)

    // Read game session state from the ViewModel.
    // These are Compose snapshot values — recomposition is triggered automatically
    // when the ViewModel mutates them (e.g. after recordPlayed() advances currentRound).
    val currentRound  = viewModel.currentRound
    val roundHistory  = viewModel.roundHistory
    val displayNames  = viewModel.displayNames
    val currentTaker  = viewModel.currentTaker

    // The contract selected by tapping one of the contract chips.
    // null = no contract selected yet (details form is hidden).
    // Non-null = a contract chip is active and the details form is shown below.
    // This is pure UI state (not game logic) so it lives in the composable.
    var selectedContract by remember { mutableStateOf<Contract?>(null) }

    // Controls whether the score history table overlay is shown.
    var showScoreHistory by remember { mutableStateOf(false) }

    // Controls whether the final score screen overlay is shown.
    var showFinalScore by remember { mutableStateOf(false) }

    // ── System back-button handling ───────────────────────────────────────────
    // A single handler covers both the main game view and the score-history overlay.
    // `enabled = !showFinalScore` defers to the Final Score screen's own handler when
    // that overlay is visible (deeper handlers have higher priority in Compose).
    BackHandler(enabled = !showFinalScore) { onEndGame() }

    // ── Overlay screens ───────────────────────────────────────────────────────
    // These replace the whole content when active; the main game column is not rendered.

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
    // the contract chips, and — when a contract is selected — the details form,
    // without navigating away.

    // Box fills the entire screen and centers the game Column horizontally.
    // On tablets in landscape the Column is capped at MAX_CONTENT_WIDTH and
    // centered, preventing the form fields and scoreboard from stretching
    // uncomfortably wide.
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
    // Outer non-scrolling column: owns imePadding() so the entire layout (scrollable
    // content + bottom bar) shifts above the keyboard as a unit.
    // fillMaxHeight() is required so the inner weight(1f) column and the
    // pinned bottom bar share available vertical space correctly.
    Column(
        modifier = Modifier
            .widthIn(max = MAX_CONTENT_WIDTH)
            .fillMaxWidth()
            .fillMaxHeight()
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

            // ── Header: history button | centred round number ────────────────
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

            // ── Compact scoreboard ────────────────────────────────────────────
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

            // ── Contract selection ────────────────────────────────────────────
            // SingleChoiceSegmentedButtonRow is the Material 3 standard for picking
            // one option from a fixed set. Tapping the already-selected segment
            // deselects it (collapses the details form).
            Text(
                text = strings.chooseContract(currentTaker),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))

            // Shared font size — all 4 segments shrink together so they always display
            // at the same size (the smallest needed across the longest label).
            // Keyed on locale so labels re-measure whenever the language changes.
            val contractLabelSize = rememberSharedAutoSizeState(locale)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Contract.entries.forEachIndexed { index, c ->
                    SegmentedButton(
                        // shape draws the correct rounded corners: round on the outer ends,
                        // straight on the inner edges between segments.
                        shape    = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = Contract.entries.size
                        ),
                        selected = selectedContract == c,
                        onClick  = { selectedContract = if (selectedContract == c) null else c },
                        // Hide the checkmark icon — the filled/outlined segment already
                        // communicates selection clearly.
                        icon     = {}
                    ) {
                        AutoSizeText(
                            text            = c.localizedName(locale),
                            modifier        = Modifier.padding(horizontal = 1.dp),
                            sharedSizeState = contractLabelSize
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Inline round details ──────────────────────────────────────────
            // key(selectedContract) discards and recreates everything inside it
            // whenever selectedContract changes, automatically resetting all form
            // state (bouts, points, bonuses) without manual reset logic.
            key(selectedContract) {
                val contract = selectedContract
                if (contract != null) {

                    // ── Form state ────────────────────────────────────────────
                    // Declared inside key() so they reset when the contract changes.
                    var bouts            by remember { mutableIntStateOf(0) }
                    var pointsText       by remember { mutableStateOf("") }
                    // When true the user enters the defenders' points; taker's points
                    // are derived on submit as: takerPoints = 91 − defenderPoints.
                    var defenderMode     by remember { mutableStateOf(false) }
                    var selectedPartner  by remember { mutableStateOf<String?>(null) }
                    var petitAuBout      by remember { mutableStateOf<String?>(null) }
                    var poignee          by remember { mutableStateOf<String?>(null) }
                    var doublePoignee    by remember { mutableStateOf<String?>(null) }
                    var triplePoignee    by remember { mutableStateOf<String?>(null) }
                    var chelem           by remember { mutableStateOf(Chelem.NONE) }
                    // The player who called/achieved the chelem; reset to null when
                    // chelem reverts to NONE.
                    var chelemPlayer     by remember { mutableStateOf<String?>(null) }

                    // Derived error flag — true when pointsText parses to an int > 91.
                    // An empty field is not an error (defaults to 0 on Confirm).
                    val pointsError = pointsText.toIntOrNull()?.let { it > 91 } == true

                    // Used to hide the software keyboard when the user taps "Done".
                    val keyboardController = LocalSoftwareKeyboardController.current

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // ── Bouts + Points side by side ───────────────────────────
                    // Placing these in a Row cuts vertical space compared to stacking them.
                    // Alignment.Bottom keeps the dropdown and text field on the same baseline.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left half: bouts dropdown (ExposedDropdownMenuBox)
                        Column(modifier = Modifier.weight(1f)) {
                            FormLabel(strings.numberOfBouts)
                            Spacer(Modifier.height(8.dp))

                            var boutsExpanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded         = boutsExpanded,
                                onExpandedChange = { boutsExpanded = !boutsExpanded },
                                modifier         = Modifier.testTag("bouts_dropdown")
                            ) {
                                OutlinedTextField(
                                    value          = bouts.toString(),
                                    onValueChange  = {},
                                    readOnly       = true,
                                    trailingIcon   = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = boutsExpanded)
                                    },
                                    colors         = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    singleLine     = true,
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
                                            text           = { Text(n.toString()) },
                                            onClick        = {
                                                bouts         = n
                                                boutsExpanded = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }
                        }

                        // Right half: points entry — camp toggle stacked above text field
                        Column(modifier = Modifier.weight(1f)) {
                            // ── Camp toggle ────────────────────────────────────
                            // The two segments let the user pick which camp's points to type.
                            // Selecting "Defenders" is a convenience; taker points are derived
                            // on confirm as: takerPoints = 91 − defenderPoints.
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = !defenderMode,
                                    onClick  = {
                                        defenderMode = false
                                        pointsText   = ""  // clear field when switching camps
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    icon  = {}
                                ) {
                                    AutoSizeText(
                                        strings.attackerMode,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                                SegmentedButton(
                                    selected = defenderMode,
                                    onClick  = {
                                        defenderMode = true
                                        pointsText   = ""  // clear field when switching camps
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    icon  = {}
                                ) {
                                    AutoSizeText(
                                        strings.defenderMode,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = pointsText,
                                onValueChange = { input ->
                                    // Accept only digit characters, at most two (max value 91).
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
                                // Show the valid range as a placeholder so the user knows
                                // what values are accepted without a separate hint.
                                placeholder     = { Text("0-91") },
                                isError         = pointsError,
                                supportingText  = if (pointsError) ({
                                    Text(
                                        text  = strings.pointsOutOfRange,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }) else null,
                                singleLine      = true,
                                modifier        = Modifier.fillMaxWidth().testTag("points_input")
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // ── Partner selection (5-player only) ─────────────────────
                    // In a 5-player game the taker calls a silent partner before the round.
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

                    // ── Player-assigned bonuses (compact grid) ─────────────────
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

                    // ── Chelem (grand slam) ────────────────────────────────────
                    var chelemExpanded by remember { mutableStateOf(false) }

                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        ExposedDropdownMenuBox(
                            expanded         = chelemExpanded,
                            onExpandedChange = { chelemExpanded = !chelemExpanded },
                            modifier         = Modifier
                                .weight(1f)
                                .testTag("chelem_dropdown")
                        ) {
                            OutlinedTextField(
                                // Show the placeholder "Chelem" when no outcome is selected.
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
                                            // Reset the associated player when a different
                                            // chelem option is selected.
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

                    // ── Chelem player selector ─────────────────────────────────
                    // Only shown when a non-NONE chelem outcome is selected.
                    if (chelem != Chelem.NONE) {
                        Spacer(Modifier.height(8.dp))
                        val chelemCandidates = buildList {
                            add(currentTaker)
                            // In a 5-player game the partner can also call chelem.
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

                    // ── Confirm ────────────────────────────────────────────────
                    AppButton(
                        text    = strings.confirmRound,
                        enabled = !pointsError,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        onClick = {
                            // Parse the typed points; default to 0 if empty, clamp to 0–91.
                            val enteredPoints = pointsText.toIntOrNull()?.coerceIn(0, 91) ?: 0
                            // When the user entered defenders' points, convert to taker's points.
                            val points = if (defenderMode) 91 - enteredPoints else enteredPoints
                            viewModel.recordPlayed(
                                contract,
                                RoundDetails(
                                    bouts         = bouts,
                                    points        = points,
                                    partnerName   = if (displayNames.size == 5) selectedPartner else null,
                                    petitAuBout   = petitAuBout,
                                    poignee       = poignee,
                                    doublePoignee = doublePoignee,
                                    triplePoignee = triplePoignee,
                                    chelem        = chelem,
                                    chelemPlayer  = if (chelem == Chelem.NONE) null else chelemPlayer
                                )
                            )
                            // Collapse the details form so the next round starts fresh.
                            selectedContract = null
                        }
                    )
                }
            }

            // ── Round history (newest-first) ──────────────────────────────────
            if (roundHistory.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                val reversedHistory = roundHistory.reversed()
                reversedHistory.forEachIndexed { index, round ->
                    RoundHistoryRow(round = round, locale = locale, strings = strings)
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

        // ── Bottom action bar ─────────────────────────────────────────────────
        // Persistent split row pinned below the scroll area.
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left half: end the game and navigate to the Final Score screen.
            AppOutlinedButton(
                text     = strings.endGame,
                onClick  = {
                    viewModel.endGame()
                    showFinalScore = true
                },
                modifier = Modifier.weight(1f)
            )
            // Right half: record a skipped round and advance to the next.
            AppButton(
                text     = strings.skipRound,
                onClick  = { viewModel.recordSkipped() },
                modifier = Modifier.weight(1f)
            )
        }
    }  // end outer Column
    }  // end Box
}

// ── Compact scoreboard ────────────────────────────────────────────────────────

// Displays all players and their cumulative scores in a compact horizontal card.
// Each player gets a column: their name on top, their running total below.
// Always visible at the top of the game page after the first round.
@Composable
private fun CompactScoreboard(
    displayNames: List<String>,
    roundHistory: List<RoundResult>,
    scoresLabel: String
) {
    Text(
        text  = scoresLabel,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(4.dp))

    // Delegate total computation to the tested helper so both places stay in sync.
    val totals = computeFinalTotals(displayNames, roundHistory)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (name in displayNames) {
                val total = totals[name] ?: 0

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text     = name,
                        style    = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text  = total.withSign(),
                        style = MaterialTheme.typography.titleMedium,
                        // Green for positive/zero scores, red for negative.
                        color = scoreColor(total)
                    )
                }
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

// An icon-only button with a bar-chart icon for opening the score history overlay.
// The contentDescription ensures screen readers announce the button's purpose.
@Composable
fun HistoryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val strings = appStrings(LocalAppLocale.current)
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector        = Icons.Default.BarChart,
            contentDescription = strings.history
        )
    }
}

// ── Round history row ─────────────────────────────────────────────────────────

// A single styled row in the round history list.
//
// Layout:  [●]  Round N: Taker — Contract · details — Outcome
//
// The leading dot (●) is colored by outcome so the user can scan results at a glance:
//   Won     → MaterialTheme.colorScheme.primary   (green in default theme)
//   Lost    → MaterialTheme.colorScheme.error     (red)
//   Skipped → MaterialTheme.colorScheme.onSurfaceVariant (muted)
//
// The `testTag` on the dot lets UI tests assert the correct outcome without
// reading color values directly.
@Composable
private fun RoundHistoryRow(
    round:   RoundResult,
    locale:  AppLocale,
    strings: AppStrings
) {
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

    val (indicatorColor, indicatorTag) = when (round.won) {
        true  -> MaterialTheme.colorScheme.primary          to "round_indicator_won"
        false -> MaterialTheme.colorScheme.error            to "round_indicator_lost"
        null  -> MaterialTheme.colorScheme.onSurfaceVariant to "round_indicator_skipped"
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text     = "●  ",
            style    = MaterialTheme.typography.bodyMedium,
            color    = indicatorColor,
            modifier = Modifier.testTag(indicatorTag)
        )
        Text(
            text     = strings.roundHistoryPrefix(round.roundNumber, round.takerName) +
                       contractText + detailsText + outcomeText,
            style    = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

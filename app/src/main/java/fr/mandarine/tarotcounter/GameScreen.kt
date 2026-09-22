package fr.mandarine.tarotcounter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import fr.mandarine.tarotcounter.ui.theme.Dimens
import fr.mandarine.tarotcounter.ui.theme.tarotColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    val currentRound   = viewModel.currentRound
    val roundHistory   = viewModel.roundHistory
    val displayNames   = viewModel.displayNames
    // The dealer rotates each round — they deal the cards but are not necessarily the attacker.
    val currentDealer  = viewModel.currentDealer

    // The player who won the bidding and took the contract (the attacker).
    // null = no attacker selected yet.
    // Any player can be the attacker, regardless of who is dealing this round.
    // This resets to null at the start of each new round (see LaunchedEffect below).
    var selectedAttacker by remember { mutableStateOf<String?>(null) }

    // True while the round-entry view is shown (contract, bouts, points, bonuses).
    // Tapping a "Who took?" tile opens it; its back arrow closes it again while
    // keeping the taker and the form, so a mis-tap never loses what was typed.
    var roundEntryOpen by remember { mutableStateOf(false) }

    // The contract selected by tapping one of the contract chips.
    // null = no contract selected yet (details form is hidden).
    // Non-null = a contract chip is active and the details form is shown below.
    // This is pure UI state (not game logic) so it lives in the composable.
    var selectedContract by remember { mutableStateOf<Contract?>(null) }

    // Controls whether the score history table overlay is shown.
    var showScoreHistory by remember { mutableStateOf(false) }

    // Controls whether the final score screen overlay is shown.
    var showFinalScore by remember { mutableStateOf(false) }

    // Controls whether the "undo previous round" confirmation dialog is visible.
    var showUndoConfirm by remember { mutableStateOf(false) }

    // Controls whether the "end game with pending points" confirmation dialog is visible.
    // This is shown when the user taps "End Game" while the points field is non-empty,
    // to protect against accidentally discarding unsaved round data.
    var showEndGameConfirm by remember { mutableStateOf(false) }

    // ── Hoisted form state ────────────────────────────────────────────────────
    // These variables are declared here (rather than inside the form block) so
    // the pinned bottom-bar Confirm button can read and submit them without
    // being inside the scrollable column.
    var bouts            by remember { mutableIntStateOf(0) }
    var pointsText       by remember { mutableStateOf("") }
    // When true the user enters the defenders' points; taker's points
    // are derived on submit as: takerPoints = 91 − defenderPoints.
    var defenderMode     by remember { mutableStateOf(false) }
    var selectedPartner  by remember { mutableStateOf<String?>(null) }
    var petitAuBout      by remember { mutableStateOf<String?>(null) }
    // Multi-player poignée declarations (issue #149): any number of players can each
    // show their own trump hand. We store selected players in a Set so toggling an
    // already-selected player is a simple `- name` operation.
    var poignees         by remember { mutableStateOf(emptySet<String>()) }
    var doublePoignees   by remember { mutableStateOf(emptySet<String>()) }
    var triplePoignees   by remember { mutableStateOf(emptySet<String>()) }
    var chelem           by remember { mutableStateOf(Chelem.NONE) }
    // The player who called/achieved the chelem; reset to null when chelem reverts to NONE.
    var chelemPlayer     by remember { mutableStateOf<String?>(null) }

    // ── Undo-restoration state ────────────────────────────────────────────────
    //
    // When the user confirms "undo", we capture the last RoundResult before
    // removing it, then restore all form fields so only the wrong value needs
    // to be corrected.
    //
    // The challenge: two existing LaunchedEffects would normally erase those
    // values immediately after restoration —
    //   • LaunchedEffect(currentRound) resets selectedAttacker on every round
    //     change, including an undo (which decrements currentRound).
    //   • LaunchedEffect(selectedContract) resets every bonus/score field
    //     whenever selectedContract changes — including when we write the
    //     restored contract.
    //
    // We coordinate them with two helpers:
    //
    //   • previousRound: tracks the previous currentRound value.
    //     LaunchedEffect(currentRound) checks whether the counter *increased*
    //     (normal forward advance → reset attacker) or *decreased* (undo →
    //     keep it; restoredRound will supply the correct value).
    //
    //   • restoredRound: holds the RoundResult being restored.
    //     LaunchedEffect(selectedContract) skips its wipe-and-clear if this
    //     is non-null, then clears the sentinel so it behaves normally again.
    //     LaunchedEffect(restoredRound) applies all field values.
    //     If the restored contract is null (skipped round), selectedContract
    //     doesn't change so LaunchedEffect(selectedContract) never fires;
    //     LaunchedEffect(restoredRound) clears the sentinel itself in that case.

    // Remembers the round counter from the previous composition so we can
    // distinguish a forward advance from an undo (backward move).
    var previousRound by remember { mutableIntStateOf(currentRound) }

    // Non-null only during the two-frame undo restoration window (see above).
    var restoredRound by remember { mutableStateOf<RoundResult?>(null) }

    // Reset the attacker selection when a new round starts (forward advance only).
    // LaunchedEffect re-runs whenever its key (currentRound) changes value.
    // Note: this runs AFTER the composition is committed, but because the
    // assignment is non-suspending the UI reflects the change on the next
    // recomposition.
    LaunchedEffect(currentRound) {
        if (currentRound > previousRound) {
            // Normal forward advance — a new round started; clear the attacker
            // and show the between-rounds view (standings + "Who took?").
            selectedAttacker = null
            roundEntryOpen   = false
        }
        // If currentRound decreased (undo), leave the attacker alone.
        // LaunchedEffect(restoredRound) will write the correct value.
        previousRound = currentRound
    }

    // Reset every form field whenever the selected contract changes (including to null).
    // LaunchedEffect re-runs on each new key value — the assignments are non-suspending
    // so the reset happens effectively in the same frame.
    //
    // Exception: skip the wipe when restoredRound is non-null. That means
    // LaunchedEffect(restoredRound) just changed selectedContract as part of
    // a restoration — the restored values must not be discarded. After skipping,
    // the sentinel is cleared so subsequent contract changes behave normally.
    // If selectedContract didn't actually change (null → null, skipped round),
    // this effect never fires; LaunchedEffect(restoredRound) clears the sentinel
    // itself in that case.
    LaunchedEffect(selectedContract) {
        if (restoredRound != null) {
            restoredRound = null  // sentinel consumed — next contract change resets normally
            return@LaunchedEffect
        }
        bouts           = 0
        pointsText      = ""
        defenderMode    = false
        selectedPartner = null
        petitAuBout     = null
        poignees        = emptySet()
        doublePoignees  = emptySet()
        triplePoignees  = emptySet()
        chelem          = Chelem.NONE
        chelemPlayer    = null
    }

    // Applies all form fields from the captured round after an undo.
    // Declared after the other two LaunchedEffects so it runs in the same
    // composition pass but later in the tree; it fires once per undo action.
    //
    // Setting selectedContract here triggers LaunchedEffect(selectedContract)
    // in the *next* recomposition — the sentinel (restoredRound) is left non-null
    // until then so that LaunchedEffect(selectedContract) knows to skip the wipe.
    // Exception: if the restored contract is null (skipped round), selectedContract
    // won't actually change, so LaunchedEffect(selectedContract) never fires; we
    // clear the sentinel here instead.
    LaunchedEffect(restoredRound) {
        val round = restoredRound ?: return@LaunchedEffect
        val details = round.details
        selectedAttacker = round.takerName
        selectedContract = round.contract
        // A played round reopens the entry view so only the wrong value needs fixing;
        // an undone *skipped* round goes back to the "Who took?" view.
        roundEntryOpen   = round.contract != null
        bouts            = details?.bouts         ?: 0
        // RoundDetails always stores taker points (already converted from
        // defenders' mode on confirm), so we restore as attacker points with
        // defenderMode = false to avoid double-conversion on the next confirm.
        pointsText       = details?.points?.toString() ?: ""
        defenderMode     = false
        selectedPartner  = details?.partnerName
        petitAuBout      = details?.petitAuBout
        // `effectivePoignees` returns the new list fields when non-empty, or falls
        // back to the legacy single-player nullable field — so undo works for both
        // old saved rounds (legacy format) and new rounds (multi-player format).
        poignees         = details?.effectivePoignees?.toSet()       ?: emptySet()
        doublePoignees   = details?.effectiveDoublePoignees?.toSet() ?: emptySet()
        triplePoignees   = details?.effectiveTriplePoignees?.toSet() ?: emptySet()
        chelem           = details?.chelem        ?: Chelem.NONE
        chelemPlayer     = details?.chelemPlayer
        // Skipped round: contract is null, so selectedContract didn't change →
        // LaunchedEffect(selectedContract) will not fire → clear sentinel here.
        if (round.contract == null) restoredRound = null
        // Non-null contract: sentinel stays set until LaunchedEffect(selectedContract)
        // fires in the next recomposition and clears it.
    }

    // Derived error flag — true when pointsText parses to an int > 91.
    // Declared here so the Confirm button in the bottom bar can read it.
    val pointsError = pointsText.toIntOrNull()?.let { it > 91 } == true

    // Total declared atouts = sum of the minimum trump thresholds for every poignée
    // declaration across all players. Only meaningful when a contract is selected
    // (i.e. the bonus grid is visible). Returns 0 when no contract is chosen so that
    // the Confirm button is not affected before the form is even shown.
    val totalDeclaredAtouts = if (selectedContract != null) {
        totalAtoutsAnnounced(
            poignees.toList(),
            doublePoignees.toList(),
            triplePoignees.toList(),
            displayNames.size
        )
    } else 0
    // True when the combined trump declarations exceed the 22 available in the deck.
    // Prevents physically impossible combinations (e.g. triple + simple = 25 > 22).
    val atoutError = totalDeclaredAtouts > TOTAL_ATOUTS_IN_DECK

    // Used to hide the software keyboard when the user taps "Confirm".
    val keyboardController = LocalSoftwareKeyboardController.current

    // ── System back-button handling ───────────────────────────────────────────
    // A single handler covers both the main game view and the score-history overlay.
    // `enabled = !showFinalScore` defers to the Final Score screen's own handler when
    // that overlay is visible (deeper handlers have higher priority in Compose).
    BackHandler(enabled = !showFinalScore) { onEndGame() }
    // In the round-entry view, system back first returns to "Who took?" (the taker
    // and the form are kept). Declared after the handler above, so it wins.
    BackHandler(enabled = !showFinalScore && !showScoreHistory && roundEntryOpen) {
        roundEntryOpen = false
    }

    // ── Overlay screens ───────────────────────────────────────────────────────
    // These replace the whole content when active; the main game column is not rendered.

    if (showFinalScore) {
        FinalScoreScreen(
            playerNames  = displayNames,
            roundHistory = roundHistory,
            onBack       = { showFinalScore = false },
            onNewGame    = onEndGame,
            onMainMenu   = onEndGame,  // both "New Game" and "Main Menu" navigate to the landing screen
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

    // ── Undo confirmation dialog ──────────────────────────────────────────────
    // AlertDialog is a Material 3 modal overlay; it does NOT replace the whole screen
    // (unlike the FinalScore/ScoreHistory overlays above), so the game content remains
    // visible behind the dimmed backdrop. The user must explicitly confirm or cancel.
    if (showUndoConfirm) {
        AlertDialog(
            onDismissRequest = { showUndoConfirm = false },
            title = { Text(strings.undoConfirmTitle) },
            text  = { Text(strings.undoConfirmBody) },
            confirmButton = {
                AppTextButton(
                    text    = strings.undoPreviousRound,
                    onClick = {
                        showUndoConfirm = false
                        // Capture the last round BEFORE removing it.
                        // LaunchedEffect(restoredRound) will apply all its fields to
                        // the form once the undo recomposition has settled, so the
                        // user only needs to correct what was wrong.
                        restoredRound = viewModel.roundHistory.lastOrNull()
                        viewModel.undoLastRound()
                    }
                )
            },
            dismissButton = {
                AppTextButton(text = strings.cancel, onClick = { showUndoConfirm = false })
            }
        )
    }

    // ── End-game confirmation dialog ─────────────────────────────────────────
    // Shown only when the user taps "End Game" while there are pending points in the
    // points field, so they cannot accidentally lose unsaved round data.
    // On confirm: the game ends normally (or is cancelled if no rounds have been played).
    // On dismiss: the dialog closes and the user continues entering points.
    if (showEndGameConfirm) {
        AlertDialog(
            onDismissRequest = { showEndGameConfirm = false },
            title = { Text(strings.endGameConfirmTitle) },
            text  = { Text(strings.endGameConfirmBody) },
            confirmButton = {
                AppTextButton(
                    text    = strings.endGame,
                    onClick = {
                        showEndGameConfirm = false
                        if (roundHistory.isEmpty()) {
                            // Zero rounds played — cancel the game silently.
                            viewModel.clearInProgressGame()
                            onEndGame()
                        } else {
                            viewModel.endGame()
                            showFinalScore = true
                        }
                    }
                )
            },
            dismissButton = {
                AppTextButton(text = strings.cancel, onClick = { showEndGameConfirm = false })
            }
        )
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
        // ── Top bar (fixed, does not scroll) ─────────────────────────────────
        // Between rounds: "Round 5" with undo + history icon buttons.
        // Round entry:    "Chloé takes" with a back arrow to change the taker.
        val attacker = selectedAttacker
        if (roundEntryOpen && attacker != null) {
            SalonTopBar(
                title                  = strings.takerTakes(attacker),
                onBack                 = { roundEntryOpen = false },
                backContentDescription = strings.changeTaker,
                modifier               = Modifier.padding(horizontal = Dimens.SpaceXs)
            )
        } else {
            SalonTopBar(
                title    = strings.roundHeader(currentRound),
                modifier = Modifier.padding(start = Dimens.SpaceS, end = Dimens.SpaceXs),
                actions  = buildList {
                    // Undo only makes sense once a round has been recorded.
                    if (roundHistory.isNotEmpty()) {
                        add(TopBarAction(Icons.AutoMirrored.Filled.Undo, strings.undoPreviousRound) {
                            showUndoConfirm = true
                        })
                    }
                    add(TopBarAction(Icons.AutoMirrored.Filled.ShowChart, strings.history) {
                        showScoreHistory = true
                    })
                }
            )
        }

        // Inner scrollable column: weight(1f) takes all vertical space between the
        // top bar and the bottom action bar. Never use fillMaxSize() with weight().
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenMargin, vertical = Dimens.SpaceS),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

          if (!roundEntryOpen || attacker == null) {
            // ── Between rounds: scores first, then "Who took?" ─────────────────
            BetweenRoundsContent(
                currentDealer = currentDealer,
                displayNames  = displayNames,
                roundHistory  = roundHistory,
                selectedTaker = selectedAttacker,
                strings       = strings,
                locale        = locale,
                onTakerChosen = { name ->
                    // A different taker invalidates the contract (and so the whole form).
                    if (name != selectedAttacker) selectedContract = null
                    selectedAttacker = name
                    roundEntryOpen   = true
                },
                onSeeAll      = { showScoreHistory = true }
            )
          } else {
            // ── Round entry (restyled in issue #199) ──────────────────────────

            // ── Contract selection ────────────────────────────────────────────
            // Only shown once an attacker has been selected — the attacker's name
            // appears in the label so the user can confirm who is playing.
            // SingleChoiceSegmentedButtonRow is the Material 3 standard for picking
            // one option from a fixed set. Tapping the already-selected segment
            // deselects it (collapses the details form).

            // rememberSharedAutoSizeState must be called unconditionally (Compose rule:
            // remember calls must not be placed inside if/when/loop branches).
            // The value is only *used* inside the if-block below.
            val contractLabelSize = rememberSharedAutoSizeState(locale)

            if (selectedAttacker != null) {
                Text(
                    text = strings.chooseContract(selectedAttacker!!),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))

                // Shared font size — all 4 segments shrink together so they always display
                // at the same size (the smallest needed across the longest label).
                // Keyed on locale so labels re-measure whenever the language changes.
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Contract.entries.forEachIndexed { index, c ->
                        SegmentedButton(
                            // Salon colours: selected segment filled felt green (issue #196).
                            colors   = salonSegmentedButtonColors(),
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
            } // end attacker-required guard

            // ── Inline round details ──────────────────────────────────────────
            // The form state (bouts, pointsText, etc.) is declared at the top of
            // GameScreen and reset via LaunchedEffect(selectedContract) — this lets
            // the Confirm button in the pinned bottom bar read and submit the values.
            val contract = selectedContract
            if (contract != null) {

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // ── Bouts + Points side by side ───────────────────────────────
                // Placing these in a Row cuts vertical space compared to stacking them.
                //
                // IntrinsicSize.Min sets the Row's height to the tallest column's natural
                // height (i.e. the height of its content without any expansion). This is
                // needed so fillMaxHeight() inside each Column has a concrete ceiling to
                // fill up to.
                //
                // Both Columns use fillMaxHeight() so they stretch to that shared height.
                // A weight(1f) Spacer between the label and the field then pushes the
                // field to the bottom of each Column, keeping both fields vertically
                // aligned even when one label wraps to more lines than the other
                // (e.g. "Nombre de bouts (oudlers)" in French wraps but "Points" does not).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),  // height = tallest column's natural height
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                    // No verticalAlignment — each Column manages alignment internally
                ) {
                    // Left half: bouts dropdown (ExposedDropdownMenuBox)
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        FormLabel(strings.numberOfBouts)
                        // Flexible spacer: grows to fill remaining Column height,
                        // pushing the dropdown flush with the bottom of the Row.
                        Spacer(Modifier.weight(1f))

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

                    // Right half: points entry with an inline camp toggle
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // Section header mirrors the "Number of bouts (oudlers)" label
                        // on the left so both halves of the Row look structurally identical.
                        FormLabel(strings.pointsHeader)
                        // Flexible spacer (mirrors the one in the left Column) so the
                        // text field always aligns with the bouts dropdown below.
                        Spacer(Modifier.weight(1f))
                        // ── Points field with trailing camp toggle ───────────────
                        // The floating label tells the user which camp's points to enter.
                        // The trailing icon (Swords = attacker, Shield = defenders) lets
                        // them switch camp without leaving the keyboard.
                        // SwordsIcon embeds the Material Symbols Outlined "swords" path
                        // (material-icons-extended 1.7.x has no sword glyph).
                        // Icons.Default.Shield is the standard Material Design shield.
                        // Together they form an immediately-recognisable attack/defend pair.
                        // Tapping it clears the current value so there is no confusion
                        // about which camp the displayed number belongs to.
                        // When the user enters defender points, the app converts on
                        // confirm: takerPoints = 91 − defenderPoints.
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
                            // Dynamic label: shows which camp the user is entering points for.
                            // AutoSizeText shrinks the label font until the full string fits
                            // on one line — this handles narrow screens and large system fonts
                            // without truncating or wrapping.
                            label = {
                                AutoSizeText(
                                    text = if (defenderMode) strings.defenderPointsLabel
                                           else strings.attackerPointsLabel
                                )
                            },
                            // Trailing icon acts as the camp toggle.
                            // The icon represents the CURRENT mode (Swords = attacker,
                            // Shield = defenders), and the content description describes
                            // what the NEXT tap will switch to, following Material
                            // accessibility guidelines for toggle controls.
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        // Clear first so no stale value carries over
                                        // to the new camp's context.
                                        pointsText   = ""
                                        defenderMode = !defenderMode
                                    },
                                    modifier = Modifier.testTag("camp_toggle")
                                ) {
                                    Icon(
                                        // SwordsIcon (Material Symbols "swords" path) = attacker.
                                        // Icons.Default.Shield (Material Icons Extended) = defenders.
                                        // Both icons are from Google's Material design language,
                                        // giving a clear, immediately-recognisable attack/defend pair.
                                        imageVector = if (defenderMode)
                                            Icons.Default.Shield  // defenders hold the shield
                                        else
                                            SwordsIcon,           // attacker wields the crossed swords
                                        // Content description names the NEXT mode so screen
                                        // readers announce the action, not the current state.
                                        contentDescription = if (defenderMode)
                                            strings.attackerPointsLabel
                                        else
                                            strings.defenderPointsLabel
                                    )
                                }
                            },
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

                // ── Partner selection (5-player only) ─────────────────────────
                // In a 5-player game the attacker calls a silent partner before the round.
                if (displayNames.size == 5) {
                    // The attacker cannot be their own partner, so exclude them.
                    val partnerOptions = displayNames.filter { it != selectedAttacker }
                    // Label on the left, dropdown on the right — same horizontal row.
                    var partnerExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier             = Modifier.fillMaxWidth(),
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Label takes the left half of the row.
                        Text(
                            text     = strings.partnerCalledByTaker,
                            style    = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        // Dropdown fills the right half.
                        ExposedDropdownMenuBox(
                            expanded         = partnerExpanded,
                            onExpandedChange = { partnerExpanded = !partnerExpanded },
                            modifier         = Modifier
                                .weight(1f)
                                .testTag("partner_dropdown")
                        ) {
                            OutlinedTextField(
                                // Show the selected partner name, or a dash when no partner chosen.
                                value         = selectedPartner ?: "—",
                                onValueChange = {},
                                readOnly      = true,
                                trailingIcon  = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = partnerExpanded)
                                },
                                colors     = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                singleLine = true,
                                // Smaller font so the text fits inside the reduced-height field.
                                textStyle  = MaterialTheme.typography.bodyMedium,
                                modifier   = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    // Compact height — bodyMedium (14 sp) fits comfortably at 48 dp.
                                    .height(48.dp)
                            )
                            ExposedDropdownMenu(
                                expanded         = partnerExpanded,
                                onDismissRequest = { partnerExpanded = false }
                            ) {
                                // Dash entry at the top lets the user clear the partner.
                                DropdownMenuItem(
                                    text           = { Text("—") },
                                    onClick        = {
                                        selectedPartner = null
                                        partnerExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                                for (name in partnerOptions) {
                                    DropdownMenuItem(
                                        text           = { Text(name) },
                                        onClick        = {
                                            selectedPartner = name
                                            partnerExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                }

                // ── Player-assigned bonuses (compact grid) ─────────────────────
                // Petit au bout stays single-select (only one player captures the
                // Petit on the last trick). The three Poignée rows are now multi-select:
                // any number of players can each show their own trump hand (issue #149).
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
                        // Invoke with the actual player count so the tooltip shows the
                        // correct trump threshold (8/10/13 for 5 players, 10/13/15 for 4, 13/15/18 for 3).
                        strings.poigneeTooltipBody(displayNames.size),
                        strings.doublePoigneeTooltipBody(displayNames.size),
                        strings.triplePoigneeTooltipBody(displayNames.size)
                    ),
                    petitAuBout     = petitAuBout,   onPetit         = { petitAuBout = it },
                    // Each poignée callback receives (playerName, isNowChecked).
                    // Adding a player: `poignees + name`; removing: `poignees - name`.
                    poignees        = poignees,       onPoignee       = { name, checked ->
                        poignees = if (checked) poignees + name else poignees - name
                    },
                    doublePoignees  = doublePoignees, onDoublePoignee = { name, checked ->
                        doublePoignees = if (checked) doublePoignees + name else doublePoignees - name
                    },
                    triplePoignees  = triplePoignees, onTriplePoignee = { name, checked ->
                        triplePoignees = if (checked) triplePoignees + name else triplePoignees - name
                    }
                )

                // ── Atout count validation error ──────────────────────────────
                // Shown when the combined minimum trump thresholds across all
                // declared poignées exceed the 22 trumps in the deck.
                if (atoutError) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = strings.atoutCountError(totalDeclaredAtouts, TOTAL_ATOUTS_IN_DECK),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("atout_count_error")
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // ── Chelem (grand slam) ────────────────────────────────────────
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

                // ── Chelem player selector ─────────────────────────────────────
                // Only shown when a non-NONE chelem outcome is selected.
                if (chelem != Chelem.NONE) {
                    Spacer(Modifier.height(8.dp))
                    val chelemCandidates = buildList {
                        // The selected attacker can always call chelem.
                        selectedAttacker?.let { add(it) }
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
                    // Using ?.let instead of !! for idiomatic null-safe access:
                    // this only renders when chelemPlayer is non-null AND the chelem
                    // type is one that was announced (realized or not).
                    if (chelem == Chelem.ANNOUNCED_REALIZED || chelem == Chelem.ANNOUNCED_NOT_REALIZED) {
                        chelemPlayer?.let { player ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text  = strings.chelemPlaysFirst(player),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
            // end inline round details
          } // end round entry

        }  // end inner scrollable Column

        // ── Bottom action bar ─────────────────────────────────────────────────
        // Pinned under the scrollable content, above a hairline.
        //   Between rounds: [End game]  [      Skip round      ]
        //   Round entry:    [End game]  [    Confirm round     ]
        // "End game" is a red text button: available, but visually the least
        // prominent, so it is hard to hit by accident.
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenMargin, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // If no rounds have been played yet, ending the game cancels it entirely
            // (issue #90). With pending points, a confirmation dialog protects the
            // user's unsaved entry first (issue #150).
            AppTextButton(
                text         = strings.endGame,
                contentColor = MaterialTheme.colorScheme.error,
                onClick      = {
                    if (pointsText.isNotBlank()) {
                        showEndGameConfirm = true
                    } else if (roundHistory.isEmpty()) {
                        viewModel.clearInProgressGame()
                        onEndGame()
                    } else {
                        viewModel.endGame()
                        showFinalScore = true
                    }
                }
            )
            if (roundEntryOpen && selectedAttacker != null) {
                // Confirm: the main action, disabled until the round is valid
                // (contract chosen, points entered and ≤ 91, trump declarations ≤ 22).
                AppButton(
                    text     = strings.confirmRound,
                    enabled  = selectedContract != null && pointsText.isNotBlank() && !pointsError && !atoutError,
                    modifier = Modifier.weight(1f),
                    onClick  = {
                        // Guards: both are checked by `enabled`, but Kotlin needs
                        // smart-cast-safe references inside the lambda.
                        val taker    = selectedAttacker ?: return@AppButton
                        val contract = selectedContract ?: return@AppButton
                        // Parse the typed points; default to 0 if empty, clamp to 0–91.
                        val enteredPoints = pointsText.toIntOrNull()?.coerceIn(0, 91) ?: 0
                        // Defenders' points are converted to the taker's: 91 − x.
                        val points = if (defenderMode) 91 - enteredPoints else enteredPoints
                        viewModel.recordPlayed(
                            taker,
                            contract,
                            RoundDetails(
                                bouts          = bouts,
                                points         = points,
                                partnerName    = if (displayNames.size == 5) selectedPartner else null,
                                petitAuBout    = petitAuBout,
                                // Always write the multi-player list fields; the legacy
                                // nullable fields stay null so nothing is double-counted.
                                poignees       = poignees.toList(),
                                doublePoignees = doublePoignees.toList(),
                                triplePoignees = triplePoignees.toList(),
                                chelem         = chelem,
                                chelemPlayer   = if (chelem == Chelem.NONE) null else chelemPlayer
                            )
                        )
                        // Deselect contract → LaunchedEffect resets all form fields;
                        // closing the entry view brings the updated standings back.
                        selectedContract = null
                        roundEntryOpen   = false
                        keyboardController?.hide()
                    }
                )
            } else {
                // Skip round: records a round with no contract (nobody took).
                AppOutlinedButton(
                    text     = strings.skipRound,
                    onClick  = { viewModel.recordSkipped() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }  // end outer Column
    }  // end Box
}

// ── Between rounds (Salon game screen, issue #198) ───────────────────────────
//
//   ┌──────────────────────────────┐
//   │        (Dealer: David)       │  brass chip
//   │ ┌── Standings ─────────────┐ │  ranked; leader row tinted brass
//   │ │ 1 (A) Alice LEADING  +312│ │
//   │ │ 2 (B) Bruno      ▼    −48│ │
//   │ └──────────────────────────┘ │
//   │ Who took?      Tap the taker │
//   │ ┌────┐ ┌────┐ ┌────┐         │  avatar tiles (3 / 2×2 / 3+2)
//   │ │(A) │ │(B) │ │(C) │         │
//   │ └────┘ └────┘ └────┘         │
//   │ ═════════ ♠ ♥ ♦ ♣ ═════════  │
//   │ Last rounds          See all │
//   │ R4 Bruno · Garde · Lost  −162│
//   └──────────────────────────────┘
@Composable
private fun BetweenRoundsContent(
    currentDealer: String,
    displayNames: List<String>,
    roundHistory: List<RoundResult>,
    selectedTaker: String?,
    strings: AppStrings,
    locale: AppLocale,
    onTakerChosen: (String) -> Unit,
    onSeeAll: () -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL)
    ) {
        // The dealer deals the cards; any player may take, so this is context only.
        DealerChip(
            text     = strings.dealerLabel(currentDealer),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Standings appear once there is something to rank.
        if (roundHistory.isNotEmpty()) {
            StandingsCard(
                standings    = computeStandings(displayNames, roundHistory),
                leadingLabel = strings.leading,
                title        = strings.standings
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title = strings.whoTook) {
                Text(
                    text  = strings.tapTheTaker,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TakerTiles(
                names         = displayNames,
                selectedTaker = selectedTaker,
                onTakerChosen = onTakerChosen
            )
        }

        if (roundHistory.isNotEmpty()) {
            SuitDivider()
            LastRoundsLog(
                rounds   = lastRounds(roundHistory),
                strings  = strings,
                locale   = locale,
                onSeeAll = onSeeAll
            )
        }
    }
}

// A small brass pill: "Dealer: David", with a playing-cards icon.
@Composable
private fun DealerChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag("dealer_chip"),
        shape    = CircleShape,
        // secondaryContainer is the brass tint; onSecondaryContainer its dark brass ink.
        color    = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Style,
                contentDescription = null, // the text says it all
                modifier           = Modifier.size(14.dp)
            )
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// The ranked standings card. Every leader (several on a tie) gets a brass-tinted
// row, a brass ring around a large avatar, a "LEADING" overline and an XL score.
@Composable
private fun StandingsCard(standings: List<Standing>, leadingLabel: String, title: String) {
    SalonCard(
        modifier       = Modifier
            .fillMaxWidth()
            .testTag("standings_card")
            // The card is announced as "Standings" by screen readers.
            .semantics { contentDescription = title },
        contentPadding = PaddingValues(0.dp)
    ) {
        Column {
            standings.forEachIndexed { index, standing ->
                if (standing.isLeader) {
                    LeaderRow(standing, leadingLabel)
                } else {
                    StandingRow(standing)
                }
                // Hairline between rows, not after the last one.
                if (index < standings.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun LeaderRow(standing: Standing, leadingLabel: String) {
    val brass = MaterialTheme.tarotColors.brass
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.tarotColors.winnerHighlight.copy(alpha = 0.5f))
            .padding(horizontal = Dimens.SpaceM, vertical = 14.dp)
            .testTag("standing_${standing.name}"),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Brass ring: a 2 dp brass border drawn 2 dp outside the avatar.
        Box(
            modifier = Modifier
                .border(2.dp, brass, CircleShape)
                .padding(4.dp)
        ) {
            PlayerAvatar(name = standing.name, seatIndex = standing.seatIndex, size = AvatarSize.L)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = leadingLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.tarotColors.brassText
            )
            Text(
                text     = standing.name,
                style    = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            ScoreText(score = standing.total, size = ScoreSize.XL)
            TrendArrow(standing.lastDelta)
        }
    }
}

@Composable
private fun StandingRow(standing: Standing) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = Dimens.SpaceM)
            .testTag("standing_${standing.name}"),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rank number, muted; tied players share a rank (1, 1, 3).
        Text(
            text     = standing.rank.toString(),
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp)
        )
        PlayerAvatar(name = standing.name, seatIndex = standing.seatIndex, size = AvatarSize.M)
        Text(
            text     = standing.name,
            style    = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        TrendArrow(standing.lastDelta)
        ScoreText(score = standing.total, size = ScoreSize.M)
    }
}

// Tiny up/down arrow showing whether the player gained or lost in the last round.
// Nothing is drawn for a skipped round, a zero change, or before the first round.
@Composable
private fun TrendArrow(lastDelta: Int?) {
    if (lastDelta == null || lastDelta == 0) return
    val up = lastDelta > 0
    Icon(
        imageVector        = if (up) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
        contentDescription = null, // decorative: the score itself carries the meaning
        tint               = if (up) MaterialTheme.tarotColors.positive else MaterialTheme.tarotColors.negative,
        modifier           = Modifier.size(20.dp).testTag(if (up) "trend_up" else "trend_down")
    )
}

// "Who took?" — one large tile per player. 3 players: one row of 3; 4 players: 2 × 2;
// 5 players: 3 + 2. The current taker (if any) is filled felt green.
@Composable
private fun TakerTiles(
    names: List<String>,
    selectedTaker: String?,
    onTakerChosen: (String) -> Unit
) {
    val columns = takerGridColumns(names.size)
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        // chunked(n) splits the list into rows of n (the last row may be shorter).
        names.withIndex().chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                for ((seat, name) in row) {
                    TakerTile(
                        name     = name,
                        seat     = seat,
                        selected = name == selectedTaker,
                        onClick  = { onTakerChosen(name) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Invisible fillers keep the last row's tiles the same width as the others.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TakerTile(
    name: String,
    seat: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    // Surface(onClick = …) is a clickable card with a ripple and button semantics.
    Surface(
        onClick  = onClick,
        modifier = modifier
            .height(96.dp)
            .testTag("taker_tile_$name")
            .semantics { this.selected = selected },
        shape    = MaterialTheme.shapes.medium,
        color    = if (selected) scheme.primary else scheme.surface,
        contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
        border   = BorderStroke(1.dp, if (selected) scheme.primary else scheme.outline)
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = Dimens.SpaceS),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS, Alignment.CenterVertically)
        ) {
            PlayerAvatar(name = name, seatIndex = seat, size = AvatarSize.M)
            // The name shrinks rather than wrapping when a tile is narrow (5 players).
            AutoSizeText(text = name, style = MaterialTheme.typography.titleSmall)
        }
    }
}

// The latest rounds, newest first, with a "See all" link to the history screen:
//   R4  Bruno · Garde · Lost        −162
//   R3  Skipped
@Composable
private fun LastRoundsLog(
    rounds: List<RoundResult>,
    strings: AppStrings,
    locale: AppLocale,
    onSeeAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        SectionHeader(title = strings.lastRounds) {
            AppTextButton(text = strings.seeAll, onClick = onSeeAll)
        }
        for (round in rounds) {
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .testTag("last_round_${round.roundNumber}"),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text     = strings.roundBadge(round.roundNumber),
                    style    = MaterialTheme.typography.labelMedium,
                    color    = muted,
                    modifier = Modifier.width(32.dp)
                )
                val contract = round.contract
                if (contract == null) {
                    // Skipped rounds are muted and carry no score.
                    Text(
                        text     = strings.skipped,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = muted,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    val outcome = if (round.won == true) strings.wonShort else strings.lostShort
                    Text(
                        text     = "${round.takerName} · ${contract.localizedName(locale)} · $outcome",
                        style    = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    round.playerScores[round.takerName]?.let { ScoreText(score = it, size = ScoreSize.S) }
                }
            }
        }
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
internal fun RoundHistoryRow(
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

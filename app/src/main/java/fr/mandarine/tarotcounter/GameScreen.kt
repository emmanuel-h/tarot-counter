package fr.mandarine.tarotcounter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.animation.AnimatedContent
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

    // Number of rounds the standings were last shown for. When a round has been
    // added since, the standings first appear as they were *before* that round,
    // then animate to the new totals and order (count-up + rows sliding, #204).
    // Starts at the current count, so resuming a game or undoing never animates.
    var standingsShownForRounds by remember { mutableIntStateOf(roundHistory.size) }

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
            // Also drop the contract, which (through LaunchedEffect(selectedContract))
            // clears points, bouts and bonuses: a round skipped while a half-filled
            // form was open must not leak those values into the next round.
            selectedContract = null
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

    // Light haptic feedback on taker selection and round confirmation (issue #204).
    val haptics = rememberHaptics()

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
    // Game over and score history replace the whole game view while they are open.
    // AnimatedContent fades through between the three (issue #204).
    val overlay = when {
        showFinalScore   -> GameOverlay.FINAL_SCORE
        showScoreHistory -> GameOverlay.HISTORY
        else             -> GameOverlay.NONE
    }
    val overlayReducedMotion = LocalReducedMotion.current
    AnimatedContent(
        targetState    = overlay,
        transitionSpec = { fadeThrough(overlayReducedMotion) },
        label          = "gameOverlay"
    ) { shownOverlay ->
    when (shownOverlay) {
    GameOverlay.FINAL_SCORE -> FinalScoreScreen(
        playerNames  = displayNames,
        roundHistory = roundHistory,
        onBack       = { showFinalScore = false },
        onNewGame    = onEndGame,
        onMainMenu   = onEndGame,  // both "New Game" and "Main Menu" navigate to the landing screen
        modifier     = modifier
    )
    GameOverlay.HISTORY -> ScoreHistoryScreen(
        playerNames  = displayNames,
        roundHistory = roundHistory,
        onBack       = { showScoreHistory = false },
        modifier     = modifier
    )
    GameOverlay.NONE -> {

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
                overline               = strings.roundHeader(currentRound),
                titleLeading           = {
                    PlayerAvatar(
                        name      = attacker,
                        seatIndex = displayNames.indexOf(attacker),
                        size      = AvatarSize.M
                    )
                },
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

          // AnimatedContent fades between "Who took?" and the round entry
          // (issue #204). Its target is the taker whose entry is open, or null.
          // transitionSpec is not @Composable, so the flag is read here first.
          val reducedMotion = LocalReducedMotion.current
          AnimatedContent(
              targetState    = if (roundEntryOpen) attacker else null,
              transitionSpec = { fadeThrough(reducedMotion) },
              label          = "roundEntry"
          ) { taker ->
          // AnimatedContent stacks its content like a Box: a Column keeps the
          // entry's sections one under another.
          Column(modifier = Modifier.fillMaxWidth()) {
          if (taker == null) {
            // ── Between rounds: scores first, then "Who took?" ─────────────────
            BetweenRoundsContent(
                currentDealer = currentDealer,
                displayNames  = displayNames,
                roundHistory  = roundHistory,
                selectedTaker = selectedAttacker,
                strings       = strings,
                locale        = locale,
                onTakerChosen = { name ->
                    haptics.play(HapticMoment.TAKER_SELECTED)
                    // A different taker invalidates the contract (and so the whole form).
                    if (name != selectedAttacker) selectedContract = null
                    selectedAttacker = name
                    roundEntryOpen   = true
                },
                onSeeAll      = { showScoreHistory = true },
                animateStandings = roundHistory.size > standingsShownForRounds,
                onStandingsShown = { standingsShownForRounds = it }
            )
          } else {
            // ── Round entry (Salon, issue #199) ───────────────────────────────
            //   Contract     [Petite ×1] [Garde ×2]      2×2 contract cards
            //                [G. sans ×4] [G. contre ×6]
            //   Bouts (0)(1)(2)(3)         needs 41     bout chips
            //   ┌ Points scored   (Attack|Defense) ┐
            //   │ 47 / 91                           │    big Cormorant number
            //   │ ✓ Made by 6 → Chloé +186          │    live result pill
            //   └───────────────────────────────────┘
            //   Partner (5 players)  avatar chips
            //   Bonuses / Chelem     (restyled in #200)
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL)
            ) {
                ContractCards(
                    selected = selectedContract,
                    label    = strings.contractLabel,
                    locale   = locale,
                    // Tapping the selected card again deselects it (collapses the form).
                    onSelect = { c -> selectedContract = if (selectedContract == c) null else c }
                )

                val contract = selectedContract
                if (contract != null) {
                    BoutChips(
                        bouts    = bouts,
                        label    = strings.boutsLabel,
                        helper   = strings.boutsNeeds(requiredPoints(bouts)),
                        onSelect = { bouts = it }
                    )

                    // The live result is computed from the same function the ViewModel
                    // uses to record the round, so the pill always matches the outcome.
                    val typedPoints = pointsText.toIntOrNull()
                    val preview = if (typedPoints != null && !pointsError) {
                        val takerPoints = if (defenderMode) 91 - typedPoints else typedPoints
                        previewRound(
                            playerNames = displayNames,
                            takerName   = taker,
                            contract    = contract,
                            details     = RoundDetails(
                                bouts          = bouts,
                                points         = takerPoints,
                                partnerName    = if (displayNames.size == 5) selectedPartner else null,
                                petitAuBout    = petitAuBout,
                                poignees       = poignees.toList(),
                                doublePoignees = doublePoignees.toList(),
                                triplePoignees = triplePoignees.toList(),
                                chelem         = chelem,
                                chelemPlayer   = chelemPlayer
                            )
                        )
                    } else null

                    PointsCard(
                        pointsText    = pointsText,
                        onPointsText  = { pointsText = it },
                        defenderMode  = defenderMode,
                        onCampChange  = { defender ->
                            if (defender != defenderMode) {
                                // Clear the value so it is never read for the wrong camp.
                                pointsText   = ""
                                defenderMode = defender
                            }
                        },
                        pointsError   = pointsError,
                        preview       = preview,
                        taker         = taker,
                        strings       = strings,
                        onDone        = { keyboardController?.hide() }
                    )

                    // ── Partner (5-player games only) ─────────────────────────
                    // The taker calls a silent partner; the taker can't call themself.
                    if (displayNames.size == 5) {
                        PartnerChips(
                            label    = strings.partnerCalledByTaker,
                            players  = displayNames,
                            taker    = taker,
                            selected = selectedPartner,
                            // Tapping the selected partner again clears the choice.
                            onSelect = { name ->
                                selectedPartner = if (selectedPartner == name) null else name
                            }
                        )
                    }
                }
            }

            // ── Bonuses: three rows, each opening a bottom sheet (issue #200) ──
            if (selectedContract != null) {
                Spacer(Modifier.height(Dimens.SpaceL))
                BonusesSection(
                    playerNames    = displayNames,
                    taker          = taker,
                    partner        = if (displayNames.size == 5) selectedPartner else null,
                    petitAuBout    = petitAuBout,
                    onPetitAuBout  = { petitAuBout = it },
                    poignees       = PoigneeDeclarations(poignees, doublePoignees, triplePoignees),
                    onPoignees     = { declared ->
                        poignees       = declared.simple
                        doublePoignees = declared.double
                        triplePoignees = declared.triple
                    },
                    atoutError     = atoutError,
                    atoutErrorText = strings.atoutCountError(totalDeclaredAtouts, TOTAL_ATOUTS_IN_DECK),
                    chelem         = chelem,
                    chelemPlayer   = chelemPlayer,
                    onChelem       = { outcome, player ->
                        chelem       = outcome
                        chelemPlayer = player
                    }
                )
                Spacer(Modifier.height(Dimens.SpaceM))
            }
            // end inline round details
          } // end round entry
          } // end Column
          } // end AnimatedContent

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
                        haptics.play(HapticMoment.ROUND_CONFIRMED)
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
    } // end GameOverlay.NONE
    } // end when
    } // end AnimatedContent
}

// The screens that can replace the game view.
private enum class GameOverlay { NONE, HISTORY, FINAL_SCORE }

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
    onSeeAll: () -> Unit,
    animateStandings: Boolean = false,
    onStandingsShown: (Int) -> Unit = {}
) {
    // Records that the standings are now shown for this many rounds (see
    // standingsShownForRounds in GameScreen). Runs after the first frame.
    LaunchedEffect(roundHistory.size) { onStandingsShown(roundHistory.size) }

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
                // The standings before the latest round: the animation's start.
                previous     = if (animateStandings && !LocalReducedMotion.current) computeStandings(displayNames, roundHistory.dropLast(1)) else null,
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

// Pause before the standings animate from the previous round to the new one,
// so the eye lands on the card first.
private const val STANDINGS_ANIMATION_DELAY_MS = 250L

// The ranked standings card. Every leader (several on a tie) gets a brass-tinted
// row, a brass ring around a large avatar, a "LEADING" overline and an XL score.
@Composable
private fun StandingsCard(
    standings: List<Standing>,
    leadingLabel: String,
    title: String,
    previous: List<Standing>? = null
) {
    // While true, the card shows `previous`; flipping it to false after a short
    // pause makes every score count to its new value and every row slide to its
    // new rank. Remembered once: later recompositions don't restart it.
    var showPrevious by remember { mutableStateOf(previous != null) }
    LaunchedEffect(Unit) {
        if (showPrevious) {
            delay(STANDINGS_ANIMATION_DELAY_MS)
            showPrevious = false
        }
    }
    val shown = if (showPrevious && previous != null) previous else standings
    // Each player's total before the round: where their score starts counting from.
    val previousTotals = previous?.associate { it.name to it.total }.orEmpty()

    SalonCard(
        modifier       = Modifier
            .fillMaxWidth()
            .testTag("standings_card")
            // The card is announced as "Standings" by screen readers.
            .semantics { contentDescription = title },
        contentPadding = PaddingValues(0.dp)
    ) {
        val reducedMotion = LocalReducedMotion.current
        Column {
            shown.forEachIndexed { index, standing ->
                // key(name): the row is tied to the player, not to its position, so
                // when the ranking changes Compose *moves* the row and
                // animatePlacement slides it to its new place (issue #204).
                key(standing.name) {
                    Column(modifier = Modifier.animatePlacement(enabled = !reducedMotion)) {
                        if (standing.isLeader) {
                            LeaderRow(standing, leadingLabel, previousTotals[standing.name])
                        } else {
                            StandingRow(standing, previousTotals[standing.name])
                        }
                        // Hairline between rows, not after the last one.
                        if (index < shown.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderRow(standing: Standing, leadingLabel: String, countFrom: Int? = null) {
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
            ScoreText(score = standing.total, size = ScoreSize.XL, animate = true, countFrom = countFrom)
            TrendArrow(standing.lastDelta)
        }
    }
}

@Composable
private fun StandingRow(standing: Standing, countFrom: Int? = null) {
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
        ScoreText(score = standing.total, size = ScoreSize.M, animate = true, countFrom = countFrom)
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
            // A minimum, not a fixed height: at 200 % font size the tile grows.
            .heightIn(min = 96.dp)
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

// ── Round entry building blocks (Salon, issue #199) ───────────────────────────

// A small muted label above a round-entry section, with an optional helper on the right.
@Composable
private fun EntryLabel(text: String, helper: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (helper != null) {
            Text(
                text  = helper,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// The four contracts as a 2 × 2 grid of cards: name in Cormorant on the left,
// multiplier on the right. The selected card is filled felt green, its multiplier
// in brass.
@Composable
private fun ContractCards(
    selected: Contract?,
    label: String,
    locale: AppLocale,
    onSelect: (Contract) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EntryLabel(label)
        // chunked(2) → two rows of two contracts.
        Contract.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                for (contract in row) {
                    val isSelected = contract == selected
                    val scheme = MaterialTheme.colorScheme
                    Surface(
                        onClick  = { onSelect(contract) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 60.dp)   // grows with large font sizes
                            .testTag("contract_${contract.name}")
                            .semantics { this.selected = isSelected },
                        shape    = MaterialTheme.shapes.medium,
                        color    = if (isSelected) scheme.primary else scheme.surface,
                        contentColor = if (isSelected) scheme.onPrimary else scheme.onSurface,
                        border   = BorderStroke(1.dp, if (isSelected) scheme.primary else scheme.outline),
                        shadowElevation = if (isSelected) 4.dp else 0.dp
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Long names ("Guard Against") shrink instead of wrapping.
                            AutoSizeText(
                                text     = contract.localizedName(locale),
                                style    = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text  = "×${contract.multiplier}",
                                style = MaterialTheme.typography.titleSmall,
                                // Brass on the felt when selected, muted otherwise.
                                color = if (isSelected) MaterialTheme.tarotColors.brassOnFelt
                                        else scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// The number of bouts (oudlers) as four pill chips, 0 to 3, with the points the
// taker needs on the right ("needs 41"). Selected chip: felt tint + felt border.
@Composable
private fun BoutChips(bouts: Int, label: String, helper: String, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EntryLabel(label, helper)
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            for (n in 0..3) {
                val isSelected = n == bouts
                val scheme = MaterialTheme.colorScheme
                Surface(
                    onClick  = { onSelect(n) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)   // grows with large font sizes
                        .testTag("bouts_chip_$n")
                        .semantics { this.selected = isSelected },
                    shape    = CircleShape,
                    color    = if (isSelected) scheme.primaryContainer else scheme.surface,
                    contentColor = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                    border   = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) scheme.primary else scheme.outline
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = n.toString(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// The points card: a large Cormorant number with "/ 91", an Attack | Defense toggle,
// and the live result pill once a valid number is typed.
@Composable
private fun PointsCard(
    pointsText: String,
    onPointsText: (String) -> Unit,
    defenderMode: Boolean,
    onCampChange: (defender: Boolean) -> Unit,
    pointsError: Boolean,
    preview: RoundPreview?,
    taker: String,
    strings: AppStrings,
    onDone: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    SalonCard(
        modifier       = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Dimens.SpaceM)
    ) {
        // ── Label + camp toggle ───────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = strings.pointsScored,
                style    = MaterialTheme.typography.labelMedium,
                color    = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            val campSize = rememberSharedAutoSizeState(strings.attackCamp)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.width(184.dp)) {
                listOf(false to strings.attackCamp, true to strings.defenseCamp)
                    .forEachIndexed { index, (defender, label) ->
                        SegmentedButton(
                            shape    = SegmentedButtonDefaults.itemShape(index, 2),
                            selected = defenderMode == defender,
                            onClick  = { onCampChange(defender) },
                            icon     = {},
                            colors   = salonSegmentedButtonColors(),
                            modifier = Modifier.testTag(if (defender) "camp_defense" else "camp_attack")
                        ) {
                            AutoSizeText(
                                text            = label,
                                modifier        = Modifier.padding(horizontal = 1.dp),
                                sharedSizeState = campSize
                            )
                        }
                    }
            }
        }

        // ── Big number + "/ 91" ───────────────────────────────────────────────
        // BasicTextField is the bare text input underneath Material's fields; we draw
        // our own underline so the number can be large and typographic.
        val underline = if (pointsError) scheme.error else scheme.primary
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            BasicTextField(
                value         = pointsText,
                onValueChange = { input ->
                    // Digits only, at most two (the maximum is 91).
                    if (input.all { it.isDigit() } && input.length <= 2) onPointsText(input)
                },
                singleLine      = true,
                textStyle       = MaterialTheme.typography.displayLarge.copy(color = scheme.onSurface),
                cursorBrush     = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier        = Modifier
                    .width(120.dp)
                    .testTag("points_input")
                    // Screen readers announce which camp's points are expected.
                    .semantics {
                        contentDescription = if (defenderMode) strings.defenderPointsLabel
                                             else strings.attackerPointsLabel
                    }
                    // 2 dp underline, red while the value is out of range.
                    .drawBehind {
                        val y = size.height - 1.dp.toPx()
                        drawLine(underline, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
                    },
                decorationBox = { inner ->
                    Box {
                        // Grey "0" hint while the field is empty.
                        if (pointsText.isEmpty()) {
                            Text(
                                text  = "0",
                                style = MaterialTheme.typography.displayLarge,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        inner()
                    }
                }
            )
            Text(
                text     = "/ 91",
                style    = MaterialTheme.typography.bodyLarge,
                color    = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        if (pointsError) {
            Text(
                text  = strings.pointsOutOfRange,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error
            )
        }

        // ── Live result pill ──────────────────────────────────────────────────
        if (preview != null) {
            val takerScore = preview.playerScores[taker] ?: 0
            val text = if (preview.won) {
                strings.resultMade(preview.margin, taker, takerScore.withSign())
            } else {
                strings.resultShort(preview.margin, taker, takerScore.withSign())
            }
            // Felt tint when made, red tint when short.
            val bg = if (preview.won) scheme.primaryContainer else scheme.errorContainer
            val fg = if (preview.won) scheme.onPrimaryContainer else scheme.onErrorContainer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg, MaterialTheme.shapes.small)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("live_result")
                    // Read as one sentence by screen readers.
                    .semantics(mergeDescendants = true) {},
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)
            ) {
                Icon(
                    imageVector        = if (preview.won) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null, // the sentence carries the meaning
                    tint               = fg,
                    modifier           = Modifier.size(18.dp)
                )
                Text(text = text, style = MaterialTheme.typography.bodyMedium, color = fg)
            }
        }
    }
}

// The 5-player partner choice: one avatar chip per player except the taker.
// The selected partner is filled felt green; tapping it again clears the choice.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartnerChips(
    label: String,
    players: List<String>,
    taker: String,
    selected: String?,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EntryLabel(label)
        // FlowRow wraps chips onto a second line when they don't fit.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
            verticalArrangement   = Arrangement.spacedBy(Dimens.SpaceS)
        ) {
            players.forEachIndexed { seat, name ->
                if (name == taker) return@forEachIndexed
                val isSelected = name == selected
                val scheme = MaterialTheme.colorScheme
                Surface(
                    onClick  = { onSelect(name) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("partner_$name")
                        .semantics { this.selected = isSelected },
                    shape    = CircleShape,
                    color    = if (isSelected) scheme.primary else scheme.surface,
                    contentColor = if (isSelected) scheme.onPrimary else scheme.onSurface,
                    border   = BorderStroke(1.dp, if (isSelected) scheme.primary else scheme.outline)
                ) {
                    Row(
                        modifier              = Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)
                    ) {
                        PlayerAvatar(name = name, seatIndex = seat, size = AvatarSize.S)
                        Text(text = name, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

package fr.mandarine.tarotcounter

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * FinalScoreScreen shows the game results when the player ends the game early
 * or when a natural end is declared.
 *
 * Layout:
 *   - Trophy icon + "Game Over" heading
 *   - Winner card (gold/amber secondaryContainer) showing name and final score
 *     (or "Tie!" with all co-winner names in case of a draw)
 *   - Full round-by-round score table, with winner column(s) highlighted
 *   - "New Game" button that returns to the setup screen
 *
 * The winner is the player with the highest cumulative total after all rounds.
 * If multiple players share the highest score, all are shown as co-winners.
 *
 * @param playerNames  Ordered list of player display names (fallbacks already resolved).
 * @param roundHistory All completed rounds in chronological order, oldest first.
 * @param onBack       Callback fired when the user taps "Back to Game" (returns to the active game).
 * @param onNewGame    Callback fired when the user taps "New Game" (navigates back to setup).
 * @param onMainMenu   Callback fired when the user taps "Main Menu" (navigates to the landing screen).
 * @param modifier     Passed from the parent (e.g. Scaffold inner padding).
 */
@Composable
fun FinalScoreScreen(
    playerNames: List<String>,
    roundHistory: List<RoundResult>,
    onBack: () -> Unit,
    onNewGame: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Read the active locale and resolve all strings once at the top of the composable.
    val strings = appStrings(LocalAppLocale.current)

    // `LocalContext.current` gives us the Activity context, which we need to:
    //   1. Call PdfExporter.generateScorePdf() (needs cacheDir)
    //   2. Call FileProvider.getUriForFile() to create a shareable content:// URI
    //   3. Call context.startActivity() to launch the OS share sheet
    val context = LocalContext.current

    // A coroutine scope tied to this composable's lifecycle. It is automatically
    // cancelled when the composable leaves the composition, preventing leaks.
    // We use it to run PDF generation on the IO dispatcher (file write).
    val coroutineScope = rememberCoroutineScope()

    // Shown as an AlertDialog when PDF generation throws an exception.
    var showExportError by remember { mutableStateOf(false) }

    // ── System back-button handling ───────────────────────────────────────────
    // Controls whether the leave-confirmation dialog is visible.
    // The dialog is triggered by the system back button (or gesture), not by the
    // in-screen back arrow — the arrow stays wired to onBack (return to game).
    var showLeaveConfirm by remember { mutableStateOf(false) }

    // BackHandler intercepts the Android system back button while this composable
    // is in the composition. Because FinalScoreScreen is placed *after* the
    // GameScreen-level BackHandler in the composition tree, this one takes priority
    // and GameScreen's handler is effectively shadowed.
    BackHandler { showLeaveConfirm = true }

    // Confirmation dialog — only rendered when showLeaveConfirm is true.
    // AlertDialog is a Material 3 modal that blocks interaction with the rest of
    // the screen until the user picks "Leave" or "Cancel".
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text(strings.backConfirmTitle) },
            text  = { Text(strings.backConfirmBody) },
            confirmButton = {
                // "Leave" navigates to the landing page (same as "New Game" button).
                AppTextButton(text = strings.backConfirmLeave, onClick = onNewGame)
            },
            dismissButton = {
                // "Cancel" closes the dialog and returns to the Final Score screen.
                AppTextButton(text = strings.cancel, onClick = { showLeaveConfirm = false })
            }
        )
    }

    // Error dialog shown if PDF generation fails (e.g. disk full).
    if (showExportError) {
        AlertDialog(
            onDismissRequest = { showExportError = false },
            title = { Text(strings.exportPdf) },
            text  = { Text(strings.exportPdfError) },
            confirmButton = {
                AppTextButton(text = strings.cancel, onClick = { showExportError = false })
            }
        )
    }

    // `computeFinalTotals` sums each player's per-round scores across all rounds.
    // It lives in GameModels so it can be unit-tested without Compose.
    val totals = computeFinalTotals(playerNames, roundHistory)

    // `findWinners` returns a list to handle ties: normally one name, multiple on a draw.
    val winners = findWinners(totals)

    // Build a set of column indices (1-based, because index 0 is the "Round" column)
    // that correspond to winner(s). Used to highlight those columns in the table.
    // Set<Int> gives O(1) membership checks inside the row composable.
    val winnerColumnIndices: Set<Int> = playerNames
        .mapIndexedNotNull { i, name -> if (name in winners) i + 1 else null }
        .toSet()

    // Box centers the content Column horizontally on wide screens (tablets in landscape).
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(
        modifier = Modifier
            .widthIn(max = MAX_CONTENT_WIDTH)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Screen header: back arrow + title ─────────────────────────────────
        // ScreenHeader is a shared composable (ScreenHeader.kt) that renders the
        // back arrow and screen title in a Row — the same pattern as ScoreHistoryScreen,
        // now unified into one place so both screens look identical at the top.
        ScreenHeader(title = strings.gameOver, onBack = onBack)

        // ── Decorative trophy icon ─────────────────────────────────────────────
        // Enlarged to 72dp and tinted gold (secondary) to make the game-ending moment
        // feel more dramatic. The icon is purely decorative — the title conveys meaning.
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.secondary  // gold/amber accent
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Winner card ───────────────────────────────────────────────────────
        // `secondaryContainer` is the gold/amber tinted container — aligns with the
        // trophy icon above and the winner-column highlight in the table.
        //
        // The card uses a scale-in + fade-in entry animation so it "pops" into view
        // when the screen first appears, giving the winner announcement more drama.
        // `visible` starts false and is set to true in a LaunchedEffect so the
        // animation fires exactly once on composition.
        var cardVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { cardVisible = true }

        AnimatedVisibility(
            visible = cardVisible,
            // scaleIn grows the card from 80% → 100%; fadeIn prevents a hard pop.
            enter = scaleIn(initialScale = 0.8f) + fadeIn()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (winners.size == 1) {
                        // Single winner ─ show "Winner", the name (with star medal), and score.
                        Text(
                            text = strings.winner,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Row so the star icon sits inline with the winner's name.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Small decorative star medal — contentDescription null because
                            // the winner's name text already conveys the meaning.
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = winners.first(),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        val score = totals[winners.first()] ?: 0
                        Text(
                            text = strings.scoreDisplay(score.withSign()),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else if (winners.isNotEmpty()) {
                        // Tie ─ list all co-winners. No star icon — no single champion.
                        Text(
                            text = strings.itsATie,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = winners.joinToString(" & "),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    // `winners.isEmpty()` can only happen with no players — impossible in practice.
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // ── Score table ───────────────────────────────────────────────────────
        // The table uses weighted columns (ScoreTableRow) so it always fills the
        // available width without horizontal scrolling, regardless of player count
        // (issue #129). Winner column(s) receive a secondaryContainer tint.
        if (roundHistory.isEmpty()) {
            // No rounds were played — show a simple notice instead of an empty table.
            Text(
                text = strings.noRoundsPlayed,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header row: localized "Round" label + one header per player name.
                // No score values for the header row — labels use the default colour.
                ScoreTableRow(
                    cells               = listOf(strings.roundColumn) + playerNames,
                    isHeader            = true,
                    winnerColumnIndices = winnerColumnIndices
                )
                HorizontalDivider()

                // buildScoreTableData() (GameModels.kt) handles the running-totals
                // accumulation loop — shared with ScoreHistoryScreen (issue #75).
                for (row in buildScoreTableData(playerNames, roundHistory)) {
                    ScoreTableRow(
                        cells               = row.cells,
                        isHeader            = false,
                        scoreValues         = row.scoreValues,
                        winnerColumnIndices = winnerColumnIndices
                    )
                }
            }   // end Column
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Action buttons row ────────────────────────────────────────────────
        // All three buttons share the row with equal widths via Modifier.weight(1f).
        // Using weight() instead of fillMaxWidth() is required inside a Row — each
        // child claims its proportional share of remaining space after unweighted
        // siblings are measured. With all three at 1f they each get exactly 1/3.
        //
        // Order (left → right):
        //   Back to Game (AppOutlinedButton) — return to the active game
        //   Main Menu    (AppOutlinedButton) — return to the landing screen
        //   New Game     (AppButton)         — primary CTA, filled container
        //
        // rememberSharedAutoSizeState ensures all three labels shrink together
        // to the same font size if any single label overflows its 1/3-width slot.
        // Keyed on the three label strings so a locale change resets the size.
        //
        // Arrangement.spacedBy puts the gap only *between* buttons (no outer margins),
        // keeping the row flush with the surrounding content padding.
        val buttonSizeState = rememberSharedAutoSizeState(
            strings.backToGame, strings.mainMenu, strings.newGame
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left: resume the current game if "End Game" was tapped by mistake.
            AppOutlinedButton(
                text            = strings.backToGame,
                onClick         = onBack,
                modifier        = Modifier.weight(1f),
                sharedSizeState = buttonSizeState
            )
            // Center: return to the landing screen (main menu).
            AppOutlinedButton(
                text            = strings.mainMenu,
                onClick         = onMainMenu,
                modifier        = Modifier.weight(1f),
                sharedSizeState = buttonSizeState
            )
            // Right: primary CTA — start a brand-new game (goes to setup screen).
            AppButton(
                text            = strings.newGame,
                onClick         = onNewGame,
                modifier        = Modifier.weight(1f),
                sharedSizeState = buttonSizeState
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Export PDF button ─────────────────────────────────────────────────
        // Placed on its own row below the primary action buttons because it is
        // a secondary action (not needed every game). Using AppOutlinedButton
        // keeps it visually lighter than the primary "New Game" button.
        //
        // On click:
        //   1. PdfExporter.generateScorePdf() builds the PDF on the IO thread.
        //   2. FileProvider converts the file path to a content:// URI so the
        //      receiving app can read our private cache file.
        //   3. ACTION_SEND launches the OS share sheet (PDF viewer, Drive, etc.).
        AppOutlinedButton(
            text    = strings.exportPdf,
            onClick = {
                coroutineScope.launch {
                    try {
                        // Generate the PDF on the IO dispatcher — writing to disk
                        // is I/O work and should never block the main (UI) thread.
                        val file = withContext(Dispatchers.IO) {
                            PdfExporter.generateScorePdf(context, playerNames, roundHistory, strings)
                        }

                        // FileProvider turns the private file path into a
                        // content:// URI that external apps are allowed to read.
                        // The authority string must match AndroidManifest.xml.
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )

                        // Build a share intent for the PDF.
                        // FLAG_GRANT_READ_URI_PERMISSION grants the chosen app
                        // temporary read access to the FileProvider URI.
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type  = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        // Intent.createChooser wraps the share intent in a system
                        // picker so the user can select which app to open the PDF with.
                        context.startActivity(
                            Intent.createChooser(shareIntent, strings.exportPdf)
                        )
                    } catch (e: Exception) {
                        // Surface any unexpected failure (disk full, PdfDocument error,
                        // FileProvider misconfiguration, etc.) as an error dialog.
                        showExportError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }   // end Column
    }   // end Box
}


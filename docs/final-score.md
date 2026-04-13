# Final Score Screen

## Purpose

The Final Score Screen is shown when the user taps **End Game** at any point during a game. It summarises the results and declares the winner.

## Accessing the Screen

- **Step 1 (contract selection):** An "End Game" button is always visible in the top-right button row, next to the "History" button (if rounds exist).
- **Step 2 (round details form):** The same "End Game" button appears in the header of the form, alongside "History" (when available).

The game can be ended at any time — even before the first round is played.

## Layout

```
← Game Over          ← shared ScreenHeader (back arrow + title in one row)

[ Trophy icon (72dp, gold) ]

╔══════════════════════════╗  ← gold/amber secondaryContainer; scale-in animation
║         Winner           ║  ← "Winner" label (or "It's a tie!")
║   ★ Alice                ║  ← Star icon + winner name in bold headline
║         +200 pts         ║  ← Final cumulative score
╚══════════════════════════╝

Round | Alice  | Bob   | Charlie
  1   |  +50   |  -25  |  -25
  2   |  +200  |  -100 |  -100
        ↑ winner column highlighted

[ Back to game ]  [ Main Menu ]  [ New Game ]
    outlined          outlined      filled

[ Share PDF ]  [ Save to device ]   ← outlined, equal width (issue #138)
```

## Winner Determination

1. Each player's scores from all completed rounds are summed using `computeFinalTotals()` in `GameModels.kt`.
2. The player(s) with the highest total are returned by `findWinners()`.
3. If exactly one player has the highest score, they are shown as the winner.
4. If two or more players share the highest score, the card shows **"It's a tie!"** with all co-winner names.

## Empty State

If the user ends the game before any round is played, the score table is replaced by a **"No rounds played"** message. The winner card shows a three-way tie at 0 in this case (or however many players there are).

## Table Columns

| Column | Width | Content |
|---|---|---|
| Round | 64 dp | Round number |
| Player (×N) | 80 dp | Cumulative score after that round |

The **winner's column** is highlighted with a gold/amber `secondary` tint and bold text throughout the table, making it easy to track the winner's score progression.

## Winner Card Visual Polish (issue #7)

The winner card was enhanced to feel more celebratory:

| Element | Before | After |
|---|---|---|
| Trophy icon size | 48dp | 72dp |
| Trophy icon tint | `primary` (green) | `secondary` (gold/amber) |
| Winner card color | `primaryContainer` | `secondaryContainer` (gold/amber) |
| Winner name | Plain text | Star icon (`Icons.Default.Star`) + name inline |
| New Game button text | `labelLarge` (default) | `titleMedium` for more visual weight |
| Winner card entry | Instant | Scale-in (80%→100%) + fade-in animation |

The tie scenario ("It's a tie!") does not show the star icon, since there is no single champion.

## Score Colour Coding

Score cells throughout the table use semantic colours for instant legibility:

| Score | Colour token | Visual |
|---|---|---|
| Positive (≥ 0) | `MaterialTheme.colorScheme.primary` | Green |
| Negative (< 0) | `MaterialTheme.colorScheme.error` | Red |

Both tokens adapt automatically to light and dark themes. The same `scoreColor()` helper (defined in `ScoreColor.kt`) is used by `CompactScoreboard` (GameScreen), `FinalScoreScreen`, and `ScoreHistoryScreen` so the convention is consistent everywhere scores appear.

## Navigation

| Action | Where | What it does |
|---|---|---|
| Back arrow (top-left) | `ScreenHeader` | Returns to the active game round. No state is lost. |
| **Back to Game** | `OutlinedButton` (bottom-left) | Same as the back arrow — resumes the current game. |
| **Main Menu** | `OutlinedButton` (bottom-centre) | Navigates to the landing screen. |
| **New Game** | `Button` (bottom-right, primary) | Navigates to the setup screen. All game state is discarded. |
| **Share PDF** | `OutlinedButton` (left, below primary row) | Generates a PDF score sheet and opens the OS share sheet (Gmail, Drive, PDF viewer, etc.). |
| **Save to device** | `OutlinedButton` (right, below primary row) | Generates a PDF score sheet and opens the system file-save picker (DocumentsUI) so the user can choose a permanent save location (e.g. Downloads). No storage permission required. |

All three bottom buttons appear on the same horizontal line with equal widths (`Modifier.weight(1f)`) and an 8 dp gap between them (`Arrangement.spacedBy`). A `rememberSharedAutoSizeState` is shared across all three labels so they always display at the same font size — the smallest needed by the longest label. This ensures they fit on all supported screen sizes (min SDK 24, down to ~360 dp wide) without overflow.

The back arrow and "Back to Game" button serve the same purpose: letting the user dismiss the final score screen if they tapped **End Game** by mistake.

**Main Menu** and **New Game** both navigate to the landing/setup screen; they are currently wired to the same `onEndGame` callback. The distinction is semantic — in future, "New Game" could pre-fill the same player list while "Main Menu" always starts blank.

## PDF Export (issue #138)

The final score screen offers two ways to export the game scores as an A4 PDF modelled on the official FFT scoring table (R-RO201206.pdf, page 10):

| Button | Flow |
|---|---|
| **Share PDF** | Generates PDF → `FileProvider` URI → `Intent.ACTION_SEND` → OS share sheet (Gmail, Drive, PDF viewer, …) |
| **Save to device** | Generates PDF → `Intent.ACTION_CREATE_DOCUMENT` → system file-save picker → user picks location → bytes copied to chosen URI |

### How it works

1. `PdfExporter.generateScorePdf()` draws the score table onto an `android.graphics.pdf.PdfDocument` page and writes it to `cacheDir/tarot_scores.pdf`. No external library is needed — Android's built-in PDF API is available since API 19.
2. **Share PDF**: `FileProvider` converts the private cache path to a `content://` URI; an `Intent.ACTION_SEND` carries it to the OS share sheet.
3. **Save to device**: `ActivityResultContracts.CreateDocument("application/pdf")` opens DocumentsUI so the user can navigate to Downloads (or any other location) and save the file there. No storage permission is required — the OS grants write access only to the URI the user explicitly chose.

### PDF layout

```
              TAROT                           13/04/2026
 ─────────────────────────────────────────────────────
  Manche │   Alice   │   Bob    │  Charlie
 ────────┼───────────┼──────────┼──────────
    1    │   +50     │  -25     │  -25
    2    │   +20     │  -10     │  -10
 ════════╪═══════════╪══════════╪══════════
  Total  │   +20     │  -10     │  -10
```

Player names longer than 12 characters are truncated with `…` to prevent column overflow in 5-player games.

## Related Files

- `FinalScoreScreen.kt` — Composable implementation
- `PdfExporter.kt` — PDF generation logic (`PdfDocument` API)
- `AndroidManifest.xml` — `FileProvider` declaration
- `res/xml/file_paths.xml` — FileProvider path configuration
- `ScreenHeader.kt` — Shared back-arrow + title header used by this screen and `ScoreHistoryScreen`
- `GameModels.kt` — `computeFinalTotals()` and `findWinners()` pure functions
- `GameScreen.kt` — bottom-bar **End Game** button (OutlinedButton), `showFinalScore` state, routing
- `FinalScoreScreenTest.kt` — UI tests (including Export PDF button visibility)
- `PdfExporterTest.kt` — Instrumented tests for PDF file generation
- `GameModelsTest.kt` — Unit tests for `computeFinalTotals` and `findWinners`

# Final Score Screen

## Purpose

The Final Score Screen is shown when the user taps **End Game** at any point during a game. It summarises the results and declares the winner.

## Accessing the Screen

- **Step 1 (contract selection):** An "End Game" button is always visible in the top-right button row, next to the "History" button (if rounds exist).
- **Step 2 (round details form):** The same "End Game" button appears in the header of the form, alongside "History" (when available).

The game can be ended at any time — even before the first round is played.

## Layout

```
[ Trophy icon ]
   Game Over

╔══════════════════════════╗
║         Winner           ║  ← "Winner" label (or "It's a tie!")
║         Alice            ║  ← Winner name in bold headline
║         +200 pts         ║  ← Final cumulative score
╚══════════════════════════╝

Round | Alice  | Bob   | Charlie
  1   |  +50   |  -25  |  -25
  2   |  +200  |  -100 |  -100
        ↑ winner column highlighted

[          New Game          ]
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

The **winner's column** is highlighted with a `secondaryContainer` tint and bold text throughout the table, making it easy to track the winner's score progression.

## Navigation

- **New Game** — navigates back to the setup screen (`LandingScreen`). All game state is discarded.

## Related Files

- `FinalScoreScreen.kt` — Composable implementation
- `GameModels.kt` — `computeFinalTotals()` and `findWinners()` pure functions
- `GameScreen.kt` — `EndGameButton` composable, `showFinalScore` state, routing
- `RoundDetailsForm.kt` — `EndGameButton` in the form header
- `FinalScoreScreenTest.kt` — UI tests
- `GameModelsTest.kt` — Unit tests for `computeFinalTotals` and `findWinners`

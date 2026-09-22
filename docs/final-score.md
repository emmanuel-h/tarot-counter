# Final Score (Game Over) Screen

## Purpose

The Game Over screen is shown when the user taps **End Game** during a game (after at least one round). It celebrates the winner, ranks every player, and shows how the scores evolved.

## Layout (Salon redesign, issue #201)

```
┌──────────────────────────────┐
│ ←  Game Over                 │  SalonTopBar (back arrow = back to game)
│ ┌── felt card ─────────────┐ │  FeltCard, scale-in + fade-in once
│ │         [trophy]         │ │  brass trophy
│ │          WINNER          │ │  "IT'S A TIE!" on a tie
│ │        Player 1          │ │  every co-winner, joined with " & "
│ │          +167            │ │  brass score
│ │   2 rounds · 3 players   │ │
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │  ranking card
│ │ 1 (1) Player 1     +167  │ │  leaders: brass rank + tint
│ │ 2 (3) Player 3       -7  │ │
│ │ 3 (2) Player 2     -160  │ │
│ └──────────────────────────┘ │
│              See all rounds  │  → round-by-round table
│ Score over time              │
│ +167 ┤      ╱‾‾‾‾‾           │  one line per player, player tone
│   +0 ┼──────────── (dashed)  │  zero baseline
│ -160 ┤         ╲___          │
│        1          2          │  round ticks + a few labels
│ ● Player 1 ● Player 2 …      │  colour key
│ [          New Game        ] │  primary
│ [          Main Menu       ] │  outlined
│          Back to game        │  text button
└──────────────────────────────┘
```

## Winner card

1. `computeFinalTotals()` (`GameModels.kt`) sums each player's round scores.
2. `findWinners()` returns the player(s) with the highest total.
3. One winner: the card shows a "WINNER" overline, the name and the score. Two or more tied: it shows "IT'S A TIE!" and every co-winner's name.

The card is a `FeltCard`: a deep felt-green ground with ivory text. The trophy, the overline and the score use `tarotColors.brassOnFelt`. Screen readers read the card as a single announcement (`mergeDescendants`).

## Ranking

The ranking comes from `computeStandings()` (`Standings.kt`, shared with the game screen). It uses competition ranks, so tied players share a rank (1, 1, 3). Each row shows the rank, avatar, name and `ScoreText`. Leader rows get a brass rank and a soft brass tint.

## Score over time chart

A Compose `Canvas` draws one line per player, in that player's tone (the same colour as their avatar). It also draws a dashed zero baseline, a tick for every round, and the highest and lowest values on the left. A legend under the chart repeats the colours with names. For screen readers, the canvas is described as "Line chart of every player's cumulative score over N rounds".

The data comes from pure functions in `ScoreChart.kt`, covered by `ScoreChartTest`:

| Function | Purpose |
|---|---|
| `cumulativeSeries(playerNames, rounds)` | For each player, the total after each round, starting at 0. Skipped rounds repeat the previous total. |
| `chartBounds(series)` | The y-axis range. It always includes 0; a flat chart gets −1..1. |
| `xAxisLabels(roundCount)` | Which round numbers to print: every round up to 6, otherwise a stepped subset. The first and last rounds are always included, with at most `MAX_X_LABELS` labels. |

## Round-by-round table

The full table (cumulative totals per round, winner columns highlighted) is no longer on this screen. **See all rounds** opens the Score History screen on top of Game Over. Its back arrow, or the system back button, returns to Game Over, not to the game.

## Empty state

If no round was played, the ranking and the chart are replaced by "No rounds played". This is rare: ending a game with no rounds normally cancels it silently.

## Score colour coding

`ScoreText` and the history table use `scoreColor()`: green (`tarotColors.positive`) for totals ≥ 0 and red (`tarotColors.negative`) below 0. The winner card's score is the exception: it is always brass, which reads well on the felt.

## Navigation

| Action | Where | What it does |
|---|---|---|
| Back arrow (top-left) | `SalonTopBar` | Returns to the active game. No state is lost. |
| **New Game** | Primary button | Navigates to the setup screen. |
| **Main Menu** | Outlined button | Navigates to the landing screen. |
| **Back to game** | Text button | Same as the back arrow. |
| **See all rounds** | Text link under the ranking | Opens the round-by-round table. |
| System back | — | Shows a "Leave the game?" confirmation (issue #38). |

The buttons are stacked full width in order of importance. **Main Menu** and **New Game** both go to the landing screen today; "New Game" could later pre-fill the same players.

There is no PDF export in the app, so the redesign had none to keep.

## Related files

- `FinalScoreScreen.kt`: the screen, `WinnerCard`, `RankingCard`, `ScoreChart`, `ChartLegend`
- `ScoreChart.kt`: pure chart data (`ScoreChartTest`)
- `Standings.kt`: `computeStandings()` (`StandingsTest`)
- `GameModels.kt`: `computeFinalTotals()` and `findWinners()` (`GameModelsTest`)
- `FinalScoreScreenTest.kt`: UI tests

# Score History Screen

## Purpose

The score history screen shows the **evolution of cumulative scores** across all completed rounds in a single table view. It is accessed from the game screen once at least one round has been recorded.

## How to Access

A bar-chart icon button (⬛) appears to the right of the "Scores" heading on the game screen after the first round completes. Tapping it opens the score history table. Tapping the back arrow returns to the game without losing any state.

## Table Layout

```
| Round | Alice | Bob  | Charlie |
|-------|-------|------|---------|
|   1   |  +50  | -25  |  -25   |
|   2   |  +20  | -10  |  -10   |
|   3   |  +80  | -40  |  -40   |
```

- **Rows** — one per completed round, oldest first (top = round 1).
- **Columns** — one per player, in setup order, plus a "Round" column on the left.
- **Cell values** — the player's **running total** after that round (not the per-round delta). Positive values are prefixed with `+` for quick readability.
- **Skipped rounds** — appear as a row where all scores are unchanged from the previous row (since skipped rounds contribute 0 points to everyone).

## Scrolling

- **Horizontal scroll** — for 5-player games where the table is wider than the screen.
- **Vertical scroll** — for long games with many rounds.

## Navigation

The screen is rendered as a local overlay inside `GameScreen` (controlled by a `showScoreHistory` boolean state). No changes to `Navigation.kt` or `MainActivity` were needed; all routing is contained within `GameScreen.kt`.

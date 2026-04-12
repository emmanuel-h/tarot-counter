# Score History Screen

## Purpose

The score history screen shows the full history of a game in one of two display modes:

- **Table view** (default) — cumulative score table, one row per completed round.
- **List view** — round-by-round detail list, newest round first (previously shown at the bottom of the game screen).

Users can switch between the two modes at any time using the segmented toggle at the top of the screen.

## How to Access

A bar-chart icon button (⬛) is always visible in the top-left corner of the game screen. Tapping it opens the score history screen. Tapping the back arrow returns to the game without losing any state.

## View Modes

### Table view (default)

```
| Round | Alice | Bob  | Charlie |
|-------|-------|------|---------|
|   1   |  +50  | -25  |  -25   |
|   2   |  +20  | -10  |  -10   |
|   3   |  +80  | -40  |  -40   |
```

- **Rows** — one per completed round, oldest first (top = round 1).
- **Columns** — one per player, in setup order, plus a "Round" column on the left.
- **Cell values** — the player's **running total** after that round (not the per-round delta). Positive values are prefixed with `+`. Positive scores appear in green (`primary`) and negative in red (`error`) — see `ScoreColor.kt`.
- **Skipped rounds** — appear as rows where all scores are unchanged from the previous row.

### List view

```
●  Round 2: Bob — Prise · 0 bouts · 50 pts — Lost (-31)
●  Round 1: Alice — Garde · 2 bouts · 56 pts — Won (+80)
```

Rounds are displayed **newest first**. Each row begins with a coloured **●** indicator:

| Colour       | Outcome |
|--------------|---------|
| Green (primary) | Won  |
| Red (error)  | Lost    |
| Grey (muted) | Skipped |

The `RoundHistoryRow` composable (in `GameScreen.kt`) handles this layout — it was moved here from the bottom of the game screen as part of issue #136.

## Toggle

The view toggle is a `SingleChoiceSegmentedButtonRow` with two segments:

| Segment label (EN) | Segment label (FR) | View shown |
|--------------------|--------------------|------------|
| Table              | Tableau            | Cumulative score table |
| List               | Liste              | Round-by-round detail list |

The toggle defaults to **Table** every time the screen is opened (state is not persisted across sessions).

## Scrolling

Both views scroll vertically as part of the outer `Column`. For table view specifically, all columns share the available width equally via `weight(1f)` so there is no horizontal scroll (issue #129).

## Navigation

The screen is rendered as a local overlay inside `GameScreen` (controlled by a `showScoreHistory` boolean state). No changes to `Navigation.kt` or `MainActivity` were needed; all routing is contained within `GameScreen.kt`.

# Score History Screen

## Purpose

The score history screen shows the full history of a game in one of two views, switched with a segmented toggle:

- **Table** (default): cumulative totals, one row per completed round.
- **List**: one card per round with its details, newest first.

## How to access

- From the game screen: the chart icon in the top bar, or **See all** under *Last rounds*.
- From the Game Over screen: **See all rounds**. There, the back arrow returns to Game Over.

The back arrow returns to where the screen was opened from, without losing any state.

## Layout (Salon restyle, issue #202)

```
┌──────────────────────────────┐
│ ←  Score history             │  SalonTopBar
│ (    Table    |    List    ) │  segmented toggle (felt when selected)
│ ┌──────────────────────────┐ │
│ │ Round  (1)    (2)    (3) │ │  sticky header: avatar + name per player
│ │      Player 1 Player 2 … │ │  leader column(s) tinted brass
│ │   1   +116    -58    -58 │ │  hairline rows, tabular figures,
│ │   2   +116    -58    -58 │ │  green ≥ 0 / red < 0
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

The top bar and the toggle stay in place. Only the table or the list scrolls, so the table header stays pinned (`LazyColumn` + `stickyHeader`).

### Table view

- **Rows**: one per completed round, oldest first. Each cell holds the player's **running total** after that round (from `buildScoreTableData()`), not the round delta. Skipped rounds repeat the previous totals.
- **Header**: pinned while scrolling. It shows each player's avatar and name above their column.
- **Leader column(s)**: the current leader, or every tied leader, gets a low-opacity brass tint over the whole column (`leaderColumns()`). It replaces the old saturated orange.
- **Hairlines** separate the rows. Numbers use the theme's tabular figures, so digits line up.
- **Width**: columns share the card width equally. If a player column would be narrower than 64 dp (`historyTableScrolls()`), for example 5 players on a narrow phone, every column keeps 64 dp and the table scrolls **horizontally** instead.

### List view

```
┌──────────────────────────────────────┐
│ (R2)  Skipped                        │  muted
└──────────────────────────────────────┘
┌──────────────────────────────────────┐
│ (R1) (1) Player 1 · Guard       +116 │  taker avatar, contract, taker delta
│          0 bouts · 60 pts  [Won]     │  Won = felt tint, Lost = red tint
└──────────────────────────────────────┘
```

Each round is a `SalonCard` showing:

- a round badge ("R4" / "M4")
- the taker's avatar and name, and the contract
- "N bouts · N pts"
- a **Won** or **Lost** chip
- the taker's score change

Skipped rounds show "Skipped" in a muted colour. The chips carry the test tags `round_indicator_won` / `round_indicator_lost`, and the skipped label carries `round_indicator_skipped`.

### Empty state

Before the first round, both views show a muted chart icon and "No rounds played yet. Scores appear here after the first round."

## Pure logic

| Function | File | Purpose |
|---|---|---|
| `buildScoreTableData(playerNames, rounds)` | `GameModels.kt` | Running totals per round, formatted with a sign |
| `historyTableScrolls(availableWidthDp, playerCount)` | `ScoreHistoryLogic.kt` | Whether the table must scroll sideways |
| `leaderColumns(playerNames, rounds)` | `ScoreHistoryLogic.kt` | Which columns get the brass tint |

These are covered by `GameModelsTest` and `ScoreHistoryLogicTest`.

## Toggle

| Segment (EN) | Segment (FR) | View |
|---|---|---|
| Table | Tableau | Cumulative score table |
| List | Liste | Round cards |

The toggle defaults to **Table** every time the screen opens.

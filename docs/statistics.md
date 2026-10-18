# Player Statistics

Issue #229 (part of #224). The **Statistics** screen shows per-player figures computed from every completed game saved on the device.

## Navigation

The home screen top bar has a leaderboard icon (label "Statistics" / "Statistiques") left of the settings gear. It opens the screen. The back arrow and the system back button return to the home screen.

## Layout

```
┌──────────────────────────────────┐
│ ←  Statistics                    │  SalonTopBar
│ Based on 7 saved games           │  muted line
│ ┌──────────────────────────────┐ │  one SalonCard per player
│ │ (A) Alice                    │ │  PlayerAvatar + name
│ │ GAMES  WINS  TAKES AVG SCORE │ │  four key figures
│ │   7     3     24 %    +45    │ │  (average is a ScoreText)
│ │ ──────────────────────────── │ │
│ │ Success by contract          │ │
│ │ Prise        3/4 made   75 % │ │
│ │ Garde        1/2 made   50 % │ │
│ │ Garde Sans               –   │ │  never taken → "–"
│ │ Garde Contre             –   │ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```

When no game is saved yet, an empty-state message replaces the cards.

## Rules

All the logic lives in `PlayerStats.kt` (pure Kotlin, covered by `PlayerStatsTest`):

| Figure | Definition |
|---|---|
| Games | Saved games the player sat in. Games with no scored round (no rounds, or only skipped ones) are ignored. |
| Wins | Games the player finished with the best total (`findWinners`). On a tie for first, every co-winner gets the win. |
| Takes | Rounds taken ÷ rounds played, as a percentage. Skipped rounds count as neither. |
| Avg score | Sum of the final totals ÷ games, rounded to the nearest integer. |
| Success by contract | For each contract: rounds made ÷ rounds taken with it. "–" when it was never taken. |

- **Player identity**: names are matched ignoring case and surrounding spaces (`playerKey`), so "Alice" and " alice" are the same person. The card shows the spelling of the most recent game.
- **Order**: most wins first, then most games, then name (alphabetical, ignoring case).
- Final totals are recomputed from the rounds (`computeFinalTotals`), so the statistics always match the score sheet.

## Limitations

- Statistics only cover the games kept on the device: the last 20 completed games (`MAX_SAVED_GAMES` in `GameStorage.kt`).
- The game in progress is not counted until it is ended.
- A player who changes the spelling of their name ("Alice" → "Alicia") appears twice.

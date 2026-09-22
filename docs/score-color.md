# Score Colour Coding

## Purpose

All score values shown in the app are colour-coded so players can read standings at a glance without having to parse numbers:

| Score | Colour token | Light theme | Dark theme |
|---|---|---|---|
| Positive (≥ 0) | `tarotColors.positive` | Green `#2E6B45` | Light green `#7FCB98` |
| Negative (< 0) | `tarotColors.negative` | Red `#9B2C2C` | Coral `#F2A195` |

## Helper: `scoreColor()`

A single `@Composable` function in `UiComponents.kt` encapsulates the decision:

```kotlin
@Composable
fun scoreColor(total: Int): Color =
    if (total >= 0) MaterialTheme.tarotColors.positive
    else MaterialTheme.tarotColors.negative
```

Using the Salon theme tokens (not hardcoded hex values — see `docs/theme.md`) ensures the colours automatically adapt to light/dark themes.

## Where it is used

| Location | What is coloured |
|---|---|
| `ScoreText` (UiComponents) | Standings, last rounds, past games, game-over ranking, round cards |
| History table cells (ScoreHistoryScreen) | Each player's running total |

The winning score on the felt winner card (Game Over) is the one exception: it is always brass (`tarotColors.brassOnFelt`), which reads well on the felt.

## Leader column (Score history table)

The current leader's column (every tied leader's) in the history table uses a soft brass background: `MaterialTheme.tarotColors.winnerHighlight` (`#F3E7C9` light / `#2F2A1C` dark) at 60 % opacity. The score text colour is still applied on top (green or red), so the semantic meaning is preserved even for the highlighted column. Both colours reach 4.5:1 on the highlight (checked by `SalonPaletteTest`).

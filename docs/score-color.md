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
| `CompactScoreboard` (GameScreen) | Each player's cumulative score text |
| `ScoreTableRow` (ScoreHistoryScreen) | Player score cells in the history table |
| `FinalScoreTableRow` (FinalScoreScreen) | Player score cells in the final results table |

## Winner Column (FinalScoreScreen only)

The winner's column in the final score table uses a soft brass background (`MaterialTheme.tarotColors.winnerHighlight`, `#F3E7C9` light / `#2F2A1C` dark) instead of the default surface colour. The score text colour is still applied on top (green or red), so the semantic meaning is preserved even for the highlighted column. Both colours reach 4.5:1 on the highlight (checked by `SalonPaletteTest`).

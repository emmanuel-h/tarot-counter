# Score Colour Coding

## Purpose

All score values shown in the app are colour-coded so players can read standings at a glance without having to parse numbers:

| Score | Colour token | Light theme | Dark theme |
|---|---|---|---|
| Positive (≥ 0) | `colorScheme.primary` | Deep forest green `#1B5E20` | Sage green `#A5D6A7` |
| Negative (< 0) | `colorScheme.error` | Material 3 red | Material 3 error red |

## Helper: `scoreColor()`

A single `@Composable` function in `ScoreColor.kt` encapsulates the decision:

```kotlin
@Composable
fun scoreColor(total: Int): Color =
    if (total >= 0) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.error
```

Using `MaterialTheme` tokens (not hardcoded hex values) ensures the colours automatically adapt to light/dark themes.

## Where it is used

| Location | What is coloured |
|---|---|
| `CompactScoreboard` (GameScreen) | Each player's cumulative score text |
| `ScoreTableRow` (ScoreHistoryScreen) | Player score cells in the history table |
| `FinalScoreTableRow` (FinalScoreScreen) | Player score cells in the final results table |

## Winner Column (FinalScoreScreen only)

The winner's column in the final score table uses a gold/amber `secondary` background (`MaterialTheme.colorScheme.secondary`) instead of the default surface colour. The score text colour is still applied on top (green or red), so the semantic meaning is preserved even for the highlighted column.

# Share the Score Sheet

Issue #234 (part of #224). From the Game Over screen, players can send the result of a game to a chat or an email, as **text** or as an **image**.

## Flow

```
Game Over                       Share dialog                    Android share sheet
┌──────────────────────────┐    ┌──────────────────────────┐    ┌──────────────────┐
│ ←  Game Over         [⇪] │ →  │ Share the score sheet    │ →  │ WhatsApp  Gmail  │
│ ┌── felt card ─────────┐ │    │ [       As text        ] │    │ Messages  Drive  │
│ │ WINNER  Alice  +150  │ │    │ [       As image       ] │    │ …                │
│ └──────────────────────┘ │    │                   Cancel │    └──────────────────┘
└──────────────────────────┘    └──────────────────────────┘
```

- The share icon sits in the `SalonTopBar` actions. It is hidden before the first round, when there is nothing to share.
- The dialog uses `AppOutlinedButton` for the two formats and `AppTextButton` for Cancel.

## Text format

`scoreSheetText()` gives one line per round instead of an aligned table, because messaging apps use proportional fonts:

```
Tarot Counter — Game Over
Winner: Alice (+150)
Sat 13 Sep · 3 rounds · 3 players

Final ranking
1. Alice +150
2. Chloé -30
3. Bob -120

Round by round
R1 · Alice · Guard · Won → Alice +120 · Bob -60 · Chloé -60
R2 · Skipped → Alice +120 · Bob -60 · Chloé -60
R3 · Bob · Small · Lost → Alice +150 · Bob -120 · Chloé -30
```

A tie reads `Tie: Bob & Chloé (+25)`. Every line is localized (EN / FR) through `AppStrings`.

## Image format

A 1080 px wide PNG in the light Salon palette, whatever the app theme, because it is read in someone else's chat:

```
┌──────────────────────────────────────────┐
│ Tarot Counter — Game Over   (felt band)  │  ivory title, brass winner line
│ Winner: Alice (+150)                     │
│ Sat 13 Sep · 3 rounds · 3 players        │
├──────────────────────────────────────────┤
│ Final ranking                            │
│ 1  Alice                           +150  │  names in seat colour, scores
│ 2  Chloé                            -30  │  green / red
│ Round by round                           │
│       Alice     Bob     Chloé            │
│ R1    +120      -60      -60             │  running totals
└──────────────────────────────────────────┘
```

The geometry comes from `scoreSheetLayout(playerCount, roundCount)` (pure, tested). `ScoreSheetShare.kt` paints it with `android.graphics.Canvas` in the Figtree font, off the main thread. Long header lines shrink to fit, and long names are cut to their column.

The file is written to `cache/shared/tarot-score-sheet.png`, always with the same name, so the cache never grows. It is shared through a `FileProvider` (`${applicationId}.fileprovider`, `res/xml/file_paths.xml`). The receiving app gets a temporary read grant (`FLAG_GRANT_READ_URI_PERMISSION`), so no storage permission is needed. If writing fails, a toast says so.

## Files

| File | Role |
|---|---|
| `ScoreSheet.kt` | Pure: `buildScoreSheet`, `scoreSheetText`, `scoreSheetLayout`, `ShareFormat` (tested by `ScoreSheetTest`) |
| `ScoreSheetShare.kt` | Android: draws the PNG, sends `ACTION_SEND` intents (excluded from PIT) |
| `FinalScoreScreen.kt` | Share icon + dialog; `onShare` parameter lets UI tests observe the choice |

## Limitations

- Sharing is offered on the Game Over screen only, not on past games of the home screen.
- With many rounds the image gets tall (64 px per round). It is still a single image.

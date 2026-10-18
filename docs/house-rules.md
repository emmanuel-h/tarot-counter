# House scoring rules (issue #236)

Settings › **Scoring** lets a table use its own house rules instead of the official
FFT scoring. Only a small, well-defined set of common variants is offered, and the
**FFT rule is always the default**, so nothing changes unless the user opts in.

## Options

| Option | FFT (default) | House rule |
|---|---|---|
| Petit au bout | **× contract**: 10 × contract multiplier (10 / 20 / 40 / 60) | **Flat 10** whatever the contract |
| Poignée | **Flat**: 20 / 30 / 40 per declaration | **× contract**: 20 / 30 / 40 × contract multiplier |

The direction of each bonus does not change. Petit au bout still goes to the camp that
achieved it, and the poignée still goes to the winning camp.

Not configurable on purpose: the base formula `(25 + margin) × multiplier`, the contract
multipliers (×1 ×2 ×4 ×6) and the chelem values. There is no single, widespread house
variant for them, so exposing them would only invite inconsistent scoring.

### Example

Garde Sans (×4), made by 10. The taker won the Petit on the last trick and showed a
simple poignée. Per defender:

| Rules | Base | Petit | Poignée | Total |
|---|---|---|---|---|
| FFT | 140 | 40 | 20 | 200 |
| Flat petit | 140 | 10 | 20 | 170 |
| Poignée × contract | 140 | 40 | 80 | 260 |
| Both | 140 | 10 | 80 | 230 |

## Layout

```
SCORING
┌──────────────────────────────┐
│ [star] Petit au bout         │
│ ( × contract  |  Flat 10   ) │   first segment = FFT
│ ──────────────────────────── │
│ [hand] Poignée               │
│ (   Flat      | × contract ) │   first segment = FFT
│ House rules. The first option│
│ of each row is the official  │
│ FFT rule (default). Changes  │
│ apply to new games only.     │
└──────────────────────────────┘
```

Each option uses a `SingleChoiceSegmentedButtonRow` with the Salon conventions: no
checkmark, `salonSegmentedButtonColors()`, and a shared `AutoSizeText` size.

## When the rules apply

- A **new game** captures the current setting (`initGame(..., scoringRules)`).
- The rules are stored with the game (`InProgressGame.scoringRules`,
  `SavedGame.scoringRules`). A game in progress, a resumed game and past games keep
  the rules they were played with, and changing the setting never re-scores them.
- The live result preview and the recorded score use the same function,
  `previewRound(..., rules)`, so they always agree.

## Implementation

- `ScoringRules` (`GameModels.kt`): `@Serializable` data class with
  `petitAuBoutMultiplied = true` and `poigneeMultiplied = false`. `ScoringRules.FFT`
  is the default instance.
- `petitAuBoutBonus(contract, rules)` and `scaledPoigneeBonus(total, contract, rules)`
  are the two places where a rule changes a number. `applyBonuses` and `previewRound`
  take `rules`, which defaults to FFT.
- Storage: DataStore key `scoring_rules`, a JSON object. Nothing stored, or an
  unreadable value, means FFT. `GameStorageInterface` has default implementations for
  `loadScoringRules` / `saveScoringRules`, so simple fakes don't need to implement them.
- `GameViewModel.scoringRules` (the setting, a `StateFlow`), `setScoringRules()` and
  `gameRules` (the current game's rules).

## Backward compatibility

Games saved before this feature have no `scoringRules` key and decode as FFT, which
is how they were scored. A rules object missing one of its fields (e.g. after a
future option is added) keeps that field at its FFT default.

## Tests

- `HouseRulesTest.kt` (unit): the defaults, both bonus functions for every contract,
  whole rounds under each combination (won and lost), the ViewModel setting, capture
  at game start, resume, and decoding of old JSON.
- `SettingsScreenTest.kt` (instrumented): the FFT defaults are selected, and taps
  report the new rules.

# Dead player and 6-player tables (issue #231)

## Rule

The FFT rules cover 3, 4 and 5 players. Larger tables use the convention shared by
every Tarot scorer: **the dealer sits out** (French: « le mort »).

| Table | Who plays a round | Plays like | Option |
|---|---|---|---|
| 3 | everybody | 3-player round | — |
| 4 | everybody, **or** the 3 non-dealers | 4- or 3-player round | "Dealer sits out" toggle, off by default |
| 5 | everybody | 5-player round (called partner) | — |
| 6 | the 5 non-dealers | 5-player round (called partner) | always on |

- The dealer rotates every round as usual, so everybody sits out in turn.
- The dead player **scores 0** for the round. The round stays zero-sum among the
  active players, and bonuses (petit au bout, poignée, chelem) only involve them.
- Poignée thresholds and the partner call follow the **active** player count
  (5 active → 8/10/13 and a called partner; 3 active → 13/15/18).

### Why not 8 players?

One competitor offers up to 8 players. No FFT rule covers it: a table of 8 would need
three dead players per round, or two tables. It is deliberately not supported.

## Screens

New game card (LandingScreen):

```
Players            (3 | 4 | 5 | 6)
(A) Alice …                          one field per seat (6 at most)
Dealer sits out          (No | Yes)  only at 4 players
  The dealer sits out each round…    note when Yes, or always at 6
First dealer      (Random | Choose)
[            Start game           ]
```

Game screen: the dealer chip reads **"Dealer: David · sits out"** and the "Who took?"
grid only has tiles for the active players (5 → 3 + 2). The partner chips and the
bonus sheets list active players only; avatars keep their seat colour.

Score history (list view): a played round names the player who sat out
("David sat out").

## Implementation

Pure logic lives in `GameModels.kt`:

- `TABLE_SIZES` — `[3, 4, 5, 6]`.
- `dealerSitsOutMandatory(n)` (6) / `dealerSitsOutOptional(n)` (4).
- `resolveDealerSitsOut(n, requested)` — the rule actually applied for a table size.
- `activePlayers(table, dealer, sitsOut)` — the players of a round, in seat order.
- `previewRound(..., deadPlayer)` — scores the active players only and adds the dead
  player with `0`. It is still the single scoring path shared by the live preview
  and `GameViewModel.recordPlayed()`.

`GameViewModel` keeps `dealerSitsOut` for the session and exposes `deadPlayer` and
`activePlayers`, both derived from the current dealer.

Player statistics (`PlayerStats.kt`) do not count a round a player sat out in their
"rounds played", so the take rate is not diluted at a table of 6.

## Persistence (backward compatible)

| Field | Type | Default | Meaning |
|---|---|---|---|
| `InProgressGame.dealerSitsOut` | `Boolean` | `false` | the session setting, restored on resume |
| `SavedGame.dealerSitsOut` | `Boolean` | `false` | kept with the finished game |
| `RoundResult.deadPlayer` | `String?` | `null` | who sat out that round |

Games saved before this change have none of these keys and decode with the defaults
(nobody sits out). A resumed game re-resolves the setting, so a table of 6 always
has a dead player.

## Tests

- `DeadPlayerTest.kt` (unit): table-size rules, active players, scoring at 6 and at
  4 with a dead dealer, bonuses, ViewModel rotation and persistence, decoding of
  pre-#231 JSON.
- `LandingScreenTest.kt` / `GameScreenTest.kt` (instrumented): the 6-player setup
  note, the 4-player toggle, no taker tile and no partner chip for the dead player.

# Game Flow

## Overview

After setting up players on the setup screen, the user taps **Start Game** to begin a session.

## Screens

### Setup Screen (`LandingScreen`)
- Choose 3–5 players via filter chips.
- Enter optional names for each player.
- Tap **Start Game** to lock in the names and navigate to the game screen.

### Game Screen (`GameScreen`)

`GameScreen` is a thin UI layer: it observes state from `GameViewModel` and calls ViewModel methods for all game actions. All scoring logic lives in the ViewModel, making it unit-testable without Compose.

**Responsibilities split:**

| Concern | Where it lives |
|---|---|
| Game session state (`currentRound`, `roundHistory`) | `GameViewModel` |
| `recordPlayed(takerName, contract, details)`, `recordSkipped`, `endGame` | `GameViewModel` |
| Dealer rotation (`currentDealer`) | `GameViewModel` |
| Taker selection, round-entry visibility (`roundEntryOpen`), contract selection, overlay visibility | `GameScreen` (local `remember` state) |
| Standings, taker grid layout, last rounds | `Standings.kt` (pure, unit-tested) |
| Shared components (`SalonCard`, `PlayerAvatar`, …) | `UiComponents.kt` |
| Bonus rows and bottom sheets | `BonusSheets.kt` (logic in `Bonuses.kt`) |

The game is divided into **rounds**. Each round has two distinct roles:

- **Dealer** — deals the cards. The dealer role rotates each round:
  - **Round 1**: a random player is chosen as the first dealer.
  - **Round 2+**: players take turns dealing in the order they were entered on the setup screen, cycling back to the first player after the last one.
- **Attacker (taker / preneur)** — the player who wins the bidding and takes the contract. *Any* player can bid for the contract regardless of who deals. The player with the highest bid becomes the attacker and their score is affected by the round outcome.

The game screen has **two views** (Salon redesign, issue #198):

1. **Between rounds** — the scores come first, then choosing the taker is a single tap:

```
┌──────────────────────────────┐
│ Round 5            [↶] [📈]  │  SalonTopBar: undo (after round 1) + history
│        (Dealer: David)       │  brass dealer chip
│ ┌──────────────────────────┐ │  Standings card (after round 1)
│ │ ((A)) LEADING      +312  │ │  leader row: brass ring, brass tint, XL score
│ │       Alice          ▲   │ │
│ │ 2  (B) Bruno     ▼   −48 │ │  rank · avatar · name · trend · score
│ │ 3  (C) Chloé     ▼   −96 │ │
│ └──────────────────────────┘ │
│ Who took?      Tap the taker │
│ ┌─────┐ ┌─────┐              │  one tile per player: 3 / 2×2 / 3+2
│ │ (A) │ │ (B) │              │
│ └─────┘ └─────┘              │
│ ══════════ ♠ ♥ ♦ ♣ ═════════ │
│ Last rounds          See all │  latest 3 rounds, newest first
│ R4 Bruno · Guard · Lost −162 │
├──────────────────────────────┤
│ End Game  [   Skip round   ] │
└──────────────────────────────┘
```

2. **Round entry**: opened by tapping a taker tile. The top bar reads "Alice takes" with a back arrow, labelled "Change taker", that returns to view 1. The arrow keeps the chosen taker, highlighted in felt green, and everything already entered in the form. The view holds the contract selector and the details form (restyled in #199). Its bottom bar is `End Game  [ Confirm round ]`.

#### Standings

`computeStandings(playerNames, rounds)` in `Standings.kt` ranks players by cumulative total:

- Best total first; tied players keep seat order.
- Ties share a rank, and the next rank is skipped (1, 1, 3). This is called *competition ranking*.
- Every player sharing the best total is a **leader**, once at least one round has been scored. Leader rows get a brass tint, a brass ring around a large avatar, a "LEADING" overline and an XL `ScoreText`.
- The small **trend arrow** shows the last round's change for each player: ▲ in green for a gain, ▼ in red for a loss. Nothing shows for a zero change, a skipped round, or before round 1.

The card appears after the first recorded round. It replaces the former compact scoreboard.

#### "Who took?" tiles

Each tile shows the player's avatar and name. Tapping a tile opens the round entry for that taker. `takerGridColumns(playerCount)` picks the grid:

- 3 players: one row of 3
- 4 players: 2 × 2
- 5 players: 3 + 2 (the last row keeps the same tile width)

Picking a **different** taker than before resets the contract and the whole form (issue #124: points always go to the selected taker).

#### Last rounds

`lastRounds(rounds)` returns the latest 3 rounds, newest first. A played round reads "R4  Bruno · Guard · Lost" with the taker's delta; a skipped round reads "Skipped", muted. **See all** opens the Score History screen.

#### Round entry (Salon, issue #199)

Round entry turns scoring into a guided panel that shows the result **before** the round is confirmed.

```
┌──────────────────────────────┐
│ ←  (C) ROUND 5               │  top bar: back arrow ("Change taker"),
│        Chloé takes           │  taker avatar, brass overline
│ Contract                     │
│ [Small      ×1][Guard    ×2] │  2 × 2 contract cards, selected = felt
│ [G. Without ×4][G. Against ×6]│
│ Bouts (oudlers)     needs 41 │
│ ( 0 ) ( 1 ) (•2 ) ( 3 )      │  bout chips, needs = requiredPoints()
│ ┌ Points scored (Attack|Defense) ┐
│ │ 47 / 91                    │ │  large Cormorant number
│ │ ✓ Made by 6 → Chloé +124   │ │  live result pill
│ └────────────────────────────┘ │
│ Partner called by the taker  │  5 players only: avatar chips
│ Bonuses                      │  3 rows → bottom sheets (#200)
├──────────────────────────────┤
│ End Game  [  Confirm round  ]│
└──────────────────────────────┘
```

**Contract cards**: one card per contract, weakest to strongest, each showing its name and multiplier. Tapping the selected card again deselects it and collapses the rest of the form.

| Contract (FR) | Contract (EN)  | Multiplier | Description                    |
|---------------|----------------|:----------:|-------------------------------|
| Prise         | Small          | ×1         | Weakest contract               |
| Garde         | Guard          | ×2         | Standard contract              |
| Garde Sans    | Guard Without  | ×4         | Play without the dog           |
| Garde Contre  | Guard Against  | ×6         | Play against the dog           |

Contract names are localized: French uses the canonical Tarot terms; English provides plain translations.

Once a contract is chosen, the rest of the form appears:

| Field | Control | Description |
|-------|---------|-------------|
| Bouts (oudlers) | Four pill chips: 0 / 1 / 2 / 3 | Number of oudlers in the taker's tricks. The helper on the right shows the points needed, e.g. "needs 41" (from `requiredPoints()`). |
| Points | Large number field + **Attack \| Defense** toggle | Points scored by the selected camp, 0–91, digits only. Switching camp clears the field so a value is never read for the wrong team. In Defense mode the app converts on confirm: `takerPoints = 91 − defenderPoints`. Values above 91 show an error and disable Confirm. For screen readers, the field is described as "Attacker (0-91)" or "Defenders (0-91)". |
| Live result | Pill under the number | Appears once a valid number is typed: "Made by 6 → Chloé +124" in a felt tint, or "Short by 4 → Chloé −174" in a red tint. It updates as any field changes, bonuses included. |
| Partner | Avatar chips (5 players only) | Every player except the taker. Tapping the selected partner again clears the choice. |
| Bonuses | Three rows that open bottom sheets | Petit au bout, Poignée, Chelem (see below) |

**`previewRound(playerNames, takerName, contract, details)`** (`GameModels.kt`) computes the live result. It returns a `RoundPreview`:

- `won`: the taker reached the required points
- `margin`: `|points − required|`
- `playerScores`: every player's delta, bonuses included

`GameViewModel.recordPlayed()` calls the same function, so the pill always shows exactly what will be recorded. `RoundPreviewTest` covers it.

#### Bonus rows and sheets (issue #200)

```
Bonuses
┌──────────────────────────────────┐
│ Petit au bout          None    › │
│ Poignée           (1) Alice    › │   red + message when trumps > 22
│ Chelem                 None    › │
└──────────────────────────────────┘
```

Tapping a row opens a modal bottom sheet. Each sheet has a title, one explanation line and a **Done** button.

| Sheet | Choice | Notes |
|-------|--------|-------|
| Petit au bout | None or one player | Picking closes the sheet. |
| Poignée | Per player: None / Simple / Double / Triple | One level per player (`PoigneeDeclarations.withLevel` removes the player from the other levels), several players may declare. The explanation line gives this game's thresholds (`poigneeThresholds`). The atout error ("Too many trumps declared") is shown in the sheet **and** on the row; Confirm stays disabled while it lasts. |
| Chelem | Outcome, then who called it | Candidates come from `chelemCandidates`: the taker, plus the partner in a 5-player game. Changing the outcome resets the player. An announced chelem with a player shows "*Alice plays first this round.*" |

**Poignée trump thresholds** vary with the number of players (official FFT rules, R-RO201206.pdf):

| Players | Simple Poignée | Double Poignée | Triple Poignée |
|---------|:--------------:|:--------------:|:--------------:|
| 3       | 13 trumps      | 15 trumps      | 18 trumps      |
| 4       | 10 trumps      | 13 trumps      | 15 trumps      |
| 5       |  8 trumps      | 10 trumps      | 13 trumps      |

The Poignée sheet's explanation line always shows the thresholds for the current game's player count.

**Atout count validation**: the minimum trump thresholds of all declared poignées must add up to at most the 22 trumps in the deck. If the declarations are impossible (e.g. triple [15] + simple [10] = 25 > 22 in a 4-player game), a red message appears in the Poignée sheet and under the Poignée row, and **Confirm** stays disabled until the declarations are corrected.

**Partner selection** is only shown in 5-player games. The taker secretly calls a partner; their identity affects score distribution at the end of the round.

**Chelem options and bonus points:**

| Value                      | Meaning                                          | Bonus per defender |
|----------------------------|--------------------------------------------------|--------------------|
| None                       | No grand slam                                    | 0                  |
| Announced & realized       | Caller announced and the attacking team won every trick | +400          |
| Not announced, realized    | Attacking team won every trick without announcing | +200              |
| Announced, not realized    | Caller announced but the attacking team failed   | −200               |
| Defenders realized         | Defending camp won every trick without announcing (R-RO201206.pdf p.6) | −200 |

The bonus is a flat amount exchanged between the taker and each defender individually — it is **not** multiplied by the contract. A positive bonus means the taker collects that amount from each defender; a negative bonus means the taker pays that amount to each defender. The partner (5-player) is not involved in the chelem bonus. The result is always zero-sum.

> **Note on "Defenders realized"**: this is a rare but official scenario from the FFT rulebook (R-RO201206.pdf page 6): *"Paradoxalement, il arrive que la défense inflige un Chelem au déclarant. Dans ce cas, chaque défenseur reçoit, en plus de la marque normale, une prime de 200 points."* The financial effect (taker pays 200 to each defender) is the same as "Announced, not realized", but the cause is different — the defenders won all tricks, not the taker failing an announced slam.

**Chelem player**: when a non-None chelem option is selected a second selector appears — "Who called the chelem?". Available choices are the taker and (in 5-player games) the partner if one has been selected. Once a player is chosen and the chelem is of the *announced* type, a note is shown reminding the table that **that player leads the first trick of the round**, overriding the normal turn order.

Tapping **Confirm round** (in the bottom bar) saves the result and moves to the next round. The button is disabled until both a contract and a non-empty score have been entered.

## Win Condition

After the taker's points and bouts are entered, the app determines whether the taker **won** or **lost** the round.

The taker must score at least the threshold for their bout count:

| Bouts (oudlers) | Points needed to win |
|-----------------|----------------------|
| 0               | 56                   |
| 1               | 51                   |
| 2               | 41                   |
| 3               | 36                   |

The three bouts are the 21 of trumps, the 1 of trump (Petit), and the Excuse. Holding more bouts reduces the required score.

## Scoring

### Round score

```
roundScore = (25 + |scoredPoints − requiredPoints(bouts)|) × contract.multiplier
```

The constant 25 is added for every contract. The absolute difference rewards or penalises proportionally to the margin of victory or defeat.

**Example:** Garde (×2), 2 bouts (threshold 41), scored 56 → diff = 15 → `(25 + 15) × 2 = 80`.

### Score distribution

Scores are zero-sum — the total across all players is always 0.

| Game mode | Taker | Partner | Each defender |
|-----------|:-----:|:-------:|:-------------:|
| 3 players | ±2 × roundScore | — | ∓roundScore |
| 4 players | ±3 × roundScore | — | ∓roundScore |
| 5 players | ±2 × roundScore | ±1 × roundScore | ∓roundScore |

The sign is **+** when the taker won, **−** when the taker lost. The same sign applies to the partner in 5-player games; defenders receive the opposite sign.

### Petit au bout bonus

The petit au bout is achieved when the **Petit (1 of trumps) is captured on the very last trick**. The bonus is awarded to the camp that achieved it — **regardless of who won the round**.

| Contract     | Bonus per defender (10 × multiplier) |
|--------------|--------------------------------------|
| Prise   ×1   | 10 pts                               |
| Garde   ×2   | 20 pts                               |
| Garde Sans ×4 | 40 pts                              |
| Garde Contre ×6 | 60 pts                            |

- **Taker's camp achieved it** (taker or partner captured the Petit on the last trick) → each defender pays the bonus to the taker.
- **Defenders' camp achieved it** → the taker pays the bonus to each defender.

The partner (5-player) does not participate in this exchange. The result is always zero-sum.

### Poignée bonus

A poignée (trump show) grants a flat bonus **per defender**, always awarded to the **winning camp** regardless of who declared it.

|----------------|--------------------|
| Simple         | 20 pts             |
| Double         | 30 pts             |
| Triple         | 40 pts             |

- **Taker wins** → the taker collects the bonus from each defender.
- **Taker loses** → each defender collects the bonus from the taker.

The partner (5-player) does not participate in the poignée bonus exchange. The result is always zero-sum.

**Example:** 4-player game, double poignée (30 pts), declared by a defender, taker loses → taker pays 30 to each of the 3 defenders (−90 for taker, +30 for each defender).

The minimum number of trumps needed to declare each type differs per player count — see the [Poignée thresholds table](#poignée-trump-thresholds) in the Inline round details section above.

## Header and bottom bar

The full round-by-round detail log is available on the **Score History screen** (history icon in the top bar, or **See all** under *Last rounds*) in **List view** — see `docs/score-history.md`.

#### Header

A `SalonTopBar`, fixed above the scrolling content:

| View | Title | Actions |
|------|-------|---------|
| Between rounds | **Round N** | **Undo** (↶, shown once at least one round is recorded) and **History** (chart icon), both 48 dp icon buttons with content descriptions |
| Round entry | **"Alice takes"** | Back arrow (content description "Change taker") returning to "Who took?" |

**Undo**: tapping it opens a confirmation dialog before the last round is removed, so nothing is lost by accident. Once confirmed:

- `GameViewModel.undoLastRound()` removes the last `RoundResult`, decrements `currentRound` and saves the updated in-progress snapshot.
- The form is refilled from the removed round. An undone *played* round reopens the round entry with its values; an undone *skipped* round returns to "Who took?".

#### Bottom action bar

The bar is pinned under the scrolling content, above a hairline, so it is always visible.

| View | Buttons |
|------|---------|
| Between rounds | **End Game** (red text button) · **Skip round** (outlined, fills the rest) |
| Round entry | **End Game** (red text button) · **Confirm round** (primary, fills the rest) |

| Button | Behaviour |
|--------|-----------|
| **End Game** | Ends the current game. It is a red *text* button: always reachable, but the least prominent, so it is hard to hit by accident. **If the points field is non-empty**, a confirmation dialog appears first (issue #150). Then, if at least one round has been played, the game is saved and the Final Score screen is shown. If no round has been played, the game is cancelled silently and the user returns to the setup screen (issue #90). |
| **Skip round** | Records the round as skipped (no contract, no score) and advances to the next one. |
| **Confirm round** | Saves the contract, score and bonuses, advances to the next round and returns to the between-rounds view. **Disabled** until a contract is selected and a valid score (0–91) is entered, and while trump declarations exceed 22. |

The bar is a direct child of the outer (non-scrollable) `Column`, which also owns `imePadding()`, so the bar and the scroll area shift up together when the keyboard opens.

## Data Model

- **`undoLastRound()`** — removes the last `RoundResult` from `roundHistory`, decrements `currentRound`, and saves the rolled-back snapshot to DataStore. No-op if `roundHistory` is empty.
- **`currentDealer: String`** (read-only property on `GameViewModel`) — the name of the player who deals cards this round, computed by `(startingIndex + currentRound − 1) % playerCount`. This is separate from the attacker, which is chosen by the user each round.
- **`recordPlayed(takerName, contract, details)`** — accepts an explicit `takerName` parameter (the attacker) rather than deriving it from the dealer rotation. This ensures scores are always attributed to the player who won the bidding.
- `Contract` enum — four contracts with `displayName` and `multiplier`.
- `Chelem` enum — five grand slam outcomes (`NONE`, `ANNOUNCED_REALIZED`, `ANNOUNCED_NOT_REALIZED`, `NOT_ANNOUNCED_REALIZED`, `DEFENDERS_REALIZED`). The last entry covers the FFT-official defenders-chelem scenario (R-RO201206.pdf p.6).
- `RoundDetails` data class — all scoring fields: bouts, points, `partnerName` (5-player only), player-assigned bonuses, the chelem outcome, and `chelemPlayer` (which player called/achieved the chelem — null when `chelem == NONE`). Since issue #149 the three Poignée fields are `List<String>` (`poignees`, `doublePoignees`, `triplePoignees`). Legacy nullable single-player fields are kept for backward-compat deserialization of old saved games; `effectivePoignees` / `effectiveDoublePoignees` / `effectiveTriplePoignees` computed properties merge both formats transparently.
- `RoundResult` data class — round number, taker name, contract (`null` if skipped), details (`null` if skipped), `won` (`null` if skipped), and `playerScores` (empty map if skipped).
- `requiredPoints(bouts)` — returns the minimum points needed to win for a given bout count.
- `takerWon(bouts, points)` — returns `true` if points ≥ `requiredPoints(bouts)`.
- `calculateRoundScore(contract, bouts, points)` — returns the base round score before distribution.
- `computePlayerScores(allPlayers, takerName, partnerName, won, roundScore)` — returns a `Map<String, Int>` of player → score for the round.
- `petitAuBoutBonus(contract)` — returns `10 × contract.multiplier`. Direction (which camp benefits) is determined in GameScreen by comparing the achiever's name against the taker/partner.
- `poigneeThresholds(playerCount)` — returns a `Triple<Int, Int, Int>` with the minimum trump counts for (simple, double, triple) Poignée, varying by player count (3 → 13/15/18, 4 → 10/13/15, 5 → 8/10/13). Used by `AppStrings` to show the correct threshold in each tooltip.
- `poigneeBonus(poignee, doublePoignee, triplePoignee)` — legacy single-player helper, returns 20 / 30 / 40 / 0. Kept for backward compat and unit tests.
- `totalPoigneeBonus(poignees, doublePoignees, triplePoignees)` — multi-player replacement (issue #149): sums `size×20 + size×30 + size×40` across all declarants. Used by `applyBonuses`.
- `totalAtoutsAnnounced(poignees, doublePoignees, triplePoignees, playerCount)` — returns the combined minimum trump thresholds claimed. Used to validate that declarations do not exceed `TOTAL_ATOUTS_IN_DECK` (22).
- `chelemBonus(chelem)` — returns the flat per-defender bonus value: +400, +200, −200, or 0.

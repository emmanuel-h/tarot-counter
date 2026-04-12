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
| Attacker selection, contract selection, overlay visibility | `GameScreen` (local `remember` state) |
| Sub-composables (`CompactBonusGrid`, `PlayerChipSelector`, etc.) | `UiComponents.kt` |

The game is divided into **rounds**. Each round has two distinct roles:

- **Dealer** — deals the cards. The dealer role rotates each round:
  - **Round 1**: a random player is chosen as the first dealer.
  - **Round 2+**: players take turns dealing in the order they were entered on the setup screen, cycling back to the first player after the last one.
- **Attacker (taker / preneur)** — the player who wins the bidding and takes the contract. *Any* player can bid for the contract regardless of who deals. The player with the highest bid becomes the attacker and their score is affected by the round outcome.

The current dealer's name is shown at the top of the round section for reference. The user then selects the attacker by tapping any player's name in the segmented-button row.

Everything is presented on **a single scrollable page**: the compact scoreboard, the dealer label, the attacker selector, the contract chips, the inline details form, and the round history log are all visible without navigating to a separate screen.

#### Attacker selection

A segmented-button row listing every player's name is shown above the contract chips. The user taps the player who won the bidding to select them as the attacker. Tapping the same player again deselects them. The **Confirm round** button stays disabled until an attacker is selected (in addition to a contract and a score).

#### Contract selection

The contract selector is **only shown after an attacker has been selected** (issue #131). This avoids presenting irrelevant information before the bidding winner is known.

Once an attacker is selected, a prompt shows their name above a row of SegmentedButtons — one per contract (weakest → strongest):

| Contract (FR) | Contract (EN)  | Multiplier | Description                    |
|---------------|----------------|:----------:|-------------------------------|
| Prise         | Small          | ×1         | Weakest contract               |
| Garde         | Guard          | ×2         | Standard contract              |
| Garde Sans    | Guard Without  | ×4         | Play without the dog           |
| Garde Contre  | Guard Against  | ×6         | Play against the dog           |

Contract names are localized: French uses the canonical Tarot terms; English provides plain translations for accessibility.

The three action buttons at the bottom of the screen let the taker confirm, skip, or end the game (see [Bottom action bar](#bottom-action-bar) below).

#### Inline round details

After a contract chip is selected, the scoring details form expands below it on the same page.
Tapping the active chip again collapses the form and deselects the contract.

| Field              | Type                        | Description |
|--------------------|-----------------------------|-------------|
| Bouts (oudlers)    | Dropdown (0 / 1 / 2 / 3)   | Number of oudlers in the taker's tricks |
| Points             | Number input with trailing camp-toggle icon | Points scored by the selected camp. The floating label shows the current camp and the valid range: **"Attacker (0-91)"** by default, or **"Defenders (0-91)"** after toggling. Tapping the trailing icon (⚔ sword = attacker, 🛡 shield = defenders) switches camps and clears the field to prevent ambiguity. When defenders' mode is active the app converts to taker points on confirm (`takerPoints = 91 − defenderPoints`). Values outside 0–91 show an error and disable the Confirm button. |
| Partner            | None or any player (5-player only) | The player called by the taker as a silent partner |
| Petit au bout      | Checkbox per player         | Player who captured the 1 of trump on the last trick |
| Poignée            | Checkbox per player         | Player who showed a simple Poignée (see thresholds below) |
| Double poignée     | Checkbox per player         | Player who showed a double Poignée (see thresholds below) |
| Triple poignée     | Checkbox per player         | Player who showed a triple Poignée (see thresholds below) |
| Chelem             | Self-labelled dropdown + player selector | Grand slam outcome and who called it. Shows "Chelem" when nothing is selected, otherwise the chosen outcome's name. |

**Poignée trump thresholds** vary with the number of players (official FFT rules, R-RO201206.pdf):

| Players | Simple Poignée | Double Poignée | Triple Poignée |
|---------|:--------------:|:--------------:|:--------------:|
| 3       | 13 trumps      | 15 trumps      | 18 trumps      |
| 4       | 10 trumps      | 13 trumps      | 15 trumps      |
| 5       |  8 trumps      | 10 trumps      | 13 trumps      |

The tooltip shown next to each Poignée label in the UI automatically displays the correct threshold for the current game's player count.

The four player-assigned bonuses are displayed in a compact grid. Each row shows a **label** with an ⓘ info icon immediately next to it (not pushed to the edge), and one **checkbox per player**. Tapping anywhere on the label row (the text or the icon) opens a tooltip describing the bonus and its point value — the entire row is the tap target, not only the small icon. Ticking a checkbox assigns that bonus to that player; ticking it again clears the assignment. At most one player can hold each bonus at a time.

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

| Poignée type   | Bonus per defender |
|----------------|--------------------|
| Simple         | 20 pts             |
| Double         | 30 pts             |
| Triple         | 40 pts             |

- **Taker wins** → the taker collects the bonus from each defender.
- **Taker loses** → each defender collects the bonus from the taker.

The partner (5-player) does not participate in the poignée bonus exchange. The result is always zero-sum.

**Example:** 4-player game, double poignée (30 pts), declared by a defender, taker loses → taker pays 30 to each of the 3 defenders (−90 for taker, +30 for each defender).

The minimum number of trumps needed to declare each type differs per player count — see the [Poignée thresholds table](#poignée-trump-thresholds) in the Inline round details section above.

## Compact Scoreboard

After the first round is completed the game screen shows a persistent **compact scoreboard** at the top of the page — one column per player with their name and running total. This stays visible at all times without opening a separate screen.

Each player column carries `Modifier.weight(1f)` so all columns share the card width equally. Player names that are too long for their column are truncated with an ellipsis ("…") rather than overflowing their neighbours — this is important in 5-player games on narrow screens.

```
Scores
┌─────────────────────────┐
│ Alice   Bob    Charlie  │
│  +80    -40     -40     │
└─────────────────────────┘
```

The full round-by-round detail log is available on the **Score History screen** (opened via the bar-chart icon in the header) in **List view** — see `docs/score-history.md`.

#### Header

The game screen header has two zones:

| Left | Center | Right (placeholder) |
|------|--------|---------------------|
| **History** icon button (bar-chart icon) — opens the full score history overlay. Only shown after at least one round has been recorded. A 48 dp spacer is shown instead when no rounds exist yet. | **Round N** — the current round number, always centred. | 48 dp spacer that mirrors the History button to keep the round number perfectly centred. |

The **History** icon button opens a full scrollable score table overlay (with running cumulative totals) for detailed review.

#### Bottom action bar

A persistent three-button bar at the bottom of the screen, always visible regardless of scroll position (issues #32, #89). All three buttons sit on a single horizontal row and each receives an equal share of the width via `Modifier.weight(1f)`.

| Button | Style | Behaviour |
|--------|-------|-----------|
| **End Game** | Filled — error container (red) | Ends the current game. **If at least one round has been played**, the game is saved and the Final Score screen is shown. **If no rounds have been played**, the game is cancelled silently — the in-progress entry is cleared and the user returns to the setup screen without anything being recorded (issue #90). The red color signals that this action terminates the game. |
| **Skip round** | Outlined | Records the current round as skipped (no contract, no score) and advances to the next one. The outlined style marks it as a secondary/neutral action. |
| **Confirm round** | Filled — primary | Saves the contract, score, and all bonus details, then advances to the next round. **Disabled** until both a contract is selected *and* a non-empty score is entered. Also disabled while the points field contains an invalid value (> 91). |

The bar is a direct child of the outer (non-scrollable) `Column`, which also owns `imePadding()` so the bar and the scroll area shift up together when the keyboard opens.

## Data Model

- **`currentDealer: String`** (read-only property on `GameViewModel`) — the name of the player who deals cards this round, computed by `(startingIndex + currentRound − 1) % playerCount`. This is separate from the attacker, which is chosen by the user each round.
- **`recordPlayed(takerName, contract, details)`** — accepts an explicit `takerName` parameter (the attacker) rather than deriving it from the dealer rotation. This ensures scores are always attributed to the player who won the bidding.
- `Contract` enum — four contracts with `displayName` and `multiplier`.
- `Chelem` enum — five grand slam outcomes (`NONE`, `ANNOUNCED_REALIZED`, `ANNOUNCED_NOT_REALIZED`, `NOT_ANNOUNCED_REALIZED`, `DEFENDERS_REALIZED`). The last entry covers the FFT-official defenders-chelem scenario (R-RO201206.pdf p.6).
- `RoundDetails` data class — all scoring fields: bouts, points, `partnerName` (5-player only), player-assigned bonuses, the chelem outcome, and `chelemPlayer` (which player called/achieved the chelem — null when `chelem == NONE`).
- `RoundResult` data class — round number, taker name, contract (`null` if skipped), details (`null` if skipped), `won` (`null` if skipped), and `playerScores` (empty map if skipped).
- `requiredPoints(bouts)` — returns the minimum points needed to win for a given bout count.
- `takerWon(bouts, points)` — returns `true` if points ≥ `requiredPoints(bouts)`.
- `calculateRoundScore(contract, bouts, points)` — returns the base round score before distribution.
- `computePlayerScores(allPlayers, takerName, partnerName, won, roundScore)` — returns a `Map<String, Int>` of player → score for the round.
- `petitAuBoutBonus(contract)` — returns `10 × contract.multiplier`. Direction (which camp benefits) is determined in GameScreen by comparing the achiever's name against the taker/partner.
- `poigneeThresholds(playerCount)` — returns a `Triple<Int, Int, Int>` with the minimum trump counts for (simple, double, triple) Poignée, varying by player count (3 → 13/15/18, 4 → 10/13/15, 5 → 8/10/13). Used by `AppStrings` to show the correct threshold in each tooltip.
- `poigneeBonus(poignee, doublePoignee, triplePoignee)` — returns the flat per-defender bonus: 20, 30, 40, or 0. Direction follows the round winner, applied in GameScreen.
- `chelemBonus(chelem)` — returns the flat per-defender bonus value: +400, +200, −200, or 0.

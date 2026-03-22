# Game Flow

## Overview

After setting up players on the setup screen, the user taps **Start Game** to begin a session.

## Screens

### Setup Screen (`LandingScreen`)
- Choose 3–5 players via filter chips.
- Enter optional names for each player.
- Tap **Start Game** to lock in the names and navigate to the game screen.

### Game Screen (`GameScreen`)

The game is divided into **rounds**. The taker for each round is determined automatically:

- **Round 1** — a random player is chosen as the first taker.
- **Round 2+** — players take turns in the order they were entered on the setup screen, cycling back to the first player after the last one.

Each round follows a two-step flow:

#### Step 1 — Pick a contract

The current taker's name is shown. Available contracts (weakest → strongest):

| Contract     | Multiplier | Description                    |
|--------------|:----------:|-------------------------------|
| Prise        | ×1         | Weakest contract               |
| Garde        | ×2         | Standard contract              |
| Garde Sans   | ×4         | Play without the dog           |
| Garde Contre | ×6         | Play against the dog           |

The taker can also **Skip round** to record the round without any details.

#### Step 2 — Round details (`RoundDetailsForm`)

After a contract is chosen, the user fills in the scoring details:

| Field              | Type                        | Description |
|--------------------|-----------------------------|-------------|
| Bouts (oudlers)    | 0 · 1 · 2 · 3 chips         | Number of oudlers in the taker's tricks |
| Points             | Number input (0–91)         | Points scored by the taker |
| Partner            | None or any player (5-player only) | The player called by the taker as a silent partner |
| Petit au bout      | None or any player          | Player who captured the 1 of trump on the last trick |
| Misère             | None or any player          | Player who declared misère |
| Double misère      | None or any player          | Player who declared double misère |
| Poignée            | None or any player          | Player who showed a poignée (10+ trumps) |
| Double poignée     | None or any player          | Player who showed a double poignée (13+ trumps) |
| Triple poignée     | None or any player          | Player who showed a triple poignée (15+ trumps) |
| Chelem             | See table below             | Grand slam outcome |

**Partner selection** is only shown in 5-player games. The taker secretly calls a partner; their identity affects score distribution at the end of the round.

**Chelem options and bonus points:**

| Value                      | Meaning                                          | Bonus per defender |
|----------------------------|--------------------------------------------------|--------------------|
| None                       | No grand slam                                    | 0                  |
| Announced & realized       | Taker announced and won every trick              | +400               |
| Not announced, realized    | Taker won every trick without announcing         | +200               |
| Announced, not realized    | Taker announced but failed to win every trick    | −200               |

The bonus is a flat amount exchanged between the taker and each defender individually — it is **not** multiplied by the contract. A positive bonus means the taker collects that amount from each defender; a negative bonus means the taker pays that amount to each defender. The partner (5-player) is not involved in the chelem bonus. The result is always zero-sum.

Tapping **Confirm round** saves the result and moves to the next round.
Tapping **← Change contract** goes back to step 1 without saving.

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

## Scoreboard & Round History

After each round is completed the game screen shows:

1. **Scores** — cumulative score per player across all completed rounds.
2. **History** — a round-by-round log, newest first.

```
Scores
Alice: +80
Bob:   -40
Charlie: -40

History
Round 2: Bob — Prise · 0 bouts · 50 pts — Lost (-31)
Round 1: Alice — Garde · 2 bouts · 56 pts — Won (+80)
```

Skipped rounds show no outcome and do not affect scores.

## Data Model

- `Contract` enum — four contracts with `displayName` and `multiplier`.
- `Chelem` enum — four grand slam outcomes (`NONE`, `ANNOUNCED_REALIZED`, `ANNOUNCED_NOT_REALIZED`, `NOT_ANNOUNCED_REALIZED`).
- `RoundDetails` data class — all scoring fields: bouts, points, `partnerName` (5-player only), and the player-assigned/chelem bonuses.
- `RoundResult` data class — round number, taker name, contract (`null` if skipped), details (`null` if skipped), `won` (`null` if skipped), and `playerScores` (empty map if skipped).
- `requiredPoints(bouts)` — returns the minimum points needed to win for a given bout count.
- `takerWon(bouts, points)` — returns `true` if points ≥ `requiredPoints(bouts)`.
- `calculateRoundScore(contract, bouts, points)` — returns the base round score before distribution.
- `computePlayerScores(allPlayers, takerName, partnerName, won, roundScore)` — returns a `Map<String, Int>` of player → score for the round.
- `poigneeBonus(poignee, doublePoignee, triplePoignee)` — returns the flat per-defender bonus: 20, 30, 40, or 0. Direction follows the round winner, applied in GameScreen.
- `chelemBonus(chelem)` — returns the flat per-defender bonus value: +400, +200, −200, or 0.

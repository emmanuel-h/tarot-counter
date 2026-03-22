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

| Contract     | Description                    |
|--------------|-------------------------------|
| Petite       | Weakest contract               |
| Pousse       | Slightly stronger than Petite  |
| Garde        | Standard contract              |
| Garde Sans   | Play without the dog           |
| Garde Contre | Play against the dog           |

The taker can also **Skip round** to record the round without any details.

#### Step 2 — Round details (`RoundDetailsForm`)

After a contract is chosen, the user fills in the scoring details:

| Field           | Type                        | Description |
|-----------------|-----------------------------|-------------|
| Bouts (oudlers) | 0 · 1 · 2 · 3 chips         | Number of oudlers in the taker's tricks |
| Points          | Number input (0–91)         | Points scored by the taker |
| Petit au bout   | None or any player          | Player who captured the 1 of trump on the last trick |
| Misère          | None or any player          | Player who declared misère |
| Double misère   | None or any player          | Player who declared double misère |
| Poignée         | None or any player          | Player who showed a poignée (10+ trumps) |
| Double poignée  | None or any player          | Player who showed a double poignée (13+ trumps) |
| Chelem          | See table below             | Grand slam outcome |

**Chelem options:**

| Value                      | Meaning |
|----------------------------|---------|
| None                       | No grand slam |
| Announced & realized       | Taker announced and won every trick |
| Announced, not realized    | Taker announced but failed to win every trick |
| Not announced, realized    | Taker won every trick without announcing |

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

The three bouts are the 21 of trumps, the 1 of trumps (Petit), and the Excuse. Holding more bouts reduces the required score.

## Round History

After each round is completed, a summary is appended to a history list at the bottom of the screen (newest round first):

```
Round 3: Alice — Garde · 2 bouts · 56 pts — Won
Round 2: Bob — Skipped
Round 1: Charlie — Petite · 1 bout · 40 pts — Lost
```

Skipped rounds show no outcome. Played rounds always show **Won** or **Lost**.

## Data Model

- `Contract` enum — the five possible contracts with display names.
- `Chelem` enum — four grand slam outcomes (`NONE`, `ANNOUNCED_REALIZED`, `ANNOUNCED_NOT_REALIZED`, `NOT_ANNOUNCED_REALIZED`).
- `RoundDetails` data class — all scoring fields for a played round (bouts, points, and the seven player-assigned/chelem bonuses).
- `RoundResult` data class — round number, taker name, contract (`null` if skipped), details (`null` if skipped), and `won` (`null` if skipped, `true`/`false` otherwise).
- `requiredPoints(bouts)` — returns the minimum points needed to win for a given bout count.
- `takerWon(bouts, points)` — returns `true` if points ≥ `requiredPoints(bouts)`.

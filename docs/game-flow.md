# Game Flow

## Overview

After setting up players on the setup screen, the user taps **Start Game** to begin a session.

## Screens

### Setup Screen (`LandingScreen`)
- Choose 3–5 players via filter chips.
- Enter optional names for each player.
- Tap **Start Game** to lock in the names and navigate to the game screen.

### Game Screen (`GameScreen`)

The game is divided into **rounds**. Each round follows a two-step flow:

#### Step 1 — Pick the taker
One button is shown per player. Tapping a player selects them as the **taker** (the one who takes the hand) and advances to step 2.

#### Step 2 — Pick a contract
The selected taker announces their contract. Available contracts (weakest → strongest):

| Contract     | Description                    |
|--------------|-------------------------------|
| Petite       | Weakest contract               |
| Pousse       | Slightly stronger than Petite  |
| Garde        | Standard contract              |
| Garde Sans   | Play without the dog           |
| Garde Contre | Play against the dog           |

The taker can also **Skip round** if no contract is announced.

A **← Change player** button lets the user go back to step 1 if the wrong player was selected.

## Round History

After each round is completed, the result is appended to a history list at the bottom of the screen (newest round first):

```
Round 3: Alice — Garde
Round 2: Bob — Skipped
Round 1: Alice — Petite
```

## Data Model

- `Contract` enum — the five possible contracts with display names.
- `RoundResult` data class — stores round number, taker name, and chosen contract (or `null` if skipped).

# Player Setup Screen

## Overview

The landing screen lets users configure a game before it starts. It currently handles two setup steps:

1. **Choose the number of players** (3, 4, or 5)
2. **Enter each player's name**

## How it works

### Player count selection

Three `FilterChip` buttons (labeled 3, 4, 5) let the user pick the number of players. The selected chip is highlighted. The default is **3 players**.

### Player name inputs

Below the chips, one `OutlinedTextField` is rendered per player (e.g. "Player 1", "Player 2", …). The number of fields updates instantly when the chip selection changes:

- Switching from 3 → 5 adds two new empty fields.
- Switching from 5 → 3 removes the last two fields (and any names typed in them).
- Names already typed in the remaining fields are **preserved** when the count changes.

### Duplicate name validation

Every player in a game must have a unique name. The app validates this in real time as the user types:

- **Name resolution** — a blank field is treated as its fallback label (`"Player 1"`, `"Player 2"`, …), the same label GameScreen would use during play. This means leaving two fields empty is caught as a duplicate.
- **Case-insensitive** — `"Alice"` and `"alice"` are considered the same name.
- **Error highlight** — any field whose resolved name clashes with another gets a red border and a `"Name already used"` hint below it. Both conflicting fields are highlighted, not just one.
- **Button disabled** — the "Start Game" button is disabled as long as at least one duplicate exists. It re-enables automatically once all names are unique.

#### Example

| Field | Typed value | Resolved as | Duplicate? |
|---|---|---|:---:|
| Player 1 | *(blank)* | `Player 1` | ✓ — clashes with Player 2 |
| Player 2 | `player 1` | `player 1` | ✓ — clashes with Player 1 |
| Player 3 | `Charlie` | `Charlie` | — |

Both Player 1 and Player 2 fields turn red; "Start Game" is disabled until one of them is changed.

#### Implementation notes

The logic lives entirely inside `LandingScreen.kt` as derived state — no extra state variables are needed:

```kotlin
// 1. Resolve blank entries to their "Player N" fallback.
val resolvedNames = playerNames.mapIndexed { i, name -> name.ifBlank { "Player ${i + 1}" } }

// 2. Lower-case everything for a case-insensitive comparison.
val lowerNames = resolvedNames.map { it.lowercase() }

// 3. A slot is a duplicate when its name appears more than once in the list.
val duplicateFlags = lowerNames.map { name -> lowerNames.count { it == name } > 1 }

// 4. Any duplicate disables the button.
val hasDuplicates = duplicateFlags.any { it }
```

Because `resolvedNames`, `lowerNames`, `duplicateFlags`, and `hasDuplicates` are plain `val`s computed inside the composable, Compose recalculates them automatically on every recomposition (i.e. every keystroke). No `remember` or `LaunchedEffect` is needed.

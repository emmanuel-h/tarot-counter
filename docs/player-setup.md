# Player Setup Screen

## Overview

The landing screen lets users configure a game before it starts. It currently handles two setup steps:

1. **Choose the number of players** (3, 4, or 5)
2. **Enter each player's name**

## Layout (Salon redesign, issue #197)

```
┌──────────────────────────────┐
│ Tarot Counter            [⚙] │  SalonTopBar: wordmark + settings icon
│ ┌── felt card ─────────────┐ │  only when a game is in progress
│ │ GAME IN PROGRESS  (A)(B)…│ │  brass overline + avatar stack
│ │ Round 5                  │ │
│ │ 4 players · Alice leads +312 │
│ │ [         Resume        ]│ │  ivory pill on the felt
│ └──────────────────────────┘ │
│ ═══════════ ♠ ♥ ♦ ♣ ═══════  │  SuitDivider (only with the resume card)
│ ┌── New Game ──────────────┐ │  SalonCard
│ │ Players        (3 | 4 | 5)│ │
│ │ (1) Player 1             │ │  PlayerNameField per seat
│ │ (2) Player 2             │ │
│ │ First Dealer (Random|Choose)│
│ │ [       Start Game      ]│ │
│ └──────────────────────────┘ │
│ Past Games                   │  SectionHeader
│ ┌──────────────────────────┐ │
│ │ 🏆 Alice             +540 │ │  one SalonCard row per game
│ │    Tue 22 Sep · 5 players · 8 rounds │
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

The screen is one scrollable column, capped at 600 dp and centred on tablets. `imePadding()` keeps the focused name field above the keyboard.

1. **Top bar**: the app name as a Cormorant wordmark (`headlineLarge`) on the left, the settings gear on the right (48 dp touch target). It replaces the old centred title and the emoji suits row.
2. **Resume card**: a `FeltCard` shown at the **top** when an unfinished game is saved. It shows:
   - a brass "GAME IN PROGRESS" overline
   - the next round number
   - the player count and the current leader with their score ("Alice leads +312"; a tie reads "Alice & Bob lead +20", and before any scored round it reads "No rounds played")
   - an `AvatarStack` of the players
   - an ivory **Resume** button
3. **Suit divider**: separates the game in progress from a new game.
4. **New game card**: a `SalonCard` titled "New Game" that groups every setup step. It holds the player count, one name field per seat, the first dealer, and Start.
5. **Past games**: a `SectionHeader` followed by one row per saved game (see below).

### Past games rows

Each row is a `SalonCard`, at least 48 dp tall, laid out like this:

- **Left:** a brass trophy.
- **Middle:** the winner's name. A tie reads "Tie: Alice & Bob"; a game with no rounds reads "No rounds played".
- **Under the name:** date · player count · round count.
- **Right:** the winner's score as a `ScoreText`.

The date comes from `formatGameDate()` (`GameDates.kt`) and uses the app language, not the device's:

- same year as today: "Tue 22 Sep" / "mar. 22 sept."
- another year: the year is added, e.g. "Sat 13 Sep 2025"

### Pure logic

| Function | File | Purpose |
|---|---|---|
| `currentLeaders(playerNames, rounds)` | `GameModels.kt` | Leader(s) and their score for the resume card; `null` before the first scored round (skipped rounds don't count) |
| `formatGameDate(datestamp, locale, now, timeZone)` | `GameDates.kt` | Past-game date in the app language, with the year only when it differs from the current one |
| `AppLocale.javaLocale` | `AppLocale.kt` | Maps EN/FR to `Locale.ENGLISH` / `Locale.FRENCH` for date formatting |

All three are covered by `HomeLogicTest`.

## How it works

### Player count selection

A compact `SingleChoiceSegmentedButtonRow` with three segments (3, 4, 5) sits on the right of the "Players" label. The selected segment is filled felt green. The default is **3 players**.

### Player name inputs

Below the player count, one `PlayerNameField` is shown per seat, with that seat's coloured avatar in the leading slot. While a field is empty, the avatar shows the seat number and the field shows a "Player N" placeholder. Once a name is typed, the avatar shows the name's initial. The number of fields updates instantly when the player count changes:

- Switching from 3 → 5 adds two new empty fields.
- Switching from 5 → 3 removes the last two fields (and any names typed in them).
- Names already typed in the remaining fields are **preserved** when the count changes.

### Duplicate name validation

Every player in a game must have a unique name. The app validates this in real time as the user types:

- **Name resolution** — a blank field is treated as its fallback label (`"Player 1"`, `"Player 2"`, …), the same label GameScreen would use during play. This means leaving two fields empty is caught as a duplicate.
- **Case-insensitive** — `"Alice"` and `"alice"` are considered the same name.
- **Error highlight** — any field whose resolved name clashes with another gets a red border and an inline `"Name already used"` message right under it. Both conflicting fields are highlighted, not just one.
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

## First Dealer selection

The "First Dealer" section sits between the player name fields and the Start Game button. It lets the user control who distributes the cards for the very first round.

### Two modes

| Mode | Behavior |
|---|---|
| **Random** (default) | The app picks a random player from the list; the choice is invisible to users. |
| **Choose** | A `SingleChoiceSegmentedButtonRow` with one segment per player appears so the user can tap a name. Defaults to the first player (index 0). |

### State

```kotlin
var useRandomDealer by remember { mutableStateOf(true) }
var selectedDealerIndex by remember { mutableIntStateOf(0) }
```

`selectedDealerIndex` is reset to 0 whenever the player count changes (inside the player-count button `onClick`) to prevent an out-of-range index after the player list shrinks.

### Callback

The `onStartGame` callback now carries a second parameter:

```kotlin
onStartGame: (names: List<String>, dealerIndex: Int?) -> Unit
```

`dealerIndex` is `null` when Random mode is active, and the chosen 0-based index when Choose mode is active.

### ViewModel integration

`GameViewModel.initGame()` accepts an optional `startingIndexOverride: Int? = null`. When `inProgressGame == null` (fresh start), it resolves the starting index as:

```
inProgressGame?.startingIndex ?: startingIndexOverride ?: displayNames.indices.random()
```

Restoring a saved game always uses the stored index; the override applies only to new games.

## Feedback button

A low-prominence `AppTextButton` sits at the very bottom of the scrollable column. Tapping it fires an `Intent(ACTION_SENDTO, Uri.parse("mailto:mandarinetech.dev@gmail.com"))`, which opens the user's default email client pre-addressed to the developer. The `ACTION_SENDTO` + `mailto:` combination ensures only email apps (not messaging apps) respond to the intent.

| Locale | Label |
|---|---|
| EN | Send Feedback |
| FR | Contacter le développeur |

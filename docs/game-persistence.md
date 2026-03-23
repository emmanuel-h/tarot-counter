# Game Persistence

## Purpose

Completed games are saved to the device so players can review past results after closing and reopening the app. The **Past Games** list appears on the setup screen whenever at least one game has been saved.

## Two kinds of saves

### In-progress game (auto-saved after every round)

The current game state is written to DataStore after **every completed or skipped round**. If the app is closed at any point, the next launch shows a **Resume Game** card on the setup screen. Tapping it restores the game exactly where it left off (same players, same round number, full history).

The in-progress entry is cleared in two situations:
- The user presses **New Game** on the Final Score screen (game is over → save as completed, clear in-progress).
- The user fills in the setup form and presses **Start Game** (they are choosing to abandon the old game and start fresh).

### Completed game (saved on "New Game")

A completed `SavedGame` entry is written when the user presses **New Game** on the Final Score screen. This is intentional:

- Pressing **New Game** signals that the game is truly over — save it.
- Pressing **Back to game** means the user wants to continue — do not save yet.
- If the game is ended before any round is played (empty `roundHistory`), nothing is saved.

## What Is Stored

### `InProgressGame` (restored on resume)

| Field | Type | Description |
|---|---|---|
| `playerNames` | `List<String>` | Display names used during the game |
| `currentRound` | `Int` | The round number to play next |
| `startingIndex` | `Int` | Index of the first taker — needed to restore the rotation formula |
| `rounds` | `List<RoundResult>` | All rounds completed so far |

### `SavedGame` (shown in Past Games)

| Field | Type | Description |
|---|---|---|
| `id` | `String` | UUID generated at save time — uniquely identifies the game |
| `datestamp` | `Long` | Unix timestamp in milliseconds (`System.currentTimeMillis()`) |
| `playerNames` | `List<String>` | Display names at the time the game was played |
| `rounds` | `List<RoundResult>` | Full round history in chronological order |
| `finalScores` | `Map<String, Int>` | Pre-computed cumulative totals (stored to avoid re-computing on every display) |

A maximum of **20 completed games** are kept. When the limit is reached, the oldest entry is dropped. There is always at most **one** in-progress game.

## Storage Technology

Games are persisted using **DataStore (Preferences)**, Android's modern replacement for `SharedPreferences`. The entire list of saved games is serialized to a single JSON string and stored under the key `saved_games` in a file named `tarot_games`.

**Why DataStore over SharedPreferences?**
- DataStore is the current Android recommendation — it uses Kotlin coroutines and is safe to read/write on a background thread without extra boilerplate.
- SharedPreferences is being phased out and has known issues with concurrent access.

**Why JSON over a database (Room)?**
- The data set is small (at most 20 games) and is always read/written as a whole list.
- JSON + DataStore requires fewer files and less setup than Room while being sufficient for this use case.

## Serialization

`kotlinx.serialization` converts Kotlin objects to/from JSON strings. The `@Serializable` annotation is applied to every type that needs to be saved:

```
Contract      (enum)
Chelem        (enum)
RoundDetails  (data class)
RoundResult   (data class)
SavedGame     (data class)
```

The Kotlin Serialization Gradle plugin generates the read/write code at compile time — no reflection is used at runtime, which keeps performance fast.

## Architecture

```
MainActivity
    │
    ├─ creates ─▶ GameViewModel (AndroidViewModel)
    │                 │
    │                 ├─ exposes ─▶ pastGames:      StateFlow<List<SavedGame>>
    │                 ├─ exposes ─▶ inProgressGame: StateFlow<InProgressGame?>
    │                 │               └─ both collected by MainActivity via collectAsState()
    │                 │
    │                 └─ delegates to ─▶ GameStorage
    │                                       ├─ loadGames()           → Flow<List<SavedGame>>
    │                                       ├─ addGame()             → suspend (completed games)
    │                                       ├─ loadInProgressGame()  → Flow<InProgressGame?>
    │                                       ├─ saveInProgressGame()  → suspend
    │                                       └─ clearInProgressGame() → suspend
    │
    ├─ passes pastGames + inProgressGame ─▶ LandingScreen
    │       └─ onResumeGame callback ◀──────────────────────
    │
    └─ passes inProgressGame + callbacks ─▶ GameScreen
            ├─ onSaveProgress: called after every round
            └─ onSaveGame:     called on "New Game" (also clears in-progress)
```

### Component responsibilities

**`GameStorage`** — thin I/O layer. Reads and writes JSON strings to DataStore. No business logic. Manages two independent keys: one for the completed-game list, one for the single in-progress entry.

**`GameViewModel`** — bridges storage and UI. Exposes two `StateFlow`s for the UI to observe, and launches coroutines to write without blocking the main thread. `saveGame()` atomically saves the completed game and clears the in-progress entry.

**`MainActivity`** — creates the ViewModel (lives at activity level so it survives screen rotations), collects both flows, and wires all callbacks. Holds `gameToResume` in Compose `remember` state to pass the restored game into `GameScreen`.

**`GameScreen`** — initialises its local state (`currentRound`, `startingIndex`, `roundHistory`) from `inProgressGame` if provided, or with fresh defaults. Calls `onSaveProgress` after every `recordPlayed` / `recordSkipped`. Does not know about DataStore.

**`LandingScreen`** — receives `pastGames` and `inProgressGame` as plain parameters and renders the cards. Does not know about DataStore.

## LandingScreen UI

### Resume Game card

Displayed at the very top (above the setup form) when `inProgressGame` is not null. Uses `primaryContainer` background to stand out from the Past Games cards.

```
╔══════════════════════════════════╗
║ Resume Game                      ║
║ Alice, Bob, Charlie              ║
║ Round 4 · 3 rounds played        ║
║ [          Resume              ] ║
╚══════════════════════════════════╝
```

Tapping **Resume** calls `onResumeGame`, which navigates straight to `GameScreen` with the saved state — the setup form is skipped entirely.

If the user ignores the card and presses **Start Game** instead, the in-progress entry is cleared and a fresh game begins.

### Past Games list

Shown below the "Start Game" button whenever `pastGames` is not empty. Each entry is a `Card` with three lines:

```
Alice, Bob, Charlie
Winner: Alice (+150)
5 rounds · 23/03/2026
```

- Line 1: player names joined with ", "
- Line 2: winner with final score, or "Tie: …" for multiple winners
- Line 3: round count and date in `dd/MM/yyyy` format

Games are listed newest-first (most recent at the top).

## Related Files

- `SavedGame.kt` — `@Serializable` data classes for `SavedGame` and `InProgressGame`
- `GameStorage.kt` — DataStore read/write and JSON serialization (both game types)
- `GameViewModel.kt` — `StateFlow` exposure and coroutine lifecycle
- `MainActivity.kt` — ViewModel creation, flow collection, and callback wiring
- `GameScreen.kt` — state restoration from `InProgressGame`, `onSaveProgress` after each round, `SavedGame` construction on "New Game"
- `LandingScreen.kt` — `ResumeGameCard` composable, `PastGameCard` composable
- `GameModels.kt` — `@Serializable` annotations on `Contract`, `Chelem`, `RoundDetails`, `RoundResult`

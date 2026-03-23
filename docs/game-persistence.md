# Game Persistence

## Purpose

Completed games are saved to the device so players can review past results after closing and reopening the app. The **Past Games** list appears on the setup screen whenever at least one game has been saved.

## When a Game Is Saved

A game is saved when the user presses **New Game** on the Final Score screen. This is intentional:

- Pressing **New Game** signals that the game is truly over — save it.
- Pressing **Back to game** means the user wants to continue — do not save yet.
- If the game is ended before any round is played (empty `roundHistory`), nothing is saved.

## What Is Stored

Each saved game is a `SavedGame` object containing:

| Field | Type | Description |
|---|---|---|
| `id` | `String` | UUID generated at save time — uniquely identifies the game |
| `datestamp` | `Long` | Unix timestamp in milliseconds (`System.currentTimeMillis()`) |
| `playerNames` | `List<String>` | Display names as they appeared during the game |
| `rounds` | `List<RoundResult>` | Full round history in chronological order |
| `finalScores` | `Map<String, Int>` | Pre-computed cumulative totals (stored to avoid re-computing on every display) |

A maximum of **20 games** are kept. When the limit is reached, the oldest entry is dropped.

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
    │                 ├─ exposes ─▶ pastGames: StateFlow<List<SavedGame>>
    │                 │               └─ collected by LandingScreen via collectAsState()
    │                 │
    │                 └─ delegates to ─▶ GameStorage
    │                                       ├─ loadGames(): Flow<List<SavedGame>>
    │                                       └─ addGame(SavedGame): suspend
    │
    └─ passes onSaveGame callback ─▶ GameScreen
                                         └─ calls onSaveGame() when user taps "New Game"
```

### Component responsibilities

**`GameStorage`** — thin I/O layer. Knows how to read/write JSON to DataStore. No business logic.

**`GameViewModel`** — bridges storage and UI. Exposes a `StateFlow` for the UI to observe, and launches coroutines to write without blocking the main thread.

**`MainActivity`** — creates the ViewModel (it lives at the activity level so it survives screen rotations) and wires the `onSaveGame` callback into `GameScreen`.

**`GameScreen`** — builds the `SavedGame` from its in-memory state and calls `onSaveGame` before delegating to `onEndGame`. It does not know about DataStore.

**`LandingScreen`** — receives `pastGames` as a plain `List<SavedGame>` parameter and renders the cards. It does not know about DataStore.

## Past Games UI (LandingScreen)

The **Past Games** section is shown below the "Start Game" button whenever `pastGames` is not empty. Each entry is a `Card` with three lines:

```
Alice, Bob, Charlie
Winner: Alice (+150)
5 rounds · 23/03/2026
```

| Line | Content |
|---|---|
| 1 | Player names joined with ", " |
| 2 | Winner with final score, or "Tie: …" for multiple winners |
| 3 | Round count and date in `dd/MM/yyyy` format |

Games are listed newest-first (most recent at the top).

## Related Files

| File | Role |
|---|---|
| `SavedGame.kt` | `@Serializable` data class for one completed game |
| `GameStorage.kt` | DataStore read/write and JSON serialization |
| `GameViewModel.kt` | `StateFlow` exposure and coroutine lifecycle |
| `MainActivity.kt` | ViewModel creation and callback wiring |
| `GameScreen.kt` | `SavedGame` construction and `onSaveGame` call |
| `LandingScreen.kt` | `PastGameCard` composable, "Past Games" section |
| `GameModels.kt` | `@Serializable` annotations on `Contract`, `Chelem`, `RoundDetails`, `RoundResult` |

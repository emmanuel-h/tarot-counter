# TarotCounter

An Android app for tracking scores in **French Tarot**, a classic French trick-taking card game.

## What the App Does

TarotCounter guides players through a game round by round:

1. **Setup** — choose 3, 4, or 5 players and optionally enter custom names; duplicate names are detected in real time and the Start button is disabled until all names are unique; a decorative `♠ ♥ ♦ ♣` header above the title sets the card-game tone; tap ☀️ or 🌙 in the top-left to toggle between light and dark mode (persisted across restarts, defaults to light); tap 🇬🇧 or 🇫🇷 in the top-right to switch the app language (persisted across restarts, defaults to device language)
2. **Contract selection** — the current taker picks their contract; a persistent **bottom action bar** always shows **End Game** (left) and **Skip round** (right) for quick access
3. **Scoring details** — enter bouts, points scored (0–91), partner (5-player), and any bonuses; a radio button lets you switch between entering the **taker's points** or the **defenders' points** (the app converts automatically using `takerPoints = 91 − defenderPoints`)
4. **Scoreboard & history** — live cumulative scores per player and a log of all rounds, newest first; each history row shows a colored **●** indicator (green = won, red = lost, grey = skipped) for at-a-glance scanning
5. **Score history table** — tap the bar-chart icon (left of the header) to see a round-by-round table of cumulative scores for every player
6. **End Game / Final Score** — tap **End Game** in the bottom bar at any point to see the final results: winner card with total score, full round-by-round table (winner's column highlighted in gold/amber), and a "New Game" button
7. **Colour-coded scores** — positive scores appear in green and negative scores in red across all score views (CompactScoreboard, ScoreHistoryScreen, FinalScoreScreen); colours adapt to light/dark theme automatically
8. **Auto-save & Resume** — the game state is saved after every round; if the app is closed mid-game, a "Resume Game" card appears on the setup screen the next time it is opened
9. **Past Games** — completed games are saved to the device; the setup screen shows a list of past results with a trophy icon next to the winner's name
10. **Back navigation** — the Android system back button always returns to the landing page; on the Final Score screen a confirmation dialog is shown first to avoid accidentally losing unsaved results

The app automatically rotates the taker each round, determines win/loss, and computes each player's score for the round.

## Game Rules Summary

### Win Condition

The taker wins if they score enough points based on the number of **bouts** (special trump cards) captured:

| Bouts captured | Points needed to win |
|:-:|:-:|
| 0 | 56 |
| 1 | 51 |
| 2 | 41 |
| 3 | 36 |

### Contracts (weakest → strongest)

| Contract | Multiplier |
|---|:---:|
| Prise | ×1 |
| Garde | ×2 |
| Garde Sans | ×4 |
| Garde Contre | ×6 |

### Round Score

```
roundScore = (25 + |scoredPoints − requiredPoints(bouts)|) × multiplier
```

Scores are zero-sum. The taker receives `±(n−1) × roundScore` for 3/4-player games, `±2 × roundScore` in 5-player games (partner gets `±1 × roundScore`); each defender receives `∓roundScore`.

### Bonuses Tracked per Round

Player-assigned bonuses are entered via a compact grid with one checkbox per player. Ticking a checkbox assigns that bonus; ticking it again clears it.

- **Petit au bout** — player who captured the 1 of trump on the last trick
- **Poignée / Double poignée / Triple poignée** — trump distribution bonuses (10 / 13 / 15 trumps shown)
- **Chelem** — grand slam outcome selected from a dropdown (announced+won, announced+lost, unannounced+won, or none), with an additional player selector to record who called it. When an announced chelem is selected and a player is chosen, the app reminds the table that this player leads the first trick.

## Architecture

Single-module Jetpack Compose app — all source lives in `:app` under `fr.mandarine.tarotcounter`.

```
app/src/main/java/fr/mandarine/tarotcounter/
├── MainActivity.kt        # Entry point, top-level navigation state, ViewModel wiring
├── Navigation.kt          # Screen enum (SETUP / GAME)
├── GameModels.kt          # Data models & pure game logic (no Android deps)
├── SavedGame.kt           # Serializable snapshot of a completed game
├── GameStorage.kt         # DataStore read/write + JSON serialization
├── GameViewModel.kt       # Exposes past games as StateFlow, fires save coroutines
├── LandingScreen.kt       # Player setup UI + Past Games list
├── GameScreen.kt          # Round management, taker rotation, history, End Game button
├── RoundDetailsForm.kt    # Scoring details form
├── ScoreHistoryScreen.kt  # Round-by-round cumulative score table
├── FinalScoreScreen.kt    # End-of-game results: winner card + full score table
└── ui/theme/              # Material 3 theme, colors, typography
```

**Key design choice**: `GameModels.kt` contains only pure Kotlin with no Android or Compose imports, making it fully unit-testable on the JVM.

**Persistence**: completed games are saved to **DataStore** as JSON (via `kotlinx.serialization`). A `GameViewModel` holds the `StateFlow<List<SavedGame>>` that the setup screen observes.

## Tech Stack

| Component | Version |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2024.09.00) |
| Design system | Material 3 (custom card-game palette — dynamic color disabled; Cinzel serif font for headings) |
| Persistence | DataStore 1.1.1 + kotlinx.serialization 1.7.3 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 (Android 15) |
| Build | AGP 9.1.0, Gradle 9.3.1, Java 11 |

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (unsigned — no credentials configured)
./gradlew assembleRelease

# Build signed release bundle for Google Play (requires signing credentials)
./gradlew bundleRelease

# Install on connected device
./gradlew installDebug
```

### Release Signing

`./gradlew bundleRelease` produces a signed `.aab` when the following credentials
are supplied via `~/.gradle/gradle.properties` (local) or environment variables (CI):

| Variable | Description |
|---|---|
| `RELEASE_KEYSTORE_FILE` | Path to the `.jks` keystore |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |

See [`docs/release-signing.md`](docs/release-signing.md) for full setup instructions.

## Testing

```bash
# Unit tests (JVM, no device needed)
./gradlew test

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

### Test Coverage

| Test file | What it covers |
|---|---|
| `GameModelsTest.kt` | Data models, win condition, score calculation, player score distribution, `computeFinalTotals`, `findWinners` |
| `TakerRotationTest.kt` | Taker rotation formula for 3–5 players |
| `AppLocaleTest.kt` | i18n string bundles: locale-specific strings, lambda formatters, enum localized names |
| `GameViewModelTest.kt` | ViewModel: locale + theme StateFlows, `setLocale`, `setTheme`, `saveGame`, `clearInProgressGame` |
| `LandingScreenTest.kt` | Setup screen UI: player count chips, name fields, duplicate validation, theme toggle chips |
| `GameScreenTest.kt` | Full game flow: contract selection, details form, history, score history navigation, End Game button |
| `ScoreHistoryScreenTest.kt` | Score history table: column headers, cumulative totals, back navigation |
| `FinalScoreScreenTest.kt` | Final score screen: winner card, tie detection, score table, New Game navigation |

## Project Structure

```
TarotCounter/
├── app/
│   ├── src/
│   │   ├── main/           # Production code
│   │   ├── test/           # Unit tests (JVM)
│   │   └── androidTest/    # Instrumented tests (device/emulator)
│   └── build.gradle.kts
├── docs/
│   ├── game-flow.md          # Game mechanics specification
│   ├── player-setup.md       # Setup screen behaviour
│   ├── score-history.md      # Score history table
│   ├── final-score.md        # Final score screen: winner card, End Game flow
│   ├── game-persistence.md   # How completed games are saved and displayed
│   ├── score-color.md        # Score colour-coding convention and scoreColor() helper
│   ├── theme.md              # Colour palette rationale and dynamic-colour policy
│   ├── app-name.md           # App name branding and locale-specific launcher labels
│   └── release-signing.md    # Release signing setup for local dev and CI
├── gradle/
│   └── libs.versions.toml  # Dependency version catalog
├── CLAUDE.md               # AI assistant instructions
└── README.md               # This file
```

## Documentation

More detailed documentation lives in [`docs/`](docs/):

- [`docs/game-flow.md`](docs/game-flow.md) — complete game mechanics, data models, round history format
- [`docs/player-setup.md`](docs/player-setup.md) — setup screen behaviour and validation rules
- [`docs/score-history.md`](docs/score-history.md) — score history table: layout, navigation, scrolling
- [`docs/final-score.md`](docs/final-score.md) — final score screen: winner card, table highlighting, New Game navigation
- [`docs/game-persistence.md`](docs/game-persistence.md) — how completed games are saved to DataStore and displayed on the setup screen
- [`docs/score-color.md`](docs/score-color.md) — score colour-coding convention: `scoreColor()` helper, where it is used, winner column
- [`docs/theme.md`](docs/theme.md) — colour palette rationale, roles, and dynamic-colour policy
- [`docs/back-navigation.md`](docs/back-navigation.md) — system back button behaviour per screen, BackHandler implementation, confirmation dialog
- [`docs/app-name.md`](docs/app-name.md) — app name branding, locale-specific launcher labels, and how the system name and in-app title relate
- [`docs/release-signing.md`](docs/release-signing.md) — how to configure release signing for local builds and CI/CD pipelines

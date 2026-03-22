# TarotCounter

An Android app for tracking scores in **French Tarot**, a classic French trick-taking card game.

## What the App Does

TarotCounter guides players through a game round by round:

1. **Setup** — choose 3, 4, or 5 players and optionally enter custom names
2. **Contract selection** — the current taker picks their contract (or skips)
3. **Scoring details** — enter bouts, points scored, partner (5-player), and any bonuses
4. **Scoreboard & history** — live cumulative scores per player and a log of all rounds, newest first

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

- **Petit au bout** — player who captured the 1 of trump on the last trick
- **Misère / Double misère** — declarations
- **Poignée / Double poignée** — trump distribution bonuses
- **Chelem** — grand slam (announced+won, announced+lost, unannounced+won, or none)

## Architecture

Single-module Jetpack Compose app — all source lives in `:app` under `fr.mandarine.tarotcounter`.

```
app/src/main/java/fr/mandarine/tarotcounter/
├── MainActivity.kt        # Entry point, top-level navigation state
├── Navigation.kt          # Screen enum (SETUP / GAME)
├── GameModels.kt          # Data models & pure game logic (no Android deps)
├── LandingScreen.kt       # Player setup UI
├── GameScreen.kt          # Round management, taker rotation, history
├── RoundDetailsForm.kt    # Scoring details form
└── ui/theme/              # Material 3 theme, colors, typography
```

**No ViewModel / state management library** — state is held in composables with `remember`/`mutableStateOf`. This is intentional: the app is small and local state is sufficient.

**Key design choice**: `GameModels.kt` contains only pure Kotlin with no Android or Compose imports, making it fully unit-testable on the JVM.

## Tech Stack

| Component | Version |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2024.09.00) |
| Design system | Material 3 (dynamic color on Android 12+) |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 (Android 15) |
| Build | AGP 9.1.0, Gradle 9.3.1, Java 11 |

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

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
| `GameModelsTest.kt` | Data models, win condition, score calculation, player score distribution |
| `TakerRotationTest.kt` | Taker rotation formula for 3–5 players |
| `LandingScreenTest.kt` | Setup screen UI: player count chips, name fields, navigation |
| `GameScreenTest.kt` | Full game flow: contract selection, details form, history |

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
│   ├── game-flow.md        # Game mechanics specification
│   └── player-setup.md     # Setup screen behaviour
├── gradle/
│   └── libs.versions.toml  # Dependency version catalog
├── CLAUDE.md               # AI assistant instructions
└── README.md               # This file
```

## Documentation

More detailed documentation lives in [`docs/`](docs/):

- [`docs/game-flow.md`](docs/game-flow.md) — complete game mechanics, data models, round history format
- [`docs/player-setup.md`](docs/player-setup.md) — setup screen behaviour and validation rules

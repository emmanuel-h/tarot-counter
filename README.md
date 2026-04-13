# TarotCounter

An Android app for tracking scores in **French Tarot**, a classic French trick-taking card game.

## What the App Does

TarotCounter guides players through a game round by round:

1. **Setup** — choose 3, 4, or 5 players and optionally enter custom names; duplicate names are detected in real time and the Start button is disabled until all names are unique; choose the **first dealer** (random or pick a specific player); a decorative `♠ ♥ ♦ ♣` header above the title sets the card-game tone; tap the **⚙ gear icon** (top-right) to open the Settings page
2. **Attacker selection + contract** — tap the player who won the bidding to set them as the **attacker** (any player can bid, not just the dealer); then pick their contract; the dealer label shows who is distributing the cards this round; a persistent **bottom action bar** always shows **End Game** (left) and **Skip round** (right) for quick access
3. **Scoring details** — enter bouts, points scored (0–91), partner (5-player), and any bonuses; a radio button lets you switch between entering the **taker's points** or the **defenders' points** (the app converts automatically using `takerPoints = 91 − defenderPoints`)
4. **Compact scoreboard** — after the first round, a persistent card at the top shows each player's running total at a glance
5. **Score history screen** — tap the bar-chart icon (always visible in the header) to open the history screen; a **segmented toggle** switches between two views: **Table** (cumulative scores per round, one row per player) and **List** (round-by-round detail log, newest first, with a coloured **●** indicator per row: green = won, red = lost, grey = skipped)
6. **End Game / Final Score** — tap **End Game** in the bottom bar at any point to see the final results: winner card with total score, full round-by-round table (winner's column highlighted in gold/amber), three action buttons (**Main Menu**, **New Game**, **Back to Game**), and two PDF export buttons: **Share PDF** (opens the OS share sheet) and **Save to device** (opens the system file-save picker so the user can save directly to Downloads or any other location — no storage permission needed)
7. **Colour-coded scores** — positive scores appear in green and negative scores in red across all score views (CompactScoreboard, ScoreHistoryScreen, FinalScoreScreen); colours adapt to light/dark theme automatically
8. **Auto-save & Resume** — the game state is saved after every round; if the app is closed mid-game, a "Resume Game" card appears on the setup screen the next time it is opened
9. **Past Games** — completed games are saved to the device; the setup screen shows a list of past results with a trophy icon next to the winner's name
10. **Back navigation** — the Android system back button always returns to the landing page; on the Final Score screen a confirmation dialog is shown first to avoid accidentally losing unsaved results
11. **Settings page** — a dedicated settings page (reachable via the ⚙ gear icon on the setup screen) consolidates theme toggle (☀️ / 🌙), language toggle (🇬🇧 / 🇫🇷), and a feedback button that opens the device's email client pre-addressed to the developer; both theme and language are persisted across restarts

The app rotates the **dealer** each round and lets the user explicitly select the **attacker** (the player who won the bidding), determines win/loss, and computes each player's score for the round.

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

| Contract (FR) | Contract (EN) | Multiplier |
|---|---|:---:|
| Prise | Small | ×1 |
| Garde | Guard | ×2 |
| Garde Sans | Guard Without | ×4 |
| Garde Contre | Guard Against | ×6 |

### Round Score

```
roundScore = (25 + |scoredPoints − requiredPoints(bouts)|) × multiplier
```

Scores are zero-sum. The taker receives `±(n−1) × roundScore` for 3/4-player games, `±2 × roundScore` in 5-player games (partner gets `±1 × roundScore`); each defender receives `∓roundScore`.

### Bonuses Tracked per Round

Player-assigned bonuses are entered via a compact grid with one checkbox per player.

- **Petit au bout** — single-select: the one player who captured the 1 of trump on the last trick
- **Poignée / Double poignée / Triple poignée** — multi-select: any number of players can each independently show their own trump hand. Each declaration contributes its own bonus to the winning camp. The minimum trump count varies by player count (3 players: 13/15/18 · 4 players: 10/13/15 · 5 players: 8/10/13) and the tooltip in the UI always shows the correct threshold for the current game. **Atout validation**: if the total declared trump thresholds exceed the 22 trumps in the deck, an error is shown and the Confirm button is disabled.
- **Chelem** — grand slam outcome selected from a dropdown (announced+won, announced+lost, unannounced+won, defenders realized, or none), with an additional player selector to record who called it. When an announced chelem is selected and a player is chosen, the app reminds the table that this player leads the first trick. The "Defenders realized" option covers the FFT-official scenario where the defending camp wins every trick without having announced it (+200 to each defender, per R-RO201206.pdf p.6).

## Architecture

Single-module Jetpack Compose app — all source lives in `:app` under `fr.mandarine.tarotcounter`.

```
app/src/main/java/fr/mandarine/tarotcounter/
├── MainActivity.kt        # Entry point, Screen enum (SETUP / GAME), top-level navigation, ViewModel wiring
├── AppLocale.kt           # Locale enum (EN / FR) + CompositionLocal provider
├── AppTheme.kt            # Theme enum (LIGHT / DARK) + CompositionLocal provider
├── AppStrings.kt          # All user-visible strings, parameterised by locale
├── GameModels.kt          # Data models, serializable SavedGame/InProgressGame, pure game logic (no Android deps)
├── GameStorage.kt         # DataStore read/write + JSON serialization
├── GameViewModel.kt       # StateFlows for games, locale, theme; save/load coroutines
├── LandingScreen.kt       # Player setup UI + Past Games list
├── GameScreen.kt          # Round management, taker rotation, details form, history, End Game button
├── ScreenHeader.kt        # Shared back-arrow header composable
├── SettingsScreen.kt      # Settings page: theme, language, feedback
├── ScoreHistoryScreen.kt  # Score history: table view + list view with toggle
├── FinalScoreScreen.kt    # End-of-game results: winner card + full score table
├── UiComponents.kt        # Shared UI: AppButton, AppOutlinedButton, AppTextButton, AutoSizeText
└── ui/theme/              # Material 3 theme, colors, typography
```

**Key design choice**: `GameModels.kt` contains only pure Kotlin with no Android or Compose imports, making it fully unit-testable on the JVM.

**Button convention**: never use raw `Button` / `OutlinedButton` / `TextButton` — always use `AppButton` / `AppOutlinedButton` / `AppTextButton` from `UiComponents.kt`. These wrappers automatically shrink labels to fit any screen width or translation length. See [`docs/ui-components.md`](docs/ui-components.md) for details.

**Tablet & landscape support**: every screen wraps its content in a `Box(contentAlignment = TopCenter)` and constrains the inner column to `MAX_CONTENT_WIDTH = 600 dp` via `widthIn`. On phones the column fills the screen normally; on 10-inch tablets in landscape the content is centered with comfortable margins on each side.

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

See [`docs/release-signing.md`](docs/release-signing.md) for full setup instructions and [`docs/app-bundle.md`](docs/app-bundle.md) for App Bundle / Play Store submission details.

### Publishing a Release

Use the `/release-store` skill to automate the full release workflow in one step:

```
/release-store minor    # bump minor version (default)
/release-store major    # bump major version
/release-store hotfix   # bump patch version
```

The skill bumps `versionCode` / `versionName` in `app/build.gradle.kts`, builds the signed `.aab`, creates a GitHub release with auto-generated notes, and uploads the artifact. See [`docs/release-workflow.md`](docs/release-workflow.md) for details.

### R8 Minification

Release builds automatically minify and shrink resources (`isMinifyEnabled = true`,
`isShrinkResources = true`). Project-specific ProGuard rules live in
`app/proguard-rules.pro` — see [`docs/release-signing.md`](docs/release-signing.md)
for details.

### Crash Reporting

TarotCounter uses Android Vitals (built into Google Play) for automatic crash
collection — no third-party SDK is required. Two types of symbols are bundled in
every release App Bundle to make crash reports readable:

- **Native debug symbols** — `ndk { debugSymbolLevel = "FULL" }` in
  `app/build.gradle.kts` embeds unstripped `.so` files so Play Console can
  symbolicate native stack frames (e.g. from Jetpack Compose's native libraries).
- **R8 mapping file** — generated automatically by R8 minification; bundled in the
  `.aab` and also archived as a `mapping.txt` asset on each GitHub release so that
  Kotlin/Java frames can always be deobfuscated.

See [`docs/crash-reporting.md`](docs/crash-reporting.md) for the full setup,
manual retrace instructions, and where to view crash reports in Play Console.

## Testing

```bash
# Unit tests (JVM, no device needed)
./gradlew test

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Mutation tests (PIT) — report: app/build/reports/pitest/index.html
./gradlew pitest

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
| `LandingScreenTest.kt` | Setup screen UI: player count chips, name fields, duplicate validation, settings gear icon |
| `SettingsScreenTest.kt` | Settings page: back navigation, theme toggle, language toggle, feedback button, section labels |
| `GameScreenTest.kt` | Full game flow: contract selection, details form, history, score history navigation, End Game button |
| `ScoreHistoryScreenTest.kt` | Score history screen: table view, list view, toggle, round indicators, back navigation |
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
│   ├── ui-components.md      # Shared UI components: AppButton, AutoSizeText guideline
│   ├── game-flow.md          # Game mechanics specification
│   ├── player-setup.md       # Setup screen behaviour
│   ├── settings.md           # Settings page: theme, language, feedback
│   ├── score-history.md      # Score history table
│   ├── final-score.md        # Final score screen: winner card, End Game flow
│   ├── game-persistence.md   # How completed games are saved and displayed
│   ├── score-color.md        # Score colour-coding convention and scoreColor() helper
│   ├── theme.md              # Colour palette rationale and dynamic-colour policy
│   ├── app-name.md           # App name branding and locale-specific launcher labels
│   ├── release-signing.md    # Release signing setup for local dev and CI
│   ├── release-workflow.md   # /release-store skill: full publish workflow
│   ├── crash-reporting.md    # Crash reporting: native symbols, mapping file, Android Vitals
│   └── mutation-testing.md   # PIT mutation testing: setup, quality gate, reading reports
├── gradle/
│   └── libs.versions.toml  # Dependency version catalog
├── CLAUDE.md               # AI assistant instructions
└── README.md               # This file
```

## Documentation

More detailed documentation lives in [`docs/`](docs/):

- [`docs/ui-components.md`](docs/ui-components.md) — shared UI building blocks: `AppButton`, `AutoSizeText`, and the button convention
- [`docs/game-flow.md`](docs/game-flow.md) — complete game mechanics, data models, round history format
- [`docs/player-setup.md`](docs/player-setup.md) — setup screen behaviour and validation rules
- [`docs/settings.md`](docs/settings.md) — settings page: theme toggle, language toggle, feedback button, navigation wiring
- [`docs/score-history.md`](docs/score-history.md) — score history screen: table view, list view, toggle, navigation
- [`docs/final-score.md`](docs/final-score.md) — final score screen: winner card, table highlighting, New Game navigation
- [`docs/game-persistence.md`](docs/game-persistence.md) — how completed games are saved to DataStore and displayed on the setup screen
- [`docs/score-color.md`](docs/score-color.md) — score colour-coding convention: `scoreColor()` helper, where it is used, winner column
- [`docs/theme.md`](docs/theme.md) — colour palette rationale, roles, and dynamic-colour policy
- [`docs/back-navigation.md`](docs/back-navigation.md) — system back button behaviour per screen, BackHandler implementation, confirmation dialog
- [`docs/app-name.md`](docs/app-name.md) — app name branding, locale-specific launcher labels, and how the system name and in-app title relate
- [`docs/release-signing.md`](docs/release-signing.md) — how to configure release signing for local builds and CI/CD pipelines
- [`docs/release-workflow.md`](docs/release-workflow.md) — automated release workflow via `/release-store` skill (version bump, AAB build, GitHub release)
- [`docs/crash-reporting.md`](docs/crash-reporting.md) — crash reporting setup: native debug symbols, R8 mapping file, Android Vitals, manual retrace

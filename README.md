# TarotCounter

An Android app for tracking scores in **French Tarot**, a classic French trick-taking card game.

## What the App Does

TarotCounter guides players through a game round by round:

1. **Home / setup** — a Salon top bar shows the app name with a **⚙** settings button. If a game is in progress, a felt-green **resume card** comes first: round number, avatar stack, current leader and score, **Resume** button. A **New Game** card groups the setup: choose 3, 4 or 5 players, enter optional names (each field shows the seat's coloured avatar), and choose the **first dealer** (random or a specific player). Duplicate names are flagged inline in real time, and Start stays disabled until every name is unique.
2. **Game screen, between rounds**: the scores come first. A dealer chip sits under the "Round N" title (with undo and history icon buttons). After round 1 comes a ranked **Standings** card: rank, avatar, name, a small ▲/▼ trend since the last round, and the score; leaders are highlighted in brass. Then **"Who took?"**: one large avatar tile per player (3 / 2×2 / 3+2). Then **Last rounds**: the latest 3, with **See all** opening the history. The bottom bar holds **End Game** (red text) and **Skip round**.
3. **Round entry**: tapping a taker tile opens "ROUND 5 · Chloé takes" (the back arrow changes the taker). Pick one of four **contract cards** (name + ×multiplier), then the **bouts** chips (0–3, with "needs 41"). Enter the **points** in a large number field with an **Attack | Defense** toggle; defense points are converted with `takerPoints = 91 − defenderPoints`. A **live result** pill ("Made by 6 → Chloé +124" / "Short by 4 → …") updates as you type, bonuses included. Then choose the partner (avatar chips, 5 players) and bonuses, and tap **Confirm round**.
4. **Score history screen**: open it from the chart icon, **See all** or **See all rounds**. A **Table | List** toggle switches views. The **Table** has a pinned header of avatars and names, running totals, brass-tinted leader column(s), and scrolls sideways for 5 players on narrow phones. The **List** shows one card per round, newest first: an "R4" badge, taker avatar, contract, bouts and points, a Won/Lost chip and the taker's score change; skipped rounds are muted. Before the first round, an empty state is shown.
5. **End Game / Game Over**: tap **End Game** at any point to see the results. A felt-green **winner card** shows the trophy, the winner (or every co-winner on a tie), their score, and the rounds and player counts. Below it come a **ranking** with avatars and a **Score over time** line chart (one line per player in their colour, zero baseline, round ticks). **See all rounds** opens the round-by-round table. The actions are stacked: **New Game**, **Main Menu**, **Back to game**.
6. **Colour-coded scores** — positive scores appear in green and negative scores in red across all score views (standings, ScoreHistoryScreen, FinalScoreScreen); colours adapt to light/dark theme automatically
7. **Auto-save & Resume** — the game state is saved after every round. If the app is closed mid-game, the resume card appears at the top of the home screen the next time it is opened.
8. **Past Games** — completed games are saved to the device. The home screen lists them, one row each: trophy, winner (or tie), date in the app language, player and round counts, and the winner's score.
9. **Back navigation** — the Android system back button closes the round entry first (back to "Who took?"), otherwise returns to the landing page; on the Final Score screen a confirmation dialog is shown first to avoid accidentally losing unsaved results
10. **Settings page**: reached from the ⚙ gear icon on the home screen. It has grouped cards with icons: **Appearance** (Light / Dark), **Language** (English / Français), **Help** (Rules ›, Send feedback ›) and **About** (version). The Rules open as a full-screen page with section headers and tables for the bouts thresholds and contract multipliers, built from the scoring code. Theme and language are persisted across restarts.

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

The round entry shows a **Bonuses** card with three rows, each with its current value (e.g. "Poignée · (1) Alice"). Tapping a row opens a bottom sheet:

- **Petit au bout**: a single choice. Pick the player who captured the 1 of trump on the last trick, or None. Picking closes the sheet.
- **Poignée**: for each player, choose **None / Simple / Double / Triple**. Any number of players may declare, one level each, and every declaration adds its bonus to the winning camp. The sheet shows the trump thresholds for the current player count (3 players: 13/15/18 · 4 players: 10/13/15 · 5 players: 8/10/13). **Atout validation**: if the declared thresholds add up to more than the 22 trumps in the deck, an error shows in the sheet and on the row, and Confirm is disabled.
- **Chelem**: the outcome (none, announced and realized, announced and not realized, not announced and realized, defenders realized), then who called it (the taker, or the partner in a 5-player game). For an announced chelem, the sheet reminds the table that this player leads the first trick. "Defenders realized" covers the official FFT case where the defending camp wins every trick without announcing it (+200 to each defender, per R-RO201206.pdf p.6).

Every bonus is included in the live result pill before the round is confirmed.

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
├── GameScreen.kt          # Game screen: standings, "Who took?" tiles, last rounds, round entry, bottom bar
├── SettingsScreen.kt      # Settings: grouped cards (appearance, language, help, about)
├── RulesScreen.kt         # Full-screen rules page with bouts / contracts tables
├── RulesData.kt           # Rules tables built from the scoring code
├── ScoreHistoryScreen.kt  # Score history: sticky-header table + round cards, with toggle
├── FinalScoreScreen.kt    # Game over: winner card, ranking, score-over-time chart
├── UiComponents.kt        # Shared UI: App* buttons, AutoSizeText, Salon components (SalonCard, PlayerAvatar, SalonTopBar…)
├── UiComponentsPreviews.kt # Light + dark @Previews of every Salon component
├── SalonUi.kt             # Pure logic behind the Salon components (initials, sizes, suit glyphs)
├── GameDates.kt           # Past-game date formatting in the app language
├── Standings.kt           # Game screen logic: ranked standings, taker grid, last rounds
├── Bonuses.kt             # Bonus logic: poignée declarations, chelem candidates
├── BonusSheets.kt         # Bonus rows + bottom sheets (Petit au bout, Poignée, Chelem)
├── ScoreChart.kt          # Game over chart data: cumulative series, bounds, x labels
├── ScoreHistoryLogic.kt   # Score history layout logic: sideways scroll, leader columns
└── ui/theme/              # "Salon" design tokens: colours (+ TarotColors), typography, shapes, spacing, contrast helper
```

**Key design choice**: `GameModels.kt` contains only pure Kotlin with no Android or Compose imports, making it fully unit-testable on the JVM.

**Button convention**: never use raw `Button` / `OutlinedButton` / `TextButton` — always use `AppButton` / `AppOutlinedButton` / `AppTextButton` from `UiComponents.kt`. These wrappers automatically shrink labels to fit any screen width or translation length. See [`docs/ui-components.md`](docs/ui-components.md) for details.

**Salon components**: screens are assembled from shared "Salon" building blocks — `SalonCard` (paper card), `PlayerAvatar` / `AvatarStack` (coloured initial circles, one colour per seat), `SuitDivider` (♠ ♥ ♦ ♣ separator), `SectionHeader`, `ScoreText` (signed, coloured, tabular score), `SalonTopBar` (title + back arrow + up to two icon actions), `SalonTextField` / `PlayerNameField`. Buttons are pills (56 dp felt-green primary, 48 dp hairline outline) and every `SegmentedButton` uses `salonSegmentedButtonColors()` (selected segment filled felt green).

**Tablet & landscape support**: every screen wraps its content in a `Box(contentAlignment = TopCenter)` and constrains the inner column to `MAX_CONTENT_WIDTH = 600 dp` via `widthIn`. On phones the column fills the screen normally; on 10-inch tablets in landscape the content is centered with comfortable margins on each side.

**Persistence**: completed games are saved to **DataStore** as JSON (via `kotlinx.serialization`). A `GameViewModel` holds the `StateFlow<List<SavedGame>>` that the setup screen observes.

## Tech Stack

| Component | Version |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2024.09.00) |
| Design system | Material 3 with the "Salon" design tokens — ivory/felt-green palette with a WCAG-checked dark variant, dynamic color disabled; bundled Cormorant Garamond (display) + Figtree (UI) fonts with tabular figures |
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
| `ColorContrastTest.kt` | WCAG relative luminance and contrast ratio helpers |
| `SalonPaletteTest.kt` | Every text/background pair of both themes ≥ 4.5:1; player tones |
| `SalonUiTest.kt` | Salon component logic: player initials, avatar/score sizes, suit glyphs, top-bar action limit |
| `RulesDataTest.kt` | Rules page tables (bout thresholds, contract multipliers) and settings strings |
| `ScoreHistoryLogicTest.kt` | Score history: when the table scrolls sideways, leader columns |
| `ScoreChartTest.kt` | Game over chart data: cumulative series, y bounds (zero included), x-axis labels |
| `BonusesTest.kt` | Bonus sheet logic: one poignée level per player, declarants order, chelem candidates, announced chelems |
| `RoundPreviewTest.kt` | Live round result: win/short margin, distribution (3 and 5 players), bonuses, zero-sum |
| `StandingsTest.kt` | Game screen logic: ranked standings (ties, leaders, trend), taker grid columns, last rounds |
| `HomeLogicTest.kt` | Home screen logic: current leader(s), past-game date formatting, app → Java locale |
| `GameViewModelTest.kt` | ViewModel: locale + theme StateFlows, `setLocale`, `setTheme`, `saveGame`, `clearInProgressGame` |
| `LandingScreenTest.kt` | Home screen: top bar, resume card, New Game card (player count, name fields with avatars, duplicate validation, dealer), past-game rows |
| `SettingsScreenTest.kt` | Settings: groups, theme/language toggles, feedback row, version, rules page (tables, close, system back) |
| `GameScreenTest.kt` | Full game flow: contract selection, details form, history, score history navigation, End Game button |
| `ScoreHistoryScreenTest.kt` | Score history: table (header avatars, totals, sideways scroll), round cards, empty state, toggle, back navigation |
| `FinalScoreScreenTest.kt` | Game over: winner card, tie, ranking order, chart description, See all rounds, stacked actions, leave confirmation |
| `SalonThemeTest.kt` | Theme wiring: light/dark tokens, `scoreColor()`, fonts, shapes |
| `SalonTopBarTest.kt` | Salon top bar: title, localized back arrow, actions, 48 dp touch targets |
| `UiComponentsTest.kt` | Shared components: App* buttons, bonus grid, chip selector, score rows, Salon card/avatar/stack/header/score/text field |

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
│   ├── ui-components.md      # Shared UI components: App* buttons, AutoSizeText, Salon components
│   ├── game-flow.md          # Game mechanics specification
│   ├── player-setup.md       # Setup screen behaviour
│   ├── settings.md           # Settings page: theme, language, feedback
│   ├── score-history.md      # Score history table
│   ├── final-score.md        # Game over screen: winner card, ranking, score chart
│   ├── game-persistence.md   # How completed games are saved and displayed
│   ├── score-color.md        # Score colour-coding convention and scoreColor() helper
│   ├── theme.md              # "Salon" design tokens: colours, dark variant, fonts, shapes, spacing
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

- [`docs/ui-components.md`](docs/ui-components.md) — shared UI building blocks: `AppButton`, `AutoSizeText`, the button convention, and the Salon components (`SalonCard`, `PlayerAvatar`, `SalonTopBar`…)
- [`docs/game-flow.md`](docs/game-flow.md) — complete game mechanics, data models, round history format
- [`docs/player-setup.md`](docs/player-setup.md) — setup screen behaviour and validation rules
- [`docs/settings.md`](docs/settings.md) — settings page: theme toggle, language toggle, feedback button, navigation wiring
- [`docs/score-history.md`](docs/score-history.md) — score history screen: table view, list view, toggle, navigation
- [`docs/final-score.md`](docs/final-score.md) — final score screen: winner card, table highlighting, New Game navigation
- [`docs/game-persistence.md`](docs/game-persistence.md) — how completed games are saved to DataStore and displayed on the setup screen
- [`docs/score-color.md`](docs/score-color.md) — score colour-coding convention: `scoreColor()` helper, where it is used, winner column
- [`docs/theme.md`](docs/theme.md) — "Salon" design tokens: light/dark palettes, player tones, contrast guarantee, typography, shapes, spacing
- [`docs/back-navigation.md`](docs/back-navigation.md) — system back button behaviour per screen, BackHandler implementation, confirmation dialog
- [`docs/app-name.md`](docs/app-name.md) — app name branding, locale-specific launcher labels, and how the system name and in-app title relate
- [`docs/release-signing.md`](docs/release-signing.md) — how to configure release signing for local builds and CI/CD pipelines
- [`docs/release-workflow.md`](docs/release-workflow.md) — automated release workflow via `/release-store` skill (version bump, AAB build, GitHub release)
- [`docs/crash-reporting.md`](docs/crash-reporting.md) — crash reporting setup: native debug symbols, R8 mapping file, Android Vitals, manual retrace

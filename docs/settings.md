# Settings Screen

## Overview

The Settings screen consolidates all user-preference controls in one place, keeping the main
setup screen (LandingScreen) uncluttered.

It is reached by tapping the **gear icon** (⚙) in the top-right corner of the landing screen.
A back arrow (←) returns the user to the landing screen.

## Controls

| Control | Values | Persisted via |
|---------|--------|---------------|
| Theme toggle | ☀️ Light / 🌙 Dark | `GameViewModel.setTheme()` → DataStore |
| Language toggle | 🇬🇧 English / 🇫🇷 French | `GameViewModel.setLocale()` → DataStore |
| Rules | Opens rules dialog (no persistence) | Local `remember` state |
| Send Feedback | Opens default email client | Android Intent (mailto:) |

### Rules dialog

Tapping **Rules** opens `RulesDialog` — a scrollable modal that covers all implemented scoring
mechanics, sourced directly from the logic in `GameModels.kt`:

| Section | Content |
|---------|---------|
| Objective | Bout thresholds (0 → 56 pts, 1 → 51, 2 → 41, 3 → 36) |
| Contracts | Prise ×1, Garde ×2, Garde Sans ×4, Garde Contre ×6 |
| Score Formula | (25 + \|actual − required\|) × multiplier |
| Score Distribution | 3/4-player vs 5-player taker/partner/defender split |
| Bonuses | Petit au bout, Poignée (simple/double/triple), Chelem |

The dialog is displayed inline on top of SettingsScreen using a standard Compose `Dialog`. It is
dismissed by tapping the **Close** button or tapping the scrim outside the dialog. The open/closed
state is held in a `var showRules by remember { mutableStateOf(false) }` local variable in
`SettingsScreen` — nothing is persisted.

Both the theme and language choices survive app restarts — they are stored with DataStore
(see `docs/game-persistence.md`) and restored in `MainActivity` via `collectAsState()`.

## Architecture

`SettingsScreen` is a stateless composable that:

1. Reads current values from `LocalAppLocale.current` and `LocalAppTheme.current`
   (the same `CompositionLocal` providers used by every other screen).
2. Calls `onThemeChange` / `onLocaleChange` lambdas when the user taps a segment —
   `MainActivity` routes these callbacks to `GameViewModel`, which persists them.
3. Calls `onBack` when the user taps the back arrow — `MainActivity` sets
   `currentScreen = Screen.SETUP`.

Navigation is handled by the same `Screen` enum that drives `LandingScreen` and `GameScreen`:

```
Screen.SETUP    ──(gear icon)──► Screen.SETTINGS
Screen.SETTINGS ──(back arrow)──► Screen.SETUP
```

## File locations

| File | Role |
|------|------|
| `app/src/main/…/SettingsScreen.kt` | Composable UI for the settings page; holds `showRules` state |
| `app/src/main/…/RulesScreen.kt` | `RulesDialog` composable |
| `app/src/androidTest/…/SettingsScreenTest.kt` | UI tests for the settings page and rules dialog |
| `app/src/main/…/AppStrings.kt` | `settings`, `settingsTitle`, `themeLabel`, `languageLabel`, `rulesButton`, `rulesTitle`, etc. |
| `app/src/main/…/MainActivity.kt` | `Screen.SETTINGS` enum value and navigation wiring |

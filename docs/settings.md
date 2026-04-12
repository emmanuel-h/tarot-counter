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
| Send Feedback | Opens default email client | Android Intent (mailto:) |

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
| `app/src/main/…/SettingsScreen.kt` | Composable UI for the settings page |
| `app/src/androidTest/…/SettingsScreenTest.kt` | UI tests for the settings page |
| `app/src/main/…/AppStrings.kt` | `settings`, `settingsTitle`, `themeLabel`, `languageLabel` strings |
| `app/src/main/…/MainActivity.kt` | `Screen.SETTINGS` enum value and navigation wiring |

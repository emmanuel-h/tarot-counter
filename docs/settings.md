# Settings Screen

## Overview

The Settings screen consolidates all user-preference controls in one place, keeping the main
setup screen (LandingScreen) uncluttered.

It is reached by tapping the **gear icon** (⚙) in the top-right corner of the landing screen.
A back arrow (←) returns the user to the landing screen.

## Layout (Salon restyle, issue #203)

```
┌──────────────────────────────────┐
│ ←  Settings                      │
│ APPEARANCE                       │
│ ┌──────────────────────────────┐ │
│ │ [palette] Theme (Light|Dark) │ │
│ └──────────────────────────────┘ │
│ LANGUAGE                         │
│ ┌──────────────────────────────┐ │
│ │ [globe] (English | Français) │ │
│ └──────────────────────────────┘ │
│ HELP                             │
│ ┌──────────────────────────────┐ │
│ │ [book]  Rules              › │ │
│ │ [mail]  Send Feedback      › │ │
│ └──────────────────────────────┘ │
│ ABOUT                            │
│ ┌──────────────────────────────┐ │
│ │ [info]  Version        2.2.0 │ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```

Each group has an upper-case heading above a `SalonCard` of list rows. Every row has a leading icon.

## Controls

| Group | Control | Values | Persisted via |
|-------|---------|--------|---------------|
| Appearance | Theme toggle | **Light / Dark** (text labels) | `GameViewModel.setTheme()` → DataStore |
| Language | Language toggle | **English / Français** (each written in its own language, not flag emoji) | `GameViewModel.setLocale()` → DataStore |
| Help | Rules › | Opens the full-screen rules page | Local `remember` state |
| Help | Send Feedback › | Opens the default email client | Android Intent (`mailto:`) |
| About | Version | `BuildConfig.VERSION_NAME` (the `buildConfig` build feature is enabled for this) | — |

`SettingsScreen` takes a `versionName` parameter that defaults to `BuildConfig.VERSION_NAME`, so tests and previews can pass a fixed value.

### Rules page

Tapping **Rules** replaces the settings screen with `RulesScreen`, a full-screen page that replaces the former dialog. Its back arrow (labelled "Close") or the system back button returns to Settings.

| Section | Content |
|---------|---------|
| Objective | Intro text + a **Bouts / Points needed** table (0 → 56, 1 → 51, 2 → 41, 3 → 36) |
| Contracts | Intro text + a **Contract / Multiplier** table (×1, ×2, ×4, ×6) |
| Score Formula | (25 + \|actual − required\|) × multiplier |
| Score Distribution | 3/4-player vs 5-player taker/partner/defender split |
| Bonuses | Petit au bout, Poignée (simple/double/triple), Chelem |

Both tables are built from the scoring code itself (`RulesData.kt`: `boutThresholdRows()` uses `requiredPoints()`, and `contractMultiplierRows()` uses `Contract.multiplier`), so the rules shown can never disagree with the computed scores. `RulesDataTest` covers them. Suit dividers separate the table sections.

Both the theme and language choices survive app restarts — they are stored with DataStore
(see `docs/game-persistence.md`) and restored in `MainActivity` via `collectAsState()`.

On **first startup**, before the user has saved any preference, the language is auto-detected
from the device's system locale (`Locale.getDefault().language`):

- System locale is French → `AppLocale.FR`
- Any other system locale → `AppLocale.EN`

This fallback logic lives in `MainActivity` and only applies while `savedLocale` is `null`
(i.e. the DataStore has not yet returned a value, or no preference has ever been saved).

## Architecture

`SettingsScreen` is a stateless composable that:

1. Reads current values from `LocalAppLocale.current` and `LocalAppTheme.current`
   (the same `CompositionLocal` providers used by every other screen).
2. Calls `onThemeChange` / `onLocaleChange` lambdas when the user taps a segment —
   `MainActivity` routes these callbacks to `GameViewModel`, which persists them.
3. Holds `showRules`: while it is true, `RulesScreen` is shown instead, and a `BackHandler` closes it.
4. Calls `onBack` when the user taps the back arrow — `MainActivity` sets
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

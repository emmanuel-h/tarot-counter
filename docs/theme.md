# Theme & Colour Palette

TarotCounter uses a custom **card-game themed** Material 3 colour palette. Dynamic colour is deliberately disabled so the theme is applied consistently across all Android versions (including Android 12+ where dynamic colour would otherwise override everything with the user's wallpaper tones).

## Why a custom palette?

The Android Studio project template ships with a generic purple/pink palette (`Purple80`/`Purple40`). That palette has no connection to French Tarot — so it was replaced with colours that evoke a real card table.

## Colour Roles

| Role | Light theme | Dark theme | Rationale |
|---|---|---|---|
| **Primary** | Deep forest green `#1B5E20` | Sage green `#A5D6A7` | Green felt card table |
| **Secondary** | Rich amber `#F9A825` | Warm gold `#FFD54F` | Gold trim on playing cards |
| **Tertiary** | Deep burgundy `#6A1B2A` | Soft rose `#EF9A9A` | Classic card-table accent |
| **Background / Surface** | Warm parchment `#F5F0E8` | Dark felt `#0D1F0F` | Parchment (light) / felt (dark) |

## Files

- `ui/theme/Color.kt` — named colour constants for both schemes
- `ui/theme/Theme.kt` — `DarkColorScheme`, `LightColorScheme`, and `TarotCounterTheme` composable

## Dynamic Colour

The `dynamicColor` parameter of `TarotCounterTheme` defaults to `false`. This means:

- The custom green/gold palette is used on **all Android versions**.
- On Android 12+, the OS would normally replace the entire palette with colours derived from the user's wallpaper. Disabling this ensures the card-game aesthetic is always present.
- The parameter is kept in the function signature so it can still be set to `true` in tests if needed.

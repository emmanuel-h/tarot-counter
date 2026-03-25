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
- `ui/theme/Type.kt` — `Typography` definition with Cinzel for headings

## Typography

Heading styles (`headlineLarge`, `headlineMedium`, `headlineSmall`, `titleLarge`) use **Cinzel**, a classical Roman-capitals serif font from Google Fonts bundled in `res/font/cinzel_regular.ttf`. It gives the app the look of antique engraved lettering that matches the tarot aesthetic.

Body and label styles (`bodyLarge`, `bodyMedium`, `bodySmall`, `labelMedium`, …) remain on the system sans-serif font (`FontFamily.Default`) for readability at small sizes.

The font file is a *variable font*, so a single TTF covers the full weight range (Regular 400 → Black 900). Two weights are declared in `CinzelFontFamily` — Normal (400) and Bold (700) — so Compose can pick the right variant when callers use `.copy(fontWeight = FontWeight.Bold)`.

### Where each heading style appears

| Style | Used in |
|---|---|
| `headlineLarge` | App title on LandingScreen |
| `headlineMedium` | "Game Over" on FinalScoreScreen |
| `headlineSmall` | Winner name inside the winner card |
| `titleLarge` | Round header on GameScreen |

## Dynamic Colour

The `dynamicColor` parameter of `TarotCounterTheme` defaults to `false`. This means:

- The custom green/gold palette is used on **all Android versions**.
- On Android 12+, the OS would normally replace the entire palette with colours derived from the user's wallpaper. Disabling this ensures the card-game aesthetic is always present.
- The parameter is kept in the function signature so it can still be set to `true` in tests if needed.

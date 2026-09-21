# Theme — "Salon" design tokens

TarotCounter's look is called **Salon**: a card table in a salon, with ivory paper cards on an ivory ground, felt green for actions, and a touch of brass for ornaments. This page describes the *design tokens* (colours, type, shapes, spacing) every screen builds on. They were introduced in issue #195, the first step of the Salon redesign epic (#206).

Dynamic colour (Material You) is deliberately disabled: the Salon palette is applied the same way on every Android version.

## Files

| File | Contents |
|---|---|
| `ui/theme/Color.kt` | Raw colour values for both themes and the player tones |
| `ui/theme/Theme.kt` | `LightColorScheme`, `DarkColorScheme`, and the `TarotCounterTheme` composable |
| `ui/theme/TarotColors.kt` | Extended colours Material has no slot for (`TarotColors`, `LocalTarotColors`, `MaterialTheme.tarotColors`) |
| `ui/theme/Type.kt` | Cormorant Garamond and Figtree font families + the `Typography` scale |
| `ui/theme/Shape.kt` | Corner radii (`Shapes`) |
| `ui/theme/Dimens.kt` | Spacing grid, screen margin, button heights, max content width |
| `ui/theme/ColorContrast.kt` | WCAG `contrastRatio()` used to test readability |

## Colours

Screens never use raw hex values. They read **semantic roles**:

- `MaterialTheme.colorScheme.*` — the standard Material 3 roles (primary, surface, onSurface…)
- `MaterialTheme.tarotColors.*` — Salon-specific roles (brass, positive, negative, felt, player tones)

### Light — "Salon"

| Role | Token | Hex | Use |
|---|---|---|---|
| Background | `colorScheme.background` | `#F6F1E7` | Ivory page ground |
| Card | `colorScheme.surface` | `#FFFDF8` | Paper cards |
| Hairline | `colorScheme.outline` | `#E4DCCB` | Card borders, dividers |
| Row separator | `colorScheme.outlineVariant` | `#EFE8DA` | Separators inside cards |
| Track | `colorScheme.surfaceVariant` | `#F1EADB` | Segmented-control track |
| Primary | `colorScheme.primary` | `#1F4D3A` | Felt green: primary buttons, selected states |
| Primary container | `colorScheme.primaryContainer` | `#E3ECE6` | Selected chip, success banner |
| Ink | `colorScheme.onSurface` | `#1E2420` | Main text |
| Muted ink | `colorScheme.onSurfaceVariant` | `#5E5A50` | Secondary text |
| Brass | `tarotColors.brass` | `#B08A3E` | Ornaments only (leader ring, suit marks), never text |
| Brass text | `tarotColors.brassText` / `colorScheme.secondary` | `#826325` | Brass-coloured text |
| Positive | `tarotColors.positive` | `#2E6B45` | Positive scores |
| Negative | `tarotColors.negative` / `colorScheme.error` | `#9B2C2C` | Negative scores, destructive actions |
| Felt card | `tarotColors.felt` / `onFelt` / `brassOnFelt` | `#1F4D3A` / `#F6F1E7` / `#E2C98E` | Hero cards: resume game, winner |
| Winner column | `tarotColors.winnerHighlight` | `#F3E7C9` | Winner column in score tables |

> The issue suggested `#8A6A28` for brass text. It reaches only 4.47:1 on the ivory ground, so a hair darker `#826325` is used instead: it passes 4.5:1 on every surface.

### Dark — "Salon at night"

Not drawn in the mockups; derived from the same hues.

| Role | Hex | Notes |
|---|---|---|
| Background | `#101A15` | Deep felt |
| Card | `#18241E` | Raised felt |
| Hairline / separator | `#34443B` / `#28362F` | |
| Track | `#22302A` | |
| Primary | `#8FC7A6` (text on it `#0C2418`) | Sage: a deep green button would disappear on deep felt |
| Primary container | `#1F4D3A` | The felt keeps its daytime green |
| Ink / muted ink | `#EDE6D6` / `#B3AC9C` | Cream text |
| Brass (ornament + text) | `#D8B56A` | Lighter brass |
| Positive / negative | `#7FCB98` / `#F2A195` | Lighter so they stay readable |
| Felt card | `#1F4D3A` / `#F6F1E7` / `#E2C98E` | Same as light |
| Winner column | `#2F2A1C` | Dark brass tint |

### Player tones

Each seat has a stable colour, used for avatars (initials in a circle) and chart lines, so a player keeps the same colour on every screen. Read it with `MaterialTheme.tarotColors.playerTone(seatIndex)`, which returns the circle colour (`container`) and the initials colour (`content`). Indices past the fifth seat wrap around, so the call never crashes.

| Seat | Light (white initials) | Dark (dark initials) |
|---|---|---|
| 1 | `#3E6B5A` green | `#8DBBA8` |
| 2 | `#8A5A3C` tobacco | `#D4A583` |
| 3 | `#4A5A8A` slate blue | `#A3B1DE` |
| 4 | `#7A4A6A` plum | `#CFA0C0` |
| 5 | `#6B6A2E` olive | `#C8C77E` |

The dark tones are lighter so chart lines stay visible (≥ 3:1) on the dark card surface.

### Readability guarantee

Every text/background pair of both themes reaches the WCAG AA ratio of **4.5:1**. `SalonPaletteTest` (unit test) computes the ratio of every pair with `contrastRatio()` and fails the build if any pair drops below it, so a future colour tweak cannot silently make text unreadable.

## Typography

Two families are bundled as static TTF files under `res/font/` (both SIL Open Font License, downloaded from Google Fonts):

| Family | Weights | Files | Used for |
|---|---|---|---|
| **Cormorant Garamond** | 600, 700 | `cormorant_garamond_semibold.ttf`, `cormorant_garamond_bold.ttf` | Display: screen titles, player names, big numbers |
| **Figtree** | 400, 500, 600 | `figtree_regular.ttf`, `figtree_medium.ttf`, `figtree_semibold.ttf` | All UI text: body, labels, buttons |

Static files are used instead of variable fonts because Android 7.x (API 24–25, the app's minimum) ignores the weight axis of variable fonts. Cinzel, the previous heading font, was removed.

### Type scale

| Style | Family | Weight | Size | Typical use |
|---|---|---|---|---|
| `displayLarge` | Cormorant | 600 | 56 sp | Points being entered |
| `displayMedium` | Cormorant | 700 | 40 sp | Winner name |
| `displaySmall` | Cormorant | 700 | 32 sp | Leader score |
| `headlineLarge` | Cormorant | 700 | 28 sp | App title |
| `headlineMedium` | Cormorant | 600 | 26 sp | Screen titles ("Round 5", "New game") |
| `headlineSmall` | Cormorant | 600 | 24 sp | Section titles ("Who took?") |
| `titleLarge` | Cormorant | 600 | 22 sp | Smaller section titles ("Past games") |
| `titleMedium` | Figtree | 600 | 16 sp | List item titles |
| `titleSmall` | Figtree | 600 | 14 sp | Small titles |
| `bodyLarge` / `Medium` / `Small` | Figtree | 400 | 16 / 14 / 13 sp | Running text |
| `labelLarge` | Figtree | 600 | 15 sp | Buttons |
| `labelMedium` | Figtree | 600 | 13 sp | Field labels |
| `labelSmall` | Figtree | 600 | 12 sp, wide spacing | Upper-case overlines ("GAME IN PROGRESS") |

### Tabular figures

Every style turns on the OpenType feature `tnum` (`fontFeatureSettings = "tnum"`). With it, every digit has the same width, so score columns line up: `+312` and `−48` stay right-aligned digit for digit. Because it is part of every style, any number in the app is aligned without extra work.

## Shapes

| Level | Radius | Used by |
|---|---|---|
| `extraSmall` | 8 dp | Tiny elements |
| `small` | 12 dp | Text fields, small chips, list rows |
| `medium` | 16 dp | Cards |
| `large` | 16 dp | Large cards, drawers |
| `extraLarge` | 28 dp | Dialogs, bottom sheets |

Buttons and segmented controls are pill-shaped: Material 3 gives them fully rounded ends by default.

## Spacing (`Dimens`)

| Token | Value | Use |
|---|---|---|
| `SpaceXs` / `SpaceS` / `SpaceM` / `SpaceL` / `SpaceXl` | 4 / 8 / 16 / 24 / 32 dp | 8 dp grid (4 dp half-step) |
| `ScreenMargin` | 20 dp | Horizontal screen margin |
| `CardPadding` | 20 dp | Inner card padding |
| `PrimaryButtonHeight` | 56 dp | Main call to action |
| `SecondaryButtonHeight` | 48 dp | Secondary actions |
| `MaxContentWidth` | 600 dp | Content column width cap on tablets |

The redesign steps that follow (#196 onward) apply these tokens to the shared components and screens.

## Dynamic Colour

The `dynamicColor` parameter of `TarotCounterTheme` is kept for API compatibility but ignored: the Salon palette is always used. On Android 12+ the OS would otherwise replace the whole palette with colours derived from the user's wallpaper.

## Dark / Light Mode Toggle

The app provides a manual theme toggle so the user can override the system setting.

### Default

Light mode (`darkTheme = false`) is **always** the default — the system dark-mode preference is intentionally ignored.

### UI

The toggle lives on the **Settings** page (see `docs/settings.md`).

### Persistence

The choice is stored in DataStore using `THEME_KEY = stringPreferencesKey("app_theme")` (the string `"LIGHT"` or `"DARK"`). It is loaded on startup and applied before the first frame via `TarotCounterTheme(darkTheme = isDarkTheme)`.

Anything that must differ between light and dark reads the theme tokens (`MaterialTheme.colorScheme` / `MaterialTheme.tarotColors`). Never call `isSystemInDarkTheme()`: it follows the phone setting, not the app's toggle.

### Architecture

| Layer | Component |
|---|---|
| Model | `AppTheme` enum (`LIGHT`, `DARK`) in `AppTheme.kt` |
| Storage | `THEME_KEY`, `loadTheme()`, `saveTheme()` in `GameStorage.kt` |
| ViewModel | `theme: StateFlow<AppTheme?>`, `setTheme()` in `GameViewModel.kt` |
| Composition | `LocalAppTheme` in `AppTheme.kt` (read via `LocalAppTheme.current`) |
| UI | `TarotCounterTheme(darkTheme = isDarkTheme)` in `MainActivity.kt` |
| Toggle | Theme selector on the Settings page (`SettingsScreen.kt`) |

## Tests

| Test | What it checks |
|---|---|
| `ColorContrastTest` (unit) | `relativeLuminance()` / `contrastRatio()` against WCAG reference values |
| `SalonPaletteTest` (unit) | Every text/background pair ≥ 4.5:1 in both themes; 5 distinct player tones; `playerTone()` wrap-around |
| `SalonThemeTest` (instrumented) | `TarotCounterTheme` provides the right tokens in light/dark; `scoreColor()` reads them; fonts load |

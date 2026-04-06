# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew testDebugUnitTest      # Run unit tests (debug variant)
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)
./gradlew lint                   # Run lint checks
./gradlew clean                  # Clean build artifacts
```

## Architecture

**Single-module Jetpack Compose app** — all code lives in `:app` under `fr.mandarine.tarotcounter`.

- **Entry point**: `MainActivity.kt` — uses `enableEdgeToEdge()` and sets a Compose content root
- **UI**: Jetpack Compose with Material Design 3; theme defined in `ui/theme/` (Color, Theme, Type)
- **Theme**: Dynamic color (Material You) on Android 12+, static purple/pink palette as fallback

## Tech Stack

- **Language**: Kotlin 2.2.10
- **UI**: Jetpack Compose (BOM 2024.09.00), Material3
- **Min SDK**: 24 / Target SDK: 36
- **Build**: AGP 9.1.0, Gradle 9.3.1, Java 11 source/target compatibility
- **Dependency versions**: Managed via `gradle/libs.versions.toml` (version catalog)

## Testing

- Unit tests: `src/test/` — JUnit 4
- Instrumented tests: `src/androidTest/` — AndroidJUnit4 + Espresso + Compose UI test
- **When adding or changing a feature, always write or update tests alongside the production code.**
  - Pure logic (data models, score calculation): unit tests in `src/test/`
  - Composable behaviour: Compose UI tests in `src/androidTest/`
  - Never leave a feature untested without an explicit note explaining why it cannot be tested.

## UI Components (`UiComponents.kt`)

All shared UI building blocks live in `UiComponents.kt`. **Never use raw Material3 button components directly** — always use the wrappers below so every button label automatically shrinks to fit its container, regardless of screen width or translation length.

| Use this | Instead of |
|---|---|
| `AppButton` | `Button` |
| `AppOutlinedButton` | `OutlinedButton` |
| `AppTextButton` | `TextButton` |
| `AutoSizeText` | `Text` inside `SegmentedButton` / `FilterChip` / any fixed-width slot |

- `AutoSizeText` reads the ambient `LocalTextStyle` (set by the enclosing composable) as its maximum font size and shrinks by 10 % per frame until the text fits or reaches `minFontSize` (default 8 sp).
- `AppButton` accepts an optional `textStyle` parameter (e.g. `MaterialTheme.typography.titleMedium`) to use a larger starting size for prominent call-to-action buttons.

### `SingleChoiceSegmentedButtonRow` — mutually exclusive options

Use `SingleChoiceSegmentedButtonRow` + `SegmentedButton` whenever the user picks **one option from a fixed set** (e.g. contract selection). Never use `FilterChip` for this purpose — chips are designed for multi-select filtering.

Mandatory conventions:
1. Pass `icon = {}` to every `SegmentedButton` — suppress the checkmark; the filled segment already communicates selection.
2. Use `AutoSizeText` (not `Text`) for every label.
3. Use `modifier = Modifier.padding(horizontal = 1.dp)` inside each label to keep text away from rounded corners.
4. **Share the font size** across all segments via `rememberSharedAutoSizeState(locale)` so every label displays at the same — smallest needed — size:

```kotlin
val labelSize = rememberSharedAutoSizeState(locale)   // resets on locale change

SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    items.forEachIndexed { index, item ->
        SegmentedButton(
            shape    = SegmentedButtonDefaults.itemShape(index, items.size),
            selected = selection == item,
            onClick  = { selection = if (selection == item) null else item },
            icon     = {}
        ) {
            AutoSizeText(
                text            = item.label,
                modifier        = Modifier.padding(horizontal = 1.dp),
                sharedSizeState = labelSize
            )
        }
    }
}
```

## Documentation
- Always add inline comments to generated code and explain key concepts. The user is new to Kotlin and Android development.
- When the user asks for a new feature, add or update relevant Markdown files in a docs/ folder.
- **Keep `README.md` in sync**: whenever you add, remove, or significantly change a feature (game rules, screens, bonuses, architecture, tech stack versions), update the relevant section(s) of `README.md` in the same commit.

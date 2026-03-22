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

## Documentation
- Always add inline comments to generated code and explain key concepts. The user is new to Kotlin and Android development.
- When the user asks for a new feature, add or update relevant Markdown files in a docs/ folder.
- **Keep `README.md` in sync**: whenever you add, remove, or significantly change a feature (game rules, screens, bonuses, architecture, tech stack versions), update the relevant section(s) of `README.md` in the same commit.

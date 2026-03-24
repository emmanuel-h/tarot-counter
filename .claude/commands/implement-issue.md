Read the GitHub issue at $ARGUMENTS and implement it end-to-end in this Android/Kotlin/Jetpack Compose repository.

Follow these steps in order:

## 1. Fetch the issue
Use `gh issue view <number> --repo <owner>/<repo>` or fetch the URL directly with `gh`. Extract:
- Title and full description
- Labels, acceptance criteria, and any linked issues
- Comments that clarify requirements

## 2. Explore the codebase
Read the relevant source files before writing any code. Understand existing patterns:
- Architecture: single-module Jetpack Compose app under `fr.mandarine.tarotcounter`
- Entry point: `MainActivity.kt`
- Theme: `ui/theme/`
- Documentation: `/docs`
- Existing screens and composables

## 3. Plan the implementation
Write a concise plan (bullet points) and show it to the user before starting. Cover:
- Which files to create or modify
- Any new dependencies needed in `gradle/libs.versions.toml`
- How the change fits the existing architecture

Wait for user confirmation before proceeding.

## 4. Implement
Make the changes. Follow project conventions:
- Kotlin 2.x idiomatic style
- Jetpack Compose + Material 3
- Inline comments on every non-trivial block (user is new to Kotlin/Android)
- Support both EN and FR locales via the existing `CompositionLocal` i18n approach (see `strings.xml` and locale switching logic)

## 5. Write or update tests
- Every new feature, bug fix or improvement should be covered by existing or new tests
- Pure logic → unit test in `src/test/` (JUnit 4)
- Composable behaviour → Compose UI test in `src/androidTest/`
- Run `./gradlew testDebugUnitTest` and fix any failures

## 6. Update documentation
- Add or update files in `docs/` describing the feature
- Keep `README.md` in sync (game rules, screens, bonuses, architecture changes)

## 7. Commit and push
- Stage only the relevant files (never `.env` or secrets)
- Write a clear conventional commit message referencing the issue number (e.g. `feat: add X (#42)`)
- Run `./gradlew lint` and fix any new warnings before committing
- Push to the current branch with `git push`

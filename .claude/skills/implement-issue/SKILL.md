---
name: implement-issue
description: Implement a GitHub issue end-to-end — branch, code, tests, docs, PR
argument-hint: <issue-url-or-number>
---

Read the GitHub issue at $ARGUMENTS and implement it end-to-end in this Android/Kotlin/Jetpack Compose repository.

Follow these steps in order:

## 1. Fetch the issue
Use `gh issue view <number> --repo <owner>/<repo>` or fetch the URL directly with `gh`. Extract:
- Title and full description
- Labels, acceptance criteria, and any linked issues
- Comments that clarify requirements

## 2. Prepare the branch
- Checkout `main` and pull latest
- Create a new branch named `<issue-number>-<short-description>` (e.g. `42-add-score-history`)

## 3. Explore the codebase
Read the relevant source files before writing any code. Understand existing patterns:
- Architecture: single-module Jetpack Compose app under `fr.mandarine.tarotcounter`
- Entry point: `MainActivity.kt`
- Theme: `ui/theme/`
- Documentation: `/docs`
- Existing screens and composables

## 4. Plan the implementation
Invoke the `fullstack-dev-skills:kotlin-specialist` skill (via the Skill tool) to get idiomatic Kotlin/Compose guidance before planning.
Write a concise plan (bullet points) and show it to the user before starting. Cover:
- Which files to create or modify
- Any new dependencies needed in `gradle/libs.versions.toml`
- How the change fits the existing architecture

## 5. Implement
Invoke the `fullstack-dev-skills:kotlin-specialist` skill (via the Skill tool) to review the implementation approach before writing code.
Make the changes. Follow project conventions:
- Kotlin 2.x idiomatic style
- Jetpack Compose + Material 3
- Inline comments on every non-trivial block (user is new to Kotlin/Android)
- Support both EN and FR locales via the existing `CompositionLocal` i18n approach (see `strings.xml` and locale switching logic)

## 6. Write or update tests
- Every new feature, bug fix or improvement should be covered by existing or new tests
- Pure logic → unit test in `src/test/` (JUnit 4)
- Composable behaviour → Compose UI test in `src/androidTest/`
- Run `./gradlew testDebugUnitTest` and fix any failures

## 6b. Mutation testing
- Run `./gradlew pitest` to validate that the new tests actually catch bugs
- The build fails if the mutation score drops below 80 % — surviving mutants must be covered by new tests
- Only unit-testable production classes are analysed (composables, DataStore I/O, and generated code are excluded — see `docs/mutation-testing.md`)
- If the score is below 80 %: open `app/build/reports/pitest/index.html`, find surviving mutants, write targeted assertions, and re-run until the gate passes

## 7. Update documentation
**This step is mandatory — never skip it.**
- Add or update files in `docs/` describing the feature
- Keep `README.md` in sync (game rules, screens, bonuses, architecture changes)
- Include the documentation changes in the same commit as the code (or a follow-up commit on the same branch before opening the PR)

## 8. Commit and open a PR
- Stage only the relevant files (never `.env` or secrets)
- Write a clear conventional commit message referencing the issue number (e.g. `feat(42): add X`)
- Run `./gradlew lint` and fix any new warnings before committing
- Push the branch and open a pull request with:
  - Title following conventional commits spec with the issue number in parentheses (e.g. `feat(42): add score history`)
  - A relevant description of the changes

## 9. Validate
Ask the user to validate the ticket.
- If they say **"go"**: merge the PR (`gh pr merge --squash`) and close the issue (`gh issue close <number>`). Then, checkout main and pull.
- Otherwise: restart from step 4 (Plan) incorporating the requested changes, then ask for validation again

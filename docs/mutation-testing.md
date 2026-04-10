# Mutation Testing

## What is mutation testing?

Mutation testing is a technique that validates how thorough our test suite really is.
The tool (PIT) automatically modifies ("mutates") the compiled bytecode — for example, it might change a `+` to a `-`, flip a `true` to `false`, or remove a conditional — and then re-runs the unit tests against each mutated version.

- **Killed mutant** — at least one test failed on the mutation → the tests caught the bug. Good.
- **Survived mutant** — all tests still passed on the mutation → the tests missed this case. This reveals a gap.

A high mutation score means the tests would catch real bugs introduced into the production code.

## Tool: PIT (Pitest)

We use [PIT](https://pitest.org) version **1.17.3**, invoked directly as a `JavaExec` Gradle task.

> **Why not the `info.solidsoft.pitest` Gradle plugin?**
> Under AGP 9.x, the plugin's `PitestPluginExtension` is registered inside the plugin's own
> `afterEvaluate` hook. This hook never fires under the current AGP / Gradle version pairing,
> leaving the extension `null` and causing the task to fail silently.
> Calling PIT's command-line entry point via `JavaExec` bypasses the plugin entirely and works
> with any Gradle/AGP version.

## Running mutation tests

```bash
# Run PIT on the debug unit-test variant
./gradlew pitest

# The HTML report opens in a browser
open app/build/reports/pitest/index.html
```

The task compiles the debug variant first (`compileDebugKotlin`), then runs PIT against the
`testDebugUnitTest` classpath.

## Quality gate

The build **fails** if the mutation score drops below **80 %**.

This threshold is intentional: it does not require 100 % (some mutants are semantically
equivalent or untestable), but it is high enough to prevent test suites that rubber-stamp
anything without actually asserting behaviour.

If you need to temporarily lower the gate while bootstrapping coverage for a new module, lower
`--mutationThreshold` in `app/build.gradle.kts`, add a comment explaining why, and raise it back
once the tests are written.

## What is analysed

Only our own production code is mutated:

- **Included**: `fr.mandarine.tarotcounter.*`
- **Excluded** (no meaningful unit-test coverage or not runnable on JVM):
  - Compose screen composables (`GameScreenKt`, `LandingScreenKt`, etc.) — only exercised by instrumented tests
  - `MainActivity` — Android lifecycle, cannot run on JVM
  - `GameStorage` — DataStore I/O, cannot run on JVM
  - Kotlin serialization-generated classes (`*$$serializer`, `*$serializer`)
  - Compose compiler-generated singletons (`ComposableSingletons*`)
  - Material theme declarations (`ui.theme.*`) — pure style constants, no logic
  - Auto-generated `R` and `BuildConfig` classes
  - Test files themselves (`*Test`, `FakeGameStorage`)

## Reading the HTML report

Open `app/build/reports/pitest/index.html` after a successful run.

- Green line numbers — covered and mutant was killed.
- Red line numbers — a mutant survived here; a new assertion is needed.
- Click a class name to see which mutations survived and on which line.

## Adding mutation tests

When PIT reports a surviving mutant:

1. Open the HTML report and find the surviving mutant's location.
2. Write a unit test in `src/test/` that specifically exercises that branch or arithmetic.
3. Re-run `./gradlew pitest` and confirm the mutant is now killed.

## Integration with the implement-issue workflow

The `/implement-issue` skill runs `./gradlew pitest` as a validation step after the regular
unit tests pass. If the mutation score drops below 80 %, the implementation is not complete —
additional tests must be written to cover the gaps before the PR can be opened.

# Release Workflow

This document describes how to publish a new version of TarotCounter to the Play Store using the `/release-store` skill.

## Quick start

```
/release-store minor
```

That single command will:
1. Bump the version (see [Versioning](#versioning) below)
2. Build a signed App Bundle (`.aab`)
3. Create a GitHub release with auto-generated release notes
4. Upload the `.aab` as a release asset
5. Print the download URL

## Prerequisites

- Signing credentials must be configured (see [`docs/release-signing.md`](release-signing.md))
- `gh` CLI must be authenticated (`gh auth status`)
- You must be on the `main` branch with a clean working tree

## Versioning

TarotCounter follows [Semantic Versioning](https://semver.org/) with two or three components:

| Release type | Command | Version change | Example |
|---|---|---|---|
| `minor` (default) | `/release-store` or `/release-store minor` | X.**Y** → X.**(Y+1)** | `1.2` → `1.3` |
| `major` | `/release-store major` | **X**.Y → **(X+1)**.0 | `1.2` → `2.0` |
| `hotfix` | `/release-store hotfix` | X.Y.**Z** → X.Y.**(Z+1)** | `1.2` → `1.2.1` |

`versionCode` (the integer Play Store uses internally) is always incremented by 1 regardless of release type.

Both values are stored in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 1       // integer, must increase on every upload
    versionName = "1.0"  // human-readable string shown in the Play Store
}
```

## What the skill does step by step

1. **Parse release type** — reads `$ARGUMENTS`, defaults to `minor`.
2. **Read current version** — extracts `versionCode` and `versionName` from `app/build.gradle.kts`.
3. **Confirm** — shows you the planned bump and waits for approval before changing anything.
4. **Patch `build.gradle.kts`** — updates both fields in place with `sed`.
5. **Build** — runs `./gradlew bundleRelease`; output is `app/build/outputs/bundle/release/app-release.aab`.
6. **Commit** — stages only `app/build.gradle.kts` and commits the version bump.
7. **GitHub release** — runs `gh release create vX.Y[.Z] --generate-notes` and uploads the `.aab` **and** the R8 mapping file (`mapping.txt`) as release assets. The mapping file is required to deobfuscate crash stack traces for that specific version — see [`docs/crash-reporting.md`](crash-reporting.md).
8. **Display URL** — retrieves and prints the asset download URL via `gh release view`.

## After the skill finishes

1. Push the version-bump commit: `git push`
2. Open [Google Play Console](https://play.google.com/console)
3. Go to **Production** (or an internal / alpha track) → **Create new release**
4. Upload the `.aab` file
5. Review and roll out

## Important reminders

- The signing keystore must be the same for every release. Losing it means you can never update the app on the Play Store.
- Never commit the keystore or `local.properties` / `gradle.properties` credentials to git.
- `versionCode` must strictly increase on every upload to the Play Store — never reuse a code.

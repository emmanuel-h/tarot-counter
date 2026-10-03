# Release Workflow

This document describes how to publish a new version of TarotCounter to the Play Store using the `/release-store` skill.

## Quick start

```
/release-store minor
```

That single command will:
1. Bump the version (see [Versioning](#versioning) below)
2. Ask which Play track to release on and whether to refresh the store screenshots
3. Optionally regenerate the screenshots on the emulator and commit them
4. Build a signed App Bundle (`.aab`)
5. Create a GitHub release with the `.aab` and the R8 mapping attached
6. Write French and English release notes (500 characters max each)
7. Upload the bundle, mapping, notes and — if asked — the screenshots to Google Play

## Prerequisites

- Signing credentials must be configured (see [`docs/release-signing.md`](release-signing.md))
- The Play service account key at `~/.config/tarotcounter/play-service-account.json` (see "Publishing to Google Play" in [`docs/store-assets.md`](store-assets.md)). The skill stops before changing anything if it is missing.
- `gh` CLI must be authenticated (`gh auth status`)
- You must be on the `main` branch with a clean working tree
- For new screenshots: an emulator (the skill boots `Medium_Phone_API_36.1` if none is running)

## Versioning

TarotCounter follows [Semantic Versioning](https://semver.org/) (`X.Y.Z`):

| Release type | Command | Version change | Example |
|---|---|---|---|
| `minor` (default) | `/release-store` or `/release-store minor` | X.**Y**.Z → X.**(Y+1)**.0 | `3.0.0` → `3.1.0` |
| `major` | `/release-store major` | **X**.Y.Z → **(X+1)**.0.0 | `3.0.0` → `4.0.0` |
| `hotfix` | `/release-store hotfix` | X.Y.**Z** → X.Y.**(Z+1)** | `3.0.0` → `3.0.1` |

`versionCode` (the integer Play Store uses internally) is always incremented by 1 regardless of release type.

Both values are stored in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 12        // integer, must increase on every upload
    versionName = "3.0.0"   // human-readable string shown in the Play Store
}
```

## What the skill does step by step

1. **Parse release type** — reads `$ARGUMENTS`, defaults to `minor`.
2. **Read current version** — extracts `versionCode` and `versionName` from `app/build.gradle.kts`.
3. **Compute the next version.**
4. **Preflight** — checks the Play service account key exists.
5. **Ask** — version (or another bump, or cancel), track (`production` or `internal`), and whether to update the screenshots.
6. **Patch `build.gradle.kts`** — updates both fields in place with `sed`.
7. **Screenshots** (if asked) — runs the `StoreScreenshots` instrumented test, pulls the 26 PNGs into `store/screenshots/`, checks every image by eye, and commits them.
8. **Build** — runs `./gradlew bundleRelease`.
9. **Commit and push** the version bump.
10. **GitHub release** — `gh release create vX.Y.Z --generate-notes`, with the `.aab` **and** `mapping.txt` attached. The mapping file deobfuscates crash stack traces for that exact version — see [`docs/crash-reporting.md`](crash-reporting.md).
11. **Release notes** — rewrites the `feat`/`fix` commits since the previous tag into user-facing French and English bullets, written to `fr-FR.txt` / `en-US.txt` and measured against Play's 500-character limit.
12. **Publish** — `tools/store/publish-play.py` uploads everything in a single Play edit. If anything fails, the edit is thrown away and nothing reaches the listing.

Google reviews every release and listing change; the version goes live once review passes.

## Important reminders

- The signing keystore must be the same for every release. Losing it means you can never update the app on the Play Store.
- Never commit the keystore, the Play service account key, or `local.properties` / `gradle.properties` credentials to git.
- `versionCode` must strictly increase on every upload to the Play Store — never reuse a code.

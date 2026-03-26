# Release Signing

Android apps must be signed with a private key before they can be published to Google Play.
This document explains how the signing configuration works and how to set it up.

## How It Works

`app/build.gradle.kts` reads four credentials at configuration time:

| Property name | What it holds |
|---|---|
| `RELEASE_KEYSTORE_FILE` | Path to the `.jks` keystore file (relative to `app/` or absolute) |
| `RELEASE_KEYSTORE_PASSWORD` | Password protecting the keystore file |
| `RELEASE_KEY_ALIAS` | Alias of the signing key inside the keystore |
| `RELEASE_KEY_PASSWORD` | Password protecting that specific key |

The build script first looks for each value in **Gradle properties**
(`findProperty("…")`), then falls back to **environment variables**
(`System.getenv("…")`).

When all four values are present a `signingConfigs.release` block is
registered and wired to the `release` build type.
When any value is missing the release artifact is produced **unsigned** —
safe for local development and debug builds.

## Local Setup (one-time)

1. **Generate a keystore** (skip if you already have one):
   ```bash
   keytool -genkeypair -v \
     -keystore release.jks \
     -alias upload \
     -keyalg RSA -keysize 2048 \
     -validity 10000
   ```
   Store the resulting `release.jks` in a safe location **outside** the
   repository (e.g. `~/keystores/tarot-counter/release.jks`).
   The `.jks` / `.keystore` extensions are in `.gitignore` as a safety net,
   but the safest practice is to never place the file inside the repo at all.

2. **Add credentials to your user-level Gradle properties** so they apply
   to every build on your machine without touching the committed
   `gradle.properties`:

   ```
   # ~/.gradle/gradle.properties
   RELEASE_KEYSTORE_FILE=/home/<you>/keystores/tarot-counter/release.jks
   RELEASE_KEY_ALIAS=upload
   RELEASE_KEYSTORE_PASSWORD=your-keystore-password
   RELEASE_KEY_PASSWORD=your-key-password
   ```

3. **Build a signed release bundle**:
   ```bash
   ./gradlew bundleRelease
   # Output: app/build/outputs/bundle/release/app-release.aab
   ```

## CI / CD Setup

Set the four values as **secret environment variables** in your pipeline
(GitHub Actions secrets, GitLab CI variables, etc.):

```
RELEASE_KEYSTORE_FILE   # path where the keystore is written on the runner
RELEASE_KEYSTORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

A typical GitHub Actions step also needs to decode the keystore from a
base64 secret and write it to disk before invoking Gradle:

```yaml
- name: Decode keystore
  run: |
    echo "${{ secrets.RELEASE_KEYSTORE_BASE64 }}" | base64 --decode \
      > $RUNNER_TEMP/release.jks
  env:
    RELEASE_KEYSTORE_FILE: ${{ runner.temp }}/release.jks

- name: Build release bundle
  run: ./gradlew bundleRelease
  env:
    RELEASE_KEYSTORE_FILE: ${{ runner.temp }}/release.jks
    RELEASE_KEYSTORE_PASSWORD: ${{ secrets.RELEASE_KEYSTORE_PASSWORD }}
    RELEASE_KEY_ALIAS: ${{ secrets.RELEASE_KEY_ALIAS }}
    RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }}
```

## R8 Minification & Resource Shrinking

Release builds have `isMinifyEnabled = true` and `isShrinkResources = true` in
`app/build.gradle.kts`. This means:

- **R8** (Google's replacement for ProGuard) rewrites and shrinks bytecode,
  removing unused classes, methods and fields, and renaming remaining symbols.
- **Resource shrinking** strips unused drawables, layouts, strings, etc. from
  the APK/AAB, reducing binary size further.

Rules that tell R8 what *not* to remove are in `app/proguard-rules.pro`:

| Rule category | Why it is needed |
|---|---|
| `SourceFile,LineNumberTable` attributes | Keeps crash stack traces readable |
| `fr.mandarine.tarotcounter.**$$serializer` | Generated serializer classes for every `@Serializable` data class / enum; R8 would otherwise strip them |
| Companion objects with `serializer()` | `Json.encodeToString` / `decodeFromString` look these up reflectively at runtime |

The Compose libraries and `kotlinx.serialization` AAR each ship their own
consumer ProGuard rules, so no extra Compose-specific rules are needed here.

## Security Notes

- **Never commit** passwords, keystore files, or any file containing real
  credentials to source control.
- The project-level `gradle.properties` (committed) contains only
  **commented-out placeholder lines** — no real values.
- Real values live in `~/.gradle/gradle.properties` locally (not committed)
  or in pipeline secrets on CI.
- `.gitignore` blocks `*.jks` and `*.keystore` as an extra safety net.

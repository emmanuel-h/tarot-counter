# App Bundle (AAB) for Google Play

Google Play requires **Android App Bundles** (`.aab`) instead of APKs for all
new app submissions. This document explains what an App Bundle is, how to build
one, and how to verify the output.

## What is an App Bundle?

An APK (Android Package) is a self-contained installable file that must include
resources for every device configuration (screen density, CPU architecture, etc.).

An AAB (Android App Bundle) is a publishing format. You upload it to Google Play,
and Play's servers generate optimised, device-specific APKs from it on the fly.
The result is a smaller download for the end user.

AGP 9.1+ (used in this project) supports `bundleRelease` out of the box — no
extra plugins or configuration are required.

## Build the App Bundle

```bash
# Requires signing credentials to be configured — see docs/release-signing.md
./gradlew bundleRelease
```

Output path:
```
app/build/outputs/bundle/release/app-release.aab
```

## Verify the Output

After the build completes, confirm the file exists:

```bash
ls -lh app/build/outputs/bundle/release/app-release.aab
```

You can also inspect the bundle with Google's
[`bundletool`](https://developer.android.com/tools/bundletool):

```bash
# Download bundletool from https://github.com/google/bundletool/releases
java -jar bundletool.jar validate --bundle=app/build/outputs/bundle/release/app-release.aab
```

## Signing

The `.aab` produced by `bundleRelease` is signed with the release key configured
in `app/build.gradle.kts`. See [`docs/release-signing.md`](release-signing.md)
for full keystore setup and CI/CD instructions.

If signing credentials are **not** configured, the bundle is produced unsigned and
cannot be uploaded to the Play Store.

## Native Debug Symbols

Even though TarotCounter contains no custom C/C++ code, Jetpack Compose ships
native libraries (e.g. `libandroidx.graphics.path.so`). Without debug symbols,
Google Play Console cannot symbolicate native crash stack traces and will display
the following warning:

> *This App Bundle contains native code, and you have not imported debug symbols.*

### Configuration

The release build type in `app/build.gradle.kts` includes:

```kotlin
ndk {
    debugSymbolLevel = "FULL"
}
```

`"FULL"` instructs AGP to package the unstripped `.so` files alongside the
stripped ones inside the App Bundle. Play Console extracts them automatically
during the upload and uses them for crash analysis.

| Value | Description |
|---|---|
| `"NONE"` | Default — triggers the Play Console warning |
| `"SYMBOL_TABLE"` | Smaller upload; only symbol tables, less detail |
| `"FULL"` | Recommended — full debug info, best crash analysis |

No additional tooling or manual upload is required; the symbols are embedded in
the `.aab` and processed by Play automatically.

## Uploading to Google Play

1. Go to the [Google Play Console](https://play.google.com/console).
2. Navigate to your app → **Production** (or an internal/alpha/beta track).
3. Create a new release and upload `app-release.aab`.
4. Complete the release checklist and submit for review.

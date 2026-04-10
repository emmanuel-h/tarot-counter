# Crash Reporting and Stack Trace Recovery

This document explains how TarotCounter crash data is collected, how crashes are
symbolicated (converted from obfuscated addresses back to readable code), and how
to analyse a crash report when something goes wrong.

## How crashes are collected

TarotCounter does not include a third-party crash SDK. Crash reports are collected
automatically by **Android Vitals**, Google's built-in telemetry system. Any app
installed from the Play Store that the user has opted into diagnostics sharing will
forward crash data — including native crashes — to the Play Console.

To view crash reports:

1. Open [Google Play Console](https://play.google.com/console)
2. Navigate to your app → **Android Vitals → Crashes & ANRs**
3. Filter by crash type, Android version, or date range

## Two types of crashes — two types of symbols

TarotCounter is a pure Kotlin app, but Jetpack Compose bundles native C++ libraries
(e.g. `libandroidx.graphics.path.so`). This means a crash report can contain two
kinds of obfuscated frames:

| Frame type | Cause | Needed to read it |
|---|---|---|
| Kotlin / Java frame | R8 renames classes and methods during minification | R8 mapping file (`mapping.txt`) |
| Native / NDK frame | Native library `.so` file is stripped of debug info | Native debug symbols (`.so` with DWARF info) |

Both symbol types are bundled in the App Bundle when you build with the project's
current configuration.

## Configuration (already in place)

### Native debug symbols

In `app/build.gradle.kts`, the release build type includes:

```kotlin
ndk {
    debugSymbolLevel = "FULL"
}
```

This instructs AGP to package the **unstripped** `.so` files (full DWARF debug
info) alongside the stripped ones inside the App Bundle. Play Console extracts
them automatically on upload and uses them to symbolicate native stack frames.

`"FULL"` provides the most detailed symbolication. The larger size has no impact
on end-user download size — Play only delivers the stripped `.so` files to devices.

### R8 mapping file

When `isMinifyEnabled = true`, R8 generates a mapping file at:

```
app/build/outputs/mapping/release/mapping.txt
```

This file maps every obfuscated name (`a.b.c`) back to its original
(`fr.mandarine.tarotcounter.GameModels`). It is automatically bundled inside the
App Bundle, so Play Console can deobfuscate Kotlin/Java crash frames without any
manual upload.

### Stack trace attributes (ProGuard rules)

`app/proguard-rules.pro` contains:

```
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```

These rules preserve the original file names and line numbers inside the binary so
that crash tools (including Play Console and Android Studio's Logcat) can show
the exact source line where a crash occurred, even after minification.

## Mapping file archival

The mapping file is **overwritten on every build**. This means that after you
publish version 1.3.0 and then build 1.4.0, you can no longer reconstruct
version 1.3.0's mapping file from your local disk.

The `/release-store` skill archives a copy of the mapping file as a GitHub
release asset (`mapping.txt`) alongside the `.aab`. This ensures you can always
retrieve the correct mapping for any previously published version from the
GitHub releases page.

## Manual stack trace deobfuscation

If you receive an obfuscated crash report (e.g. from a beta tester), you can
deobfuscate it locally using the `retrace` tool bundled with the Android SDK
command-line tools:

```bash
# Download the mapping file for the affected release from GitHub Releases,
# then run:
$ANDROID_HOME/cmdline-tools/latest/bin/retrace \
  mapping.txt \
  obfuscated_trace.txt
```

`retrace` reads each obfuscated class and method name from the stack trace and
replaces it with the original name using the mapping file.

For **native** crash frames, Android Studio's **LLDB** debugger and the
`ndk-stack` tool can process the DWARF symbols:

```bash
# Replace <abi> with the device ABI, e.g. arm64-v8a
$ANDROID_HOME/ndk/<version>/ndk-stack \
  -sym app/build/outputs/native-debug-symbols/release/out/<abi>/ \
  -dump native_trace.txt
```

## Automatic deobfuscation in Android Studio

Starting with **Android Studio Otter 3 Feature Drop** and **AGP 9.0**, Logcat
automatically deobfuscates R8 stack traces when the mapping file is present in
`app/build/outputs/mapping/`. This means debug and local release builds are
deobfuscated transparently in the IDE without any manual steps.

## Summary of what each build produces

| Build command | Symbols | Mapping file | Use case |
|---|---|---|---|
| `./gradlew assembleDebug` | Full debug symbols | Not generated (no R8) | Local development |
| `./gradlew assembleRelease` | Stripped `.so` + mapping | `build/outputs/mapping/release/` | Local release testing |
| `./gradlew bundleRelease` | Stripped + unstripped `.so` | Bundled in `.aab` + `build/outputs/mapping/release/` | Play Store upload |

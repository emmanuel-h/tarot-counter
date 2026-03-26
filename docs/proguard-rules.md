# ProGuard / R8 Rules

This document explains every rule in `app/proguard-rules.pro` and why it is
needed for a correct release build.

## Background

Release builds run through **R8**, Google's code shrinker (the successor to
ProGuard).  R8 removes unused classes, methods, and fields, and renames the
remaining ones to single-letter symbols.  This reduces APK/AAB size but can
break code that relies on reflection or on predictable class/method names.

Keep-rules tell R8 what it must *not* remove or rename.

## Rule-by-rule breakdown

### Stack traces

```
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```

Without these, minified stack traces show `SourceFile` and meaningless line
numbers (e.g. `SourceFile:1`), making crash reports unreadable.  These two
lines preserve the original file names and line numbers in the binary so that
tools like Firebase Crashlytics can de-obfuscate traces automatically.

### kotlinx.serialization — annotations

```
-keepattributes *Annotation*, InnerClasses
```

`@Serializable`, `@SerialName`, and other serialization annotations must
survive shrinking.  The serialization runtime reads these annotations
reflectively at startup to verify the generated code; if they are stripped,
deserialization throws an exception.

### kotlinx.serialization — generated serializer classes

```
-keep,includedescriptorclasses class fr.mandarine.tarotcounter.**$$serializer { *; }
```

For every class annotated with `@Serializable` (e.g. `SavedGame`,
`InProgressGame`, `RoundResult`, `RoundDetails`, `Contract`, `Chelem`), the
Kotlin compiler generates a companion `$$serializer` class at compile time.
R8 cannot see that these classes are used (they are referenced only through
reflection), so without this rule it strips them — causing a
`SerializationException` the first time the app tries to read or write JSON.

`includedescriptorclasses` also preserves the method and field type
descriptors inside those classes, which the serialization runtime needs to
match fields by name.

### kotlinx.serialization — companion serializer() method

```
-keepclassmembers class fr.mandarine.tarotcounter.** {
    *** Companion;
}
-keepclasseswithmembers class fr.mandarine.tarotcounter.** {
    kotlinx.serialization.KSerializer serializer(...);
}
```

`Json.encodeToString<T>()` and `Json.decodeFromString<T>()` look up the
serializer for `T` at runtime via `T.Companion.serializer()`.  If R8 renames
or removes the `Companion` object or the `serializer()` method, the call fails
with a `NoSuchMethodException`.  These two rules keep both the companion object
reference and any method whose signature returns a `KSerializer`.

## What is NOT in this file (and why)

| Library | Where its rules live |
|---|---|
| Jetpack Compose | Bundled inside each Compose AAR (`consumer-rules.pro`); AGP merges them automatically |
| `kotlinx.serialization` core | Bundled inside the serialization AAR; the rules above are *additions* for app-level classes |
| AndroidX DataStore | Bundled inside the DataStore AAR |
| Material 3 | Bundled inside the Material AAR |

AGP merges consumer rules from all AARs before passing the combined rule set
to R8, so there is no need to duplicate library-owned rules here.

## Verifying the rules

```bash
# Build a release AAB and confirm R8 succeeds
./gradlew bundleRelease

# Mapping file (used to de-obfuscate crash reports)
# app/build/outputs/mapping/release/mapping.txt
```

The mapping file is produced automatically alongside the AAB.  Upload it to
the Google Play Console (or Firebase Crashlytics) so that minified stack traces
are expanded back to readable class and method names.

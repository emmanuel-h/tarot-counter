# ── Stack traces ─────────────────────────────────────────────────────────────
# Preserve file names and line numbers so crash reports remain readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── kotlinx.serialization ────────────────────────────────────────────────────
# The library ships its own consumer rules (via its AAR), but the rules below
# are the recommended explicit additions for app code.

# Keep all annotations (required for @Serializable, @SerialName, etc. to work).
-keepattributes *Annotation*, InnerClasses

# Keep the generated $$serializer classes that R8 would otherwise strip.
# These classes are created at compile time for every @Serializable class.
-keep,includedescriptorclasses class fr.mandarine.tarotcounter.**$$serializer { *; }

# Keep companion objects that expose a serializer() method (used by Json.encode/decode).
-keepclassmembers class fr.mandarine.tarotcounter.** {
    *** Companion;
}
-keepclasseswithmembers class fr.mandarine.tarotcounter.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Jetpack Compose ───────────────────────────────────────────────────────────
# Compose libraries bundle their own consumer ProGuard rules inside their AARs,
# so no extra rules are required here.  The proguard-android-optimize.txt
# baseline (referenced in build.gradle.kts) covers the Android framework side.

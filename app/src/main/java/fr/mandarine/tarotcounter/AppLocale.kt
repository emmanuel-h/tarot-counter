package fr.mandarine.tarotcounter

import androidx.compose.runtime.staticCompositionLocalOf

// ── Locale enum ───────────────────────────────────────────────────────────────
//
// EN = English, FR = French.
// More locales can be added here in the future without changing any other code.
enum class AppLocale { EN, FR }

// ── CompositionLocal ──────────────────────────────────────────────────────────
//
// CompositionLocal lets any composable in the tree read the current locale without
// threading it through every function parameter.
//
// Usage:  val locale = LocalAppLocale.current
//         val strings = appStrings(locale)
//
// `staticCompositionLocalOf` is used instead of `compositionLocalOf` because locale
// changes always recompose the entire tree (via CompositionLocalProvider in MainActivity),
// so the extra per-caller tracking of `compositionLocalOf` would add cost with no benefit.
// The default value is EN so that Previews and tests that don't set a provider
// still compile and render correctly.
val LocalAppLocale = staticCompositionLocalOf { AppLocale.EN }

// The java.util.Locale matching each app language — used to format dates
// ("Sat 13 Sep" / "sam. 13 sept.") in the app's language rather than the device's.
val AppLocale.javaLocale: java.util.Locale
    get() = when (this) {
        AppLocale.EN -> java.util.Locale.ENGLISH
        AppLocale.FR -> java.util.Locale.FRENCH
    }

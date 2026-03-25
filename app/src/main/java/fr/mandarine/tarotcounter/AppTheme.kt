package fr.mandarine.tarotcounter

import androidx.compose.runtime.staticCompositionLocalOf

// ── Theme enum ─────────────────────────────────────────────────────────────────
//
// LIGHT = light mode (parchment background, dark text).
// DARK  = dark mode  (felt-green background, light text).
//
// This mirrors how AppLocale.kt defines EN / FR — a simple enum that the rest
// of the app can switch on without depending on Android's system theme APIs.
enum class AppTheme { LIGHT, DARK }

// ── CompositionLocal ──────────────────────────────────────────────────────────
//
// LocalAppTheme lets any composable in the tree read the current theme without
// threading it through every function parameter.
//
// Usage:  val theme = LocalAppTheme.current
//
// `staticCompositionLocalOf` is used (same reason as LocalAppLocale): theme
// changes always recompose the entire tree, so per-caller tracking would add
// cost with no benefit. The default is LIGHT so Previews and tests that don't
// provide a value still compile and render correctly.
val LocalAppTheme = staticCompositionLocalOf { AppTheme.LIGHT }

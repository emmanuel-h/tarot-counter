package fr.mandarine.tarotcounter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// ── Light colour scheme — "Salon" ────────────────────────────────────────────
// Ivory ground, paper cards, felt-green actions, ink text.
// Every slot is filled on purpose: any slot left out falls back to Material's
// baseline lavender, which would leak into components like dialogs and menus.
val LightColorScheme = lightColorScheme(
    primary              = SalonFelt,          // primary buttons, selected states
    onPrimary            = SalonPaperWhite,
    primaryContainer     = SalonFeltTint,      // selected chip, success banner
    onPrimaryContainer   = SalonFeltTintInk,
    inversePrimary       = NightSage,

    secondary            = SalonBrassText,     // brass accent (used sparingly)
    onSecondary          = SalonPaperWhite,
    secondaryContainer   = SalonBrassTint,     // dealer chip
    onSecondaryContainer = SalonBrassTintInk,

    tertiary             = SalonPlayerTones[2], // slate blue — rarely used
    onTertiary           = SalonPaperWhite,
    tertiaryContainer    = SalonTrack,
    onTertiaryContainer  = SalonInk,

    error                = SalonNegative,      // destructive actions
    onError              = SalonPaperWhite,
    errorContainer       = SalonNegativeTint,
    onErrorContainer     = SalonNegativeTintInk,

    background           = SalonIvory,
    onBackground         = SalonInk,
    surface              = SalonPaper,         // cards, sheets
    onSurface            = SalonInk,
    surfaceVariant       = SalonTrack,         // segmented-control track
    onSurfaceVariant     = SalonInkMuted,
    surfaceTint          = SalonFelt,
    inverseSurface       = NightFeltCard,
    inverseOnSurface     = NightCream,

    // Material components pick their background from these container levels
    // (e.g. Card → surfaceContainerHighest, dialogs → surfaceContainerHigh).
    surfaceContainerLowest  = SalonPaperWhite,
    surfaceContainerLow     = SalonPaper,
    surfaceContainer        = SalonCream,
    surfaceContainerHigh    = SalonLinen,
    surfaceContainerHighest = SalonTrack,
    surfaceBright           = SalonPaper,
    surfaceDim              = SalonHairline,

    outline              = SalonHairline,      // card borders
    outlineVariant       = SalonHairlineSoft,  // row separators
    scrim                = SalonInk,
)

// ── Dark colour scheme — "Salon at night" ────────────────────────────────────
// Deep felt ground, raised felt cards, cream text, sage actions.
val DarkColorScheme = darkColorScheme(
    primary              = NightSage,
    onPrimary            = NightOnSage,
    primaryContainer     = SalonFelt,          // the felt keeps its deep green at night
    onPrimaryContainer   = NightFeltTintInk,
    inversePrimary       = SalonFelt,

    secondary            = NightBrass,
    onSecondary          = NightFeltGround,
    secondaryContainer   = NightBrassTint,
    onSecondaryContainer = NightBrassTintInk,

    tertiary             = NightPlayerTones[2],
    onTertiary           = NightFeltGround,
    tertiaryContainer    = NightTrack,
    onTertiaryContainer  = NightCream,

    error                = NightNegative,
    onError              = NightOnNegative,
    errorContainer       = NightNegativeTint,
    onErrorContainer     = NightNegativeTintInk,

    background           = NightFeltGround,
    onBackground         = NightCream,
    surface              = NightFeltCard,
    onSurface            = NightCream,
    surfaceVariant       = NightTrack,
    onSurfaceVariant     = NightCreamMuted,
    surfaceTint          = NightSage,
    inverseSurface       = SalonPaper,
    inverseOnSurface     = SalonInk,

    surfaceContainerLowest  = NightFeltLowest,
    surfaceContainerLow     = NightFeltLow,
    surfaceContainer        = NightFeltCard,
    surfaceContainerHigh    = NightFeltHigh,
    surfaceContainerHighest = NightTrack,
    surfaceBright           = NightTrack,
    surfaceDim              = NightFeltGround,

    outline              = NightHairline,
    outlineVariant       = NightHairlineSoft,
    scrim                = NightFeltLowest,
)

// ── Theme entry-point ─────────────────────────────────────────────────────────
// dynamicColor is intentionally false: we always apply the Salon palette
// regardless of Android version. On Android 12+ the OS would otherwise replace
// every colour with the user's wallpaper tones, making the custom theme useless.
@Composable
fun TarotCounterTheme(
    // Default is false (light mode) — the user's system setting does not drive this.
    // Pass `darkTheme = true` when the user has chosen dark mode via the theme toggle.
    darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER")
    dynamicColor: Boolean = false, // kept for API compatibility; always the Salon palette
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val tarotColors = if (darkTheme) DarkTarotColors else LightTarotColors

    // CompositionLocalProvider makes `tarotColors` available to every composable
    // below it through `MaterialTheme.tarotColors` (see TarotColors.kt).
    CompositionLocalProvider(LocalTarotColors provides tarotColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            shapes      = Shapes,
            content     = content
        )
    }
}

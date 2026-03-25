package fr.mandarine.tarotcounter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Dark colour scheme — felt table at night ─────────────────────────────────
// Primary surfaces are very dark green; accents are sage, gold, and rose.
private val DarkColorScheme = darkColorScheme(
    primary          = GreenLight,      // sage green — buttons, active chips
    onPrimary        = FeltDark,        // text/icons on top of primary
    primaryContainer = Color(0xFF1E3A20), // slightly lighter felt for containers
    onPrimaryContainer = GreenLight,

    secondary        = GoldLight,       // warm gold — secondary actions
    onSecondary      = FeltDark,
    secondaryContainer = Color(0xFF3E2E00),
    onSecondaryContainer = GoldLight,

    tertiary         = BurgundyLight,   // soft rose — highlights / warnings
    onTertiary       = FeltDark,

    background       = FeltDark,        // full-page background
    onBackground     = Color(0xFFE8F5E9), // off-white text on dark felt
    surface          = Color(0xFF122614), // card/sheet surfaces — slightly lighter than background
    onSurface        = Color(0xFFE8F5E9),
    surfaceVariant   = Color(0xFF1A3320),
    onSurfaceVariant = Color(0xFFB0C4B1),
)

// ── Light colour scheme — parchment in daylight ──────────────────────────────
// Primary surfaces are warm cream; accents are deep green, amber, and burgundy.
private val LightColorScheme = lightColorScheme(
    primary          = GreenDark,       // deep forest green — buttons, active chips
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8E6C9), // light green tint for containers
    onPrimaryContainer = Color(0xFF1B5E20),

    secondary        = GoldDark,        // rich amber — secondary actions
    onSecondary      = Color(0xFF1C1600),
    secondaryContainer = Color(0xFFFFECB3),
    onSecondaryContainer = Color(0xFF5C3A00),

    tertiary         = BurgundyDark,    // deep burgundy — highlights
    onTertiary       = Color(0xFFFFFFFF),

    background       = ParchmentLight,  // warm parchment background
    onBackground     = Color(0xFF1A1C19),
    surface          = Color(0xFFFAF5ED), // slightly warmer card/sheet surfaces
    onSurface        = Color(0xFF1A1C19),
    surfaceVariant   = Color(0xFFEDE8D8),
    onSurfaceVariant = Color(0xFF4A4A3A),
)

// ── Theme entry-point ─────────────────────────────────────────────────────────
// dynamicColor is intentionally false: we always apply our card-game palette
// regardless of Android version. On Android 12+ the OS would otherwise replace
// every colour with the user's wallpaper tones, making the custom theme useless.
@Composable
fun TarotCounterTheme(
    // Default is false (light mode) — the user's system setting no longer drives this.
    // Pass `darkTheme = true` when the user has chosen dark mode via the theme toggle.
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false, // disabled — use our card-game palette consistently
    content: @Composable () -> Unit
) {
    // Pick the right scheme. Dynamic color is kept as a parameter so callers
    // (e.g. tests) can still opt in, but the default is always false.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}

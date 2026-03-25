package fr.mandarine.tarotcounter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fr.mandarine.tarotcounter.R

// ── Cinzel font family ────────────────────────────────────────────────────────
// Cinzel is a classical Roman-capitals serif font from Google Fonts.
// It evokes the look of engraved lettering on antique playing cards, giving the
// app a sophisticated card-game feel without sacrificing legibility.
//
// The file in res/font/ is a *variable* font: one TTF that contains the full
// weight range (Regular 400 → Black 900).  We declare two named weights so
// Compose can select the right axis value when rendering each style.
// Using the same resource file twice with different `weight` values is the
// standard Compose pattern for variable fonts — Compose forwards the weight
// to the font engine automatically.
val CinzelFontFamily = FontFamily(
    // Regular weight (400) — used for headlineLarge / headlineMedium text
    Font(resId = R.font.cinzel_regular, weight = FontWeight.Normal),
    // Bold weight (700) — used when callers do .copy(fontWeight = FontWeight.Bold)
    Font(resId = R.font.cinzel_regular, weight = FontWeight.Bold)
)

// ── Typography ────────────────────────────────────────────────────────────────
// Material 3 defines a layered type scale.  We override only the *heading*
// styles with Cinzel and leave every body / label style on the system font
// (FontFamily.Default) for maximum readability at smaller sizes.
//
// Heading styles affected (as requested in issue #2):
//   headlineLarge   — app title on LandingScreen
//   headlineMedium  — "Game Over" on FinalScoreScreen
//   headlineSmall   — winner name inside the winner card
//   titleLarge      — round header on GameScreen
//
// Body / label styles are intentionally NOT changed; they remain on the system
// sans-serif font so they stay crisp and easy to read in tight spaces.
val Typography = Typography(
    // ── Heading styles (Cinzel) ───────────────────────────────────────────────
    headlineLarge = TextStyle(
        fontFamily = CinzelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize    = 32.sp,
        lineHeight  = 40.sp,
        letterSpacing = 0.sp   // Cinzel's built-in spacing is already generous
    ),
    headlineMedium = TextStyle(
        fontFamily = CinzelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize    = 28.sp,
        lineHeight  = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = CinzelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize    = 24.sp,
        lineHeight  = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = CinzelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize    = 22.sp,
        lineHeight  = 28.sp,
        letterSpacing = 0.sp
    ),

    // ── Body style (system font) — kept here for documentation clarity ────────
    // Material 3 defaults already use FontFamily.Default for body/label styles;
    // we list bodyLarge explicitly to make the intent clear in code.
    bodyLarge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.5.sp
    )
    // bodyMedium, bodySmall, labelMedium, etc. are NOT listed here,
    // which means Material3 will fall back to its own defaults (system font).
)

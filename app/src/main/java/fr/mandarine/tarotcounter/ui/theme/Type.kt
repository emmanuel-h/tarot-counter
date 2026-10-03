package fr.mandarine.tarotcounter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.mandarine.tarotcounter.R

// ── Font families ────────────────────────────────────────────────────────────
// Both fonts are bundled as *static* TTF files in res/font/ (one file per weight)
// rather than variable fonts: Android 7.x (API 24–25, our minSdk) ignores the
// weight axis of variable fonts, so static files are the only way to get the
// right weight on every device. Both families are licensed under the SIL OFL.

/**
 * Cormorant Garamond — an elegant Garamond display serif.
 * Used for the "salon" voice: screen titles, player names, big numbers.
 */
val CormorantGaramond = FontFamily(
    Font(resId = R.font.cormorant_garamond_semibold, weight = FontWeight.SemiBold), // 600
    Font(resId = R.font.cormorant_garamond_bold,     weight = FontWeight.Bold),     // 700
)

/**
 * Figtree — a clean geometric sans-serif, very legible at small sizes.
 * Used for all UI text: body, labels, buttons.
 */
val Figtree = FontFamily(
    Font(resId = R.font.figtree_regular,  weight = FontWeight.Normal),   // 400
    Font(resId = R.font.figtree_medium,   weight = FontWeight.Medium),   // 500
    Font(resId = R.font.figtree_semibold, weight = FontWeight.SemiBold), // 600
)

// ── Tabular figures ──────────────────────────────────────────────────────────
// By default, digits in most fonts are "proportional": a 1 is narrower than an 8,
// so columns of scores wobble. The OpenType feature "tnum" (tabular numbers)
// makes every digit the same width so numbers line up in columns.
// We turn it on for *every* style below, so any score, in any style, aligns.
//
// "lnum" (lining numbers) is added too: Cormorant Garamond draws *old-style*
// figures by default, where "1" looks like a small-caps "I" and 3, 4, 5, 7, 9
// dip below the line — charming in prose, confusing in "Round 1" or a score.
private const val TABULAR_FIGURES = "tnum, lnum"

/**
 * Builds one text style of the scale. A small helper so every style shares the
 * same tabular-figure setting and the table below stays readable.
 */
private fun salonStyle(
    family: FontFamily,
    weight: FontWeight,
    size: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp,
) = TextStyle(
    fontFamily          = family,
    fontWeight          = weight,
    fontSize            = size,
    lineHeight          = lineHeight,
    letterSpacing       = letterSpacing,
    fontFeatureSettings = TABULAR_FIGURES,
)

// ── Typography ───────────────────────────────────────────────────────────────
// Material 3 defines 15 named styles. Every one is set here so no text falls
// back to the system font.
//
//   display*  (Cormorant)  big numbers: points entered, winner name, leader score
//   headline* (Cormorant)  app title, screen titles, section titles
//   titleLarge (Cormorant) smaller section titles ("Past games")
//   titleMedium/Small (Figtree) list-item titles, player names in rows
//   body*     (Figtree)    running text
//   label*    (Figtree)    buttons, field labels, overlines ("GAME IN PROGRESS")
val Typography = Typography(
    displayLarge   = salonStyle(CormorantGaramond, FontWeight.SemiBold, 56.sp, 60.sp),
    displayMedium  = salonStyle(CormorantGaramond, FontWeight.Bold,     40.sp, 44.sp),
    displaySmall   = salonStyle(CormorantGaramond, FontWeight.Bold,     32.sp, 36.sp),

    headlineLarge  = salonStyle(CormorantGaramond, FontWeight.Bold,     28.sp, 34.sp, 0.04.em),
    headlineMedium = salonStyle(CormorantGaramond, FontWeight.SemiBold, 26.sp, 32.sp, 0.02.em),
    headlineSmall  = salonStyle(CormorantGaramond, FontWeight.SemiBold, 24.sp, 30.sp),

    titleLarge     = salonStyle(CormorantGaramond, FontWeight.SemiBold, 22.sp, 28.sp),
    titleMedium    = salonStyle(Figtree,           FontWeight.SemiBold, 16.sp, 24.sp),
    titleSmall     = salonStyle(Figtree,           FontWeight.SemiBold, 14.sp, 20.sp),

    bodyLarge      = salonStyle(Figtree,           FontWeight.Normal,   16.sp, 24.sp),
    bodyMedium     = salonStyle(Figtree,           FontWeight.Normal,   14.sp, 20.sp),
    bodySmall      = salonStyle(Figtree,           FontWeight.Normal,   13.sp, 18.sp),

    labelLarge     = salonStyle(Figtree,           FontWeight.SemiBold, 15.sp, 20.sp, 0.02.em),
    labelMedium    = salonStyle(Figtree,           FontWeight.SemiBold, 13.sp, 18.sp, 0.04.em),
    // Used in upper case for small overlines, hence the wide letter spacing.
    labelSmall     = salonStyle(Figtree,           FontWeight.SemiBold, 12.sp, 16.sp, 0.14.em),
)

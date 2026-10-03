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

// ── Font family ──────────────────────────────────────────────────────────────
// The whole app uses a single family, Figtree, so every screen reads the same.
// Hierarchy comes from size and weight only (Regular → Bold), never from a
// second typeface. The weights are bundled as *static* TTF files in res/font/
// (one file per weight) rather than a variable font: Android 7.x (API 24–25,
// our minSdk) ignores the weight axis of variable fonts, so static files are
// the only way to get the right weight on every device. Figtree is licensed
// under the SIL OFL.

/**
 * Figtree — a clean geometric sans-serif, very legible at small sizes.
 * Used for every text style: titles, big numbers, body, labels, buttons.
 */
val Figtree = FontFamily(
    Font(resId = R.font.figtree_regular,  weight = FontWeight.Normal),   // 400
    Font(resId = R.font.figtree_medium,   weight = FontWeight.Medium),   // 500
    Font(resId = R.font.figtree_semibold, weight = FontWeight.SemiBold), // 600
    Font(resId = R.font.figtree_bold,     weight = FontWeight.Bold),     // 700
)

// ── Tabular figures ──────────────────────────────────────────────────────────
// By default, digits in most fonts are "proportional": a 1 is narrower than an 8,
// so columns of scores wobble. The OpenType feature "tnum" (tabular numbers)
// makes every digit the same width so numbers line up in columns.
// We turn it on for *every* style below, so any score, in any style, aligns.
//
// "lnum" (lining numbers) is added too, so digits always sit on the baseline
// (some fonts default to *old-style* figures that dip below the line).
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
// Material 3 defines 15 named styles. Every one is set here, all in Figtree, so
// no text falls back to the system font and no screen mixes typefaces.
//
//   display*   big numbers: points entered, winner name, leader score
//   headline*  app title, screen titles, section titles
//   title*     smaller section titles, list-item titles, player names in rows
//   body*      running text
//   label*     buttons, field labels, overlines ("GAME IN PROGRESS")
val Typography = Typography(
    displayLarge   = salonStyle(Figtree, FontWeight.SemiBold, 56.sp, 60.sp),
    displayMedium  = salonStyle(Figtree, FontWeight.Bold,     40.sp, 44.sp),
    displaySmall   = salonStyle(Figtree, FontWeight.Bold,     32.sp, 36.sp),

    headlineLarge  = salonStyle(Figtree, FontWeight.Bold,     28.sp, 34.sp),
    headlineMedium = salonStyle(Figtree, FontWeight.SemiBold, 26.sp, 32.sp),
    headlineSmall  = salonStyle(Figtree, FontWeight.SemiBold, 24.sp, 30.sp),

    titleLarge     = salonStyle(Figtree, FontWeight.SemiBold, 22.sp, 28.sp),
    titleMedium    = salonStyle(Figtree, FontWeight.SemiBold, 16.sp, 24.sp),
    titleSmall     = salonStyle(Figtree, FontWeight.SemiBold, 14.sp, 20.sp),

    bodyLarge      = salonStyle(Figtree, FontWeight.Normal,   16.sp, 24.sp),
    bodyMedium     = salonStyle(Figtree, FontWeight.Normal,   14.sp, 20.sp),
    bodySmall      = salonStyle(Figtree, FontWeight.Normal,   13.sp, 18.sp),

    labelLarge     = salonStyle(Figtree, FontWeight.SemiBold, 15.sp, 20.sp, 0.02.em),
    labelMedium    = salonStyle(Figtree, FontWeight.SemiBold, 13.sp, 18.sp, 0.04.em),
    // Used in upper case for small overlines, hence the wide letter spacing.
    labelSmall     = salonStyle(Figtree, FontWeight.SemiBold, 12.sp, 16.sp, 0.14.em),
)

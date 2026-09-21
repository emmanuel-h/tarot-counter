package fr.mandarine.tarotcounter.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

// ─────────────────────────────────────────────────────────────────────────────
// WCAG 2.x contrast helpers.
//
// Used by SalonPaletteTest to prove that every text/background pair of both
// themes is readable (≥ 4.5:1, the WCAG AA level for normal text).
// Formulas: https://www.w3.org/TR/WCAG21/#dfn-relative-luminance
// ─────────────────────────────────────────────────────────────────────────────

/** Minimum contrast ratio for normal-size text (WCAG AA). */
const val MIN_TEXT_CONTRAST = 4.5

/**
 * Converts one sRGB channel (0.0–1.0) to linear light.
 *
 * Screens store colours "gamma encoded": the stored value is not proportional to
 * the light emitted. This undoes that encoding. Very dark values use a straight
 * line; the rest follow a 2.4 power curve.
 */
private fun linearize(channel: Float): Double =
    if (channel <= 0.04045f) channel / 12.92
    else ((channel + 0.055) / 1.055).pow(2.4)

/**
 * Relative luminance of [color]: 0.0 for black, 1.0 for white.
 *
 * Green weighs the most because the human eye is most sensitive to it.
 * Alpha is ignored — callers compare opaque colours.
 */
fun relativeLuminance(color: Color): Double =
    0.2126 * linearize(color.red) +
    0.7152 * linearize(color.green) +
    0.0722 * linearize(color.blue)

/**
 * WCAG contrast ratio between two colours, from 1.0 (identical) to 21.0
 * (black on white). The order of the arguments does not matter.
 */
fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    // The formula always divides the lighter luminance by the darker one;
    // the +0.05 models ambient light reflecting off the screen.
    val lighter = maxOf(la, lb)
    val darker  = minOf(la, lb)
    return (lighter + 0.05) / (darker + 0.05)
}

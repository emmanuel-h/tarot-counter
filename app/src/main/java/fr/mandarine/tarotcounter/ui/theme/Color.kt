package fr.mandarine.tarotcounter.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// "Salon" design tokens — raw colour values (issue #195).
//
// The app looks like a card table in a salon: ivory paper cards on an ivory
// ground, felt green for actions, and a touch of brass for ornaments.
//
// These are *raw* values. Screens should never use them directly; they read the
// semantic roles instead:
//   - MaterialTheme.colorScheme.*  (primary, surface, onSurface, …) — see Theme.kt
//   - MaterialTheme.tarotColors.*  (brass, positive, negative, player tones) — see TarotColors.kt
//
// Every text/background pair below reaches the WCAG AA 4.5:1 contrast ratio;
// SalonPaletteTest (src/test) checks that automatically.
// ─────────────────────────────────────────────────────────────────────────────

// ── Light: "Salon" ────────────────────────────────────────────────────────────
val SalonIvory          = Color(0xFFF6F1E7) // page background
val SalonPaper          = Color(0xFFFFFDF8) // cards
val SalonPaperWhite     = Color(0xFFFFFFFF) // text fields, lowest container
val SalonCream          = Color(0xFFFBF7EE) // default container (menus)
val SalonLinen          = Color(0xFFF5EFE3) // high container
val SalonTrack          = Color(0xFFF1EADB) // segmented-control track, chip ground
val SalonHairline       = Color(0xFFE4DCCB) // card borders, dividers
val SalonHairlineSoft   = Color(0xFFEFE8DA) // row separators inside cards
val SalonInk            = Color(0xFF1E2420) // main text
val SalonInkMuted       = Color(0xFF5E5A50) // secondary text
val SalonFelt           = Color(0xFF1F4D3A) // felt green: primary buttons, selected states
val SalonFeltTint       = Color(0xFFE3ECE6) // light felt tint: selected chip, success banner
val SalonFeltTintInk    = Color(0xFF1D4A33) // text on the felt tint
val SalonBrass          = Color(0xFFB08A3E) // brass ornaments (non-text: rings, suit marks)
// The issue proposed #8A6A28 for brass text, but it only reaches 4.47:1 on the
// ivory ground. #826325 is a hair darker and passes 4.5:1 on every surface.
val SalonBrassText      = Color(0xFF826325)
val SalonBrassOnFelt    = Color(0xFFE2C98E) // brass text on a felt-green card
val SalonBrassTint      = Color(0xFFEFE6D2) // dealer chip ground
val SalonBrassTintInk   = Color(0xFF5A4A24) // text on the brass tint
val SalonWinnerTint     = Color(0xFFF3E7C9) // winner column highlight
val SalonPositive       = Color(0xFF2E6B45) // positive scores
val SalonNegative       = Color(0xFF9B2C2C) // negative scores, destructive actions
val SalonNegativeTint   = Color(0xFFF6DEDA) // destructive container
val SalonNegativeTintInk = Color(0xFF6E1B1B) // text on the destructive container

// ── Dark: "Salon at night" ────────────────────────────────────────────────────
// Same hues as the light palette: a deep felt ground, raised felt cards, cream
// text, and lighter brass / positive / negative so they stay readable.
val NightFeltGround     = Color(0xFF101A15) // page background
val NightFeltCard       = Color(0xFF18241E) // cards
val NightFeltLowest     = Color(0xFF0C1410) // lowest container
val NightFeltLow        = Color(0xFF141F1A) // low container
val NightFeltHigh       = Color(0xFF1D2B24) // high container
val NightTrack          = Color(0xFF22302A) // segmented-control track
val NightHairline       = Color(0xFF34443B) // card borders, dividers
val NightHairlineSoft   = Color(0xFF28362F) // row separators inside cards
val NightCream          = Color(0xFFEDE6D6) // main text
val NightCreamMuted     = Color(0xFFB3AC9C) // secondary text
val NightSage           = Color(0xFF8FC7A6) // primary on dark: buttons, selected states
val NightOnSage         = Color(0xFF0C2418) // text on sage buttons
val NightFeltTintInk    = Color(0xFFD7EADF) // text on the felt container
val NightBrass          = Color(0xFFD8B56A) // brass ornaments and brass text
val NightBrassTint      = Color(0xFF3A3222) // dealer chip ground
val NightBrassTintInk   = Color(0xFFEAD9B0) // text on the brass tint
val NightWinnerTint     = Color(0xFF2F2A1C) // winner column highlight
val NightPositive       = Color(0xFF7FCB98) // positive scores
val NightNegative       = Color(0xFFF2A195) // negative scores, destructive actions
val NightOnNegative     = Color(0xFF3A0E0E) // text on a destructive button
val NightNegativeTint   = Color(0xFF4A1E1C) // destructive container
val NightNegativeTintInk = Color(0xFFFAD6D0) // text on the destructive container

// ── Player tones ─────────────────────────────────────────────────────────────
// One stable colour per seat (index 0..4), used by avatars and charts so a
// player keeps the same colour everywhere. Light tones carry white initials;
// the lighter dark-theme tones carry dark ink initials.
val SalonPlayerTones = listOf(
    Color(0xFF3E6B5A), // seat 1 — green
    Color(0xFF8A5A3C), // seat 2 — tobacco
    Color(0xFF4A5A8A), // seat 3 — slate blue
    Color(0xFF7A4A6A), // seat 4 — plum
    Color(0xFF6B6A2E), // seat 5 — olive (5-player games)
)
val NightPlayerTones = listOf(
    Color(0xFF8DBBA8), // seat 1 — green
    Color(0xFFD4A583), // seat 2 — tobacco
    Color(0xFFA3B1DE), // seat 3 — slate blue
    Color(0xFFCFA0C0), // seat 4 — plum
    Color(0xFFC8C77E), // seat 5 — olive
)

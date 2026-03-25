package fr.mandarine.tarotcounter.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary: deep forest green ───────────────────────────────────────────────
// Light theme uses a dark green that reads well on white backgrounds.
// Dark theme uses a muted sage green that is legible on dark surfaces.
val GreenDark   = Color(0xFF1B5E20) // deep forest green  — primary (light)
val GreenLight  = Color(0xFFA5D6A7) // soft sage green    — primary (dark)

// ── Secondary: warm gold / amber ─────────────────────────────────────────────
// Evokes the gold trim on classic playing cards.
val GoldDark    = Color(0xFFF9A825) // rich amber         — secondary (light)
val GoldLight   = Color(0xFFFFD54F) // warm gold          — secondary (dark)

// Muted dark amber used as the winner-column background in dark mode.
// GoldLight (0xFFFFD54F) is too flashy on a dark felt surface, so we use a
// deeper, less saturated tone that still reads as "gold" without glaring.
val GoldWinnerDark = Color(0xFF9A7200) // dark muted gold  — winner column (dark)

// ── Tertiary: deep burgundy / rose ───────────────────────────────────────────
// A classic card-table accent colour.
val BurgundyDark  = Color(0xFF6A1B2A) // deep burgundy    — tertiary (light)
val BurgundyLight = Color(0xFFEF9A9A) // soft rose        — tertiary (dark)

// ── Surfaces ─────────────────────────────────────────────────────────────────
// Dark theme: dark felt-like tone (very dark green-grey).
// Light theme: warm parchment/cream (off-white with a slight warm tint).
val FeltDark    = Color(0xFF0D1F0F) // near-black felt    — background/surface (dark)
val ParchmentLight = Color(0xFFF5F0E8) // warm parchment  — background/surface (light)

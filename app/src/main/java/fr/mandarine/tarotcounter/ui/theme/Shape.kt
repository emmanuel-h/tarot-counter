package fr.mandarine.tarotcounter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Shapes as MaterialShapes

// ── Salon shapes ─────────────────────────────────────────────────────────────
// Material 3 components pick their corner radius from these five levels, e.g.
//   - small      → text fields, small chips               → 12 dp
//   - medium     → cards                                  → 16 dp
//   - large      → navigation drawers, large cards        → 16 dp
//   - extraLarge → dialogs, bottom sheets                 → 28 dp
//
// Buttons and segmented buttons are *pill-shaped* by default in Material 3
// (fully rounded ends), which is exactly what the Salon design asks for, so
// they need no entry here.
//
// `import … as MaterialShapes` renames the Material class locally so our value
// can simply be called `Shapes`, matching `Typography` in Type.kt.
val Shapes = MaterialShapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

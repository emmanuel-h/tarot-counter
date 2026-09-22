package fr.mandarine.tarotcounter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback

// ─────────────────────────────────────────────────────────────────────────────
// Light haptic feedback (issue #204): a short tick when a taker is chosen and a
// firmer "confirm" when a round is recorded. Android skips them automatically
// when the user turned touch feedback off.
// ─────────────────────────────────────────────────────────────────────────────

/** The [Haptics] of the current screen, remembered across recompositions. */
@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}

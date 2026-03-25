package fr.mandarine.tarotcounter

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Returns the semantic text color for a score value.
 *
 * Colour convention:
 *   - **Positive** (≥ 0): `MaterialTheme.colorScheme.primary`  — green, "winning"
 *   - **Negative** (< 0): `MaterialTheme.colorScheme.error`    — red, "losing"
 *
 * Both colours come from the active [MaterialTheme], so they automatically
 * adapt to light vs. dark mode without any hardcoded hex values.
 *
 * This helper is used by [CompactScoreboard] (GameScreen), the table in
 * [FinalScoreScreen], and the table in [ScoreHistoryScreen] so the colour
 * convention stays consistent across all three views.
 *
 * @param total The cumulative score value to colour.
 * @return A [Color] token from the current colour scheme.
 */
@Composable
fun scoreColor(total: Int): Color =
    // Green for winning (positive / zero), red for losing (negative).
    if (total >= 0) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.error

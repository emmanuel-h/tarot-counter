package fr.mandarine.tarotcounter

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * ScreenHeader is a shared navigation header used by overlay screens
 * (FinalScoreScreen, ScoreHistoryScreen) to ensure a consistent look.
 *
 * It renders a back-arrow [IconButton] on the leading edge followed by the
 * screen [title] in headlineSmall style — the same visual pattern that was
 * previously hand-coded in ScoreHistoryScreen.
 *
 * @param title    The screen title displayed next to the back arrow.
 * @param onBack   Called when the user taps the back arrow.
 * @param modifier Optional modifier forwarded to the outer [Row].
 */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Read the active locale so the back-arrow accessibility label is localized,
    // matching the pattern used in every other composable in this project.
    val strings = appStrings(LocalAppLocale.current)

    // Row aligns its children horizontally: arrow on the left, title to its right.
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // IconButton provides a touch-target around the arrow icon.
        // contentDescription is read by screen-readers (accessibility).
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = strings.backToGame
            )
        }

        // headlineSmall matches the weight used in ScoreHistoryScreen's original header.
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

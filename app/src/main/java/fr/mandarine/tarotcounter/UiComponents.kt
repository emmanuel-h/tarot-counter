package fr.mandarine.tarotcounter

import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Shared UI building blocks for the whole app.
//
// Rule: NEVER use raw Button / OutlinedButton / TextButton directly.
//       Always use AppButton / AppOutlinedButton / AppTextButton so that every
//       button label automatically shrinks to fit its container.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single-line [Text] that automatically shrinks its font size to fit the available width.
 *
 * How it works:
 *  1. We read [LocalTextStyle] to get the ambient font size (set by the enclosing composable,
 *     e.g. [Button] uses `labelLarge` ≈ 14 sp, [TextButton] uses `labelLarge` too).
 *     If a custom [style] is passed its font size overrides the ambient one.
 *  2. After each layout pass Compose calls [onTextLayout] with the result.
 *  3. If [hasVisualOverflow] is true, the text was clipped — we reduce [fontSize] by 10 %
 *     and trigger a recomposition, which re-measures with the smaller size.
 *  4. Repeat until the text fits or [minFontSize] is reached.
 *
 * Convergence is fast (typically 1–3 extra frames, invisible to the user).
 * The font size is remembered per ([text], [maxFontSize]), so switching language or
 * receiving a new style resets it.
 *
 * @param style   Extra [TextStyle] merged on top of the ambient style. Use this to set
 *                a larger starting size (e.g. `MaterialTheme.typography.titleMedium`).
 *                Leave unset to inherit from the surrounding composable.
 * @param minFontSize  Smallest allowed size (sp). Below this we stop shrinking; the text
 *                     may still be partially clipped but remains legible.
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    minFontSize: Float = 8f
) {
    // Merge caller-provided style on top of the ambient style from LocalTextStyle.
    // If `style` is TextStyle.Default (nothing set) this is a no-op.
    val mergedStyle = LocalTextStyle.current.merge(style)

    // Determine the starting (maximum) font size from the resolved style.
    // TextUnit.Unspecified.value returns NaN; fall back to 14 sp (labelLarge) in that case.
    val rawFontSize = mergedStyle.fontSize.value
    val maxFontSizeSp = if (rawFontSize.isNaN()) 14f else rawFontSize

    // fontSize is state so that changing it triggers a recomposition + re-measure.
    // Keyed on (text, maxFontSizeSp) so a language change or style change resets to max.
    var fontSizeSp by remember(text, maxFontSizeSp) { mutableFloatStateOf(maxFontSizeSp) }

    Text(
        text = text,
        modifier = modifier,
        maxLines = 1,
        softWrap = false,   // never wrap; we shrink the font instead
        style = mergedStyle.copy(fontSize = fontSizeSp.sp),
        onTextLayout = { result ->
            // hasVisualOverflow is true when glyphs are drawn outside the measured bounds.
            if (result.hasVisualOverflow && fontSizeSp > minFontSize) {
                fontSizeSp = (fontSizeSp * 0.9f).coerceAtLeast(minFontSize)
            }
        }
    )
}

/**
 * A filled [Button] whose label automatically shrinks to fit its width.
 *
 * Prefer this over [Button] + [Text] everywhere in the app. The label will
 * never be clipped, regardless of screen size or future translation length.
 *
 * @param textStyle  Optional style override (e.g. [MaterialTheme.typography.titleMedium]
 *                   for a larger call-to-action button). Merged on top of the button's
 *                   default `labelLarge` ambient style.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = TextStyle.Default
) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled) {
        AutoSizeText(text, style = textStyle)
    }
}

/**
 * An [OutlinedButton] whose label automatically shrinks to fit its width.
 *
 * Prefer this over [OutlinedButton] + [Text] everywhere in the app.
 */
@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        AutoSizeText(text)
    }
}

/**
 * A [TextButton] whose label automatically shrinks to fit its width.
 *
 * Prefer this over [TextButton] + [Text] everywhere in the app (e.g. dialog actions).
 */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        AutoSizeText(text)
    }
}

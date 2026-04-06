package fr.mandarine.tarotcounter

import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
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
 * Returns a [MutableFloatState] to be shared across several [AutoSizeText] instances that sit
 * inside the same fixed-width row (e.g. [SingleChoiceSegmentedButtonRow]).
 *
 * When the shared state is passed to each [AutoSizeText] via [AutoSizeText.sharedSizeState],
 * all labels shrink together: the first label that overflows reduces the shared size, and every
 * other label immediately recomposes at the same smaller size. The result is a uniform font size
 * across the whole row — always the smallest size that fits the longest label.
 *
 * Pass any values that should trigger a reset as [keys] (typically the current locale), so labels
 * re-measure from the maximum size whenever the text content changes.
 *
 * Usage:
 * ```kotlin
 * val labelSize = rememberSharedAutoSizeState(locale)
 * SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
 *     items.forEachIndexed { index, item ->
 *         SegmentedButton(
 *             shape    = SegmentedButtonDefaults.itemShape(index, items.size),
 *             selected = selection == item,
 *             onClick  = { selection = item },
 *             icon     = {}
 *         ) {
 *             AutoSizeText(
 *                 text            = item.label,
 *                 modifier        = Modifier.padding(horizontal = 2.dp),
 *                 sharedSizeState = labelSize
 *             )
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun rememberSharedAutoSizeState(vararg keys: Any?): MutableFloatState =
    // Float.MAX_VALUE means "no override yet — use the ambient max font size".
    // Each AutoSizeText will coerce this down to its own max before using it.
    remember(*keys) { mutableFloatStateOf(Float.MAX_VALUE) }

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
 * @param style          Extra [TextStyle] merged on top of the ambient style. Use this to set
 *                       a larger starting size (e.g. `MaterialTheme.typography.titleMedium`).
 *                       Leave unset to inherit from the surrounding composable.
 * @param minFontSize    Smallest allowed size (sp). Below this we stop shrinking; the text
 *                       may still be partially clipped but remains legible.
 * @param sharedSizeState  Optional shared state from [rememberSharedAutoSizeState]. When set,
 *                       all [AutoSizeText] instances using the same state shrink together,
 *                       producing a uniform font size across a row of buttons.
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    minFontSize: Float = 8f,
    sharedSizeState: MutableFloatState? = null
) {
    // Merge caller-provided style on top of the ambient style from LocalTextStyle.
    // If `style` is TextStyle.Default (nothing set) this is a no-op.
    val mergedStyle = LocalTextStyle.current.merge(style)

    // Determine the starting (maximum) font size from the resolved style.
    // TextUnit.Unspecified.value returns NaN; fall back to 14 sp (labelLarge) in that case.
    val rawFontSize = mergedStyle.fontSize.value
    val maxFontSizeSp = if (rawFontSize.isNaN()) 14f else rawFontSize

    // Own per-instance size state — used when no shared state is provided.
    // Keyed on (text, maxFontSizeSp) so a language change or style change resets to max.
    var ownFontSizeSp by remember(text, maxFontSizeSp) { mutableFloatStateOf(maxFontSizeSp) }

    // Effective font size: shared state takes priority.
    // We coerce the shared value down to our own max so that a freshly initialised
    // shared state (Float.MAX_VALUE) never exceeds the ambient style's font size.
    val fontSizeSp = if (sharedSizeState != null) {
        sharedSizeState.floatValue.coerceAtMost(maxFontSizeSp)
    } else {
        ownFontSizeSp
    }

    Text(
        text = text,
        modifier = modifier,
        maxLines = 1,
        softWrap = false,   // never wrap; we shrink the font instead
        style = mergedStyle.copy(fontSize = fontSizeSp.sp),
        onTextLayout = { result ->
            // hasVisualOverflow is true when glyphs are drawn outside the measured bounds.
            if (result.hasVisualOverflow && fontSizeSp > minFontSize) {
                val reduced = (fontSizeSp * 0.9f).coerceAtLeast(minFontSize)
                if (sharedSizeState != null) {
                    // Only push the shared state downward — never let one label increase
                    // the size that another label already needed to reduce.
                    if (reduced < sharedSizeState.floatValue) {
                        sharedSizeState.floatValue = reduced
                    }
                } else {
                    ownFontSizeSp = reduced
                }
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

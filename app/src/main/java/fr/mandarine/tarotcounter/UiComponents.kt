package fr.mandarine.tarotcounter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mandarine.tarotcounter.ui.theme.GoldWinnerDark
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Shared UI building blocks for the whole app.
//
// Rule: NEVER use raw Button / OutlinedButton / TextButton directly.
//       Always use AppButton / AppOutlinedButton / AppTextButton so that every
//       button label automatically shrinks to fit its container.
// ─────────────────────────────────────────────────────────────────────────────

// ── Custom vector icons ───────────────────────────────────────────────────────
//
// Material Icons Extended does not include a sword, so we build a minimal 24 × 24
// vector here.  Both icons use `by lazy` so the ImageVector is constructed at most
// once per process and reused across all recompositions.
//
// The `path { }` DSL uses PathBuilder coordinates in the 24 × 24 viewport.
// SolidColor(Color.Black) is the fill; the Icon composable tints it with
// LocalContentColor, so the actual fill colour never appears on screen.

/** Sword pointing upward — used to indicate "attacker (taker)" mode.
 *
 * Shape (24 × 24 viewport):
 *  • Blade body : constant 2 units wide (x 11–13), y 5–14.
 *  • Blade tip  : triangular taper only at the very top — point at (12, 1),
 *                 reaching full blade width at y 5.
 *  • Guard      : 10 units wide (x 7–17), 2 units tall at y 14–16.
 *  • Handle     : 2 units wide, y 16–21.
 */
val SwordIcon: ImageVector by lazy {
    ImageVector.Builder(
        name           = "Sword",
        defaultWidth   = 24.dp,
        defaultHeight  = 24.dp,
        viewportWidth  = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 1f)      // blade tip — sharp point
        lineTo(13f, 5f)      // right edge of triangular taper
        lineTo(13f, 14f)     // blade body right edge, constant width to crossguard
        lineTo(17f, 14f)     // crossguard extends right (10 units total)
        lineTo(17f, 16f)     // crossguard bottom-right
        lineTo(13f, 16f)     // handle top-right
        lineTo(13f, 21f)     // handle bottom-right
        lineTo(11f, 21f)     // handle bottom-left
        lineTo(11f, 16f)     // handle top-left
        lineTo(7f,  16f)     // crossguard bottom-left
        lineTo(7f,  14f)     // crossguard top-left
        lineTo(11f, 14f)     // blade body left edge at crossguard
        lineTo(11f, 5f)      // blade body left edge up
        close()              // left edge of triangular taper back to tip
    }.build()
}

/** Shield outline — used to indicate "defenders" mode. */
val ShieldIcon: ImageVector by lazy {
    ImageVector.Builder(
        name           = "Shield",
        defaultWidth   = 24.dp,
        defaultHeight  = 24.dp,
        viewportWidth  = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        // Classic shield: wide at the top, narrowing to a point at the bottom.
        // Path follows the Material Design "Shield" reference shape (24 × 24 viewport).
        moveTo(12f, 1f)           // top centre
        lineTo(3f, 5f)            // top-left corner
        verticalLineTo(11f)       // left side straight down
        // Lower-left curve sweeping down to the bottom centre point.
        curveToRelative(0f, 5.55f, 3.84f, 10.74f, 9f, 12f)
        // Lower-right curve mirroring the left, sweeping back up.
        curveToRelative(5.16f, -1.26f, 9f, -6.45f, 9f, -12f)
        verticalLineTo(5f)        // right side straight up
        close()                   // back to top centre
    }.build()
}

// Maximum content width for all screens.
// On large screens (e.g. 10-inch tablets in landscape) the content is constrained
// to this width and centered horizontally so it never stretches uncomfortably wide.
// 600 dp matches the Material Design "compact/medium" breakpoint guideline.
internal val MAX_CONTENT_WIDTH = 600.dp

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
 * @param colors     Optional color override (e.g. [ButtonDefaults.buttonColors] with
 *                   [MaterialTheme.colorScheme.errorContainer] for a destructive action).
 *                   Defaults to the standard filled-button scheme.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = TextStyle.Default,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    // Optional shared state from rememberSharedAutoSizeState(): when passed,
    // this button's label shrinks together with sibling buttons that share the
    // same state, producing a uniform font size across the whole row.
    sharedSizeState: MutableFloatState? = null
) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled, colors = colors) {
        AutoSizeText(text, style = textStyle, sharedSizeState = sharedSizeState)
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
    enabled: Boolean = true,
    // Optional shared state from rememberSharedAutoSizeState(): when passed,
    // this button's label shrinks together with sibling buttons that share the
    // same state, producing a uniform font size across the whole row.
    sharedSizeState: MutableFloatState? = null
) {
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        AutoSizeText(text, sharedSizeState = sharedSizeState)
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

// ─────────────────────────────────────────────────────────────────────────────
// Reusable game-form building blocks
//
// These composables were originally private inside GameScreen.kt. Moving them
// here makes them available to any screen that needs them and lets them be
// tested and maintained without touching the main game-screen file.
// ─────────────────────────────────────────────────────────────────────────────

// A small bold label placed above a form section.
// `fillMaxWidth()` ensures the label stretches to align with the field below it.
@Composable
fun FormLabel(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth()
    )
}

// Holds the display data and state callbacks for one row of the bonus grid.
// Declared at file scope (not inside the composable) so it is not recreated on
// every recomposition of [CompactBonusGrid].
//
// Two selection modes are supported:
//
//   Single-select  (multiSelect = false) — used for Petit au bout.
//     At most one player can be assigned at a time. `value` holds the selected
//     player name (or null). `onSelect` receives the new selection (or null to clear).
//
//   Multi-select   (multiSelect = true)  — used for the three Poignée rows.
//     Any number of players can declare simultaneously. `values` holds the current
//     set of selected names. `onToggle` receives (playerName, isNowChecked) so the
//     caller can add or remove the player from its set independently.
data class BonusRow(
    val label: String,
    val tooltip: String,
    // ── Single-select fields (petit au bout) ──────────────────────────────────
    val value: String?              = null,
    val onSelect: ((String?) -> Unit)? = null,
    // ── Multi-select fields (poignées, issue #149) ────────────────────────────
    val values: Set<String>                     = emptySet(),
    val onToggle: ((String, Boolean) -> Unit)?  = null,
    val multiSelect: Boolean                    = false
)

// A label cell that wraps both a bonus name and a decorative ⓘ icon inside a
// single [TooltipBox] so that tapping anywhere on the cell opens the description.
//
// label : localized bonus name — shown as text and as the tooltip title.
// body  : multi-line tooltip body (rules + point value).
//
// Layout note: this composable is intentionally content-sized (no fillMaxWidth).
// The enclosing Row in [CompactBonusGrid] carries the weight() modifier that
// determines the label column width, keeping checkboxes properly aligned.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusLabelCell(label: String, body: String) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope        = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            // RichTooltip supports a title and a multi-line body text.
            RichTooltip(title = { Text(label) }) {
                Text(body)
            }
        },
        state = tooltipState
    ) {
        // Only the text + icon are clickable — empty space in the label column is not.
        Row(
            modifier = Modifier.clickable { scope.launch { tooltipState.show() } },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = label,
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Decorative info icon — the clickable Row above handles all input.
            Icon(
                imageVector        = Icons.Default.Info,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier
                    .padding(start = 2.dp)
                    .size(14.dp)
            )
        }
    }
}

// A small ⓘ [IconButton] that opens a [RichTooltip] when tapped.
// Used next to standalone dropdowns (e.g. the Chelem dropdown in the round form)
// where a full [BonusLabelCell] would not fit.
//
// title : the bonus name shown as the tooltip heading.
// body  : multi-line explanation text (rules + point value).
//
// `isPersistent = true` keeps the tooltip open until the user dismisses it —
// important on mobile where there is no hover event to close it automatically.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusInfoIcon(title: String, body: String) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope        = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip(title = { Text(title) }) {
                Text(body)
            }
        },
        state = tooltipState
    ) {
        // Small icon button — 20 dp keeps it compact inside a row.
        IconButton(
            onClick  = { scope.launch { tooltipState.show() } },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Info,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(14.dp)
            )
        }
    }
}

// Compact grid showing four player-assigned bonuses (petit au bout, poignée,
// double poignée, triple poignée).
//
// Layout:
//   Row 0 (header):  empty label column | Player1 | Player2 | …
//   Row 1–4 (data):  label + ⓘ          | ☑/☐    | ☑/☐    | …
//
// Each player cell holds a [Checkbox]:
//   • Petit au bout row   — single-select: ticking a box deselects the previous one.
//   • Poignée rows        — multi-select:  any number of boxes can be ticked at once.
//     (Issue #149: multiple players may each show their own trump hand independently.)
//
// bonusLabels      : four localized label strings (parallel to the state params).
// bonusTooltips    : four tooltip body strings shown when the ⓘ is tapped.
// petitAuBout      : currently selected player for Petit au bout, or null.
// onPetit          : called with the selected player name (or null to clear).
// poignees         : set of players who declared a simple poignée.
// onPoignee        : called with (playerName, isNowChecked) when a box is toggled.
// doublePoignees   : set of players who declared a double poignée.
// onDoublePoignee  : called with (playerName, isNowChecked) when a box is toggled.
// triplePoignees   : set of players who declared a triple poignée.
// onTriplePoignee  : called with (playerName, isNowChecked) when a box is toggled.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactBonusGrid(
    playerNames: List<String>,
    bonusLabels: List<String>,
    bonusTooltips: List<String>,
    petitAuBout: String?,           onPetit: (String?) -> Unit,
    poignees: Set<String>,          onPoignee: (String, Boolean) -> Unit,
    doublePoignees: Set<String>,    onDoublePoignee: (String, Boolean) -> Unit,
    triplePoignees: Set<String>,    onTriplePoignee: (String, Boolean) -> Unit
) {
    // Build one BonusRow per bonus. The first row (Petit au bout) uses single-select;
    // the three Poignée rows use multi-select (issue #149).
    val bonuses = listOf(
        BonusRow(
            label    = bonusLabels[0],
            tooltip  = bonusTooltips[0],
            value    = petitAuBout,
            onSelect = onPetit
        ),
        BonusRow(
            label       = bonusLabels[1],
            tooltip     = bonusTooltips[1],
            values      = poignees,
            onToggle    = onPoignee,
            multiSelect = true
        ),
        BonusRow(
            label       = bonusLabels[2],
            tooltip     = bonusTooltips[2],
            values      = doublePoignees,
            onToggle    = onDoublePoignee,
            multiSelect = true
        ),
        BonusRow(
            label       = bonusLabels[3],
            tooltip     = bonusTooltips[3],
            values      = triplePoignees,
            onToggle    = onTriplePoignee,
            multiSelect = true
        )
    )

    // Label column: wide enough for the bonus text + ⓘ icon.
    // Reduced from 0.42 → 0.36 so the gap between the label and the first
    // checkbox column is eliminated, freeing ~6 % more width for player columns.
    val labelWeight = 0.36f
    // Each player column gets an equal share of the remaining width.
    val colWeight   = (1f - labelWeight) / playerNames.size

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Header row: one column title per player ───────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty space above the label column.
            Spacer(Modifier.weight(labelWeight))
            for (name in playerNames) {
                Text(
                    text      = name,
                    style     = MaterialTheme.typography.labelSmall,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    // Center the name over its checkbox column.
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.weight(colWeight)
                )
            }
        }

        // ── One row per bonus ─────────────────────────────────────────────────
        for (row in bonuses) {
            Row(
                // heightIn(min = 48.dp) enforces Material's recommended minimum
                // touch-target height, making checkboxes easier to tap on small screens.
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label column: BonusLabelCell makes the text + icon tappable;
                // empty space in the weight-based column is not clickable.
                Row(
                    modifier = Modifier.weight(labelWeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BonusLabelCell(label = row.label, body = row.tooltip)
                }

                // One Checkbox per player. Rendering logic differs by selection mode:
                //   Single-select (Petit au bout): ticking a box also clears any other.
                //   Multi-select  (Poignées):      each box toggles independently.
                for (name in playerNames) {
                    if (row.multiSelect) {
                        // Multi-select: pass (name, newCheckedState) to the caller.
                        Checkbox(
                            checked         = name in row.values,
                            onCheckedChange = { checked ->
                                row.onToggle?.invoke(name, checked)
                            },
                            modifier = Modifier.weight(colWeight)
                        )
                    } else {
                        // Single-select: ticking an unchecked box assigns that player;
                        // ticking the already-checked player clears it (passes null).
                        Checkbox(
                            checked         = row.value == name,
                            onCheckedChange = { checked ->
                                row.onSelect?.invoke(if (checked) name else null)
                            },
                            modifier = Modifier.weight(colWeight)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Score colour helper
// ─────────────────────────────────────────────────────────────────────────────

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

// ─────────────────────────────────────────────────────────────────────────────
// Shared score-table building blocks
//
// These were extracted from ScoreHistoryScreen and FinalScoreScreen (issue #75)
// to eliminate duplication: any bug fix or visual change now applies everywhere.
// ─────────────────────────────────────────────────────────────────────────────

// Relative weight for the "Round" / "Manche" column.
// Smaller than a player column because round numbers are at most two digits.
// Using weight (rather than a fixed dp width) makes the table responsive:
// all columns together always fill exactly the available screen width, so
// 4- and 5-player games never require horizontal scrolling (issue #129).
internal const val SCORE_TABLE_ROUND_COL_WEIGHT: Float = 0.8f

// Relative weight for each player column.
// All player columns share the same weight, so they are evenly distributed
// across the remaining width after the round column has taken its share.
internal const val SCORE_TABLE_PLAYER_COL_WEIGHT: Float = 1f

/**
 * A single horizontal row in a score table.
 *
 * Replaces the near-identical `ScoreTableRow` (ScoreHistoryScreen) and
 * `FinalScoreTableRow` (FinalScoreScreen) that existed before issue #75.
 *
 * Each cell uses [Modifier.weight] so the row always fills the available width,
 * regardless of how many players are in the game (issue #129).
 * The first column (index 0) uses [SCORE_TABLE_ROUND_COL_WEIGHT]; all others
 * use [SCORE_TABLE_PLAYER_COL_WEIGHT].
 *
 * Long player names are automatically shrunk via [AutoSizeText] so they never
 * overflow their cell. All cells in the same row share a [rememberSharedAutoSizeState]
 * so they all display at the same — smallest-needed — font size.
 *
 * @param cells               Text content for each cell in left-to-right order.
 * @param isHeader            If true, renders all text in bold (header row).
 * @param scoreValues         Optional parallel list of raw score integers for colour
 *                            coding. A null entry means "use the default text colour";
 *                            a non-null entry is passed to [scoreColor].
 * @param winnerColumnIndices Zero-based column indices to highlight with a gold background
 *                            and bold text. Defaults to an empty set (no highlighting),
 *                            so ScoreHistoryScreen can use this composable unchanged.
 */
@Composable
fun ScoreTableRow(
    cells: List<String>,
    isHeader: Boolean,
    scoreValues: List<Int?>? = null,
    winnerColumnIndices: Set<Int> = emptySet()
) {
    // One shared state per row: all AutoSizeText instances in this row will shrink
    // together so every cell displays at the same font size. Keyed on the cell
    // contents so a data change (new round added) resets the shared size to maximum.
    val rowSizeState = rememberSharedAutoSizeState(*cells.toTypedArray())

    // RowScope is the implicit receiver here, so Modifier.weight() is available
    // inside forEachIndexed without any extra ceremony.
    Row(modifier = Modifier.fillMaxWidth()) {
        cells.forEachIndexed { index, text ->
            // Round column is slightly narrower than player columns (0.8 vs 1.0).
            // weight() distributes the Row's full width among all children
            // proportionally, so the table always fits without horizontal scrolling.
            val colWeight = if (index == 0) SCORE_TABLE_ROUND_COL_WEIGHT
                            else            SCORE_TABLE_PLAYER_COL_WEIGHT

            val isWinnerColumn = index in winnerColumnIndices

            // Winner columns: saturated amber in light mode, muted dark gold in dark mode.
            // `Color.Unspecified` leaves the background transparent (no winner highlight).
            val bgColor = when {
                isWinnerColumn && isSystemInDarkTheme() -> GoldWinnerDark
                isWinnerColumn                          -> MaterialTheme.colorScheme.secondary
                else                                    -> Color.Unspecified
            }
            val bgModifier = if (bgColor != Color.Unspecified) Modifier.background(bgColor)
                             else Modifier

            // Semantic colour for score cells: green (positive/zero) or red (negative).
            // Header rows and the round-number column (null scoreValue) always use default.
            val textColor = if (!isHeader && scoreValues != null) {
                val value = scoreValues.getOrNull(index)
                if (value != null) scoreColor(value) else Color.Unspecified
            } else {
                Color.Unspecified
            }

            Box(
                modifier = Modifier
                    .weight(colWeight)          // proportional width — fills screen
                    .then(bgModifier)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // AutoSizeText shrinks the font until the text fits its cell.
                // The shared state ensures every cell in this row uses the same size,
                // which keeps the row visually consistent (no cell looks different).
                // `textAlign = TextAlign.Center` is baked into the style so the text
                // stays centred after the font is scaled down.
                val cellStyle = if (isHeader || isWinnerColumn) {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center,
                        color      = textColor
                    )
                } else {
                    MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        color     = textColor
                    )
                }
                AutoSizeText(
                    text            = text,
                    style           = cellStyle,
                    sharedSizeState = rowSizeState
                )
            }
        }
    }
}

// Shows a "None" chip followed by one chip per player name.
// Tapping a player assigns them to the bonus; tapping the selected player
// again (or "None") clears the selection.
//
// label          : localized section header text shown above the chips.
// noneLabel      : localized label for the "nobody" chip.
// selectedPlayer : the currently assigned player name, or null if nobody.
// onSelect       : callback with the new player name, or null to clear.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerChipSelector(
    label: String,
    noneLabel: String,
    selectedPlayer: String?,
    playerNames: List<String>,
    onSelect: (String?) -> Unit
) {
    FormLabel(label)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedPlayer == null,
            onClick  = { onSelect(null) },
            label    = { Text(noneLabel) }
        )
        for (name in playerNames) {
            // Tapping the already-selected player deselects them (null = nobody).
            FilterChip(
                selected = selectedPlayer == name,
                onClick  = { onSelect(if (selectedPlayer == name) null else name) },
                label    = { Text(name) }
            )
        }
    }
}

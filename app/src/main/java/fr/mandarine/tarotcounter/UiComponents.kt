package fr.mandarine.tarotcounter

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import fr.mandarine.tarotcounter.ui.theme.Dimens
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mandarine.tarotcounter.ui.theme.tarotColors
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Shared UI building blocks for the whole app.
//
// Rule: NEVER use raw Button / OutlinedButton / TextButton directly.
//       Always use AppButton / AppOutlinedButton / AppTextButton so that every
//       button label automatically shrinks to fit its container.
// ─────────────────────────────────────────────────────────────────────────────

// ── Custom vector icon ────────────────────────────────────────────────────────
//
// Material Icons Extended 1.7.x does not include a crossed-swords icon, so we
// embed the path data from the Material Symbols Outlined "swords" glyph here.
//
// The icon uses `by lazy` so the ImageVector is built at most once per process
// and reused across all recompositions.
//
// Source: https://fonts.google.com/icons — Material Symbols Outlined "swords",
// FILL 0, wght 400, GRAD 0, opsz 24.  The original SVG uses a 960 × 960
// viewport with the Y-origin at −960 (i.e. viewBox="0 -960 960 960"), so
// addGroup applies a +960 Y-translation to remap it into Android's (0,0)…
// (960,960) space.  SolidColor(Color.Black) is the nominal fill; the Icon
// composable tints it with LocalContentColor at render time, so the literal
// fill colour never appears on screen.
//
// The shield counterpart uses Icons.Default.Shield from Material Icons Extended.

/** Crossed swords — used to indicate "attacker (taker)" mode. */
val SwordsIcon: ImageVector by lazy {
    // Parse the raw SVG path string from Material Symbols into Compose PathNodes.
    // This avoids hand-translating ~30 SVG commands into PathBuilder calls.
    val nodes = PathParser().parsePathString(
        "M762-96 645-212l-88 88-28-28q-23-23-23-57t23-57l169-169q-23-23 57-23t57 23l28 28" +
        "-88 88 116 117q12 12 12 28t-12 28l-50 50q-12 12-28 12t-28-12Zm118-628L426-270l5 4" +
        "q23 23 23 57t-23 57l-28 28-88-88L198-96q-12 12-28 12t-28-12l-50-50q-12-12-12-28" +
        "t12-28l116-117-88-88 28-28q23-23 57-23t57 23l4 5 454-454h160v160Z" +
        "M334-583l24-23 23-24-23 24-24 23Z" +
        "m-56 57L80-724v-160h160l198 198-57 56-174-174h-47v47l174 174-56 57Z" +
        "m92 199 430-430v-47h-47L323-374l47 47Z" +
        "m0 0-24-23-23-24 23 24 24 23Z"
    ).toNodes()

    ImageVector.Builder(
        name           = "Swords",
        defaultWidth   = 24.dp,
        defaultHeight  = 24.dp,
        viewportWidth  = 960f,   // Material Symbols uses a 960 × 960 viewport
        viewportHeight = 960f
    )
        // Shift the origin: SVG Y-axis runs from −960 to 0; Android's runs 0 to 960.
        .addGroup(translationY = 960f)
        .addPath(pathData = nodes, fill = SolidColor(Color.Black))
        .clearGroup()
        .build()
}

// Maximum content width for all screens.
// On large screens (e.g. 10-inch tablets in landscape) the content is constrained
// to this width and centered horizontally so it never stretches uncomfortably wide.
// 600 dp matches the Material Design "compact/medium" breakpoint guideline.
internal val MAX_CONTENT_WIDTH = 600.dp

// Opacity Material 3 applies to disabled content; reused for our custom borders.
private const val DISABLED_ALPHA = 0.38f

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
 *             icon     = {},
 *             colors   = salonSegmentedButtonColors()
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
    // Salon restyle (issue #196): a 56 dp tall pill in felt green.
    //  - heightIn(min = …) keeps the 56 dp height but lets the button grow if the
    //    user picked a very large system font.
    //  - CircleShape on a rectangle gives fully rounded ("pill") ends.
    //  - The default colours already come from colorScheme.primary = felt green
    //    (sage in dark mode), so no colour override is needed here.
    Button(
        onClick        = onClick,
        modifier       = modifier.heightIn(min = Dimens.PrimaryButtonHeight),
        enabled        = enabled,
        shape          = CircleShape,
        colors         = colors,
        contentPadding = PaddingValues(horizontal = Dimens.SpaceL)
    ) {
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
    // Salon restyle (issue #196): a 48 dp pill with a 1 dp hairline border.
    // The border colour is colorScheme.outline (the Salon hairline). The label is
    // set to colorScheme.primary (felt green) explicitly: recent Material 3
    // versions default outlined-button text to a muted grey (onSurfaceVariant).
    OutlinedButton(
        onClick        = onClick,
        modifier       = modifier.heightIn(min = Dimens.SecondaryButtonHeight),
        enabled        = enabled,
        shape          = CircleShape,
        colors         = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        border         = BorderStroke(
            width = 1.dp,
            // A disabled button keeps the hairline but fades it, like its label.
            color = if (enabled) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.outline.copy(alpha = DISABLED_ALPHA)
        ),
        contentPadding = PaddingValues(horizontal = Dimens.SpaceL)
    ) {
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
 *   - **Positive** (≥ 0): `MaterialTheme.tarotColors.positive` — green, "winning"
 *   - **Negative** (< 0): `MaterialTheme.tarotColors.negative` — red, "losing"
 *
 * Both colours come from the active Salon theme tokens, so they automatically
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
    if (total >= 0) MaterialTheme.tarotColors.positive
    else MaterialTheme.tarotColors.negative

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

            // Winner columns get a soft brass tint from the active theme (light or
            // dark follows the app's own toggle, not the system setting).
            // `Color.Unspecified` leaves the background transparent (no winner highlight).
            val bgColor = if (isWinnerColumn) MaterialTheme.tarotColors.winnerHighlight
                          else Color.Unspecified
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

// ─────────────────────────────────────────────────────────────────────────────
// Salon building blocks (issue #196)
//
// Screens of the Salon redesign are assembled from these parts instead of raw
// Material defaults. The pure logic behind them (initials, sizes, suit glyphs,
// top-bar limits) lives in SalonUi.kt so it can be unit-tested.
// Previews for every component (light + dark) are in UiComponentsPreviews.kt.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A Salon "paper" card: paper surface, 16 dp corners, 1 dp hairline border and
 * a soft, low shadow.
 *
 * @param title           Optional heading drawn at the top of the card
 *                        (Cormorant, e.g. "New game").
 * @param contentPadding  Space between the border and the content.
 * @param content         The card body. `ColumnScope.() -> Unit` means the lambda
 *                        runs *inside* a Column, so children stack vertically and
 *                        may use Column-only modifiers such as `align`.
 */
@Composable
fun SalonCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    contentPadding: PaddingValues = PaddingValues(Dimens.CardPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier,
        // Shapes.medium is 16 dp in the Salon theme (ui/theme/Shape.kt).
        shape     = MaterialTheme.shapes.medium,
        // Material's Card defaults to surfaceContainerHighest (the segmented track
        // colour); the Salon card is plain paper = colorScheme.surface.
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor   = MaterialTheme.colorScheme.onSurface
        ),
        border    = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier            = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
        ) {
            if (title != null) {
                Text(
                    text     = title,
                    style    = MaterialTheme.typography.headlineMedium,
                    // heading() lets screen-reader users jump from card to card.
                    modifier = Modifier.semantics { heading() }
                )
            }
            content()
        }
    }
}

/**
 * A circle showing a player's initial, in the colour of their seat.
 *
 * The colour comes from `MaterialTheme.tarotColors.playerTone(seatIndex)`, so a
 * player keeps the same colour on every screen (and in charts).
 *
 * @param name      The player's name; its first letter is drawn (see [playerInitial]).
 * @param seatIndex 0-based seat of the player — picks the colour.
 * @param size      [AvatarSize.S] (24 dp), [AvatarSize.M] (36 dp) or [AvatarSize.L] (56 dp).
 * @param ringColor Optional colour of a thin ring around the circle. Used by
 *                  [AvatarStack] to separate overlapping avatars; `null` = no ring.
 */
@Composable
fun PlayerAvatar(
    name: String,
    seatIndex: Int,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.M,
    ringColor: Color? = null
) {
    val tone        = MaterialTheme.tarotColors.playerTone(seatIndex)
    val description = appStrings(LocalAppLocale.current).playerAvatar(name)

    // The ring is drawn as a border *inside* the circle, so the avatar keeps
    // its exact diameter whether or not it has a ring.
    val ringModifier = if (ringColor != null) {
        Modifier.border(size.stackRing, ringColor, CircleShape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size.diameter)
            .clip(CircleShape)
            .background(tone.container)
            .then(ringModifier)
            // clearAndSetSemantics replaces the inner Text ("A") with one clear
            // description ("Player Alice") for screen readers.
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = playerInitial(name),
            color = tone.content,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize      = size.fontSizeSp.sp,
                lineHeight    = size.fontSizeSp.sp,
                letterSpacing = 0.sp
            )
        )
    }
}

/**
 * A row of overlapping [PlayerAvatar]s — e.g. "who is playing" on the resume card.
 *
 * Players are drawn in seat order: the avatar at list index `i` gets seat `i`'s
 * colour. Each avatar slides a third of its width under the previous one; a
 * thin ring in [ringColor] (normally the colour of the card behind) keeps them
 * visually separate.
 *
 * @param names     Player names in seat order.
 * @param size      Avatar size; [AvatarSize.S] by default, as stacks are compact.
 * @param ringColor Ring colour; defaults to the card surface.
 */
@Composable
fun AvatarStack(
    names: List<String>,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.S,
    ringColor: Color = MaterialTheme.colorScheme.surface
) {
    // A *negative* spacing makes each child start before the previous one ends,
    // which produces the overlap. Later children are drawn on top.
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(-size.stackOverlap)
    ) {
        names.forEachIndexed { seat, name ->
            PlayerAvatar(name = name, seatIndex = seat, size = size, ringColor = ringColor)
        }
    }
}

/**
 * A decorative separator between sections: a double hairline on each side of
 * the four card suits ♠ ♥ ♦ ♣, drawn in brass.
 *
 * The suits are *text* glyphs (see [SUIT_GLYPHS]), never emoji. The divider is
 * hidden from screen readers — it carries no information.
 */
@Composable
fun SuitDivider(modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.outline

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
    ) {
        DoubleHairline(color = lineColor)
        Text(
            text  = SUIT_GLYPHS,
            color = MaterialTheme.tarotColors.brass,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, lineHeight = 14.sp)
        )
        DoubleHairline(color = lineColor)
    }
}

// Two parallel 1 dp lines, 3 dp apart, filling the remaining width of the Row.
// `RowScope.` as a receiver gives access to Modifier.weight().
@Composable
private fun RowScope.DoubleHairline(color: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(4.dp)
            // drawBehind draws directly on the canvas: cheaper than two Boxes.
            .drawBehind {
                val stroke = 1.dp.toPx()
                // Top line, then bottom line — each centred on its 1 dp row.
                drawLine(color, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
                drawLine(
                    color,
                    Offset(0f, size.height - stroke / 2),
                    Offset(size.width, size.height - stroke / 2),
                    stroke
                )
            }
    )
}

/**
 * A left-aligned section title in Cormorant, with an optional trailing action
 * (e.g. a "See all" link) pushed to the right edge.
 *
 * @param title    The section title.
 * @param trailing Optional composable drawn at the end of the row.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier          = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = title,
            style    = MaterialTheme.typography.headlineSmall,
            // weight(1f) takes all free space, pushing `trailing` to the right.
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        trailing?.invoke()
    }
}

/**
 * A signed score such as "+312" or "-48", coloured green (≥ 0) or red (< 0).
 *
 * Every Salon text style uses tabular figures, so scores stacked in a column
 * line up digit by digit.
 *
 * @param score The score to display; formatted with [withSign].
 * @param size  [ScoreSize.S], [ScoreSize.M] or [ScoreSize.XL].
 */
@Composable
fun ScoreText(
    score: Int,
    modifier: Modifier = Modifier,
    size: ScoreSize = ScoreSize.M
) {
    Text(
        text     = score.withSign(),
        modifier = modifier,
        color    = scoreColor(score),
        maxLines = 1,
        style    = MaterialTheme.typography.titleMedium.copy(
            fontSize   = size.fontSizeSp.sp,
            lineHeight = size.fontSizeSp.sp * 1.2f
        )
    )
}

/**
 * The Salon top bar: optional back arrow, title on the left, and up to
 * [MAX_TOP_BAR_ACTIONS] icon buttons on the right. Replaces the old ScreenHeader.
 *
 * All buttons have 48 dp touch targets (Material's accessibility minimum).
 *
 * @param title                  Screen title (Cormorant).
 * @param onBack                 Back-arrow callback; `null` hides the arrow.
 * @param backContentDescription Screen-reader label of the back arrow;
 *                               defaults to the localized "Back to game".
 * @param actions                Icon buttons on the right (at most two).
 */
@Composable
fun SalonTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    actions: List<TopBarAction> = emptyList()
) {
    requireValidTopBarActions(actions)
    val strings = appStrings(LocalAppLocale.current)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            // Without a back arrow the title aligns with the screen content.
            .padding(start = if (onBack != null) 0.dp else Dimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backContentDescription ?: strings.backToGame
                )
            }
        }
        Text(
            text     = title,
            style    = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Dimens.SpaceXs)
                .semantics { heading() }
        )
        for (action in actions) {
            IconButton(onClick = action.onClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector        = action.icon,
                    contentDescription = action.contentDescription,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Salon colours for every [SegmentedButton]: the selected segment is filled
 * felt green (sage at night) with light text; the others are paper with a
 * hairline border.
 *
 * Pass it to each segment: `SegmentedButton(…, colors = salonSegmentedButtonColors())`.
 */
@Composable
fun salonSegmentedButtonColors(): SegmentedButtonColors {
    val scheme = MaterialTheme.colorScheme
    return SegmentedButtonDefaults.colors(
        activeContainerColor   = scheme.primary,
        activeContentColor     = scheme.onPrimary,
        activeBorderColor      = scheme.primary,
        inactiveContainerColor = scheme.surface,
        inactiveContentColor   = scheme.onSurface,
        inactiveBorderColor    = scheme.outline
    )
}

/**
 * A Salon text field: paper-white fill, 12 dp corners, a hairline border that
 * turns felt green on focus, and an optional leading slot (e.g. an avatar).
 *
 * @param placeholder    Grey hint shown while the field is empty.
 * @param leadingContent Optional composable at the start of the field.
 * @param isError        Draws the border and supporting text in the error colour.
 * @param supportingText Optional message under the field (e.g. "Name already used").
 */
@Composable
fun SalonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        modifier        = modifier,
        singleLine      = true,
        isError         = isError,
        keyboardOptions = keyboardOptions,
        textStyle       = MaterialTheme.typography.bodyLarge,
        // Shapes.small is 12 dp in the Salon theme.
        shape           = MaterialTheme.shapes.small,
        placeholder     = placeholder?.let { { Text(it) } },
        leadingIcon     = leadingContent,
        supportingText  = supportingText?.let { { Text(it) } },
        colors          = OutlinedTextFieldDefaults.colors(
            focusedContainerColor   = scheme.surfaceContainerLowest,
            unfocusedContainerColor = scheme.surfaceContainerLowest,
            disabledContainerColor  = scheme.surfaceContainerLowest,
            errorContainerColor     = scheme.surfaceContainerLowest,
            focusedBorderColor      = scheme.primary,
            unfocusedBorderColor    = scheme.outline
        )
    )
}

/**
 * A [SalonTextField] for a player's name, with that player's [PlayerAvatar] in
 * the leading slot. While the name is empty, the avatar shows the initial of
 * the [placeholder] (e.g. "P" for "Player 1").
 *
 * @param seatIndex   0-based seat — picks the avatar colour.
 * @param placeholder Fallback name shown when the field is empty.
 */
@Composable
fun PlayerNameField(
    value: String,
    onValueChange: (String) -> Unit,
    seatIndex: Int,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null
) {
    SalonTextField(
        value          = value,
        onValueChange  = onValueChange,
        modifier       = modifier,
        placeholder    = placeholder,
        isError        = isError,
        supportingText = supportingText,
        // Names start with a capital letter: the keyboard shifts automatically.
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        leadingContent = {
            PlayerAvatar(
                name      = value.ifBlank { placeholder },
                seatIndex = seatIndex,
                size      = AvatarSize.M
            )
        }
    )
}

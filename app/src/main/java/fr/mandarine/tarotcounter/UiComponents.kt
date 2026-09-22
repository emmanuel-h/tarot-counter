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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.mandarine.tarotcounter.ui.theme.tarotColors

// ─────────────────────────────────────────────────────────────────────────────
// Shared UI building blocks for the whole app.
//
// Rule: NEVER use raw Button / OutlinedButton / TextButton directly.
//       Always use AppButton / AppOutlinedButton / AppTextButton so that every
//       button label automatically shrinks to fit its container.
// ─────────────────────────────────────────────────────────────────────────────

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
    enabled: Boolean = true,
    // Optional label colour, e.g. colorScheme.error for "End game".
    // Color.Unspecified keeps Material's default (colorScheme.primary).
    contentColor: Color = Color.Unspecified
) {
    TextButton(
        onClick  = onClick,
        modifier = modifier,
        enabled  = enabled,
        colors   = if (contentColor == Color.Unspecified) ButtonDefaults.textButtonColors()
                   else ButtonDefaults.textButtonColors(contentColor = contentColor)
    ) {
        AutoSizeText(text)
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
 * This helper is used by [ScoreText] (standings, rounds, past games), the table in
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
 * The Salon "hero" card: felt-green ground, ivory text, 16 dp corners and a
 * stronger shadow than [SalonCard]. Used sparingly, for the one thing that
 * matters most on a screen (the game in progress, the winner).
 *
 * The felt stays deep green in dark mode too (`tarotColors.felt`), so the card
 * reads as the same object in both themes.
 */
@Composable
fun FeltCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Dimens.CardPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier,
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.tarotColors.felt,
            // contentColor becomes the default colour of every Text and Icon inside.
            contentColor   = MaterialTheme.tarotColors.onFelt
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier            = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            content             = content
        )
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
 * @param label     Text drawn in the circle instead of the name's initial
 *                  (e.g. the seat number while a name field is still empty).
 */
@Composable
fun PlayerAvatar(
    name: String,
    seatIndex: Int,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.M,
    ringColor: Color? = null,
    label: String? = null
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
            text  = label ?: playerInitial(name),
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
            // Wide letter spacing (like the mockup's 0.5em) so the four suits breathe.
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize      = 14.sp,
                lineHeight    = 14.sp,
                letterSpacing = 0.3.em
            )
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
 * @param titleStyle             Text style of the title; the home screen passes
 *                               `headlineLarge` to show the app name as a wordmark.
 * @param overline               Optional small brass line above the title, shown in
 *                               upper case (e.g. "ROUND 5" above "Chloé takes").
 * @param titleLeading           Optional composable between the back arrow and the
 *                               title (e.g. the taker's avatar).
 */
@Composable
fun SalonTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    actions: List<TopBarAction> = emptyList(),
    titleStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    overline: String? = null,
    titleLeading: (@Composable () -> Unit)? = null
) {
    requireValidTopBarActions(actions)
    val strings = appStrings(LocalAppLocale.current)

    Row(
        modifier = modifier
            .fillMaxWidth()
            // At least 64 dp; a bit taller when an overline is stacked above the title.
            .heightIn(min = 64.dp)
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
        if (titleLeading != null) {
            Box(modifier = Modifier.padding(start = Dimens.SpaceXs)) { titleLeading() }
        }
        // Title block: optional brass overline stacked above the title.
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Dimens.SpaceS.takeIf { titleLeading != null } ?: Dimens.SpaceXs)
        ) {
            if (overline != null) {
                Text(
                    text  = overline.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.tarotColors.brassText
                )
            }
            Text(
                text     = title,
                style    = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
        }
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
 * the leading slot. While the name is empty, the avatar shows the seat number
 * (1, 2, 3…) — "P" for every "Player N" placeholder would not tell seats apart.
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
                size      = AvatarSize.M,
                label     = if (value.isBlank()) (seatIndex + 1).toString() else null
            )
        }
    )
}

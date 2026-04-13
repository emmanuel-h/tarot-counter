# Shared UI Components

All reusable UI building blocks live in `UiComponents.kt`.

## SwordsIcon

```kotlin
val SwordsIcon: ImageVector
```

A custom `ImageVector` embedding the **Material Symbols Outlined "swords"** path (FILL 0, wght 400, GRAD 0, opsz 24), used as the **attacker (taker)** camp toggle icon in the Points field of `GameScreen`. Defined here because `material-icons-extended` 1.7.x does not ship a swords glyph.

The path data is parsed at first use via `PathParser.parsePathString()` from the raw SVG `d` attribute sourced from [fonts.google.com/icons](https://fonts.google.com/icons). The original SVG uses a 960 × 960 viewport with the Y-origin at −960 (`viewBox="0 -960 960 960"`); an `addGroup(translationY = 960f)` call remaps it into Android's (0, 0) … (960, 960) coordinate space.

The companion **shield** icon for the defenders camp uses `Icons.Default.Shield` from Material Icons Extended — no custom definition needed.

Both icons are tinted at render time by `LocalContentColor` (via the `Icon` composable), so the `SolidColor(Color.Black)` fill never appears directly.

---

## MAX_CONTENT_WIDTH

```kotlin
internal val MAX_CONTENT_WIDTH = 600.dp
```

The maximum width used by every screen's content column. On large screens (10-inch tablets in landscape, ~960–1280 dp wide) the content is constrained to 600 dp and centered inside a `Box` so it never stretches uncomfortably wide.

**Pattern used in each screen:**

```kotlin
Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter
) {
    Column(
        modifier = Modifier
            .widthIn(max = MAX_CONTENT_WIDTH)
            .fillMaxWidth()
            // … screen-specific scroll + padding modifiers
    ) {
        // screen content
    }
}
```

For `GameScreen` (which has a bottom action bar that must pin to the bottom), add `.fillMaxHeight()` after `.fillMaxWidth()`.

---

## Rule: always use the App* button wrappers

**Never use raw Material3 button composables directly.** Use the wrappers below instead so that every button label automatically shrinks to fit its container — no clipping, no wrapping, regardless of screen size or translation length.

| Use this | Instead of |
|---|---|
| `AppButton` | `Button` |
| `AppOutlinedButton` | `OutlinedButton` |
| `AppTextButton` | `TextButton` |

### Why

French labels (and future translations) are often longer than English ones. A raw `Button { Text("Attaquant") }` clips the label on narrow screens. The `App*` wrappers use `AutoSizeText` internally, which detects overflow after each layout pass and reduces the font by 10 % per frame until the text fits.

---

## AutoSizeText

```kotlin
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    minFontSize: Float = 8f
)
```

A single-line `Text` that automatically shrinks its font size to fit the available width.

**How it works:**
1. Reads `LocalTextStyle.current` (the ambient style set by the enclosing composable, e.g. `Button` sets `labelLarge` ≈ 14 sp) and merges the optional `style` parameter on top.
2. After each layout pass, `onTextLayout` fires. If `hasVisualOverflow` is `true`, `fontSize` is reduced by 10 % and a recomposition is triggered.
3. Repeat until the text fits or `minFontSize` is reached (default 8 sp).

Convergence typically takes 1–3 extra frames, invisible to the user.

**`style` parameter:** pass a `TextStyle` to override the ambient font size, e.g. `MaterialTheme.typography.titleMedium` for a larger call-to-action. All other style properties (color, font family…) are still inherited from the ambient.

**Inside `SegmentedButton`:** always add a horizontal padding modifier so the text stays away from the button's rounded corners, and pass `icon = {}` to suppress the default checkmark — selection state is communicated via the filled background color alone.

For a row of segments where all labels should display at the **same font size**, use `rememberSharedAutoSizeState` (see [SingleChoiceSegmentedButtonRow](#singlechoicesegmentedbuttonrow)) instead of a standalone `AutoSizeText` — this ensures every segment shrinks together to the smallest size needed by the longest label.

```kotlin
// Two-segment row (no shared size needed for just 2 labels)
SegmentedButton(
    selected = !defenderMode,
    onClick  = { … },
    shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
    icon     = {}   // no checkmark — more space for the label
) {
    AutoSizeText(
        text     = strings.attackerMode,
        modifier = Modifier.padding(horizontal = 1.dp)
    )
}
```

---

## rememberSharedAutoSizeState

```kotlin
@Composable
fun rememberSharedAutoSizeState(vararg keys: Any?): MutableFloatState
```

Returns a shared font-size state for use with `AutoSizeText.sharedSizeState`. When several `AutoSizeText` instances in the same row share this state, they all reduce together — the first label that overflows drags the others down, so every segment always displays at the same size.

Pass the current locale (and any other value whose change should trigger a full re-measure) as `keys`:

```kotlin
val labelSize = rememberSharedAutoSizeState(locale)
```

---

## SingleChoiceSegmentedButtonRow

Use `SingleChoiceSegmentedButtonRow` + `SegmentedButton` for **mutually exclusive single-choice** options (e.g. contract selection, mode toggle). Never use `FilterChip` for this — chips are for multi-select filtering.

**Required conventions:**
- `icon = {}` on every `SegmentedButton` — the filled segment already signals selection.
- `AutoSizeText` (not `Text`) for every label.
- `modifier = Modifier.padding(horizontal = 2.dp)` inside each label.
- `sharedSizeState = rememberSharedAutoSizeState(locale)` so all labels display at the same font size.

```kotlin
val labelSize = rememberSharedAutoSizeState(locale)

SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    items.forEachIndexed { index, item ->
        SegmentedButton(
            shape    = SegmentedButtonDefaults.itemShape(index, items.size),
            selected = selection == item,
            onClick  = { selection = if (selection == item) null else item },
            icon     = {}
        ) {
            AutoSizeText(
                text            = item.label,
                modifier        = Modifier.padding(horizontal = 2.dp),
                sharedSizeState = labelSize
            )
        }
    }
}
```

---

## ScoreTableRow

```kotlin
@Composable
fun ScoreTableRow(
    cells: List<String>,
    isHeader: Boolean,
    scoreValues: List<Int?>? = null,
    winnerColumnIndices: Set<Int> = emptySet()
)
```

A single horizontal row in a score table. Used by both `ScoreHistoryScreen` and `FinalScoreScreen`.

- **Column widths:** uses `Modifier.weight()` — index 0 ("Round") gets weight `SCORE_TABLE_ROUND_COL_WEIGHT` (0.8f); all other columns get `SCORE_TABLE_PLAYER_COL_WEIGHT` (1.0f). This distributes the full available width proportionally, so all columns are always visible on screen regardless of player count (no horizontal scrolling needed, even with 5 players).
- **Text sizing:** each cell uses `AutoSizeText` with a shared size state per row, so long player names shrink gracefully and all cells in a row stay at the same font size.
- **`isHeader`:** renders all text bold (for the header row).
- **`scoreValues`:** parallel list of raw integers for semantic colour coding via `scoreColor()`. Pass `null` or include `null` entries to skip colouring for that cell. Index 0 should always be `null` (round-number column has no colour).
- **`winnerColumnIndices`:** zero-based column indices highlighted with a gold/amber background and bold text. Defaults to `emptySet()` (no highlighting), so `ScoreHistoryScreen` can use this composable without any extra arguments. `FinalScoreScreen` passes the winner column indices.

**Usage (ScoreHistoryScreen — no winner highlighting):**
```kotlin
ScoreTableRow(
    cells    = listOf(strings.roundColumn) + playerNames,
    isHeader = true
)
```

**Usage (FinalScoreScreen — with winner highlighting):**
```kotlin
ScoreTableRow(
    cells               = row.cells,
    isHeader            = false,
    scoreValues         = row.scoreValues,
    winnerColumnIndices = winnerColumnIndices
)
```

The data for each row is produced by `buildScoreTableData()` in `GameModels.kt`.

---

## FormLabel

```kotlin
@Composable
fun FormLabel(text: String)
```

A small bold label placed above a form section (e.g. above the bouts dropdown or the bonus grid). Uses `MaterialTheme.typography.titleSmall` and fills the available width.

```kotlin
FormLabel(strings.numberOfBouts)
```

### Side-by-side form fields with aligned labels and inputs (IntrinsicSize pattern)

When two form fields are placed in a `Row` side by side, both their **labels** and their **input controls** need to align vertically — even when one label is longer than the other (e.g. "Nombre de bouts (oudlers)" wraps to two lines in French while "Points" is always one line).

Using `verticalAlignment = Alignment.Bottom` on the Row aligns the form fields but pushes the shorter column down, misaligning its label (issue #145). The correct pattern is:

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        // Row height = tallest column's natural (non-expanded) height.
        // Required so fillMaxHeight() inside each Column has a concrete ceiling.
        .height(IntrinsicSize.Min),
    horizontalArrangement = Arrangement.spacedBy(16.dp)
    // No verticalAlignment — each Column handles its own vertical layout.
) {
    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
        FormLabel(strings.numberOfBouts)  // label at top
        Spacer(Modifier.weight(1f))       // pushes the field to the bottom
        // … form field (e.g. dropdown)
    }
    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
        FormLabel(strings.pointsHeader)   // label at top — vertically aligned with the left label
        Spacer(Modifier.weight(1f))       // pushes the field to the bottom
        // … form field (e.g. OutlinedTextField)
    }
}
```

Key points:
- `Modifier.height(IntrinsicSize.Min)` gives the Row a fixed height equal to the tallest column's content height (before any expansion). This is what makes `fillMaxHeight()` inside children meaningful.
- `fillMaxHeight()` on each Column makes it expand to that shared height.
- `Spacer(Modifier.weight(1f))` inside each Column acts as a flexible gap — it takes whatever vertical space remains after the label, pushing the form control to the bottom of the Column.
- Result: labels are top-aligned, form controls are bottom-aligned, regardless of how much each label wraps.

---

## BonusInfoIcon

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusInfoIcon(title: String, body: String)
```

A small ⓘ `IconButton` (20 dp) that opens a `RichTooltip` when tapped. Use it next to standalone dropdowns where a full label row would not fit (e.g. next to the Chelem dropdown in the round form). The tooltip is persistent — it stays open until the user dismisses it.

```kotlin
BonusInfoIcon(title = strings.chelemLabel, body = strings.chelemTooltipBody)
```

---

## BonusLabelCell

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusLabelCell(label: String, body: String)
```

A tappable cell showing a bonus name followed by a small ⓘ icon. Tapping anywhere on the cell opens a `RichTooltip` explaining the bonus. Used as the label column in `CompactBonusGrid`. Content-sized — the enclosing `Row` carries the weight modifier.

---

## CompactBonusGrid

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactBonusGrid(
    playerNames: List<String>,
    bonusLabels: List<String>,
    bonusTooltips: List<String>,
    petitAuBout: String?,     onPetit: (String?) -> Unit,
    poignee: String?,         onPoignee: (String?) -> Unit,
    doublePoignee: String?,   onDoublePoignee: (String?) -> Unit,
    triplePoignee: String?,   onTriplePoignee: (String?) -> Unit
)
```

A compact grid showing four player-assigned bonuses (petit au bout, poignée, double poignée, triple poignée). The header row shows player names; each data row shows a bonus label + ⓘ on the left and one `Checkbox` per player on the right. Ticking a checked box clears the assignment (sets it back to `null`).

**Layout weights:** the label column occupies `0.36f` of the total row width; each player column receives an equal share of the remaining `0.64f`. Player names in the header are centred over their checkbox column and truncated with ellipsis when the name exceeds the available width (5-player games on narrow screens).

---

## PlayerChipSelector

```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerChipSelector(
    label: String,
    noneLabel: String,
    selectedPlayer: String?,
    playerNames: List<String>,
    onSelect: (String?) -> Unit
)
```

A `FlowRow` of `FilterChip`s — one "None" chip followed by one chip per player. Used for the **chelem player selector**. Tapping the already-selected player deselects them (passes `null` to `onSelect`).

The partner selector in 5-player games uses an inline `ExposedDropdownMenuBox` (in `GameScreen.kt`) instead — label on the left, dropdown on the right, no "None" entry.

---

## AppButton

```kotlin
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = TextStyle.Default,
    colors: ButtonColors = ButtonDefaults.buttonColors()
)
```

A filled `Button` with an auto-sizing label. Use `textStyle` for prominent call-to-action buttons that need a larger starting size, and `colors` to override the fill color (e.g. for destructive actions):

```kotlin
// Standard button
AppButton(text = strings.startGame, onClick = { onStartGame() })

// Prominent CTA with larger text
AppButton(
    text      = strings.newGame,
    onClick   = onNewGame,
    modifier  = Modifier.fillMaxWidth(),
    textStyle = MaterialTheme.typography.titleMedium
)

// Destructive action (red background)
AppButton(
    text    = strings.endGame,
    onClick = onEndGame,
    colors  = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor   = MaterialTheme.colorScheme.onErrorContainer
    )
)
```

---

## AppOutlinedButton

```kotlin
@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
)
```

An `OutlinedButton` with an auto-sizing label. Use for secondary actions alongside a filled `AppButton`.

```kotlin
AppOutlinedButton(text = strings.backToGame, onClick = onBack)
```

---

## AppTextButton

```kotlin
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
)
```

A `TextButton` with an auto-sizing label. Typically used for dialog actions.

```kotlin
AlertDialog(
    confirmButton = { AppTextButton(text = strings.confirm, onClick = onConfirm) },
    dismissButton = { AppTextButton(text = strings.cancel,  onClick = onDismiss) }
)
```

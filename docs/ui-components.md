# Shared UI Components

All reusable UI building blocks live in `UiComponents.kt`.

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

## FormLabel

```kotlin
@Composable
fun FormLabel(text: String)
```

A small bold label placed above a form section (e.g. above the bouts dropdown or the bonus grid). Uses `MaterialTheme.typography.titleSmall` and fills the available width.

```kotlin
FormLabel(strings.numberOfBouts)
```

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

A `FlowRow` of `FilterChip`s — one "None" chip followed by one chip per player. Used for the partner selector (5-player games) and the chelem player selector. Tapping the already-selected player deselects them.

---

## AppButton

```kotlin
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = TextStyle.Default
)
```

A filled `Button` with an auto-sizing label. Use `textStyle` for prominent call-to-action buttons that need a larger starting size:

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

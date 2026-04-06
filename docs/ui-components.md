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

**Inside `SegmentedButton`:** always add a horizontal padding modifier so the text stays away from the button's rounded corners:
```kotlin
AutoSizeText(
    text     = strings.attackerMode,
    modifier = Modifier.padding(horizontal = 4.dp)
)
```

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

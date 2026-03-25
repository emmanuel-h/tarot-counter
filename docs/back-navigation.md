# Back Navigation

## Overview

The Android system back button (hardware key or gesture swipe) is handled consistently across all screens. The destination is always the **landing page** — there is no intermediate back stack.

## Behaviour by screen

| Screen | System back button |
|---|---|
| Game screen | Navigate directly to landing page (no dialog) |
| Score history overlay | Navigate directly to landing page (no dialog) |
| Final Score screen | Show a confirmation dialog first |

The **back arrow** in the screen header is a separate affordance and is unaffected: it continues to navigate one level up within the game flow (e.g. history → game, final score → game).

## Implementation

Back interception uses `BackHandler` from `androidx.activity.compose`. A `BackHandler` is a composable that registers a callback with the activity's `OnBackPressedDispatcher`. The most recently composed enabled handler wins, which makes the overlay pattern work naturally.

### Game screen and history overlay

A single `BackHandler` in `GameScreen.kt` covers both the main game view and the score-history overlay:

```kotlin
// Disabled when Final Score is showing — that screen handles back itself.
BackHandler(enabled = !showFinalScore) { onEndGame() }
```

When `showScoreHistory` is true (history overlay is visible), this handler still fires and calls `onEndGame()`, which navigates to the landing page.

### Final Score screen

`FinalScoreScreen.kt` registers its own `BackHandler` (deeper in the composition tree, so higher priority). It sets a boolean flag to show a Material 3 `AlertDialog`:

```kotlin
var showLeaveConfirm by remember { mutableStateOf(false) }
BackHandler { showLeaveConfirm = true }

if (showLeaveConfirm) {
    AlertDialog(
        title = { Text(strings.backConfirmTitle) },
        text  = { Text(strings.backConfirmBody) },
        confirmButton = { TextButton(onClick = onNewGame) { Text(strings.backConfirmLeave) } },
        dismissButton = { TextButton(onClick = { showLeaveConfirm = false }) { Text(strings.cancel) } }
    )
}
```

Tapping **Leave** fires `onNewGame` (navigates to landing page). Tapping **Cancel** closes the dialog and stays on the Final Score screen.

## Localisation

The dialog strings are defined in `AppLocale.kt` for both locales:

| Key | English | French |
|---|---|---|
| `backConfirmTitle` | Leave the game? | Quitter la partie ? |
| `backConfirmBody` | The game results won't be saved if you leave now. | Les résultats ne seront pas sauvegardés si vous quittez maintenant. |
| `backConfirmLeave` | Leave | Quitter |

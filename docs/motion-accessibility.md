# Motion, haptics and accessibility (issue #204)

The last layer of the Salon redesign: movement that explains what changed, a light touch of haptics, and checks that the app works for everyone.

## Motion

| Where | What moves | How |
|---|---|---|
| Between screens (Home ↔ Game ↔ Settings) | Fade through: the old screen fades out quickly, then the new one fades in while growing from 92 % | `AnimatedContent` in `MainActivity`, `fadeThrough()` in `Motion.kt` (300 ms) |
| Game ↔ Score history ↔ Game over | Same fade through | `AnimatedContent` over `GameOverlay` in `GameScreen` |
| "Who took?" ↔ round entry | Same fade through, so the round entry panel appears and disappears smoothly | `AnimatedContent` keyed on the taker whose entry is open |
| Standings after **Confirm round** | Each score **counts** from its previous total to the new one, and the rows **slide** to their new ranks | See below |
| Game over winner card | Scales in from 80 % with a fade, once | `AnimatedVisibility` (unchanged) |

### How the standings animate

When a round has just been added, the standings card first shows the standings from **before** that round. `GameScreen.standingsShownForRounds` tracks this, starting at the current round count, so resuming a game or undoing a round never animates. After a 250 ms pause, the card switches to the new standings:

- **Count-up**: `ScoreText(animate = true)` holds the shown number in an `Animatable` and glides it to each new score over 600 ms. `countFrom` starts a freshly composed text, such as the row of a player who just took the lead, at their previous total.
- **Re-order**: each row is wrapped in `key(playerName)`, so Compose moves the row rather than rebuilding it. `Modifier.animatePlacement()` in `Motion.kt` then slides it from its old position to its new one with a spring.

### Reduced motion

The Android setting **Accessibility → Remove animations** (and the developer option "Animator duration scale: off") sets the system animator duration scale to 0. `MainActivity` reads it once through `rememberSystemReducedMotion()` and provides `LocalReducedMotion`. With it on:

- screen changes use no transition (`EnterTransition.None` / `ExitTransition.None`),
- scores jump straight to their value (`scoreAnimationMillis(true) == 0`),
- standings skip the "previous round" step and the row sliding.

The pure helpers `isReducedMotion`, `scoreAnimationMillis`, `screenTransitionMillis` and `hapticTypeFor` live in `MotionLogic.kt` and are covered by `MotionTest`. The composable parts (`Motion.kt`, `Haptics.kt`) are excluded from mutation testing like other Compose code.

## Haptics

`Haptics` (`MotionLogic.kt`, remembered by `rememberHaptics()` in `Haptics.kt`) plays two light effects through Compose's `LocalHapticFeedback`. Android skips them when the user has turned touch feedback off.

| Moment | Effect |
|---|---|
| A "Who took?" tile is tapped | `HapticFeedbackType.SegmentTick` (a short tick) |
| **Confirm round** | `HapticFeedbackType.Confirm` |

## Insets (edge-to-edge)

`MainActivity` pads every screen with the `Scaffold` insets (status and navigation bars) and marks them as consumed (`consumeWindowInsets`). As a result, `imePadding()` on the home and game screens adds only the part of the keyboard that sits above the navigation bar, with no double gap. Bottom sheets add `navigationBarsPadding()` themselves.

## Accessibility pass

- **TalkBack labels**: every avatar reads "Player Alice"; every icon button has a content description (settings, undo, history, back = "Back to game", "Change taker", "Close"). Decorative icons (trophy, chevrons, suits) are hidden. The winner card and the live result pill are read as one sentence (`mergeDescendants`). The score chart has a text description.
- **Touch targets**: every tappable element is at least 48 dp. That covers top-bar icon buttons, taker tiles (96 dp), bout chips, partner chips, bonus rows (52 dp), settings rows (56 dp) and past-game rows.
- **Font scale 200 %**: checked on the phone emulator. Fixed heights that could clip text became minimums (taker tiles, contract cards, bout chips). Labels in fixed-width slots shrink through `AutoSizeText`.
- **Tablet landscape** (10", 2560 × 1600): home, game, round entry, game over and score history all stay centred at 600 dp.

## Bugs found during the pass

- The round-entry sections were laid out on top of each other once they sat inside `AnimatedContent`, which stacks its content like a `Box`. They are now wrapped in a `Column`, and a layout test guards the order.
- Skipping a round while a half-filled round entry was open kept its points and contract for the next round. End Game then warned about unsaved points. A new round now clears the contract and, with it, the whole form. A regression test covers this.

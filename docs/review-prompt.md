# Play Store Review Prompt

Issue [#223](https://github.com/emmanuel-h/tarot-counter/issues/223).

## Why

The Play Store showed no star rating for the app, while every competitor ranked above it
does. To collect ratings from satisfied players, the app uses the
[Google Play In-App Review API](https://developer.android.com/guide/playcore/in-app-review):
Play draws its own "Rate this app" sheet over the app, and the player can rate without
leaving it.

## When the review is asked

```
finished game #1 ──► Game Over                 (nothing)
finished game #2 ──► Game Over                 (nothing)
finished game #3 ──► Game Over ─(1.5 s)─► Play review sheet  → flag "asked" saved
finished game #4+ ─► Game Over                 (nothing, ever again)
```

- **Only on the Game Over screen**, never mid-game. The request starts 1.5 s after Game
  Over appears so the final scores are seen first; leaving Game Over during that pause
  (back to the game, New Game, Main Menu) cancels it.
- **From the 3rd finished game** (`ReviewPromptPolicy.GAMES_BEFORE_REVIEW = 3`). A game is
  "finished" when it is ended with at least one round played (skipped rounds count). A
  game ended with no rounds is cancelled and does not count.
- **Once**: after the flow has launched, a persisted flag stops any further request. Play
  also applies its own quota, so the sheet may not even appear when requested; the app
  cannot tell.
- **Result ignored**: the API does not say whether the player rated, and the game flow is
  the same either way.
- If Play is unavailable (no Play Store, offline…), the launch fails silently and the flag
  is **not** saved, so it is tried again after the next finished game.

The prompt is drawn by Google Play in the device language, so the app has no new strings.

## Code

| File | Role |
|------|------|
| `ReviewPrompt.kt` → `ReviewPromptPolicy` | Pure rule: `shouldAsk(finishedGames, alreadyAsked)` = not asked yet **and** ≥ 3 games |
| `ReviewPrompt.kt` → `PlayReviewLauncher` | Wraps Play's `ReviewManager`: `requestReview()` then `launchReview(activity, info)` (review-ktx suspend helpers). Returns `true` when the flow ran, `false` on any error |
| `GameStorage.kt` | DataStore keys `finished_game_count` (Int) and `review_requested` (Boolean) |
| `GameViewModel.kt` | `endGame()` increments the counter once per game id (ending the same game twice — Game Over → back → End Game — counts once); `shouldRequestReview: StateFlow<Boolean>` combines both keys through the policy; `markReviewRequested()` saves the flag |
| `GameScreen.kt` | `requestReview: suspend () -> Boolean` parameter; a `LaunchedEffect(showFinalScore, shouldRequestReview)` calls it on Game Over and marks the review as requested when it returns `true` |
| `MainActivity.kt` | Passes `PlayReviewLauncher(this).launch(this)` as `requestReview` |

Dependency: `com.google.android.play:review-ktx` (version catalog entry `play-review-ktx`).

## Tests

| Test | Covers |
|------|--------|
| `src/test/…/ReviewPromptTest.kt` | Policy (below / at / above threshold, already asked); ViewModel counting (no rounds, same game twice, several games), `shouldRequestReview` and `markReviewRequested` |
| `src/androidTest/…/GameScreenTest.kt` | Review requested on Game Over of the 3rd game and flag saved; not before the threshold; not mid-game; not again once asked |
| `src/androidTest/…/PlayReviewLauncherTest.kt` | The real launcher against Play's `FakeReviewManager` (no UI shown) |

`PlayReviewLauncher` is excluded from PIT because it needs an Activity and Play Services;
the decision logic (`ReviewPromptPolicy`, `GameViewModel`) is mutation-tested.

## Testing the real sheet

The real sheet only appears for an app installed from Play. Use **internal app sharing**
or an internal testing track, sign in with a tester account that has not reviewed the app,
and finish three games. Note that the quota means the sheet may only show once per
account in a while; clearing the Play Store data resets it on a test device.

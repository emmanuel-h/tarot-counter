package fr.mandarine.tarotcounter

import android.app.Activity
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview

// ── Play Store review prompt (issue #223) ─────────────────────────────────────
//
// After a few finished games, the app asks Google Play to show its native
// "Rate this app" sheet (the In-App Review API). The sheet is drawn by Play
// itself over the Game Over screen; the app never sees whether the user rated.
//
// The code is split in two on purpose:
//   - ReviewPromptPolicy: a pure decision ("should we ask now?") with no Android
//     dependency, so it is unit-tested on the JVM and mutation-tested by PIT.
//   - PlayReviewLauncher: the thin Android wrapper that actually talks to Play.

// Pure "should we ask for a review?" rules.
//
// An `object` in Kotlin is a singleton: there is exactly one instance, created on
// first use, so callers write `ReviewPromptPolicy.shouldAsk(...)` without `new`.
object ReviewPromptPolicy {

    // Number of finished games after which the review is requested. The 3rd game
    // means the player has come back to the app several times — a good sign they
    // like it — while still being early enough to reach most players.
    // `const val` is a compile-time constant (inlined wherever it is used).
    const val GAMES_BEFORE_REVIEW = 3

    // Returns true when the review flow should be launched now.
    //
    // finishedGames : how many games the user has finished (ended with ≥ 1 round).
    // alreadyAsked  : true once the flow has been launched; we never ask twice
    //                 (Play also enforces its own quota on top of this).
    fun shouldAsk(finishedGames: Int, alreadyAsked: Boolean): Boolean =
        !alreadyAsked && finishedGames >= GAMES_BEFORE_REVIEW
}

// Launches the Google Play In-App Review flow.
//
// `ReviewManager` is Play's entry point. Production code gets the real one from
// `ReviewManagerFactory.create(context)`; tests pass a `FakeReviewManager`, which
// behaves the same way but never shows any UI.
class PlayReviewLauncher(private val reviewManager: ReviewManager) {

    // Convenience constructor used by MainActivity: builds the real Play manager.
    // `applicationContext` avoids holding on to the Activity longer than needed.
    constructor(activity: Activity) : this(ReviewManagerFactory.create(activity.applicationContext))

    // Asks Play for a review "ticket" (ReviewInfo), then shows the review sheet.
    //
    // `suspend` means it pauses the calling coroutine while Play answers, without
    // blocking the UI thread. The review-ktx helpers `requestReview()` and
    // `launchReview()` wrap Play's callback-based Task API into suspend functions.
    //
    // Returns true when the flow was launched. Play does not say whether the user
    // actually rated (or whether the sheet was even displayed, if the quota is
    // reached), so the result carries no more information than "it ran".
    // Returns false when Play is unavailable (no Play Store, offline, …): the
    // exception is swallowed so the game flow is never disturbed, and the caller
    // can try again after a later game.
    suspend fun launch(activity: Activity): Boolean = try {
        val reviewInfo = reviewManager.requestReview()
        reviewManager.launchReview(activity, reviewInfo)
        true
    } catch (e: kotlinx.coroutines.CancellationException) {
        // Never swallow cancellation: it means the caller's coroutine was stopped
        // (e.g. the user left Game Over), and coroutines rely on it propagating.
        throw e
    } catch (_: Exception) {
        false
    }
}

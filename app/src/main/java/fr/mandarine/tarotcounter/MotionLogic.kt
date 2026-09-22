package fr.mandarine.tarotcounter

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

// ─────────────────────────────────────────────────────────────────────────────
// Pure (non-composable) logic of the motion and haptics polish (issue #204),
// kept apart from Motion.kt / Haptics.kt so it can be unit-tested and
// mutation-tested on the JVM (MotionTest).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * True when animations must be skipped. "Remove animations" (accessibility) and
 * the developer option "Animator duration scale: off" both set the system
 * animator duration scale to 0.
 */
fun isReducedMotion(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f

/** Duration of the score count-up after a round, in milliseconds (0 = instant). */
fun scoreAnimationMillis(reducedMotion: Boolean): Int = if (reducedMotion) 0 else 600

/** Duration of a screen-to-screen fade-through, in milliseconds (0 = instant). */
fun screenTransitionMillis(reducedMotion: Boolean): Int = if (reducedMotion) 0 else 300

/** The two haptic moments of the app. */
enum class HapticMoment { TAKER_SELECTED, ROUND_CONFIRMED }

/** Which system haptic effect each moment plays. */
fun hapticTypeFor(moment: HapticMoment): HapticFeedbackType = when (moment) {
    HapticMoment.TAKER_SELECTED  -> HapticFeedbackType.SegmentTick
    HapticMoment.ROUND_CONFIRMED -> HapticFeedbackType.Confirm
}

/** Plays haptic [HapticMoment]s through Compose's [HapticFeedback]. */
class Haptics(private val feedback: HapticFeedback) {
    fun play(moment: HapticMoment) = feedback.performHapticFeedback(hapticTypeFor(moment))
}


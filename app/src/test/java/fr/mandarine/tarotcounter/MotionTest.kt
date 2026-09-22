package fr.mandarine.tarotcounter

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the motion and haptics helpers (Motion.kt, Haptics.kt — issue #204). */
class MotionTest {

    // ── Reduced motion ────────────────────────────────────────────────────────

    @Test
    fun `animator scale zero means reduced motion`() {
        assertTrue(isReducedMotion(0f))
        assertFalse(isReducedMotion(1f))
        assertFalse(isReducedMotion(0.5f))
        assertFalse(isReducedMotion(10f))
    }

    @Test
    fun `durations collapse to zero with reduced motion`() {
        assertEquals(600, scoreAnimationMillis(reducedMotion = false))
        assertEquals(0, scoreAnimationMillis(reducedMotion = true))
        assertEquals(300, screenTransitionMillis(reducedMotion = false))
        assertEquals(0, screenTransitionMillis(reducedMotion = true))
    }

    // ── Haptics ───────────────────────────────────────────────────────────────

    @Test
    fun `each moment has its own haptic effect`() {
        assertEquals(HapticFeedbackType.SegmentTick, hapticTypeFor(HapticMoment.TAKER_SELECTED))
        assertEquals(HapticFeedbackType.Confirm, hapticTypeFor(HapticMoment.ROUND_CONFIRMED))
    }

    @Test
    fun `haptics play the mapped effect`() {
        val played = mutableListOf<HapticFeedbackType>()
        val fake = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                played += hapticFeedbackType
            }
        }
        val haptics = Haptics(fake)
        haptics.play(HapticMoment.TAKER_SELECTED)
        haptics.play(HapticMoment.ROUND_CONFIRMED)
        assertEquals(listOf(HapticFeedbackType.SegmentTick, HapticFeedbackType.Confirm), played)
    }
}

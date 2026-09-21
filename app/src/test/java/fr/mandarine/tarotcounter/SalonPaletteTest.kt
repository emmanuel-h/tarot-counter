package fr.mandarine.tarotcounter

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import fr.mandarine.tarotcounter.ui.theme.DarkColorScheme
import fr.mandarine.tarotcounter.ui.theme.DarkTarotColors
import fr.mandarine.tarotcounter.ui.theme.LightColorScheme
import fr.mandarine.tarotcounter.ui.theme.LightTarotColors
import fr.mandarine.tarotcounter.ui.theme.MIN_TEXT_CONTRAST
import fr.mandarine.tarotcounter.ui.theme.TarotColors
import fr.mandarine.tarotcounter.ui.theme.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the Salon palette (issue #195): readability of both themes and the
 * stable player-tone lookup.
 *
 * Run with: ./gradlew testDebugUnitTest
 */
class SalonPaletteTest {

    // ── Contrast ──────────────────────────────────────────────────────────────

    /**
     * Every (text, background) pair a screen may draw, for one theme.
     * Each pair is labelled so a failure says exactly which pair is too faint.
     */
    private fun textPairs(scheme: ColorScheme, tarot: TarotColors): List<Triple<String, Color, Color>> {
        // Text colours that can appear on any of the neutral grounds.
        val inks = mapOf(
            "onSurface" to scheme.onSurface,
            "onSurfaceVariant" to scheme.onSurfaceVariant,
            "primary" to scheme.primary,
            "error" to scheme.error,
            "brassText" to tarot.brassText,
            "positive" to tarot.positive,
            "negative" to tarot.negative,
        )
        // The neutral grounds those texts sit on.
        val grounds = mapOf(
            "background" to scheme.background,
            "surface" to scheme.surface,
            "surfaceVariant" to scheme.surfaceVariant,
            "surfaceContainer" to scheme.surfaceContainer,
            "surfaceContainerHighest" to scheme.surfaceContainerHighest,
            "winnerHighlight" to tarot.winnerHighlight,
        )
        val neutral = inks.flatMap { (inkName, ink) ->
            grounds.map { (groundName, ground) -> Triple("$inkName on $groundName", ink, ground) }
        }
        // "on-X" pairs: text drawn on a filled coloured element.
        val filled = listOf(
            Triple("onBackground on background", scheme.onBackground, scheme.background),
            Triple("onPrimary on primary", scheme.onPrimary, scheme.primary),
            Triple("onPrimaryContainer on primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer),
            Triple("onSecondary on secondary", scheme.onSecondary, scheme.secondary),
            Triple("onSecondaryContainer on secondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer),
            Triple("onTertiary on tertiary", scheme.onTertiary, scheme.tertiary),
            Triple("onTertiaryContainer on tertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer),
            Triple("onError on error", scheme.onError, scheme.error),
            Triple("onErrorContainer on errorContainer", scheme.onErrorContainer, scheme.errorContainer),
            Triple("inverseOnSurface on inverseSurface", scheme.inverseOnSurface, scheme.inverseSurface),
            Triple("onFelt on felt", tarot.onFelt, tarot.felt),
            Triple("brassOnFelt on felt", tarot.brassOnFelt, tarot.felt),
            Triple("positive on primaryContainer", tarot.positive, scheme.primaryContainer),
        )
        // Initials drawn on every player avatar.
        val tones = tarot.playerTones.mapIndexed { i, tone ->
            Triple("onPlayerTone on seat $i", tarot.onPlayerTone, tone)
        }
        return neutral + filled + tones
    }

    private fun assertReadable(theme: String, pairs: List<Triple<String, Color, Color>>) {
        // Collect every failure first so one run reports them all.
        val failures = pairs
            .map { (label, fg, bg) -> label to contrastRatio(fg, bg) }
            .filter { (_, ratio) -> ratio < MIN_TEXT_CONTRAST }
            .map { (label, ratio) -> "$label = %.2f:1".format(ratio) }
        assertTrue("$theme theme pairs below $MIN_TEXT_CONTRAST:1: $failures", failures.isEmpty())
    }

    @Test
    fun `every light theme text pair reaches 4_5 to 1`() {
        assertReadable("Light", textPairs(LightColorScheme, LightTarotColors))
    }

    @Test
    fun `every dark theme text pair reaches 4_5 to 1`() {
        assertReadable("Dark", textPairs(DarkColorScheme, DarkTarotColors))
    }

    @Test
    fun `dark player tones stand out from the dark card surface`() {
        // Chart lines and avatars are non-text graphics: WCAG asks for 3:1
        // against their background so they remain visible at night.
        DarkTarotColors.playerTones.forEach { tone ->
            assertTrue(contrastRatio(tone, DarkColorScheme.surface) >= 3.0)
        }
    }

    @Test
    fun `positive and negative differ from each other in both themes`() {
        assertNotEquals(LightTarotColors.positive, LightTarotColors.negative)
        assertNotEquals(DarkTarotColors.positive, DarkTarotColors.negative)
    }

    // ── Player tones ──────────────────────────────────────────────────────────

    @Test
    fun `both themes provide five distinct player tones`() {
        // Tarot is played with 3 to 5 players: one tone per seat, no duplicates.
        listOf(LightTarotColors, DarkTarotColors).forEach { colors ->
            assertEquals(5, colors.playerTones.size)
            assertEquals(5, colors.playerTones.toSet().size)
        }
    }

    @Test
    fun `playerTone returns the tone of the seat and the initials colour`() {
        LightTarotColors.playerTones.forEachIndexed { seat, tone ->
            val result = LightTarotColors.playerTone(seat)
            assertEquals(tone, result.container)
            assertEquals(LightTarotColors.onPlayerTone, result.content)
        }
    }

    @Test
    fun `playerTone wraps around past the last seat`() {
        assertEquals(LightTarotColors.playerTones[0], LightTarotColors.playerTone(5).container)
        assertEquals(LightTarotColors.playerTones[2], LightTarotColors.playerTone(7).container)
    }

    @Test
    fun `playerTone never crashes on a negative index`() {
        // -1 mod 5 = 4 → the last tone.
        assertEquals(LightTarotColors.playerTones[4], LightTarotColors.playerTone(-1).container)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `TarotColors rejects an empty tone list`() {
        TarotColors(
            brass = Color.Black, brassText = Color.Black, positive = Color.Black,
            negative = Color.Black, felt = Color.Black, onFelt = Color.White,
            brassOnFelt = Color.White, winnerHighlight = Color.White,
            playerTones = emptyList(), onPlayerTone = Color.White,
        )
    }
}

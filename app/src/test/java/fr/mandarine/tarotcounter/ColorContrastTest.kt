package fr.mandarine.tarotcounter

import androidx.compose.ui.graphics.Color
import fr.mandarine.tarotcounter.ui.theme.contrastRatio
import fr.mandarine.tarotcounter.ui.theme.relativeLuminance
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the WCAG contrast helpers in ui/theme/ColorContrast.kt.
 *
 * Expected values come from the WCAG 2.1 formulas and match what online
 * contrast checkers (e.g. WebAIM) report for the same colours.
 *
 * Run with: ./gradlew testDebugUnitTest
 */
class ColorContrastTest {

    private val delta = 0.005

    // ── relativeLuminance ─────────────────────────────────────────────────────

    @Test
    fun `black has zero luminance`() {
        assertEquals(0.0, relativeLuminance(Color.Black), delta)
    }

    @Test
    fun `white has full luminance`() {
        assertEquals(1.0, relativeLuminance(Color.White), delta)
    }

    @Test
    fun `pure red, green and blue use their channel weights`() {
        // For a pure primary colour only one channel is lit (linear value 1.0),
        // so the luminance equals that channel's weight.
        assertEquals(0.2126, relativeLuminance(Color(0xFFFF0000)), 0.0001)
        assertEquals(0.7152, relativeLuminance(Color(0xFF00FF00)), 0.0001)
        assertEquals(0.0722, relativeLuminance(Color(0xFF0000FF)), 0.0001)
    }

    @Test
    fun `mid grey uses the power curve branch`() {
        // #808080: channel 0.502 → ((0.502 + 0.055) / 1.055)^2.4 ≈ 0.2159
        assertEquals(0.2159, relativeLuminance(Color(0xFF808080)), 0.0005)
    }

    @Test
    fun `very dark grey uses the linear branch`() {
        // #0A0A0A: channel 10/255 ≈ 0.0392 ≤ 0.04045 → 0.0392 / 12.92 ≈ 0.003035
        assertEquals(0.003035, relativeLuminance(Color(0xFF0A0A0A)), 0.00001)
    }

    @Test
    fun `channel just above the linear threshold uses the power curve`() {
        // #0B0B0B: channel 11/255 ≈ 0.0431, just above 0.04045, so it must go
        // through the power curve (≈ 0.003347), not the linear branch (≈ 0.003339).
        assertEquals(0.003347, relativeLuminance(Color(0xFF0B0B0B)), 0.000002)
    }

    // ── contrastRatio ─────────────────────────────────────────────────────────

    @Test
    fun `black on white is 21 to 1`() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), delta)
    }

    @Test
    fun `identical colours are 1 to 1`() {
        assertEquals(1.0, contrastRatio(Color(0xFF1F4D3A), Color(0xFF1F4D3A)), delta)
    }

    @Test
    fun `argument order does not matter`() {
        val felt = Color(0xFF1F4D3A)
        val ivory = Color(0xFFF6F1E7)
        assertEquals(contrastRatio(felt, ivory), contrastRatio(ivory, felt), 0.0)
    }

    @Test
    fun `mid grey on white matches the reference value`() {
        // WebAIM reports 3.95:1 for #808080 on #FFFFFF.
        assertEquals(3.95, contrastRatio(Color(0xFF808080), Color.White), delta)
    }

    @Test
    fun `felt green on ivory matches the reference value`() {
        // Salon primary on the page background: 8.55:1.
        assertEquals(8.55, contrastRatio(Color(0xFF1F4D3A), Color(0xFFF6F1E7)), delta)
    }
}

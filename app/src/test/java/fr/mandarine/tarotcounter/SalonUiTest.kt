package fr.mandarine.tarotcounter

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure logic behind the Salon shared components (SalonUi.kt).
 */
class SalonUiTest {

    // ── playerInitial ─────────────────────────────────────────────────────────

    @Test
    fun `initial is the upper-cased first letter`() {
        assertEquals("A", playerInitial("alice"))
        assertEquals("B", playerInitial("Bruno"))
    }

    @Test
    fun `surrounding spaces are ignored`() {
        assertEquals("C", playerInitial("   chloé  "))
    }

    @Test
    fun `blank or empty name gives a question mark`() {
        assertEquals("?", playerInitial(""))
        assertEquals("?", playerInitial("    "))
    }

    @Test
    fun `accented precomposed letter is kept and upper-cased`() {
        assertEquals("É", playerInitial("élodie"))
    }

    @Test
    fun `letter with a combining accent is kept whole`() {
        // "e" + U+0301 (combining acute accent) is two Chars but one grapheme.
        val decomposed = "émile"
        assertEquals("É", playerInitial(decomposed))
    }

    @Test
    fun `single-letter name returns that letter`() {
        assertEquals("Z", playerInitial("z"))
    }

    @Test
    fun `surrogate pair is not cut in half`() {
        // U+1D49C (mathematical script A) is stored as two UTF-16 Chars.
        val scriptA = "𝒜bc"
        assertEquals("𝒜", playerInitial(scriptA))
    }

    @Test
    fun `default player names show their number`() {
        assertEquals("3", playerInitial("Player 3"))
        assertEquals("12", playerInitial("Joueur  12"))
    }

    @Test
    fun `a name that is only a number keeps its first character`() {
        // One word: no "last word is a number" rule, just the first grapheme.
        assertEquals("4", playerInitial("42"))
    }

    @Test
    fun `a trailing word with letters is not treated as a number`() {
        assertEquals("A", playerInitial("Alice B2"))
    }

    // ── AvatarSize ────────────────────────────────────────────────────────────

    @Test
    fun `avatar diameters match the design`() {
        assertEquals(24.dp, AvatarSize.S.diameter)
        assertEquals(36.dp, AvatarSize.M.diameter)
        assertEquals(56.dp, AvatarSize.L.diameter)
    }

    @Test
    fun `avatar initials grow with the circle`() {
        assertTrue(AvatarSize.S.fontSizeSp < AvatarSize.M.fontSizeSp)
        assertTrue(AvatarSize.M.fontSizeSp < AvatarSize.L.fontSizeSp)
    }

    @Test
    fun `stack overlap is a third of the diameter`() {
        assertEquals(8.dp, AvatarSize.S.stackOverlap)
        assertEquals(12.dp, AvatarSize.M.stackOverlap)
    }

    @Test
    fun `small avatars get a thinner stack ring`() {
        assertEquals(1.5.dp, AvatarSize.S.stackRing)
        assertEquals(2.dp, AvatarSize.M.stackRing)
        assertEquals(2.dp, AvatarSize.L.stackRing)
    }

    // ── ScoreSize ─────────────────────────────────────────────────────────────

    @Test
    fun `score sizes grow from S to XL`() {
        assertEquals(15f, ScoreSize.S.fontSizeSp)
        assertEquals(17f, ScoreSize.M.fontSizeSp)
        assertEquals(30f, ScoreSize.XL.fontSizeSp)
    }

    // ── SUIT_GLYPHS ───────────────────────────────────────────────────────────

    @Test
    fun `suit glyphs are the four suits forced to text presentation`() {
        assertEquals("♠︎ ♥︎ ♦︎ ♣︎", SUIT_GLYPHS)
    }

    // ── Top bar actions ───────────────────────────────────────────────────────

    private val dummyIcon = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).build()

    private fun action(label: String = "a") = TopBarAction(dummyIcon, label) {}

    @Test
    fun `zero to two actions are accepted`() {
        requireValidTopBarActions(emptyList())
        requireValidTopBarActions(listOf(action()))
        requireValidTopBarActions(listOf(action(), action()))
    }

    @Test
    fun `three actions are rejected`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            requireValidTopBarActions(listOf(action(), action(), action()))
        }
        assertEquals("SalonTopBar supports at most 2 actions, got 3", error.message)
    }

    @Test
    fun `top bar action keeps its fields`() {
        var clicked = false
        val onClick = { clicked = true }
        val a = TopBarAction(dummyIcon, "Settings", onClick)
        assertSame(dummyIcon, a.icon)
        assertEquals("Settings", a.contentDescription)
        a.onClick()
        assertTrue(clicked)
    }

    // ── Strings ───────────────────────────────────────────────────────────────

    @Test
    fun `avatar description is localized`() {
        assertEquals("Player Alice", appStrings(AppLocale.EN).playerAvatar("Alice"))
        assertEquals("Joueur Alice", appStrings(AppLocale.FR).playerAvatar("Alice"))
    }
}

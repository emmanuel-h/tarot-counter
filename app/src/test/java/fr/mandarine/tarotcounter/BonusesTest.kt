package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the bonus sheet logic in Bonuses.kt (issue #200). */
class BonusesTest {

    private val players = listOf("Alice", "Bob", "Chloé", "David")

    // ── PoigneeDeclarations ───────────────────────────────────────────────────

    @Test
    fun `nobody declared by default`() {
        val none = PoigneeDeclarations()
        players.forEach { assertEquals(PoigneeLevel.NONE, none.levelOf(it)) }
        assertEquals(emptyList<String>(), none.declarants(players))
    }

    @Test
    fun `levelOf reads each set`() {
        val d = PoigneeDeclarations(simple = setOf("Alice"), double = setOf("Bob"), triple = setOf("Chloé"))
        assertEquals(PoigneeLevel.SIMPLE, d.levelOf("Alice"))
        assertEquals(PoigneeLevel.DOUBLE, d.levelOf("Bob"))
        assertEquals(PoigneeLevel.TRIPLE, d.levelOf("Chloé"))
        assertEquals(PoigneeLevel.NONE, d.levelOf("David"))
    }

    @Test
    fun `withLevel moves a player between levels`() {
        val d = PoigneeDeclarations()
            .withLevel("Alice", PoigneeLevel.DOUBLE)
            .withLevel("Alice", PoigneeLevel.TRIPLE)
        assertEquals(PoigneeDeclarations(triple = setOf("Alice")), d)
    }

    @Test
    fun `withLevel to simple then none clears the player`() {
        val simple = PoigneeDeclarations().withLevel("Bob", PoigneeLevel.SIMPLE)
        assertEquals(PoigneeDeclarations(simple = setOf("Bob")), simple)
        assertEquals(PoigneeDeclarations(), simple.withLevel("Bob", PoigneeLevel.NONE))
    }

    @Test
    fun `withLevel keeps other players`() {
        val d = PoigneeDeclarations(simple = setOf("Alice"))
            .withLevel("Bob", PoigneeLevel.DOUBLE)
        assertEquals(setOf("Alice"), d.simple)
        assertEquals(setOf("Bob"), d.double)
        assertTrue(d.triple.isEmpty())
    }

    @Test
    fun `declarants follow seat order`() {
        val d = PoigneeDeclarations(simple = setOf("David"), triple = setOf("Bob"))
        assertEquals(listOf("Bob", "David"), d.declarants(players))
    }

    // ── chelemCandidates ──────────────────────────────────────────────────────

    @Test
    fun `only the taker calls a chelem below five players`() {
        assertEquals(listOf("Alice"), chelemCandidates("Alice", "Bob", 4))
        assertEquals(listOf("Alice"), chelemCandidates("Alice", null, 3))
    }

    @Test
    fun `the partner may also call it with five players`() {
        assertEquals(listOf("Alice", "Bob"), chelemCandidates("Alice", "Bob", 5))
        assertEquals(listOf("Alice"), chelemCandidates("Alice", null, 5))
    }

    // ── isAnnouncedChelem ─────────────────────────────────────────────────────

    @Test
    fun `announced chelems are detected`() {
        assertTrue(isAnnouncedChelem(Chelem.ANNOUNCED_REALIZED))
        assertTrue(isAnnouncedChelem(Chelem.ANNOUNCED_NOT_REALIZED))
        assertFalse(isAnnouncedChelem(Chelem.NONE))
        assertFalse(isAnnouncedChelem(Chelem.NOT_ANNOUNCED_REALIZED))
        assertFalse(isAnnouncedChelem(Chelem.DEFENDERS_REALIZED))
    }

    // ── Strings ───────────────────────────────────────────────────────────────

    @Test
    fun `poignee explanation uses the thresholds of the player count`() {
        assertTrue(appStrings(AppLocale.EN).poigneeExplain(4).contains("simple 10, double 13, triple 15"))
        assertTrue(appStrings(AppLocale.FR).poigneeExplain(5).contains("simple 8, double 10, triple 13"))
        assertEquals("Done", appStrings(AppLocale.EN).done)
        assertEquals("Bonus", appStrings(AppLocale.FR).bonusesLabel)
    }
}

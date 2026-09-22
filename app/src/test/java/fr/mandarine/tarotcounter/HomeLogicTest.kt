package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Unit tests for the pure logic behind the Salon home screen (issue #197):
 * [currentLeaders], [formatGameDate], [javaLocale] and the new strings.
 */
class HomeLogicTest {

    private val players = listOf("Alice", "Bob", "Chloé")

    private fun round(n: Int, scores: Map<String, Int>) =
        RoundResult(n, "Alice", if (scores.isEmpty()) null else Contract.GARDE, null,
            if (scores.isEmpty()) null else true, scores)

    // ── currentLeaders ────────────────────────────────────────────────────────

    @Test
    fun `no rounds means no leader`() {
        assertNull(currentLeaders(players, emptyList()))
    }

    @Test
    fun `only skipped rounds means no leader`() {
        assertNull(currentLeaders(players, listOf(round(1, emptyMap()), round(2, emptyMap()))))
    }

    @Test
    fun `single leader with cumulative score`() {
        val rounds = listOf(
            round(1, mapOf("Alice" to 100, "Bob" to -50, "Chloé" to -50)),
            round(2, emptyMap()),
            round(3, mapOf("Alice" to -40, "Bob" to 80, "Chloé" to -40))
        )
        assertEquals(Leaders(listOf("Alice"), 60), currentLeaders(players, rounds))
    }

    @Test
    fun `tied leaders are all returned`() {
        val rounds = listOf(round(1, mapOf("Alice" to 20, "Bob" to 20, "Chloé" to -40)))
        assertEquals(Leaders(listOf("Alice", "Bob"), 20), currentLeaders(players, rounds))
    }

    @Test
    fun `leader can have a negative score`() {
        val rounds = listOf(round(1, mapOf("Alice" to -10, "Bob" to -10, "Chloé" to -20)))
        assertEquals(Leaders(listOf("Alice", "Bob"), -10), currentLeaders(players, rounds))
    }

    // ── formatGameDate ────────────────────────────────────────────────────────

    private val utc = TimeZone.getTimeZone("UTC")

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, 0)
        }.timeInMillis

    @Test
    fun `same year omits the year in english`() {
        val date = millis(2026, Calendar.SEPTEMBER, 13)
        val now  = millis(2026, Calendar.DECEMBER, 31)
        assertEquals("Sun 13 Sep", formatGameDate(date, AppLocale.EN, now, utc))
    }

    @Test
    fun `same year in french uses french names`() {
        val date = millis(2026, Calendar.SEPTEMBER, 13)
        val now  = millis(2026, Calendar.SEPTEMBER, 22)
        assertEquals("dim. 13 sept.", formatGameDate(date, AppLocale.FR, now, utc))
    }

    @Test
    fun `other year appends the year`() {
        val date = millis(2025, Calendar.SEPTEMBER, 13)
        val now  = millis(2026, Calendar.JANUARY, 1)
        assertEquals("Sat 13 Sep 2025", formatGameDate(date, AppLocale.EN, now, utc))
    }

    @Test
    fun `year boundary is computed in the given time zone`() {
        // 31 Dec 2025 23:30 UTC is already 1 Jan 2026 in Paris.
        val paris = TimeZone.getTimeZone("Europe/Paris")
        val date  = millis(2025, Calendar.DECEMBER, 31, hour = 23)
        val now   = millis(2026, Calendar.JUNE, 1)
        assertEquals("Thu 1 Jan", formatGameDate(date, AppLocale.EN, now, paris))
        assertEquals("Wed 31 Dec 2025", formatGameDate(date, AppLocale.EN, now, utc))
    }

    @Test
    fun `default parameters use the clock and device zone`() {
        val now = System.currentTimeMillis()
        // Same instant as "now" → never shows a year.
        assertEquals(
            formatGameDate(now, AppLocale.EN, now, TimeZone.getDefault()),
            formatGameDate(now, AppLocale.EN)
        )
    }

    // ── javaLocale ────────────────────────────────────────────────────────────

    @Test
    fun `app locales map to java locales`() {
        assertEquals(Locale.ENGLISH, AppLocale.EN.javaLocale)
        assertEquals(Locale.FRENCH, AppLocale.FR.javaLocale)
    }

    // ── Strings ───────────────────────────────────────────────────────────────

    @Test
    fun `leader line handles one leader and ties`() {
        val en = appStrings(AppLocale.EN)
        val fr = appStrings(AppLocale.FR)
        assertEquals("Alice leads +312", en.leaderLine(listOf("Alice"), "+312"))
        assertEquals("Alice & Bob lead +20", en.leaderLine(listOf("Alice", "Bob"), "+20"))
        assertEquals("Alice mène avec +312", fr.leaderLine(listOf("Alice"), "+312"))
        assertEquals("Alice & Bob mènent avec +20", fr.leaderLine(listOf("Alice", "Bob"), "+20"))
    }

    @Test
    fun `player count and labels are localized`() {
        assertEquals("4 players", appStrings(AppLocale.EN).playerCount(4))
        assertEquals("4 joueurs", appStrings(AppLocale.FR).playerCount(4))
        assertEquals("Game in progress", appStrings(AppLocale.EN).gameInProgress)
        assertEquals("Partie en cours", appStrings(AppLocale.FR).gameInProgress)
        assertEquals("Players", appStrings(AppLocale.EN).playersLabel)
        assertEquals("Joueurs", appStrings(AppLocale.FR).playersLabel)
    }
}

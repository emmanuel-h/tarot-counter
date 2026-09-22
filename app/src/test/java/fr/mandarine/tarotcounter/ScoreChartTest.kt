package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for the score-over-time chart data (ScoreChart.kt, issue #201). */
class ScoreChartTest {

    private val players = listOf("Alice", "Bob", "Chloé")

    private fun played(vararg scores: Pair<String, Int>) =
        RoundResult(1, "Alice", Contract.GARDE, null, true, mapOf(*scores))
    private val skipped = RoundResult(2, "Alice", null, null, null)

    // ── cumulativeSeries ──────────────────────────────────────────────────────

    @Test
    fun `series start at zero and accumulate, skipped rounds repeat`() {
        val rounds = listOf(
            played("Alice" to 50, "Bob" to -25, "Chloé" to -25),
            skipped,
            played("Alice" to -30, "Bob" to 60, "Chloé" to -30)
        )
        assertEquals(
            listOf(
                listOf(0, 50, 50, 20),
                listOf(0, -25, -25, 35),
                listOf(0, -25, -25, -55)
            ),
            cumulativeSeries(players, rounds)
        )
    }

    @Test
    fun `no rounds gives a single zero point per player`() {
        assertEquals(listOf(listOf(0), listOf(0), listOf(0)), cumulativeSeries(players, emptyList()))
    }

    // ── chartBounds ───────────────────────────────────────────────────────────

    @Test
    fun `bounds span the lowest and highest values`() {
        assertEquals(-55 to 50, chartBounds(listOf(listOf(0, 50, 20), listOf(0, -55))))
    }

    @Test
    fun `zero is always inside the bounds`() {
        assertEquals(0 to 80, chartBounds(listOf(listOf(10, 80))))
        assertEquals(-40 to 0, chartBounds(listOf(listOf(-10, -40))))
    }

    @Test
    fun `a flat chart gets a non-empty range`() {
        assertEquals(-1 to 1, chartBounds(listOf(listOf(0, 0), listOf(0))))
        assertEquals(-1 to 1, chartBounds(emptyList()))
    }

    // ── xAxisLabels ───────────────────────────────────────────────────────────

    @Test
    fun `few rounds - every round is labelled`() {
        assertEquals(emptyList<Int>(), xAxisLabels(0))
        assertEquals(listOf(1), xAxisLabels(1))
        assertEquals(listOf(1, 2, 3, 4, 5, 6), xAxisLabels(6))
    }

    @Test
    fun `many rounds - stepped labels ending on the last round`() {
        assertEquals(listOf(1, 3, 5, 7), xAxisLabels(7))
        assertEquals(listOf(1, 3, 5, 7, 10), xAxisLabels(10))
        assertEquals(listOf(1, 5, 9, 13, 17, 20), xAxisLabels(20))
    }

    @Test
    fun `labels never exceed the maximum`() {
        for (n in 1..60) {
            val labels = xAxisLabels(n)
            assert(labels.size <= MAX_X_LABELS) { "$n rounds gave ${labels.size} labels" }
            assertEquals(n, labels.last())
            assertEquals(1, labels.first())
        }
    }

    // ── Strings ───────────────────────────────────────────────────────────────

    @Test
    fun `game over strings are localized`() {
        assertEquals("Score over time", appStrings(AppLocale.EN).scoreOverTime)
        assertEquals("Évolution des scores", appStrings(AppLocale.FR).scoreOverTime)
        assertEquals("See all rounds", appStrings(AppLocale.EN).seeAllRounds)
        assertEquals(
            "Line chart of every player's cumulative score over 8 rounds",
            appStrings(AppLocale.EN).scoreChartDescription(8)
        )
    }
}

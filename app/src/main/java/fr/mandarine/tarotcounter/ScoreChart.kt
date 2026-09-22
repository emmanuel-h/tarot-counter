package fr.mandarine.tarotcounter

import kotlin.math.ceil

// ─────────────────────────────────────────────────────────────────────────────
// Pure data behind the "Score over time" line chart of the game-over screen
// (issue #201). The chart itself is drawn with a Compose Canvas in
// FinalScoreScreen.kt; everything that can be computed without drawing lives
// here and is unit-tested in ScoreChartTest.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Each player's cumulative score after every round, in [playerNames] order.
 *
 * Every list starts with 0 (before round 1) and has `rounds.size + 1` points, so
 * point `i` is the total after round `i`. Skipped rounds repeat the previous total.
 *
 * Example: rounds +50 then −30 for Alice → Alice: [0, 50, 20].
 */
fun cumulativeSeries(playerNames: List<String>, rounds: List<RoundResult>): List<List<Int>> =
    playerNames.map { name ->
        // runningFold emits the start value, then the running sum after each round.
        rounds.runningFold(0) { total, round -> total + round.playerScores.getOrDefault(name, 0) }
    }

/**
 * The lowest and highest values of the y axis. Zero is always included so the
 * baseline can be drawn; a flat chart (every value 0) gets −1..1 so the axis
 * never has zero height (which would divide by zero when scaling).
 */
fun chartBounds(series: List<List<Int>>): Pair<Int, Int> {
    val values = series.flatten()
    val low  = minOf(0, values.minOrNull() ?: 0)
    val high = maxOf(0, values.maxOrNull() ?: 0)
    return if (low == high) -1 to 1 else low to high
}

/** At most this many round numbers are printed under the x axis. */
const val MAX_X_LABELS = 6

/**
 * The round numbers printed under the x axis: every round when there are few,
 * otherwise one every few rounds — always including the first and the last.
 */
fun xAxisLabels(roundCount: Int): List<Int> {
    if (roundCount <= 0) return emptyList()
    if (roundCount <= MAX_X_LABELS) return (1..roundCount).toList()
    // ceil keeps the number of labels at or below MAX_X_LABELS.
    val step = ceil(roundCount / (MAX_X_LABELS - 1).toDouble()).toInt()
    // Drop a stepped label that would sit right next to the last one.
    val stepped = (1 until roundCount step step).filter { roundCount - it >= step / 2 + 1 }
    return stepped + roundCount
}

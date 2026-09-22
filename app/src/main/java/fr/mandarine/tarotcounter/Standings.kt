package fr.mandarine.tarotcounter

// ─────────────────────────────────────────────────────────────────────────────
// Pure logic behind the Salon game screen (issue #198): the ranked standings,
// the layout of the "Who took?" tiles and the "Last rounds" mini log.
// No Compose here, so everything is unit-tested on the JVM (StandingsTest).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * One line of the standings card.
 *
 * @property name      The player's display name.
 * @property seatIndex 0-based seat — picks the avatar colour.
 * @property total     Cumulative score so far.
 * @property rank      1-based rank. Tied players share a rank and the next rank is
 *                     skipped ("competition ranking": 1, 1, 3, 4).
 * @property isLeader  True for every player sharing the best total, once at least
 *                     one round has been scored.
 * @property lastDelta Score change in the most recent round, or null when there is
 *                     no round yet or the last round was skipped. Drives the small
 *                     up/down trend arrow.
 */
data class Standing(
    val name: String,
    val seatIndex: Int,
    val total: Int,
    val rank: Int,
    val isLeader: Boolean,
    val lastDelta: Int?
)

/**
 * Builds the standings, best total first. Players with equal totals keep their
 * seat order, so the list never "jumps" between two tied players.
 */
fun computeStandings(playerNames: List<String>, rounds: List<RoundResult>): List<Standing> {
    val totals = computeFinalTotals(playerNames, rounds)
    // Before any scored round everybody is at 0: nobody is "leading" yet.
    val anyScored = rounds.any { it.playerScores.isNotEmpty() }
    val best      = totals.values.maxOrNull()
    // The last round's per-player scores; empty (→ null deltas) when it was skipped.
    val lastScores = rounds.lastOrNull()?.playerScores.orEmpty()

    return playerNames
        .mapIndexed { seat, name ->
            val total = totals.getValue(name)
            Standing(
                name      = name,
                seatIndex = seat,
                total     = total,
                // Rank = 1 + number of players strictly ahead of this one.
                rank      = 1 + totals.values.count { it > total },
                isLeader  = anyScored && total == best,
                lastDelta = lastScores[name]
            )
        }
        // sortedWith is stable: equal totals keep their seat order.
        .sortedWith(compareByDescending { it.total })
}

/**
 * Number of columns of the "Who took?" tile grid:
 * 3 players → one row of 3, 4 players → 2 × 2, 5 players → 3 + 2.
 */
fun takerGridColumns(playerCount: Int): Int = if (playerCount == 4) 2 else 3

/** How many rounds the "Last rounds" mini log shows. */
const val LAST_ROUNDS_COUNT = 3

/** The latest [count] rounds, newest first, for the "Last rounds" mini log. */
fun lastRounds(rounds: List<RoundResult>, count: Int = LAST_ROUNDS_COUNT): List<RoundResult> =
    rounds.takeLast(count).asReversed()

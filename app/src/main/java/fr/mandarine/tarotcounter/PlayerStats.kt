package fr.mandarine.tarotcounter

// ─────────────────────────────────────────────────────────────────────────────
// Player statistics across saved games (issue #229).
//
// Pure Kotlin (no Android / Compose imports), so everything here is unit-tested
// on the JVM in PlayerStatsTest. The Statistics screen only displays the result.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * How often one player took one contract, and how often they made it.
 *
 * @property taken How many rounds the player took with this contract.
 * @property won   How many of those rounds the player made (won).
 */
data class ContractStats(val taken: Int, val won: Int) {
    /** Success rate in percent (0–100), or null when the contract was never taken. */
    val successPercent: Int? get() = percentOrNull(won, taken)
}

/**
 * Everything the Statistics screen shows for one player.
 *
 * @property name         Display name (the spelling from the most recent game).
 * @property gamesPlayed  Saved games (with at least one scored round) the player sat in.
 * @property wins         Games the player finished first in (a tie counts for every co-winner).
 * @property roundsPlayed Scored (not skipped) rounds of those games.
 * @property roundsTaken  Rounds in which the player was the taker.
 * @property totalScore   Sum of the player's final scores over all their games.
 * @property contracts    One entry per [Contract], in the enum's order (Prise → Garde Contre).
 */
data class PlayerStats(
    val name: String,
    val gamesPlayed: Int,
    val wins: Int,
    val roundsPlayed: Int,
    val roundsTaken: Int,
    val totalScore: Int,
    val contracts: Map<Contract, ContractStats>
) {
    /** Share of rounds taken, in percent (0–100), or null when no round was played. */
    val takePercent: Int? get() = percentOrNull(roundsTaken, roundsPlayed)

    /** Share of games won, in percent (0–100), or null when no game was played. */
    val winPercent: Int? get() = percentOrNull(wins, gamesPlayed)

    /**
     * Average final score per game, rounded to the nearest integer
     * (0 when no game was played — never shown in practice).
     */
    val averageScore: Int
        get() = if (gamesPlayed == 0) 0 else Math.round(totalScore.toDouble() / gamesPlayed).toInt()
}

/**
 * Returns `part / whole` as a rounded percentage, or null when `whole` is 0
 * (there is nothing to divide by, so the UI shows "–" instead of a number).
 */
fun percentOrNull(part: Int, whole: Int): Int? =
    if (whole == 0) null else Math.round(part * 100.0 / whole).toInt()

/**
 * The key used to recognise the same player across games: names are compared
 * ignoring case and surrounding spaces, so "Alice", "alice" and " ALICE " are
 * one person. Different spellings ("Alice" / "Alicia") stay different players.
 */
fun playerKey(name: String): String = name.trim().lowercase()

/**
 * Computes the statistics of every player found in [games].
 *
 * Rules:
 * - Games without any scored round (no rounds, or only skipped ones) are ignored:
 *   they have no winner and no taker.
 * - Skipped rounds never count as played or taken.
 * - A win is finishing with the best final total ([findWinners]); on a tie
 *   every co-winner gets the win.
 * - The displayed name is the spelling of the first game in [games] that contains
 *   the player. Saved games are stored newest first, so this is the latest spelling.
 *
 * The result is sorted by wins (most first), then games played, then name.
 */
fun computePlayerStats(games: List<SavedGame>): List<PlayerStats> {
    // A small mutable accumulator per player; turned into an immutable PlayerStats at the end.
    class Acc(val name: String) {
        var games = 0
        var wins = 0
        var rounds = 0
        var taken = 0
        var total = 0
        // One [taken, won] pair per contract. IntArray is a fixed-size array of ints.
        val perContract = Contract.entries.associateWith { IntArray(2) }
    }

    // LinkedHashMap keeps insertion order, which makes the name tie-breaker stable.
    val accs = LinkedHashMap<String, Acc>()

    for (game in games) {
        // Only rounds with a contract were actually played and scored.
        val scored = game.rounds.filter { it.contract != null }
        if (scored.isEmpty()) continue

        // Recompute totals from the rounds so the stats never disagree with the score sheet.
        val totals  = computeFinalTotals(game.playerNames, game.rounds)
        val winners = findWinners(totals).map(::playerKey).toSet()

        // `distinctBy` guards against a (malformed) game listing the same name twice.
        for (name in game.playerNames.distinctBy(::playerKey)) {
            val key = playerKey(name)
            // `getOrPut` returns the existing accumulator or creates and stores a new one.
            val acc = accs.getOrPut(key) { Acc(name.trim()) }
            acc.games++
            if (key in winners) acc.wins++
            acc.rounds += scored.size
            acc.total  += totals[name] ?: 0
        }

        for (round in scored) {
            val acc = accs[playerKey(round.takerName)] ?: continue
            // `!!` is safe: `scored` only contains rounds whose contract is non-null.
            val counts = acc.perContract.getValue(round.contract!!)
            acc.taken++
            counts[0]++
            if (round.won == true) counts[1]++
        }
    }

    return accs.values
        .map { acc ->
            PlayerStats(
                name         = acc.name,
                gamesPlayed  = acc.games,
                wins         = acc.wins,
                roundsPlayed = acc.rounds,
                roundsTaken  = acc.taken,
                totalScore   = acc.total,
                contracts    = acc.perContract.mapValues { (_, c) -> ContractStats(c[0], c[1]) }
            )
        }
        // compareByDescending/thenBy build a multi-key comparator, applied left to right.
        .sortedWith(
            compareByDescending<PlayerStats> { it.wins }
                .thenByDescending { it.gamesPlayed }
                .thenBy { it.name.lowercase() }
        )
}

/** How many saved games actually feed the statistics (those with a scored round). */
fun countedGames(games: List<SavedGame>): Int =
    games.count { game -> game.rounds.any { it.contract != null } }

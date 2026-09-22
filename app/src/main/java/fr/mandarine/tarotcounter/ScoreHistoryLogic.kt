package fr.mandarine.tarotcounter

// ─────────────────────────────────────────────────────────────────────────────
// Pure layout decisions of the Score history table (issue #202).
// No Compose here: unit-tested in ScoreHistoryLogicTest.
// ─────────────────────────────────────────────────────────────────────────────

/** Width of the "Round" column of the history table, in dp. */
const val HISTORY_ROUND_COLUMN_DP = 48f

/** Narrowest a player column may get before the table scrolls sideways, in dp. */
const val HISTORY_MIN_PLAYER_COLUMN_DP = 64f

/**
 * True when the table does not fit in [availableWidthDp]: the player columns would
 * be narrower than [HISTORY_MIN_PLAYER_COLUMN_DP]. The table then keeps that width
 * per column and scrolls horizontally (5 players on a narrow phone).
 */
fun historyTableScrolls(availableWidthDp: Float, playerCount: Int): Boolean =
    HISTORY_ROUND_COLUMN_DP + playerCount * HISTORY_MIN_PLAYER_COLUMN_DP > availableWidthDp

/**
 * The players whose column is tinted brass: the current leader(s), or nobody
 * before the first scored round.
 */
fun leaderColumns(playerNames: List<String>, rounds: List<RoundResult>): Set<String> =
    currentLeaders(playerNames, rounds)?.names?.toSet().orEmpty()

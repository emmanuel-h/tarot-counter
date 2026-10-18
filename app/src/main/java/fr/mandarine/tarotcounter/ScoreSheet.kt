package fr.mandarine.tarotcounter

// ─────────────────────────────────────────────────────────────────────────────
// Shareable score sheet (issue #234).
//
// Pure Kotlin: builds the content of the score sheet once (ScoreSheet), then
// formats it as plain text (scoreSheetText) or computes where everything goes on
// the shared PNG (ScoreSheetLayout). The Android drawing and the share intent
// live in ScoreSheetShare.kt; everything here is unit-tested in ScoreSheetTest.
// ─────────────────────────────────────────────────────────────────────────────

/** The two formats offered by the Share action of the Game Over screen. */
enum class ShareFormat { TEXT, IMAGE }

/**
 * One line of the round-by-round part: a short description of the round and
 * the running totals after it.
 *
 * @property label   "R1 · Alice · Garde · Won" or "R3 · Skipped".
 * @property badge   Just the round badge, "R1" — the first column of the image table.
 * @property totals  Running total of every player after this round, in seat order.
 */
data class ScoreSheetRound(val label: String, val badge: String, val totals: List<Int>)

/**
 * Everything printed on a score sheet, already localized.
 *
 * @property title       "Tarot Counter — Game Over".
 * @property winnerLine  "Winner: Alice (+428)" or "It's a tie!: Alice & Bob (+20)".
 * @property detailLine  "Sat 13 Sep · 8 rounds · 4 players".
 * @property ranking     Final standings, best first (competition ranks).
 * @property playerNames Players in seat order (the column order of the table).
 * @property rounds      One entry per round, oldest first.
 */
data class ScoreSheet(
    val title: String,
    val winnerLine: String,
    val detailLine: String,
    val rankingTitle: String,
    val ranking: List<Standing>,
    val roundsTitle: String,
    val playerNames: List<String>,
    val rounds: List<ScoreSheetRound>
)

/**
 * Builds the score sheet of a finished game.
 *
 * @param dateText The game date, already formatted ([formatGameDate]); passed in
 *                 so tests do not depend on the clock or the time zone.
 */
fun buildScoreSheet(
    playerNames: List<String>,
    roundHistory: List<RoundResult>,
    locale: AppLocale,
    dateText: String
): ScoreSheet {
    val strings  = appStrings(locale)
    val totals   = computeFinalTotals(playerNames, roundHistory)
    val winners  = findWinners(totals)
    val best     = winners.firstOrNull()?.let { totals.getValue(it) } ?: 0

    // Running totals after each round; cumulativeSeries starts with a 0 per player.
    val series = cumulativeSeries(playerNames, roundHistory)

    val rounds = roundHistory.mapIndexed { i, round ->
        // A played round reads "Alice · Garde · Won"; a skipped one "Skipped".
        val description = if (round.contract == null) strings.skipped else listOf(
            round.takerName,
            round.contract.localizedName(locale),
            if (round.won == true) strings.wonShort else strings.lostShort
        ).joinToString(" · ")
        ScoreSheetRound(
            label  = "${strings.roundBadge(round.roundNumber)} · $description",
            badge  = strings.roundBadge(round.roundNumber),
            totals = series.map { it[i + 1] }
        )
    }

    return ScoreSheet(
        title        = "${strings.appTitle} — ${strings.gameOver}",
        // One winner: "Winner: Alice (+150)"; a tie: "Tie: Alice & Bob (+20)".
        winnerLine   = if (winners.size == 1) strings.shareWinner(winners.first(), best.withSign())
                       else "${strings.tieResult(winners.joinToString(" & "))} (${best.withSign()})",
        detailLine   = listOf(
            dateText,
            strings.roundCount(roundHistory.size),
            strings.playerCount(playerNames.size)
        ).joinToString(" · "),
        rankingTitle = strings.shareRankingTitle,
        ranking      = computeStandings(playerNames, roundHistory),
        roundsTitle  = strings.shareRoundsTitle,
        playerNames  = playerNames,
        rounds       = rounds
    )
}

/**
 * Formats the score sheet as plain text, for messaging apps:
 *
 * ```
 * Tarot Counter — Game Over
 * Winner: Alice (+150)
 * Sat 13 Sep · 2 rounds · 3 players
 *
 * Final ranking
 * 1. Alice +150
 * 2. Chloé -30
 *
 * Round by round
 * R1 · Alice · Garde · Won → Alice +120 · Bob -60 · Chloé -60
 * ```
 *
 * Messaging apps use proportional fonts, so the text uses one line per round
 * instead of a column-aligned table.
 */
fun scoreSheetText(sheet: ScoreSheet): String = buildString {
    appendLine(sheet.title)
    appendLine(sheet.winnerLine)
    appendLine(sheet.detailLine)
    appendLine()
    appendLine(sheet.rankingTitle)
    for (s in sheet.ranking) appendLine("${s.rank}. ${s.name} ${s.total.withSign()}")
    if (sheet.rounds.isNotEmpty()) {
        appendLine()
        appendLine(sheet.roundsTitle)
        for (round in sheet.rounds) {
            // zip pairs each name with its total: ("Alice", 120) → "Alice +120".
            val totals = sheet.playerNames.zip(round.totals) { name, t -> "$name ${t.withSign()}" }
            appendLine("${round.label} → ${totals.joinToString(" · ")}")
        }
    }
}.trimEnd()

// ── Image layout ──────────────────────────────────────────────────────────────

/**
 * Pixel geometry of the shared PNG. Computed here (pure, testable) and only
 * *drawn* by ScoreSheetShare.kt.
 *
 * ```
 *  ┌──────────────────────────────── width ─┐
 *  │ title / winner / detail     (header)    │
 *  │ Final ranking                           │
 *  │ 1  Alice                          +150  │  rankingRows × rowHeight
 *  │ Round by round                          │
 *  │ #   │ Alice │ Bob  │ Chloé              │  table header row
 *  │ R1  │ +120  │ -60  │ -60                │  tableRows × rowHeight
 *  └─────────────────────────────────────────┘
 * ```
 */
data class ScoreSheetLayout(
    val width: Int,
    val height: Int,
    val margin: Int,
    val rowHeight: Int,
    val headerHeight: Int,
    val sectionTitleHeight: Int,
    /** Top of the first ranking row. */
    val rankingTop: Int,
    /** Top of the table header row (player names). */
    val tableTop: Int,
    /** Left x of each table column: the round column first, then one per player. */
    val columnLefts: List<Int>,
    /** Width of one player column. */
    val playerColumnWidth: Int
)

/** Width of the shared image, in pixels (a phone screen width, crisp in chats). */
const val SHEET_WIDTH = 1080
private const val SHEET_MARGIN = 64
private const val SHEET_ROW_HEIGHT = 64
private const val SHEET_HEADER_HEIGHT = 260
private const val SHEET_SECTION_TITLE_HEIGHT = 96
private const val SHEET_ROUND_COLUMN_WIDTH = 140

/**
 * Computes the layout for [playerCount] players and [roundCount] rounds.
 * The round-by-round table is left out (no section) when there are no rounds.
 */
fun scoreSheetLayout(playerCount: Int, roundCount: Int): ScoreSheetLayout {
    require(playerCount > 0) { "playerCount must be positive, got $playerCount" }
    val inner        = SHEET_WIDTH - 2 * SHEET_MARGIN
    val playerWidth  = (inner - SHEET_ROUND_COLUMN_WIDTH) / playerCount
    val rankingTop   = SHEET_HEADER_HEIGHT + SHEET_SECTION_TITLE_HEIGHT
    val rankingEnd   = rankingTop + playerCount * SHEET_ROW_HEIGHT
    val tableTop     = rankingEnd + SHEET_SECTION_TITLE_HEIGHT
    // The table has a header row plus one row per round.
    val height = if (roundCount == 0) rankingEnd + SHEET_MARGIN
                 else tableTop + (roundCount + 1) * SHEET_ROW_HEIGHT + SHEET_MARGIN
    val columns = listOf(SHEET_MARGIN) +
        (0 until playerCount).map { SHEET_MARGIN + SHEET_ROUND_COLUMN_WIDTH + it * playerWidth }
    return ScoreSheetLayout(
        width              = SHEET_WIDTH,
        height             = height,
        margin             = SHEET_MARGIN,
        rowHeight          = SHEET_ROW_HEIGHT,
        headerHeight       = SHEET_HEADER_HEIGHT,
        sectionTitleHeight = SHEET_SECTION_TITLE_HEIGHT,
        rankingTop         = rankingTop,
        tableTop           = tableTop,
        columnLefts        = columns,
        playerColumnWidth  = playerWidth
    )
}

/**
 * File name of the shared image, e.g. "tarot-score-sheet.png". Always the same
 * name, so sharing again overwrites the previous file instead of filling the cache.
 */
const val SCORE_SHEET_FILE_NAME = "tarot-score-sheet.png"

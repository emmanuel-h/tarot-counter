package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests for the shareable score sheet (ScoreSheet.kt, issue #234).
class ScoreSheetTest {

    private val names = listOf("Alice", "Bob", "Chloé")

    private val rounds = listOf(
        RoundResult(1, "Alice", Contract.GARDE, null, true,
            mapOf("Alice" to 120, "Bob" to -60, "Chloé" to -60)),
        RoundResult(2, "Bob", null, null, null),  // skipped
        RoundResult(3, "Bob", Contract.PRISE, null, false,
            mapOf("Alice" to 30, "Bob" to -60, "Chloé" to 30))
    )

    private fun sheet(locale: AppLocale = AppLocale.EN, history: List<RoundResult> = rounds) =
        buildScoreSheet(names, history, locale, dateText = "Sat 13 Sep")

    // ── buildScoreSheet ───────────────────────────────────────────────────────

    @Test fun `header lines in English`() {
        val s = sheet()
        assertEquals("Tarot Counter — Game Over", s.title)
        assertEquals("Winner: Alice (+150)", s.winnerLine)
        assertEquals("Sat 13 Sep · 3 rounds · 3 players", s.detailLine)
        assertEquals("Final ranking", s.rankingTitle)
        assertEquals("Round by round", s.roundsTitle)
    }

    @Test fun `header lines in French`() {
        val s = sheet(AppLocale.FR)
        assertEquals("Tarot — Fin de partie", s.title)
        assertEquals("Gagnant·e : Alice (+150)", s.winnerLine)
        assertEquals("Classement final", s.rankingTitle)
        assertEquals("Manche par manche", s.roundsTitle)
        assertEquals("M1 · Alice · Garde · Gagnée", s.rounds[0].label)
    }

    @Test fun `a tie lists every co-winner`() {
        val tie = listOf(
            RoundResult(1, "Alice", Contract.PRISE, null, false,
                mapOf("Alice" to -50, "Bob" to 25, "Chloé" to 25))
        )
        assertEquals("Tie: Bob & Chloé (+25)", sheet(history = tie).winnerLine)
    }

    @Test fun `ranking is best first`() {
        assertEquals(listOf("Alice", "Chloé", "Bob"), sheet().ranking.map { it.name })
        assertEquals(listOf(1, 2, 3), sheet().ranking.map { it.rank })
    }

    @Test fun `rounds carry labels and running totals`() {
        val r = sheet().rounds
        assertEquals(3, r.size)
        assertEquals("R1 · Alice · Guard · Won", r[0].label)
        assertEquals("R1", r[0].badge)
        assertEquals(listOf(120, -60, -60), r[0].totals)
        assertEquals("R2 · Skipped", r[1].label)
        assertEquals(listOf(120, -60, -60), r[1].totals)  // a skipped round changes nothing
        assertEquals("R3 · Bob · Small · Lost", r[2].label)
        assertEquals(listOf(150, -120, -30), r[2].totals)
        assertEquals(names, sheet().playerNames)
    }

    // ── scoreSheetText ────────────────────────────────────────────────────────

    @Test fun `text has every section`() {
        val expected = """
            Tarot Counter — Game Over
            Winner: Alice (+150)
            Sat 13 Sep · 3 rounds · 3 players

            Final ranking
            1. Alice +150
            2. Chloé -30
            3. Bob -120

            Round by round
            R1 · Alice · Guard · Won → Alice +120 · Bob -60 · Chloé -60
            R2 · Skipped → Alice +120 · Bob -60 · Chloé -60
            R3 · Bob · Small · Lost → Alice +150 · Bob -120 · Chloé -30
        """.trimIndent()
        assertEquals(expected, scoreSheetText(sheet()))
    }

    @Test fun `text without rounds has no round section`() {
        val text = scoreSheetText(sheet(history = emptyList()))
        assertFalse(text.contains("Round by round"))
        // Nobody scored: everyone shares rank 1.
        assertTrue(text.endsWith("1. Alice +0\n1. Bob +0\n1. Chloé +0"))
    }

    // ── scoreSheetLayout ──────────────────────────────────────────────────────

    @Test fun `layout geometry for 3 players and 2 rounds`() {
        val l = scoreSheetLayout(playerCount = 3, roundCount = 2)
        assertEquals(SHEET_WIDTH, l.width)
        assertEquals(64, l.margin)
        assertEquals(64, l.rowHeight)
        assertEquals(260, l.headerHeight)
        assertEquals(96, l.sectionTitleHeight)
        assertEquals(260 + 96, l.rankingTop)
        assertEquals(356 + 3 * 64 + 96, l.tableTop)
        // Header row + 2 round rows + bottom margin.
        assertEquals(l.tableTop + 3 * 64 + 64, l.height)
        // Inner width 952, round column 140 → 812 / 3 = 270 per player.
        assertEquals(270, l.playerColumnWidth)
        assertEquals(listOf(64, 204, 474, 744), l.columnLefts)
    }

    @Test fun `layout without rounds stops after the ranking`() {
        val l = scoreSheetLayout(playerCount = 4, roundCount = 0)
        assertEquals(356 + 4 * 64 + 64, l.height)
        assertEquals(5, l.columnLefts.size)
        assertEquals(203, l.playerColumnWidth)
    }

    @Test fun `columns fit inside the margins`() {
        for (players in 1..8) {
            val l = scoreSheetLayout(players, 10)
            val lastRight = l.columnLefts.last() + l.playerColumnWidth
            assertTrue("players=$players", lastRight <= l.width - l.margin)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `layout rejects zero players`() {
        scoreSheetLayout(0, 1)
    }

    @Test fun `share strings in both languages`() {
        assertEquals("Winner: A (+1)", EnStrings.shareWinner("A", "+1"))
        assertEquals("Gagnant·e : A (+1)", FrStrings.shareWinner("A", "+1"))
        assertEquals("tarot-score-sheet.png", SCORE_SHEET_FILE_NAME)
    }
}

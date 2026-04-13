package fr.mandarine.tarotcounter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a PDF score sheet for a completed Tarot game.
 *
 * The layout is inspired by the official French Federation of Tarot (FFT)
 * scoring table shown on page 10 of R-RO201206.pdf:
 *   - Title + date header
 *   - Column headers (round label + one column per player)
 *   - One data row per completed round, showing cumulative scores
 *   - A bold "Total" row with the final scores
 *
 * Uses Android's built-in `android.graphics.pdf.PdfDocument` API, available
 * since API 19. No external PDF library is needed.
 *
 * The generated file is written to the app's private cache directory
 * (`Context.cacheDir`) so it can be shared via FileProvider without
 * requiring any storage permission on the device.
 */
object PdfExporter {

    // A4 portrait dimensions in points. PdfDocument uses 72 DPI, so:
    //   210 mm × 297 mm  →  595 pt × 842 pt.
    private const val PAGE_WIDTH  = 595
    private const val PAGE_HEIGHT = 842

    // Left/right margin. Content fills the remaining horizontal space.
    private const val MARGIN = 40f

    // Height of each table row in points (text + small top-padding).
    private const val ROW_HEIGHT = 20f

    // Fixed width for the "Round/Manche" column (round numbers are short).
    private const val ROUND_COL_WIDTH = 60f

    /**
     * Generates a PDF file and returns it.
     *
     * @param context      Android context — used to resolve `cacheDir`.
     * @param playerNames  Ordered list of player display names.
     * @param roundHistory All completed rounds, oldest first.
     * @param strings      Localised string bundle (for column headers and labels).
     * @return A [File] in the app's cache directory containing the generated PDF.
     */
    fun generateScorePdf(
        context: Context,
        playerNames: List<String>,
        roundHistory: List<RoundResult>,
        strings: AppStrings
    ): File {
        // PdfDocument is Android's built-in single-page or multi-page PDF builder.
        val document = PdfDocument()

        // A PageInfo describes the page's pixel dimensions and its 1-based page number.
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)

        // Draw all content onto the page's canvas, then finalise the page.
        drawScoreSheet(page.canvas, playerNames, roundHistory, strings)
        document.finishPage(page)

        // Write the PDF bytes to a file in the private cache directory.
        // The name is fixed; a new export always overwrites the previous one.
        val file = File(context.cacheDir, "tarot_scores.pdf")
        file.outputStream().use { out -> document.writeTo(out) }
        document.close()

        return file
    }

    // ── Private drawing helpers ───────────────────────────────────────────────

    /**
     * Draws the entire score sheet onto [canvas].
     *
     * Layout (top → bottom):
     *   1. Title + date line
     *   2. Thick horizontal rule
     *   3. Column header row (Round | Player…)
     *   4. Data rows (one per round, cumulative scores)
     *   5. Thick rule + bold Total row
     */
    private fun drawScoreSheet(
        canvas: Canvas,
        playerNames: List<String>,
        roundHistory: List<RoundResult>,
        strings: AppStrings
    ) {
        val contentWidth = PAGE_WIDTH - 2 * MARGIN

        // Current vertical drawing position (baseline for text).
        // We advance `y` after each element.
        var y = MARGIN + 22f

        // ── Paints ────────────────────────────────────────────────────────────
        // Each paint object is configured once and reused for multiple draw calls.

        // Large centred bold title ("TAROT").
        val titlePaint = makePaint(textSize = 22f, bold = true, align = Paint.Align.CENTER)

        // Smaller right-aligned date.
        val datePaint = makePaint(textSize = 11f, color = Color.DKGRAY, align = Paint.Align.RIGHT)

        // Bold centred column headers.
        val headerPaint = makePaint(textSize = 12f, bold = true, align = Paint.Align.CENTER)

        // Normal centred data cells.
        val cellPaint = makePaint(textSize = 11f, align = Paint.Align.CENTER)

        // Bold centred totals row.
        val totalPaint = makePaint(textSize = 11f, bold = true, align = Paint.Align.CENTER)

        // Thin rule drawn between rows.
        val thinRule = makePaint(color = Color.LTGRAY, strokeWidth = 0.5f)

        // Thicker rule drawn above and below the header and totals rows.
        val thickRule = makePaint(color = Color.BLACK, strokeWidth = 1f)

        // ── Title line ────────────────────────────────────────────────────────

        // "TAROT" centred across the content area.
        canvas.drawText("TAROT", MARGIN + contentWidth / 2f, y, titlePaint)

        // Date right-aligned at the same y position (baseline aligned).
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        canvas.drawText(dateStr, MARGIN + contentWidth, y, datePaint)

        y += 10f  // small gap below the title text

        // Thick rule separating the title from the table.
        canvas.drawLine(MARGIN, y, MARGIN + contentWidth, y, thickRule)
        y += 16f  // gap before the header row text baseline

        // ── Column layout ─────────────────────────────────────────────────────
        // Column 0 is the fixed-width round-number column.
        // Columns 1..n are equal-width player columns filling the remaining space.
        val playerColWidth = if (playerNames.isEmpty()) contentWidth
                             else (contentWidth - ROUND_COL_WIDTH) / playerNames.size

        // Returns the horizontal centre of column `index` (0 = round column).
        fun colCenterX(index: Int): Float = when (index) {
            0    -> MARGIN + ROUND_COL_WIDTH / 2f
            else -> MARGIN + ROUND_COL_WIDTH + (index - 1) * playerColWidth + playerColWidth / 2f
        }

        // ── Header row ────────────────────────────────────────────────────────

        canvas.drawText(strings.roundColumn, colCenterX(0), y, headerPaint)
        playerNames.forEachIndexed { i, name ->
            // Truncate names longer than 12 characters to prevent overflow in
            // narrow columns (e.g. 5-player games on A4).
            val label = if (name.length > 12) name.take(11) + "\u2026" else name
            canvas.drawText(label, colCenterX(i + 1), y, headerPaint)
        }

        y += 5f  // gap between header text and the rule below it
        canvas.drawLine(MARGIN, y, MARGIN + contentWidth, y, thickRule)
        y += ROW_HEIGHT - 5f  // advance to the next text baseline

        // ── Data rows ─────────────────────────────────────────────────────────
        // `buildScoreTableData` (GameModels.kt) computes the running cumulative
        // totals and formats them as signed strings ("+50", "-25").
        val tableRows = buildScoreTableData(playerNames, roundHistory)
        tableRows.forEach { row ->
            // row.cells[0] = round number as string; row.cells[1..n] = cumulative scores.
            row.cells.forEachIndexed { colIndex, cell ->
                canvas.drawText(cell, colCenterX(colIndex), y, cellPaint)
            }

            y += 4f   // small padding before the thin rule
            canvas.drawLine(MARGIN, y, MARGIN + contentWidth, y, thinRule)
            y += ROW_HEIGHT - 4f
        }

        // ── Total row ─────────────────────────────────────────────────────────
        if (roundHistory.isNotEmpty()) {
            // Thick rule above the totals row to visually separate it.
            canvas.drawLine(MARGIN, y - ROW_HEIGHT + 4f, MARGIN + contentWidth, y - ROW_HEIGHT + 4f, thickRule)

            // `computeFinalTotals` sums each player's round scores across all rounds.
            val totals = computeFinalTotals(playerNames, roundHistory)

            canvas.drawText("Total", colCenterX(0), y, totalPaint)
            playerNames.forEachIndexed { i, name ->
                val score = totals[name] ?: 0
                canvas.drawText(score.withSign(), colCenterX(i + 1), y, totalPaint)
            }
        }
    }

    /**
     * Creates a [Paint] object with the given attributes.
     *
     * Paint objects are reused for multiple draw calls — this helper avoids
     * the verbose `Paint().apply { … }` boilerplate that would otherwise repeat
     * the same six properties for every paint we configure.
     *
     * @param textSize   Font size in points (default 12).
     * @param color      Text/line colour (default black).
     * @param bold       Whether to use a bold typeface (default false).
     * @param align      Text alignment anchor (default LEFT; use CENTER for table cells).
     * @param strokeWidth Stroke width for line-drawing paints (default 0 = hairline).
     */
    private fun makePaint(
        textSize: Float = 12f,
        color: Int = Color.BLACK,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT,
        strokeWidth: Float = 0f
    ): Paint = Paint().apply {
        this.color       = color
        this.textSize    = textSize
        this.typeface    = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        this.textAlign   = align
        this.strokeWidth = strokeWidth
        // ANTI_ALIAS_FLAG makes text edges smooth instead of jagged.
        isAntiAlias      = true
    }
}

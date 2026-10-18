package fr.mandarine.tarotcounter

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import fr.mandarine.tarotcounter.ui.theme.SalonBrassOnFelt
import fr.mandarine.tarotcounter.ui.theme.SalonBrassText
import fr.mandarine.tarotcounter.ui.theme.SalonFelt
import fr.mandarine.tarotcounter.ui.theme.SalonHairline
import fr.mandarine.tarotcounter.ui.theme.SalonInk
import fr.mandarine.tarotcounter.ui.theme.SalonInkMuted
import fr.mandarine.tarotcounter.ui.theme.SalonIvory
import fr.mandarine.tarotcounter.ui.theme.SalonNegative
import fr.mandarine.tarotcounter.ui.theme.SalonPaper
import fr.mandarine.tarotcounter.ui.theme.SalonPlayerTones
import fr.mandarine.tarotcounter.ui.theme.SalonPositive
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// Android side of "Share the score sheet" (issue #234): draws the PNG and opens
// the system share sheet. The content and the geometry come from the pure,
// unit-tested ScoreSheet.kt; this file only paints and sends.
//
// Excluded from PIT (it needs a real Android Bitmap / Context).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Opens the Android share sheet with the score sheet as plain text.
 *
 * `ACTION_SEND` + `text/plain` is understood by every messaging app;
 * `createChooser` always shows the picker, even if the user set a default app.
 */
fun shareScoreSheetText(context: Context, sheet: ScoreSheet) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, sheet.title)
        putExtra(Intent.EXTRA_TEXT, scoreSheetText(sheet))
    }
    context.startActivity(Intent.createChooser(send, sheet.title))
}

/**
 * Draws the score sheet as a PNG in the app cache and opens the share sheet with it.
 *
 * The file is exposed through a FileProvider (`content://` URI, declared in the
 * manifest + res/xml/file_paths.xml): the receiving app gets a temporary read
 * permission, so no storage permission is needed.
 *
 * Call it off the main thread for long games: drawing and compressing take a moment.
 * Returns the written file, or null when the image could not be written.
 */
fun writeScoreSheetImage(context: Context, sheet: ScoreSheet): File? = runCatching {
    val dir  = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, SCORE_SHEET_FILE_NAME)
    val bitmap = drawScoreSheet(context, sheet)
    // `use` closes the stream even if compress throws.
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    file
}.getOrNull()

/** Sends an image written by [writeScoreSheetImage] to the share sheet. */
fun shareScoreSheetImage(context: Context, sheet: ScoreSheet, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, sheet.title)
        // ClipData lets the chooser show a thumbnail and carries the read grant.
        clipData = ClipData.newRawUri(sheet.title, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, sheet.title))
}

// Paints the sheet on a Bitmap with the classic android.graphics API (Canvas + Paint).
// The light Salon palette is used whatever the app theme: the image is meant to be
// read in someone else's chat, where an ivory sheet reads best.
private fun drawScoreSheet(context: Context, sheet: ScoreSheet): Bitmap {
    val layout = scoreSheetLayout(sheet.playerNames.size, sheet.rounds.size)
    // createBitmap (core-ktx) defaults to ARGB_8888: 8 bits per colour channel + alpha.
    val bitmap = createBitmap(layout.width, layout.height)
    val canvas = Canvas(bitmap)

    // The app font (Figtree); falls back to the system font if loading fails.
    val regular = runCatching { ResourcesCompat.getFont(context, R.font.figtree_regular) }.getOrNull()
        ?: Typeface.DEFAULT
    val bold = runCatching { ResourcesCompat.getFont(context, R.font.figtree_bold) }.getOrNull()
        ?: Typeface.DEFAULT_BOLD

    // A small factory for text paints; ANTI_ALIAS smooths the glyph edges.
    fun paint(size: Float, color: Int, face: Typeface = regular, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color; typeface = face; textAlign = align
        }
    fun scoreColor(v: Int) = (if (v >= 0) SalonPositive else SalonNegative).toArgb()
    fun tone(seat: Int) = SalonPlayerTones[seat % SalonPlayerTones.size].toArgb()
    // Baseline of a text centred vertically in a row that starts at `top`.
    fun baseline(top: Int, p: Paint) =
        top + layout.rowHeight / 2f - (p.descent() + p.ascent()) / 2f

    canvas.drawColor(SalonIvory.toArgb())

    // ── Felt header: title, winner, detail ────────────────────────────────────
    canvas.drawRect(0f, 0f, layout.width.toFloat(), layout.headerHeight.toFloat(),
        Paint().apply { color = SalonFelt.toArgb() })
    val m = layout.margin.toFloat()
    val maxWidth = layout.width - 2 * m
    // Shrinks a header paint until its text fits on one line (long names on a tie).
    fun fitted(text: String, p: Paint): Paint {
        while (p.measureText(text) > maxWidth && p.textSize > 20f) p.textSize -= 2f
        return p
    }
    canvas.drawText(sheet.title, m, 84f, fitted(sheet.title, paint(44f, SalonPaper.toArgb(), bold)))
    canvas.drawText(sheet.winnerLine, m, 156f,
        fitted(sheet.winnerLine, paint(52f, SalonBrassOnFelt.toArgb(), bold)))
    canvas.drawText(sheet.detailLine, m, 218f, fitted(sheet.detailLine, paint(32f, SalonPaper.toArgb())))

    val sectionPaint = paint(36f, SalonInk.toArgb(), bold)
    val rowPaint     = paint(34f, SalonInk.toArgb())
    val hairline     = Paint().apply { color = SalonHairline.toArgb(); strokeWidth = 2f }
    val right        = (layout.width - layout.margin).toFloat()

    // ── Final ranking ─────────────────────────────────────────────────────────
    canvas.drawText(sheet.rankingTitle, m, layout.rankingTop - 28f, sectionPaint)
    sheet.ranking.forEachIndexed { i, s ->
        val top = layout.rankingTop + i * layout.rowHeight
        val rankPaint = paint(34f, if (s.isLeader) SalonBrassText.toArgb() else SalonInkMuted.toArgb(), bold)
        canvas.drawText(s.rank.toString(), m, baseline(top, rankPaint), rankPaint)
        canvas.drawText(s.name, m + 72f, baseline(top, rowPaint), paint(34f, tone(s.seatIndex), bold))
        val scorePaint = paint(34f, scoreColor(s.total), bold, Paint.Align.RIGHT)
        canvas.drawText(s.total.withSign(), right, baseline(top, scorePaint), scorePaint)
        canvas.drawLine(m, top + layout.rowHeight.toFloat(), right, top + layout.rowHeight.toFloat(), hairline)
    }

    // ── Round-by-round table (running totals) ─────────────────────────────────
    if (sheet.rounds.isNotEmpty()) {
        canvas.drawText(sheet.roundsTitle, m, layout.tableTop - 28f, sectionPaint)
        val colW = layout.playerColumnWidth.toFloat()
        // Header row: player names, centred in their column, in their seat colour.
        sheet.playerNames.forEachIndexed { seat, name ->
            val p = paint(30f, tone(seat), bold, Paint.Align.CENTER)
            val x = layout.columnLefts[seat + 1] + colW / 2f
            // Long names are cut to fit the column (breakText counts the chars that fit).
            val fit = p.breakText(name, true, colW - 12f, null)
            canvas.drawText(name.take(fit), x, baseline(layout.tableTop, p), p)
        }
        canvas.drawLine(m, layout.tableTop + layout.rowHeight.toFloat(), right,
            layout.tableTop + layout.rowHeight.toFloat(), hairline)
        sheet.rounds.forEachIndexed { r, round ->
            val top = layout.tableTop + (r + 1) * layout.rowHeight
            val badgePaint = paint(30f, SalonInkMuted.toArgb())
            canvas.drawText(round.badge, m, baseline(top, badgePaint), badgePaint)
            round.totals.forEachIndexed { seat, total ->
                val p = paint(30f, scoreColor(total), align = Paint.Align.CENTER)
                canvas.drawText(total.withSign(), layout.columnLefts[seat + 1] + colW / 2f, baseline(top, p), p)
            }
        }
    }
    return bitmap
}

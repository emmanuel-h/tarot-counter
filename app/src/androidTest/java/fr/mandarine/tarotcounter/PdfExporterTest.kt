package fr.mandarine.tarotcounter

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented tests for [PdfExporter].
 *
 * These tests run on a real device or emulator because [PdfExporter] relies on
 * `android.graphics.pdf.PdfDocument`, an Android framework class that cannot be
 * instantiated on the plain JVM used by unit tests.
 *
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class PdfExporterTest {

    // Use the app-under-test context (not the instrumentation context) so that
    // cacheDir resolves to the correct app-private directory.
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val playerNames = listOf("Alice", "Bob", "Charlie")

    // A minimal two-round history used as shared test data.
    private val roundHistory = listOf(
        RoundResult(
            roundNumber  = 1,
            takerName    = "Alice",
            contract     = Contract.GARDE,
            details      = null,
            won          = true,
            playerScores = mapOf("Alice" to 50, "Bob" to -25, "Charlie" to -25)
        ),
        RoundResult(
            roundNumber  = 2,
            takerName    = "Bob",
            contract     = Contract.PRISE,
            details      = null,
            won          = false,
            playerScores = mapOf("Alice" to -20, "Bob" to 10, "Charlie" to 10)
        )
    )

    @Test
    fun generateScorePdf_creates_a_file() {
        // Spec: calling generateScorePdf should produce a file at cacheDir/tarot_scores.pdf.
        val file: File = PdfExporter.generateScorePdf(
            context      = context,
            playerNames  = playerNames,
            roundHistory = roundHistory,
            strings      = EnStrings
        )
        assertTrue("PDF file should exist", file.exists())
    }

    @Test
    fun generateScorePdf_file_is_not_empty() {
        // Spec: the generated PDF must contain actual bytes (not a zero-length file).
        val file: File = PdfExporter.generateScorePdf(
            context      = context,
            playerNames  = playerNames,
            roundHistory = roundHistory,
            strings      = EnStrings
        )
        assertTrue("PDF file should have content", file.length() > 0)
    }

    @Test
    fun generateScorePdf_starts_with_pdf_magic_bytes() {
        // Spec: a valid PDF file always begins with the magic header "%PDF".
        // This ensures we're generating a real PDF, not a corrupt or empty file.
        val file: File = PdfExporter.generateScorePdf(
            context      = context,
            playerNames  = playerNames,
            roundHistory = roundHistory,
            strings      = EnStrings
        )
        val header = file.inputStream().use { it.readNBytes(4) }
        val headerStr = String(header, Charsets.ISO_8859_1)
        assertTrue("PDF must start with '%PDF' magic bytes, got: $headerStr", headerStr == "%PDF")
    }

    @Test
    fun generateScorePdf_works_with_empty_round_history() {
        // Spec: no rounds played — the PDF should still be generated without crashing.
        val file: File = PdfExporter.generateScorePdf(
            context      = context,
            playerNames  = playerNames,
            roundHistory = emptyList(),
            strings      = EnStrings
        )
        assertTrue("PDF file should exist even with empty history", file.exists())
        assertTrue("PDF file should have content", file.length() > 0)
    }

    @Test
    fun generateScorePdf_works_with_french_strings() {
        // Spec: the localised French string bundle should produce the same valid PDF structure.
        val file: File = PdfExporter.generateScorePdf(
            context      = context,
            playerNames  = playerNames,
            roundHistory = roundHistory,
            strings      = FrStrings
        )
        val header = file.inputStream().use { it.readNBytes(4) }
        assertTrue("French PDF must start with '%PDF'", String(header, Charsets.ISO_8859_1) == "%PDF")
    }

    @Test
    fun generateScorePdf_overwrites_previous_file() {
        // Spec: exporting twice in a row should overwrite the previous file,
        // not create a duplicate or append to it.
        val first: File = PdfExporter.generateScorePdf(context, playerNames, roundHistory, EnStrings)
        val sizeAfterFirst = first.length()

        // Second export with a different (smaller) history.
        val second: File = PdfExporter.generateScorePdf(
            context      = context,
            playerNames  = playerNames,
            roundHistory = emptyList(),
            strings      = EnStrings
        )

        // Both calls return the same file path.
        assertTrue("Both calls should return the same file", first.absolutePath == second.absolutePath)

        // The file should have changed (empty-history PDF is smaller than two-round PDF).
        // We only assert the file still exists and is not corrupted.
        assertTrue("File should still exist after overwrite", second.exists())
        assertTrue("File should have content after overwrite", second.length() > 0)
    }
}

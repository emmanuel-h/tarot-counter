package fr.mandarine.tarotcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for the i18n string infrastructure in AppLocale.kt.
 *
 * These run on the JVM without a device — no Compose runtime is needed because
 * AppStrings is a plain Kotlin data class and appStrings() is a pure function.
 *
 * Run with: ./gradlew testDebugUnitTest
 */
class AppLocaleTest {

    // ── appStrings() returns the correct bundle ───────────────────────────────

    @Test
    fun appStrings_EN_returns_english_title() {
        assertEquals("Tarot Counter", appStrings(AppLocale.EN).appTitle)
    }

    @Test
    fun appStrings_FR_returns_french_title() {
        assertEquals("Compteur de points", appStrings(AppLocale.FR).appTitle)
    }

    @Test
    fun english_and_french_titles_are_different() {
        assertNotEquals(
            appStrings(AppLocale.EN).appTitle,
            appStrings(AppLocale.FR).appTitle
        )
    }

    // ── playerFallback lambda ─────────────────────────────────────────────────

    @Test
    fun en_playerFallback_formats_correctly() {
        val strings = appStrings(AppLocale.EN)
        assertEquals("Player 1", strings.playerFallback(1))
        assertEquals("Player 3", strings.playerFallback(3))
    }

    @Test
    fun fr_playerFallback_formats_correctly() {
        val strings = appStrings(AppLocale.FR)
        assertEquals("Joueur 1", strings.playerFallback(1))
        assertEquals("Joueur 5", strings.playerFallback(5))
    }

    // ── roundsPlayed plural handling ──────────────────────────────────────────

    @Test
    fun en_roundsPlayed_singular() {
        assertEquals("1 round played", appStrings(AppLocale.EN).roundsPlayed(1))
    }

    @Test
    fun en_roundsPlayed_plural() {
        assertEquals("3 rounds played", appStrings(AppLocale.EN).roundsPlayed(3))
    }

    @Test
    fun fr_roundsPlayed_singular() {
        assertEquals("1 manche jouée", appStrings(AppLocale.FR).roundsPlayed(1))
    }

    @Test
    fun fr_roundsPlayed_plural() {
        assertEquals("4 manches jouées", appStrings(AppLocale.FR).roundsPlayed(4))
    }

    // ── roundCount plural handling ────────────────────────────────────────────

    @Test
    fun en_roundCount_singular() {
        assertEquals("1 round", appStrings(AppLocale.EN).roundCount(1))
    }

    @Test
    fun en_roundCount_plural() {
        assertEquals("5 rounds", appStrings(AppLocale.EN).roundCount(5))
    }

    @Test
    fun fr_roundCount_singular() {
        assertEquals("1 manche", appStrings(AppLocale.FR).roundCount(1))
    }

    @Test
    fun fr_roundCount_plural() {
        assertEquals("2 manches", appStrings(AppLocale.FR).roundCount(2))
    }

    // ── winnerResult and tieResult ────────────────────────────────────────────

    @Test
    fun en_winnerResult_formats_correctly() {
        val result = appStrings(AppLocale.EN).winnerResult("Alice", "+", 150)
        assertEquals("Winner: Alice (+150)", result)
    }

    @Test
    fun fr_winnerResult_formats_correctly() {
        val result = appStrings(AppLocale.FR).winnerResult("Alice", "+", 150)
        assertEquals("Gagnant : Alice (+150)", result)
    }

    @Test
    fun en_tieResult_formats_correctly() {
        val result = appStrings(AppLocale.EN).tieResult("Alice & Bob")
        assertEquals("Tie: Alice & Bob", result)
    }

    @Test
    fun fr_tieResult_formats_correctly() {
        val result = appStrings(AppLocale.FR).tieResult("Alice & Bob")
        assertEquals("Égalité : Alice & Bob", result)
    }

    // ── Chelem.localizedName ──────────────────────────────────────────────────

    @Test
    fun chelem_none_localized_en() {
        assertEquals("None", Chelem.NONE.localizedName(AppLocale.EN))
    }

    @Test
    fun chelem_none_localized_fr() {
        assertEquals("Aucun", Chelem.NONE.localizedName(AppLocale.FR))
    }

    @Test
    fun chelem_announced_realized_localized_fr() {
        assertEquals("Annoncé et réalisé", Chelem.ANNOUNCED_REALIZED.localizedName(AppLocale.FR))
    }

    @Test
    fun chelem_announced_not_realized_localized_fr() {
        assertEquals("Annoncé, non réalisé", Chelem.ANNOUNCED_NOT_REALIZED.localizedName(AppLocale.FR))
    }

    @Test
    fun chelem_not_announced_realized_localized_fr() {
        assertEquals("Non annoncé, réalisé", Chelem.NOT_ANNOUNCED_REALIZED.localizedName(AppLocale.FR))
    }

    // ── Contract.localizedName ────────────────────────────────────────────────

    @Test
    fun contract_names_are_same_in_both_locales() {
        // Prise, Garde, Garde Sans, Garde Contre are French Tarot terms used internationally.
        for (contract in Contract.entries) {
            assertEquals(
                "Contract ${contract.name} should have the same display name in EN and FR",
                contract.localizedName(AppLocale.EN),
                contract.localizedName(AppLocale.FR)
            )
        }
    }

    // ── Round history string builders ─────────────────────────────────────────

    @Test
    fun en_roundHistoryPrefix_formats_correctly() {
        val result = appStrings(AppLocale.EN).roundHistoryPrefix(3, "Alice")
        assertEquals("Round 3: Alice — ", result)
    }

    @Test
    fun fr_roundHistoryPrefix_formats_correctly() {
        val result = appStrings(AppLocale.FR).roundHistoryPrefix(3, "Alice")
        assertEquals("Manche 3 : Alice — ", result)
    }

    @Test
    fun en_wonOutcome_with_score() {
        assertEquals(" — Won (+80)", appStrings(AppLocale.EN).wonOutcome(" (+80)"))
    }

    @Test
    fun fr_wonOutcome_with_score() {
        assertEquals(" — Gagné (+80)", appStrings(AppLocale.FR).wonOutcome(" (+80)"))
    }

    @Test
    fun en_lostOutcome_with_score() {
        assertEquals(" — Lost (-40)", appStrings(AppLocale.EN).lostOutcome(" (-40)"))
    }

    @Test
    fun fr_lostOutcome_with_score() {
        assertEquals(" — Perdu (-40)", appStrings(AppLocale.FR).lostOutcome(" (-40)"))
    }
}

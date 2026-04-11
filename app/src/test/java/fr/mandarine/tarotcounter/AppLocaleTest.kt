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
        assertEquals("Tarot", appStrings(AppLocale.FR).appTitle)
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
        // formattedScore is pre-formatted via Int.withSign() at the call site.
        val result = appStrings(AppLocale.EN).winnerResult("Alice", "+150")
        assertEquals("Winner: Alice (+150)", result)
    }

    @Test
    fun fr_winnerResult_formats_correctly() {
        val result = appStrings(AppLocale.FR).winnerResult("Alice", "+150")
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
    fun contract_localizedName_en_prise() {
        assertEquals("Small", Contract.PRISE.localizedName(AppLocale.EN))
    }

    @Test
    fun contract_localizedName_en_garde() {
        assertEquals("Guard", Contract.GARDE.localizedName(AppLocale.EN))
    }

    @Test
    fun contract_localizedName_en_garde_sans() {
        assertEquals("Guard Without", Contract.GARDE_SANS.localizedName(AppLocale.EN))
    }

    @Test
    fun contract_localizedName_en_garde_contre() {
        assertEquals("Guard Against", Contract.GARDE_CONTRE.localizedName(AppLocale.EN))
    }

    @Test
    fun contract_localizedName_fr_prise() {
        assertEquals("Prise", Contract.PRISE.localizedName(AppLocale.FR))
    }

    @Test
    fun contract_localizedName_fr_garde() {
        assertEquals("Garde", Contract.GARDE.localizedName(AppLocale.FR))
    }

    @Test
    fun contract_localizedName_fr_garde_sans() {
        assertEquals("Garde Sans", Contract.GARDE_SANS.localizedName(AppLocale.FR))
    }

    @Test
    fun contract_localizedName_fr_garde_contre() {
        assertEquals("Garde Contre", Contract.GARDE_CONTRE.localizedName(AppLocale.FR))
    }

    @Test
    fun contract_localizedName_en_and_fr_differ() {
        // English translations must be distinct from the French Tarot terms.
        for (contract in Contract.entries) {
            assertNotEquals(
                "Contract ${contract.name} EN and FR names should differ",
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

    // ── pointsOutOfRange error string ─────────────────────────────────────────

    @Test
    fun en_pointsOutOfRange_is_non_empty() {
        // The error string must be present so the UI can display it in the TextField.
        val msg = appStrings(AppLocale.EN).pointsOutOfRange
        assert(msg.isNotBlank()) { "EN pointsOutOfRange should not be blank" }
    }

    @Test
    fun fr_pointsOutOfRange_is_non_empty() {
        val msg = appStrings(AppLocale.FR).pointsOutOfRange
        assert(msg.isNotBlank()) { "FR pointsOutOfRange should not be blank" }
    }

    @Test
    fun en_and_fr_pointsOutOfRange_are_different() {
        // Both locales must provide distinct translations — they must not fall back to the same string.
        assertNotEquals(
            appStrings(AppLocale.EN).pointsOutOfRange,
            appStrings(AppLocale.FR).pointsOutOfRange
        )
    }

    // ── roundHeader lambda ────────────────────────────────────────────────────

    @Test
    fun en_roundHeader_formats_correctly() {
        assertEquals("Round 5", appStrings(AppLocale.EN).roundHeader(5))
    }

    @Test
    fun fr_roundHeader_formats_correctly() {
        // In French "Round" becomes "Manche".
        assertEquals("Manche 5", appStrings(AppLocale.FR).roundHeader(5))
    }

    // ── chooseContract lambda ─────────────────────────────────────────────────

    @Test
    fun en_chooseContract_formats_correctly() {
        assertEquals("Alice — choose a contract:", appStrings(AppLocale.EN).chooseContract("Alice"))
    }

    @Test
    fun fr_chooseContract_formats_correctly() {
        // Note the French typographic space before the colon.
        assertEquals("Alice — choisissez un contrat :", appStrings(AppLocale.FR).chooseContract("Alice"))
    }

    // ── resumeRoundDetail lambda ──────────────────────────────────────────────

    @Test
    fun en_resumeRoundDetail_formats_correctly() {
        // Combines the round number with a pre-formatted "rounds played" label.
        val label = appStrings(AppLocale.EN).roundsPlayed(2)  // "2 rounds played"
        assertEquals("Round 3 · $label", appStrings(AppLocale.EN).resumeRoundDetail(3, label))
    }

    @Test
    fun fr_resumeRoundDetail_formats_correctly() {
        val label = appStrings(AppLocale.FR).roundsPlayed(2)  // "2 manches jouées"
        assertEquals("Manche 3 · $label", appStrings(AppLocale.FR).resumeRoundDetail(3, label))
    }

    // ── boutsPointsDetail lambda ──────────────────────────────────────────────

    @Test
    fun boutsPointsDetail_is_language_neutral() {
        // "bouts" and "pts" are the same Tarot shorthand in both languages.
        val en = appStrings(AppLocale.EN).boutsPointsDetail(2, 56)
        val fr = appStrings(AppLocale.FR).boutsPointsDetail(2, 56)
        assertEquals(" · 2 bouts · 56 pts", en)
        assertEquals("Both locales must produce the same boutsPointsDetail string", en, fr)
    }

    // ── scoreDisplay lambda ───────────────────────────────────────────────────

    @Test
    fun scoreDisplay_formats_positive_score() {
        // formattedScore is pre-formatted via Int.withSign() at the call site.
        val en = appStrings(AppLocale.EN).scoreDisplay("+120")
        val fr = appStrings(AppLocale.FR).scoreDisplay("+120")
        assertEquals("+120 pts", en)
        assertEquals("Both locales must produce the same scoreDisplay string", en, fr)
    }

    @Test
    fun scoreDisplay_formats_negative_score() {
        assertEquals("-45 pts", appStrings(AppLocale.EN).scoreDisplay("-45"))
    }

    // ── chelemPlaysFirst lambda ───────────────────────────────────────────────

    @Test
    fun en_chelemPlaysFirst_formats_correctly() {
        assertEquals("Alice plays first this round.", appStrings(AppLocale.EN).chelemPlaysFirst("Alice"))
    }

    @Test
    fun fr_chelemPlaysFirst_formats_correctly() {
        assertEquals("Alice joue en premier ce tour.", appStrings(AppLocale.FR).chelemPlaysFirst("Alice"))
    }

    // ── Chelem EN localized names (non-NONE values) ───────────────────────────

    @Test
    fun chelem_announced_realized_localized_en() {
        assertEquals("Announced & realized", Chelem.ANNOUNCED_REALIZED.localizedName(AppLocale.EN))
    }

    @Test
    fun chelem_announced_not_realized_localized_en() {
        assertEquals("Announced, not realized", Chelem.ANNOUNCED_NOT_REALIZED.localizedName(AppLocale.EN))
    }

    @Test
    fun chelem_not_announced_realized_localized_en() {
        assertEquals("Not announced, realized", Chelem.NOT_ANNOUNCED_REALIZED.localizedName(AppLocale.EN))
    }

    // ── Camp-toggle label strings (issue #115) ───────────────────────────────
    // Verifies the points field labels used by the inline camp toggle.

    @Test
    fun en_attackerPointsLabel_and_defenderPointsLabel_differ() {
        val strings = appStrings(AppLocale.EN)
        assertNotEquals(strings.attackerPointsLabel, strings.defenderPointsLabel)
        assertEquals("Attacker's pts (0-91)", strings.attackerPointsLabel)
        assertEquals("Defenders' pts (0-91)", strings.defenderPointsLabel)
    }

    @Test
    fun fr_attackerPointsLabel_and_defenderPointsLabel_differ() {
        val strings = appStrings(AppLocale.FR)
        assertNotEquals(strings.attackerPointsLabel, strings.defenderPointsLabel)
        assertEquals("Pts attaquant (0-91)", strings.attackerPointsLabel)
        assertEquals("Pts défenseurs (0-91)", strings.defenderPointsLabel)
    }
}

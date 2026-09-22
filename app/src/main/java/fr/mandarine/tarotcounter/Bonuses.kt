package fr.mandarine.tarotcounter

// ─────────────────────────────────────────────────────────────────────────────
// Pure logic behind the bonus rows and bottom sheets of the round entry
// (issue #200). No Compose here: unit-tested on the JVM in BonusesTest.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The trump show ("poignée") a single player declared in a round.
 * A player shows at most one poignée, so the levels are mutually exclusive.
 */
enum class PoigneeLevel { NONE, SIMPLE, DOUBLE, TRIPLE }

/**
 * Every poignée declared in a round, one set of players per level — the same shape
 * as [RoundDetails.poignees] / `doublePoignees` / `triplePoignees`.
 *
 * `data class` gives value equality and `copy()`, which suits immutable Compose state.
 */
data class PoigneeDeclarations(
    val simple: Set<String> = emptySet(),
    val double: Set<String> = emptySet(),
    val triple: Set<String> = emptySet()
) {
    /** The level [player] declared, or [PoigneeLevel.NONE]. */
    fun levelOf(player: String): PoigneeLevel = when (player) {
        in triple -> PoigneeLevel.TRIPLE
        in double -> PoigneeLevel.DOUBLE
        in simple -> PoigneeLevel.SIMPLE
        else      -> PoigneeLevel.NONE
    }

    /**
     * Returns a copy where [player] declared exactly [level]: they are removed from
     * every other level, so one player can never hold two poignées.
     */
    fun withLevel(player: String, level: PoigneeLevel): PoigneeDeclarations = PoigneeDeclarations(
        simple = if (level == PoigneeLevel.SIMPLE) simple + player else simple - player,
        double = if (level == PoigneeLevel.DOUBLE) double + player else double - player,
        triple = if (level == PoigneeLevel.TRIPLE) triple + player else triple - player
    )

    /** Players who declared any poignée, in [playerOrder] (seat order). */
    fun declarants(playerOrder: List<String>): List<String> =
        playerOrder.filter { levelOf(it) != PoigneeLevel.NONE }
}

/**
 * Players who may call a chelem: the taker, plus the called partner in a 5-player
 * game (only once a partner has been chosen).
 */
fun chelemCandidates(taker: String, partner: String?, playerCount: Int): List<String> =
    if (playerCount == 5 && partner != null) listOf(taker, partner) else listOf(taker)

/** True when the chelem outcome was announced, so its caller leads the first trick. */
fun isAnnouncedChelem(chelem: Chelem): Boolean =
    chelem == Chelem.ANNOUNCED_REALIZED || chelem == Chelem.ANNOUNCED_NOT_REALIZED

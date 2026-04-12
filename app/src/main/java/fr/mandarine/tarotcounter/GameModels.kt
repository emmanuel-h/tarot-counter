package fr.mandarine.tarotcounter

import kotlin.math.abs
import kotlinx.serialization.Serializable

// Formats an integer score with an explicit sign so the direction is always visible:
//   42  → "+42"
//   -5  → "-5"
//   0   → "+0"
// Used everywhere a cumulative or final score is displayed in the UI.
fun Int.withSign(): String = if (this >= 0) "+$this" else "$this"

// The contracts a player can announce in French Tarot, ordered from weakest to strongest.
// Each entry has a `displayName` shown in the UI and a `multiplier` used in score calculation.
//   PRISE    × 1  (formerly called "Petite" in some regional variants)
//   GARDE    × 2
//   GARDE_SANS   × 4  (taker bets they need no help from the dog)
//   GARDE_CONTRE × 6  (taker gives the dog to the defenders)
//
// @Serializable tells the Kotlin compiler to generate JSON read/write code for this enum.
// Without it, the serialization library would not know how to save/load Contract values.
@Serializable
enum class Contract(val displayName: String, val multiplier: Int) {
    PRISE("Prise", 1),
    GARDE("Garde", 2),
    GARDE_SANS("Garde Sans", 4),
    GARDE_CONTRE("Garde Contre", 6)
}

// The possible chelem (grand slam) outcomes for a round.
// A chelem means winning every single trick.
//
// The first three cases describe the taker's camp announcing or achieving a grand slam.
// The last case (DEFENDERS_REALIZED) covers the rare but legal scenario — described in
// R-RO201206.pdf page 6 — where the defending camp wins every single trick without having
// announced it. The taker pays 200 points to each defender in addition to the normal score.
@Serializable
enum class Chelem(val displayName: String) {
    NONE("None"),
    ANNOUNCED_REALIZED("Announced & realized"),
    ANNOUNCED_NOT_REALIZED("Announced, not realized"),
    NOT_ANNOUNCED_REALIZED("Not announced, realized"),
    // Defenders won every trick without announcing it (R-RO201206.pdf p.6).
    // Financial effect: each defender receives +200; taker pays −200 per defender.
    DEFENDERS_REALIZED("Defenders realized")
}

// Total number of trump cards in a standard French Tarot deck.
// Trumps 1–21 plus the Excuse = 22 cards.
// Used to validate that the combined atout thresholds declared by all players
// do not exceed what physically exists in the deck.
const val TOTAL_ATOUTS_IN_DECK = 22

// All the bonus and scoring details collected after a contract is chosen.
//
// Fields that are "assigned to a player" use String? — null means nobody claimed that bonus.
// For example, `petitAuBout = "Alice"` means Alice captured the 1 of trump on the last trick.
//
// `partnerName` is only relevant in a 5-player game: the taker calls one other player as their
// silent partner. It is null for 3- and 4-player games.
//
// Multi-player poignée (issue #149):
//   Any number of players can independently declare a simple, double, or triple poignée.
//   The new `poignees` / `doublePoignees` / `triplePoignees` lists hold all declarants.
//
//   The legacy nullable fields (`poignee`, `doublePoignee`, `triplePoignee`) are kept so that
//   old saved-game JSON (which contains a single player name string) still deserialises without
//   error — kotlinx.serialization uses named keys, so new fields not present in old JSON just
//   receive their default values. The `effectivePoignees` computed properties bridge the two:
//   they return the new list when non-empty, or wrap the old nullable field as a singleton list.
@Serializable
data class RoundDetails(
    val bouts: Int,             // number of oudlers (0–3) in the taker's tricks
    val points: Int,            // points scored by the taker (0–91)
    val partnerName: String?,   // taker's called partner (5-player only); null otherwise
    val petitAuBout: String?,   // player who captured the 1 of trump on the last trick, or null

    // ── Legacy single-player poignée fields (kept for backward-compat deserialization) ──
    // New code always writes to the list fields below; these are only populated when
    // reading old saved games that were created before issue #149.
    val poignee: String?       = null, // player who showed a simple poignée, or null (legacy)
    val doublePoignee: String? = null, // player who showed a double poignée, or null (legacy)
    val triplePoignee: String? = null, // player who showed a triple poignée, or null (legacy)

    // ── New multi-player poignée fields (issue #149) ──────────────────────────
    // Any number of players can declare a poignée simultaneously (e.g. both the taker
    // and a defender each show their own trump hand). Each declaration contributes its
    // full bonus to the winning camp independently.
    val poignees: List<String>       = emptyList(), // all players who declared simple poignée
    val doublePoignees: List<String> = emptyList(), // all players who declared double poignée
    val triplePoignees: List<String> = emptyList(), // all players who declared triple poignée

    val chelem: Chelem,         // grand slam outcome
    // The player who called or achieved the chelem. Null when chelem == NONE.
    // In a 3- or 4-player game only the taker can call it; in a 5-player game the
    // partner can also announce a chelem. When a chelem is announced, that player
    // leads the first trick of the round — regardless of the normal turn order.
    val chelemPlayer: String? = null
) {
    // ── Migration helpers ─────────────────────────────────────────────────────
    // These computed properties make the rest of the codebase migration-transparent:
    // they return the new list when it contains entries, or fall back to the legacy
    // single-player field by wrapping it in a singleton list (or empty list if null).
    // `listOfNotNull` produces an empty list when its argument is null.

    /** All players who declared a simple poignée (new or legacy format). */
    val effectivePoignees: List<String>
        get() = poignees.ifEmpty { listOfNotNull(poignee) }

    /** All players who declared a double poignée (new or legacy format). */
    val effectiveDoublePoignees: List<String>
        get() = doublePoignees.ifEmpty { listOfNotNull(doublePoignee) }

    /** All players who declared a triple poignée (new or legacy format). */
    val effectiveTriplePoignees: List<String>
        get() = triplePoignees.ifEmpty { listOfNotNull(triplePoignee) }
}

// Returns the minimum number of points the taker needs to win, based on
// how many bouts (oudlers) they hold in their tricks.
//
// The three bouts are the 21 of trumps, the 1 of trumps (Petit), and the Excuse.
// The more bouts you hold, the fewer points you need:
//   0 bouts → 56 pts   1 bout  → 51 pts
//   2 bouts → 41 pts   3 bouts → 36 pts
fun requiredPoints(bouts: Int): Int = when (bouts) {
    0    -> 56
    1    -> 51
    2    -> 41
    3    -> 36
    else -> throw IllegalArgumentException("bouts must be 0–3, got $bouts")
}

// Returns true if the taker wins the round.
// The taker wins when their scored points reach or exceed the threshold for their bout count.
fun takerWon(bouts: Int, points: Int): Boolean = points >= requiredPoints(bouts)

// Returns the base round score before distributing it between players.
//
// Formula: (25 + |actual − required|) × contract.multiplier
//
// The constant 25 is added regardless of the margin — it represents the base value of any
// contract. The absolute difference rewards or penalises proportionally to how much the taker
// exceeded or missed the threshold.
//
// Examples (Garde × 2):
//   2 bouts, scored 56 → required 41 → diff 15 → (25+15)×2 = 80
//   2 bouts, scored 35 → required 41 → diff  6 → (25+ 6)×2 = 62
fun calculateRoundScore(contract: Contract, bouts: Int, points: Int): Int {
    val diff = abs(points - requiredPoints(bouts))
    return (25 + diff) * contract.multiplier
}

// Distributes the round score to every player and returns a map of player → points.
//
// Rules (sign is + if taker won, − if taker lost):
//
//   3 or 4 players — no partner:
//     taker          : sign × (n−1) × roundScore
//     each defender  : −sign × roundScore
//
//   5 players — partner is called:
//     taker          : sign × 2 × roundScore
//     partner        : sign × 1 × roundScore
//     each defender  : −sign × roundScore   (there are 3 defenders)
//
// In all cases the sum of all scores is 0 (zero-sum game).
fun computePlayerScores(
    allPlayers: List<String>,
    takerName: String,
    partnerName: String?,   // null for 3- and 4-player games
    won: Boolean,
    roundScore: Int
): Map<String, Int> {
    val sign = if (won) 1 else -1
    // In 5-player mode, the 3 defenders each contribute/receive roundScore,
    // and the attackers split that total 2:1 (taker:partner).
    // In 3/4-player mode, the taker collects from all (n−1) defenders.
    val numDefenders = if (partnerName != null) 3 else allPlayers.size - 1
    val takerMultiplier = if (partnerName != null) 2 else numDefenders

    return allPlayers.associateWith { player ->
        when (player) {
            takerName   -> sign * takerMultiplier * roundScore
            partnerName -> sign * roundScore
            else        -> -sign * roundScore
        }
    }
}

// Returns the minimum number of trumps a player must show to declare a Poignée,
// based on the number of players in the game.
//
// The official FFT rules (R-RO201206.pdf) specify different thresholds per player count:
//
//   3 players  →  simple: 13,  double: 15,  triple: 18
//   4 players  →  simple: 10,  double: 13,  triple: 15
//   5 players  →  simple:  8,  double: 10,  triple: 13
//
// Returns a Triple of (simpleThreshold, doubleThreshold, tripleThreshold).
// Throws IllegalArgumentException for any player count outside 3–5.
fun poigneeThresholds(playerCount: Int): Triple<Int, Int, Int> = when (playerCount) {
    3    -> Triple(13, 15, 18)
    4    -> Triple(10, 13, 15)
    5    -> Triple( 8, 10, 13)
    else -> throw IllegalArgumentException("playerCount must be 3–5, got $playerCount")
}

// Returns the petit-au-bout bonus amount per defender.
//
// The petit au bout is achieved when the Petit (1 of trumps) is captured on the
// very last trick. The bonus is awarded to the **camp that achieved it** —
// regardless of whether that camp won or lost the round overall.
//
// Value: 10 × contract.multiplier  (scales with the contract, like the base score)
//
// Distribution rule applied in GameScreen:
//   Determine which camp `petitAuBout` player belongs to:
//     - taker's camp  (taker or partner) → sign = +1  → taker gains, defenders pay
//     - defenders' camp                  → sign = −1  → each defender gains, taker pays
//   taker delta    = sign × petitAuBoutBonus × numDefenders
//   defender delta = −sign × petitAuBoutBonus   (per defender)
//   partner delta  = 0   (partner is not involved in the petit-au-bout bonus)
fun petitAuBoutBonus(contract: Contract): Int = 10 * contract.multiplier

// Returns the flat poignée (trump show) bonus per defender for a single declaration.
//
// Exactly one of the three parameters should be non-null at most per call.
// The bonus is the amount exchanged between the taker and *each* defender:
//   triplePoignee → 40 pts
//   doublePoignee → 30 pts
//   poignee       → 20 pts
//   none declared →  0 pts
//
// Crucially, the bonus always goes to the **winning camp**, regardless of who
// declared it. The sign is applied in GameScreen using the round outcome:
//   taker won  → taker collects bonus from each defender
//   taker lost → each defender collects bonus from the taker
//
// Note: use `totalPoigneeBonus` for multi-player scenarios (issue #149).
fun poigneeBonus(
    poignee: String?,
    doublePoignee: String?,
    triplePoignee: String?
): Int = when {
    triplePoignee != null -> 40
    doublePoignee != null -> 30
    poignee       != null -> 20
    else                  ->  0
}

// Returns the **total** poignée bonus per defender across all players who declared
// a poignée in the same round (issue #149).
//
// In the updated rules any number of players can each show their own trump hand —
// for example both the taker (simple, 20 pts) and a defender (double, 30 pts)
// may declare simultaneously. Each declaration contributes independently:
//   total = (simple count × 20) + (double count × 30) + (triple count × 40)
//
// The bonus still always goes to the **winning camp** as a whole — the direction
// is determined by the round outcome, not by who declared.
//
// Parameters:
//   poignees       : all players who declared a simple poignée
//   doublePoignees : all players who declared a double poignée
//   triplePoignees : all players who declared a triple poignée
fun totalPoigneeBonus(
    poignees: List<String>,
    doublePoignees: List<String>,
    triplePoignees: List<String>
): Int = poignees.size * 20 + doublePoignees.size * 30 + triplePoignees.size * 40

// Returns the total number of atouts (trumps) that all declared poignées claim
// to represent. Used to validate that declarations do not exceed what is physically
// possible given the 22 trumps in the deck.
//
// Each poignée type requires a minimum trump count that varies by player count
// (from `poigneeThresholds`). When multiple players declare, their minimum
// requirements add up — if the sum exceeds `TOTAL_ATOUTS_IN_DECK` (22) then at
// least one player must be lying, and the form should reject the input.
//
// Example (4 players, thresholds 10 / 13 / 15):
//   Alice declares triple (15) + Bob declares simple (10) → total = 25 > 22 → invalid
//   Alice declares simple (10) + Bob declares simple (10) → total = 20 ≤ 22 → valid
fun totalAtoutsAnnounced(
    poignees: List<String>,
    doublePoignees: List<String>,
    triplePoignees: List<String>,
    playerCount: Int
): Int {
    val (simpleMin, doubleMin, tripleMin) = poigneeThresholds(playerCount)
    return poignees.size * simpleMin +
           doublePoignees.size * doubleMin +
           triplePoignees.size * tripleMin
}

// Returns the flat chelem (grand slam) bonus.
//
// The value is the amount the taker collects from (or pays to) *each* defender:
//   +400 → announced & realized    (taker wins 400 from each defender)
//   +200 → not announced, realized (taker wins 200 from each defender)
//   -200 → announced, not realized (taker pays 200 to each defender)
//     0  → no chelem, no adjustment
//
// Distribution rule applied in GameScreen:
//   taker delta   = chelemBonus × numDefenders
//   defender delta = −chelemBonus   (per defender)
//   partner delta  = 0              (partner is not involved in chelem bonus)
fun chelemBonus(chelem: Chelem): Int = when (chelem) {
    Chelem.NONE                   ->    0
    Chelem.ANNOUNCED_REALIZED     ->  400
    Chelem.NOT_ANNOUNCED_REALIZED ->  200
    Chelem.ANNOUNCED_NOT_REALIZED -> -200
    // Defenders won every trick: each defender receives 200 from the taker
    // (R-RO201206.pdf p.6). Same sign as ANNOUNCED_NOT_REALIZED but different semantics.
    Chelem.DEFENDERS_REALIZED     -> -200
}

// Applies all three bonus adjustments to a base score map and returns the final scores.
//
// The three bonuses — petit au bout, poignée, and chelem — each adjust the scores
// after the base round result is computed by `computePlayerScores`. They are
// applied sequentially, each building on the previous result.
//
// Why this lives here and not in GameScreen:
//   The bonus logic is pure domain arithmetic — no UI or Compose code needed.
//   Keeping it in GameModels lets us unit-test it on the JVM, just like the
//   other scoring functions above.
//
// Parameters:
//   baseScores   : score map from `computePlayerScores` (already zero-sum)
//   contract     : the contract for this round (used to scale petit-au-bout)
//   details      : all bonus flags collected from the round
//   takerName    : display name of the taker
//   won          : true if the taker won (determines poignée direction)
//   numDefenders : number of defending players (2 in 3-player, 3 in 4- or 5-player)
//
// Returns a new map with all three bonuses applied; the result is still zero-sum.
fun applyBonuses(
    baseScores: Map<String, Int>,
    contract: Contract,
    details: RoundDetails,
    takerName: String,
    won: Boolean,
    numDefenders: Int
): Map<String, Int> {

    // ── Petit au bout ──────────────────────────────────────────────────────
    // The bonus goes to whichever camp captured the Petit on the last trick,
    // regardless of who won the round overall.
    // Taker's camp = taker + partner; defenders' camp = everyone else.
    val pabAmount = if (details.petitAuBout != null) petitAuBoutBonus(contract) else 0
    // +1 if the achiever is in the taker's camp, -1 if they are a defender, 0 if nobody.
    val pabSign = when (details.petitAuBout) {
        null                -> 0
        takerName,
        details.partnerName -> +1   // taker's camp achieved it
        else                -> -1   // defenders' camp achieved it
    }
    val scoresAfterPab = if (pabAmount == 0) baseScores else {
        baseScores.mapValues { (player, score) ->
            when (player) {
                takerName           -> score + pabSign * pabAmount * numDefenders
                details.partnerName -> score                        // partner unaffected
                else                -> score - pabSign * pabAmount  // each defender
            }
        }
    }

    // ── Poignée (trump show) ───────────────────────────────────────────────
    // The bonus always goes to the winning camp, regardless of who declared it.
    //   taker won  → taker collects bonus from each defender
    //   taker lost → each defender collects bonus from the taker
    // The partner (5-player only) is not involved.
    //
    // `effectivePoignees` / `effectiveDoublePoignees` / `effectiveTriplePoignees`
    // transparently handle both old saved games (single player in legacy nullable
    // fields) and new games (multi-player lists from issue #149).
    val pBonus = totalPoigneeBonus(
        details.effectivePoignees,
        details.effectiveDoublePoignees,
        details.effectiveTriplePoignees
    )
    val pSign  = if (won) 1 else -1
    val scoresAfterPoignee = if (pBonus == 0) scoresAfterPab else {
        scoresAfterPab.mapValues { (player, score) ->
            when (player) {
                takerName           -> score + pSign * pBonus * numDefenders
                details.partnerName -> score                       // partner unaffected
                else                -> score - pSign * pBonus      // each defender
            }
        }
    }

    // ── Chelem (grand slam) ────────────────────────────────────────────────
    // The taker collects or pays a fixed amount from/to each defender.
    // The partner (5-player only) is not involved.
    val cBonus = chelemBonus(details.chelem)
    return if (cBonus == 0) scoresAfterPoignee else {
        scoresAfterPoignee.mapValues { (player, score) ->
            when (player) {
                takerName           -> score + cBonus * numDefenders  // taker collects/pays all
                details.partnerName -> score                          // partner unaffected
                else                -> score - cBonus                 // each defender pays/receives
            }
        }
    }
}

// Computes each player's cumulative total across all rounds.
//
// This is a pure function (no Compose/UI dependencies) so it can be unit-tested
// on the JVM and reused across multiple screens (e.g. ScoreHistoryScreen and FinalScoreScreen).
//
// Skipped rounds contribute 0 to every player's total because their `playerScores` is emptyMap().
//
// Example (2 rounds, 3 players):
//   Round 1 → Alice +50, Bob −25, Charlie −25
//   Round 2 → Alice −30, Bob +15, Charlie +15
//   Result  → Alice +20, Bob −10, Charlie −10
fun computeFinalTotals(
    playerNames: List<String>,
    roundHistory: List<RoundResult>
): Map<String, Int> =
    playerNames.associateWith { name ->
        roundHistory.sumOf { it.playerScores.getOrDefault(name, 0) }
    }

// Holds the data for one row in the score table — used by buildScoreTableData()
// and consumed by the ScoreTableRow composable in UiComponents.kt.
//
// `cells`       : formatted text for every column (round number first, then player totals).
// `scoreValues` : raw integers for colour coding — null at index 0 (round-number column
//                 has no semantic colour), non-null elsewhere.
data class ScoreRowData(
    val cells: List<String>,
    val scoreValues: List<Int?>
)

// Builds the list of per-round data rows for the score table.
//
// This pure function (no Compose/UI dependencies) was extracted to eliminate the
// identical running-totals accumulation loop that was copy-pasted between
// ScoreHistoryScreen and FinalScoreScreen (issue #75). Keeping it here means any
// bug fix or formatting change automatically applies to both screens, and it can
// be unit-tested on the JVM without an emulator.
//
// Algorithm: iterate rounds in order, maintaining a mutable running-total map.
// For each round:
//   1. Add the round's per-player delta to the running totals.
//   2. Format each total as "+N" (≥0) or "-N" (<0).
//   3. Emit a ScoreRowData with the formatted strings and the raw integer totals.
//
// Skipped rounds (playerScores == emptyMap) contribute 0 to every player's total.
fun buildScoreTableData(
    playerNames: List<String>,
    roundHistory: List<RoundResult>
): List<ScoreRowData> {
    // Start every player at 0 and accumulate as we iterate.
    val runningTotals = playerNames.associateWith { 0 }.toMutableMap()

    return roundHistory.map { round ->
        // Add this round's contribution to each player's running total.
        for (name in playerNames) {
            runningTotals[name] =
                (runningTotals[name] ?: 0) + round.playerScores.getOrDefault(name, 0)
        }

        // Formatted cell strings: round number first, then cumulative totals with sign.
        val cells = buildList {
            add(round.roundNumber.toString())
            for (name in playerNames) {
                val total = runningTotals[name] ?: 0
                // Always show the sign so positive/negative is immediately visible.
                add(total.withSign())
            }
        }

        // Parallel raw integers for colour coding.
        // Index 0 is null — the round-number column has no semantic colour.
        val scoreValues: List<Int?> = buildList {
            add(null)
            for (name in playerNames) {
                add(runningTotals[name] ?: 0)
            }
        }

        ScoreRowData(cells = cells, scoreValues = scoreValues)
    }
}

// SavedGame is a snapshot of a completed game stored on the device.
//
// It contains everything needed to display a summary card in the "Past Games" list:
//   id          : a unique identifier generated when the game is saved (UUID string).
//   datestamp   : Unix timestamp in milliseconds (System.currentTimeMillis()) — used to
//                 display the date and to sort games newest-first.
//   playerNames : display names at the time the game was played.
//   rounds      : the full list of RoundResult entries from that game session.
//   finalScores : cumulative total per player (pre-computed for fast display).
//
// All fields are val (immutable) because a past game never changes after it is saved.
@Serializable
data class SavedGame(
    val id: String,
    val datestamp: Long,
    val playerNames: List<String>,
    val rounds: List<RoundResult>,
    val finalScores: Map<String, Int>
)

// InProgressGame captures the state of a game that has not been ended yet.
// It is written to DataStore after every round so that if the app is closed,
// the game can be resumed exactly where it left off.
//
//   gameId        : a stable UUID generated when the game starts. It is carried all
//                   the way through to SavedGame.id when the game is ended. This
//                   guarantees that ending the same game multiple times (e.g. after
//                   navigating back from the Final Score screen) always produces the
//                   same ID, so GameStorage can upsert instead of duplicating.
//                   Default "" means the field is optional in stored JSON — old
//                   DataStore entries without this field deserialise safely, and
//                   GameScreen generates a fresh UUID whenever it encounters a blank.
//   playerNames   : display names used during the game (already resolved from raw input).
//   currentRound  : the round number that would be played next (always ≥ 2 after the
//                   first round, because saving happens after incrementing).
//   startingIndex : the index into playerNames of the player who took first in round 1.
//                   Needed to restore the taker-rotation formula correctly.
//   rounds        : all rounds completed so far, in chronological order.
@Serializable
data class InProgressGame(
    val gameId: String = "",
    val playerNames: List<String>,
    val currentRound: Int,
    val startingIndex: Int,
    val rounds: List<RoundResult>
)

// Returns the name(s) of the player(s) with the highest cumulative total.
//
// Returns an empty list when `totals` is empty (no players).
// Returns multiple names when two or more players share the highest score (tie).
//
// Examples:
//   {Alice: 50, Bob: -25, Charlie: -25} → ["Alice"]
//   {Alice: 10, Bob: 10, Charlie: -20}  → ["Alice", "Bob"]   (tie)
fun findWinners(totals: Map<String, Int>): List<String> {
    val maxScore = totals.values.maxOrNull() ?: return emptyList()
    // `filterValues` keeps only entries whose value matches the max, then `.keys`
    // gives back the player names — no need to unpack each entry manually.
    return totals.filterValues { it == maxScore }.keys.toList()
}

// Stores the outcome of a single completed round.
// `contract`     is null when the round was skipped (no contract announced).
// `details`      is null when the round was skipped (there is nothing to score).
// `won`          is null when skipped, true if the taker won, false if they lost.
// `playerScores` maps each player's display name to their score for this round.
//                It is an empty map for skipped rounds.
@Serializable
data class RoundResult(
    val roundNumber: Int,
    val takerName: String,
    val contract: Contract?,                  // null = skipped
    val details: RoundDetails?,               // null = skipped
    val won: Boolean?,                        // null = skipped, true = taker won, false = taker lost
    val playerScores: Map<String, Int> = emptyMap()  // empty for skipped rounds
)

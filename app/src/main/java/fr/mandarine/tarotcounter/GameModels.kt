package fr.mandarine.tarotcounter

import kotlin.math.abs
import kotlinx.serialization.Serializable

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
@Serializable
enum class Chelem(val displayName: String) {
    NONE("None"),
    ANNOUNCED_REALIZED("Announced & realized"),
    ANNOUNCED_NOT_REALIZED("Announced, not realized"),
    NOT_ANNOUNCED_REALIZED("Not announced, realized")
}

// All the bonus and scoring details collected after a contract is chosen.
//
// Fields that are "assigned to a player" use String? — null means nobody claimed that bonus.
// For example, `petitAuBout = "Alice"` means Alice captured the 1 of trump on the last trick.
//
// `partnerName` is only relevant in a 5-player game: the taker calls one other player as their
// silent partner. It is null for 3- and 4-player games.
@Serializable
data class RoundDetails(
    val bouts: Int,             // number of oudlers (0–3) in the taker's tricks
    val points: Int,            // points scored by the taker (0–91)
    val partnerName: String?,   // taker's called partner (5-player only); null otherwise
    val petitAuBout: String?,   // player who captured the 1 of trump on the last trick, or null
    val poignee: String?,       // player who showed a poignée (10+ trumps), or null
    val doublePoignee: String?, // player who showed a double poignée (13+ trumps), or null
    val triplePoignee: String? = null, // player who showed a triple poignée (15+ trumps), or null
    val chelem: Chelem,         // grand slam outcome
    // The player who called or achieved the chelem. Null when chelem == NONE.
    // In a 3- or 4-player game only the taker can call it; in a 5-player game the
    // partner can also announce a chelem. When a chelem is announced, that player
    // leads the first trick of the round — regardless of the normal turn order.
    val chelemPlayer: String? = null
)

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

// Returns the flat poignée (trump show) bonus per defender.
//
// Exactly one of the three parameters should be non-null at most per round.
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
    val pBonus = poigneeBonus(details.poignee, details.doublePoignee, details.triplePoignee)
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
                val sign = if (total >= 0) "+" else ""
                add("$sign$total")
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

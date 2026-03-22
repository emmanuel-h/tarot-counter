package fr.mandarine.tarotcounter

import kotlin.math.abs

// The contracts a player can announce in French Tarot, ordered from weakest to strongest.
// Each entry has a `displayName` shown in the UI and a `multiplier` used in score calculation.
//   PRISE    × 1  (formerly called "Petite" in some regional variants)
//   GARDE    × 2
//   GARDE_SANS   × 4  (taker bets they need no help from the dog)
//   GARDE_CONTRE × 6  (taker gives the dog to the defenders)
enum class Contract(val displayName: String, val multiplier: Int) {
    PRISE("Prise", 1),
    GARDE("Garde", 2),
    GARDE_SANS("Garde Sans", 4),
    GARDE_CONTRE("Garde Contre", 6)
}

// The possible chelem (grand slam) outcomes for a round.
// A chelem means winning every single trick.
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
data class RoundDetails(
    val bouts: Int,             // number of oudlers (0–3) in the taker's tricks
    val points: Int,            // points scored by the taker (0–91)
    val partnerName: String?,   // taker's called partner (5-player only); null otherwise
    val petitAuBout: String?,   // player who captured the 1 of trump on the last trick, or null
    val misere: String?,        // player who declared misère, or null
    val doubleMisere: String?,  // player who declared double misère, or null
    val poignee: String?,       // player who showed a poignée (10+ trumps), or null
    val doublePoignee: String?, // player who showed a double poignée (13+ trumps), or null
    val chelem: Chelem          // grand slam outcome
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

// Stores the outcome of a single completed round.
// `contract`     is null when the round was skipped (no contract announced).
// `details`      is null when the round was skipped (there is nothing to score).
// `won`          is null when skipped, true if the taker won, false if they lost.
// `playerScores` maps each player's display name to their score for this round.
//                It is an empty map for skipped rounds.
data class RoundResult(
    val roundNumber: Int,
    val takerName: String,
    val contract: Contract?,                  // null = skipped
    val details: RoundDetails?,               // null = skipped
    val won: Boolean?,                        // null = skipped, true = taker won, false = taker lost
    val playerScores: Map<String, Int> = emptyMap()  // empty for skipped rounds
)

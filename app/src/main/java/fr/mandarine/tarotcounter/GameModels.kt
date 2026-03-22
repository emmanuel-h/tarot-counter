package fr.mandarine.tarotcounter

// The contracts a player can announce in French Tarot, ordered from weakest to strongest.
// Each entry has a `displayName` shown in the UI.
enum class Contract(val displayName: String) {
    PETITE("Petite"),
    POUSSE("Pousse"),
    GARDE("Garde"),
    GARDE_SANS("Garde Sans"),
    GARDE_CONTRE("Garde Contre")
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
data class RoundDetails(
    val bouts: Int,             // number of oudlers (0–3) in the taker's tricks
    val points: Int,            // points scored by the taker (0–91)
    val petitAuBout: String?,   // player who captured the 1 of trump on the last trick, or null
    val misere: String?,        // player who declared misère, or null
    val doubleMisere: String?,  // player who declared double misère, or null
    val poignee: String?,       // player who showed a poignée (10+ trumps), or null
    val doublePoignee: String?, // player who showed a double poignée (13+ trumps), or null
    val chelem: Chelem          // grand slam outcome
)

// Stores the outcome of a single completed round.
// `contract` is null when the round was skipped (no contract announced).
// `details`  is null when the round was skipped (there is nothing to score).
data class RoundResult(
    val roundNumber: Int,
    val takerName: String,
    val contract: Contract?,   // null = skipped
    val details: RoundDetails? // null = skipped
)

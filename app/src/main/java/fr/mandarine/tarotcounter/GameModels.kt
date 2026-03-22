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

// Stores the outcome of a single completed round.
// `contract` is null when the round was skipped (no contract announced).
data class RoundResult(
    val roundNumber: Int,
    val takerName: String,   // the player who took the hand
    val contract: Contract?  // null = round skipped
)

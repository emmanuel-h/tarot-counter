package fr.mandarine.tarotcounter

import kotlinx.serialization.Serializable

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

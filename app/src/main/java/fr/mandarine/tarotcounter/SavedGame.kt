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
//   playerNames   : display names used during the game (already resolved from raw input).
//   currentRound  : the round number that would be played next (always ≥ 2 after the
//                   first round, because saving happens after incrementing).
//   startingIndex : the index into playerNames of the player who took first in round 1.
//                   Needed to restore the taker-rotation formula correctly.
//   rounds        : all rounds completed so far, in chronological order.
@Serializable
data class InProgressGame(
    val playerNames: List<String>,
    val currentRound: Int,
    val startingIndex: Int,
    val rounds: List<RoundResult>
)

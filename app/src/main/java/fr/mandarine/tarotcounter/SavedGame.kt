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

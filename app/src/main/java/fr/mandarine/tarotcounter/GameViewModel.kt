package fr.mandarine.tarotcounter

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// GameViewModel keeps app-level state that must survive screen rotations and
// bridges the UI (Compose) with storage (DataStore).
//
// It extends AndroidViewModel (instead of plain ViewModel) because it needs an
// Application context to construct GameStorage — a plain ViewModel has no context.
//
// The ViewModel is created once per activity and lives until the activity is
// permanently destroyed (e.g. back-pressed to home, or "New Game" action here).
//
// Constructor strategy:
//   - The `internal` primary constructor accepts a GameStorageInterface so unit
//     tests can inject a FakeGameStorage without touching Android DataStore.
//   - The secondary constructor (the one used by Android's ViewModelProvider)
//     takes only the Application and wires up the real DataStore-backed storage.
class GameViewModel internal constructor(
    application: Application,
    private val storage: GameStorageInterface
) : AndroidViewModel(application) {

    // This is the constructor Android's ViewModelProvider calls via reflection.
    // It delegates to the primary constructor with the real storage implementation.
    constructor(application: Application) : this(application, GameStorage(application))

    // pastGames exposes the list of completed saved games as a StateFlow so Compose
    // can observe it with `collectAsState()`.
    //
    // `stateIn` converts the cold Flow from DataStore into a hot StateFlow:
    //   - `WhileSubscribed(5_000)` keeps the upstream active for 5 s after the
    //     last collector disappears (e.g. during a brief configuration change).
    //     This avoids restarting the DataStore read on every screen rotation.
    //   - `initialValue = emptyList()` provides a value immediately before the
    //     first disk read completes, so the UI never sees a null/uninitialized state.
    val pastGames: StateFlow<List<SavedGame>> = storage.loadGames()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // inProgressGame exposes the currently running (not yet completed) game, or null
    // if there is no game in progress. The UI observes this to show the "Resume" card.
    val inProgressGame: StateFlow<InProgressGame?> = storage.loadInProgressGame()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // locale exposes the user's saved language preference, or null if none has been saved yet.
    // MainActivity uses null to mean "fall back to the device's system locale".
    val locale: StateFlow<AppLocale?> = storage.loadLocale()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // theme exposes the user's saved theme preference, or null if none has been saved yet.
    // MainActivity resolves null → AppTheme.LIGHT (light mode is the default).
    val theme: StateFlow<AppTheme?> = storage.loadTheme()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // ── Game session state ────────────────────────────────────────────────────
    //
    // These fields are set by initGame() and mutated by recordPlayed(), recordSkipped(),
    // and endGame(). Living here (rather than inside the GameScreen composable) lets the
    // three action methods be tested on the JVM without a running Compose tree.

    // Stable identifier for the current game session — reused if the game is ended
    // multiple times (e.g. after navigating back from Final Score) so GameStorage
    // treats repeated saves as upserts instead of duplicates.
    private var _gameId: String = ""

    // Index into _displayNames of the player who took first in round 1.
    // Used to restore the taker-rotation formula correctly after a resume.
    private var _startingIndex: Int = 0

    // Resolved display names for this session (typed name or locale fallback).
    // Set once by initGame(); does not change during the session.
    private var _displayNames: List<String> = emptyList()

    // Observable round counter. `mutableIntStateOf` is a Compose snapshot primitive:
    // any composable that reads `currentRound` automatically recomposes when it changes.
    // `private set` prevents callers from mutating it directly.
    var currentRound by mutableIntStateOf(1)
        private set

    // Observable round history. `mutableStateListOf` notifies Compose when items are
    // added, so the history list and scoreboard recompose automatically after each round.
    val roundHistory = mutableStateListOf<RoundResult>()

    // Read-only accessors — GameScreen reads these but must not write them directly.
    val displayNames: List<String> get() = _displayNames
    val startingIndex: Int get() = _startingIndex
    val gameId: String get() = _gameId

    // The display name of the player whose turn it is to *deal* the cards this round.
    // Deals rotate through players in setup order starting from a random first dealer.
    // This is NOT the attacker — any player can bid and become the attacker.
    // Derived from _startingIndex and currentRound so it updates automatically
    // when currentRound (a snapshot state) changes during composition.
    val currentDealer: String
        get() {
            if (_displayNames.isEmpty()) return ""
            val index = (_startingIndex + currentRound - 1) % _displayNames.size
            return _displayNames[index]
        }

    // Initializes (or restores) a game session.
    //
    // displayNames         : resolved player names — blank entries must have been replaced
    //                        by the caller (e.g. "Player 1") before passing them in.
    // inProgressGame       : non-null when resuming a saved game; null for a fresh start.
    // startingIndexOverride: optional index of the player chosen to deal first (0-based).
    //                        Only used for a fresh start (inProgressGame == null).
    //                        Null means the first dealer is picked randomly (legacy behaviour).
    fun initGame(
        displayNames: List<String>,
        inProgressGame: InProgressGame?,
        startingIndexOverride: Int? = null
    ) {
        _displayNames  = displayNames
        _gameId        = inProgressGame?.gameId?.ifBlank { UUID.randomUUID().toString() }
                            ?: UUID.randomUUID().toString()
        // Priority: restored index > explicit choice > random.
        // `inProgressGame?.startingIndex` is non-null when resuming, so the override
        // is correctly ignored in that case.
        _startingIndex = inProgressGame?.startingIndex
            ?: startingIndexOverride
            ?: displayNames.indices.random()
        currentRound   = inProgressGame?.currentRound ?: 1
        roundHistory.clear()
        inProgressGame?.rounds?.let { roundHistory.addAll(it) }
    }

    // Records a completed round (contract + details), advances the round counter,
    // and persists the updated in-progress snapshot to DataStore.
    //
    // All scoring logic runs here — previously these calculations lived inside the
    // GameScreen composable as a local `fun recordPlayed()`, which meant they were
    // recreated on every recomposition and could not be unit-tested.
    //
    // takerName : the player who won the bidding and took the contract (the attacker).
    //             This is explicitly passed by the UI — it is NOT derived from the
    //             dealer rotation, because any player can bid regardless of who deals.
    fun recordPlayed(takerName: String, contract: Contract, details: RoundDetails) {
        val won      = takerWon(details.bouts, details.points)
        val score    = calculateRoundScore(contract, details.bouts, details.points)
        val base     = computePlayerScores(
            allPlayers  = _displayNames,
            takerName   = takerName,
            partnerName = details.partnerName,
            won         = won,
            roundScore  = score
        )
        // 3/4-player: every non-taker is a defender; 5-player: exactly 3 defenders.
        val numDef   = if (details.partnerName != null) 3 else _displayNames.size - 1
        val scores   = applyBonuses(base, contract, details, takerName, won, numDef)
        roundHistory.add(RoundResult(currentRound, takerName, contract, details, won, scores))
        currentRound++
        saveInProgressGame(buildProgressSnapshot())
    }

    // Records a skipped round (no contract selected), advances the round counter,
    // and persists the updated in-progress snapshot.
    fun recordSkipped() {
        roundHistory.add(
            RoundResult(
                roundNumber = currentRound,
                // For skipped rounds, record the dealer's name as the taker (no attacker was chosen).
                takerName   = currentDealer,
                contract    = null,
                details     = null,
                won         = null
            )
        )
        currentRound++
        saveInProgressGame(buildProgressSnapshot())
    }

    // Saves the completed game to the history list when at least one round was played,
    // or clears the in-progress entry without saving if no rounds were recorded.
    //
    // Calling saveGame() also clears the in-progress entry via saveGame()'s implementation.
    // The caller (GameScreen) is responsible for navigating to the Final Score screen.
    fun endGame() {
        if (roundHistory.isNotEmpty()) {
            saveGame(
                SavedGame(
                    id          = _gameId,
                    datestamp   = System.currentTimeMillis(),
                    playerNames = _displayNames,
                    rounds      = roundHistory.toList(),
                    finalScores = computeFinalTotals(_displayNames, roundHistory)
                )
            )
        } else {
            // No rounds were played — discard the in-progress game so it does not
            // linger as a resumable entry on the landing screen (issue #90).
            clearInProgressGame()
        }
    }

    // Builds an InProgressGame snapshot from the current session state.
    // Called after every recordPlayed/recordSkipped to keep DataStore in sync.
    private fun buildProgressSnapshot() = InProgressGame(
        gameId        = _gameId,
        playerNames   = _displayNames,
        currentRound  = currentRound,
        startingIndex = _startingIndex,
        rounds        = roundHistory.toList()
    )

    // Saves the completed game to the past-games list and clears the in-progress state.
    //
    // This is called when the user presses "End Game" (not "New Game"), so the game is
    // persisted immediately — even if the app is closed while the Final Score screen is open.
    // Clearing in-progress in the same coroutine ensures the DataStore is never left in a
    // state where the same game appears both as in-progress and as completed.
    //
    // `viewModelScope.launch` starts a coroutine tied to the ViewModel's lifecycle:
    // if the ViewModel is cleared the coroutine is automatically cancelled,
    // preventing memory leaks.
    fun saveGame(game: SavedGame) {
        viewModelScope.launch {
            storage.addGame(game)
            storage.clearInProgressGame()
        }
    }

    // Writes the current round-by-round state to disk so it can be restored later.
    // Called after every completed or skipped round.
    fun saveInProgressGame(game: InProgressGame) {
        viewModelScope.launch {
            storage.saveInProgressGame(game)
        }
    }

    // Removes the in-progress game from storage.
    // Called when the user starts a fresh game from the setup screen, so a stale
    // in-progress entry from a previous session does not show up as resumable.
    fun clearInProgressGame() {
        viewModelScope.launch {
            storage.clearInProgressGame()
        }
    }

    // Persists the user's chosen language so it is restored on the next app launch.
    fun setLocale(locale: AppLocale) {
        viewModelScope.launch {
            storage.saveLocale(locale)
        }
    }

    // Persists the user's chosen theme so it is restored on the next app launch.
    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            storage.saveTheme(theme)
        }
    }
}

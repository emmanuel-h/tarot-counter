package fr.mandarine.tarotcounter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
class GameViewModel(application: Application) : AndroidViewModel(application) {

    // Storage layer — handles all DataStore read/write operations.
    private val storage = GameStorage(application)

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

    // Saves the completed game to the past-games list and clears the in-progress state.
    // Both writes are performed sequentially in the same coroutine so the DataStore
    // is never left in a state where the game appears both in-progress and completed.
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
}

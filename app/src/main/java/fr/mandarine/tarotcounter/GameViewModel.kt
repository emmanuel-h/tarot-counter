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

    // pastGames exposes the list of saved games as a StateFlow so Compose can
    // observe it with `collectAsState()`.
    //
    // `stateIn` converts the cold Flow from DataStore into a hot StateFlow:
    //   - `WhileSubscribed(5_000)` keeps the upstream active for 5 s after the
    //     last collector disappears (e.g. during a brief configuration change).
    //     This avoids restarting the DataStore read on every screen rotation.
    //   - `initialValue = emptyList()` provides a value immediately before the
    //     first disk read completes, so the UI never sees a null/uninitialized state.
    val pastGames: StateFlow<List<SavedGame>> = storage.loadGames()
        .stateIn(
            scope          = viewModelScope,
            started        = SharingStarted.WhileSubscribed(5_000),
            initialValue   = emptyList()
        )

    // Saves `game` to disk in the background.
    //
    // `viewModelScope.launch` starts a coroutine tied to the ViewModel's lifecycle:
    // if the ViewModel is cleared the coroutine is automatically cancelled,
    // preventing memory leaks.
    fun saveGame(game: SavedGame) {
        viewModelScope.launch {
            storage.addGame(game)
        }
    }
}

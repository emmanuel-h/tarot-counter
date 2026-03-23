package fr.mandarine.tarotcounter

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ── DataStore setup ──────────────────────────────────────────────────────────
//
// `preferencesDataStore` is a Kotlin property delegate that creates (or reuses)
// a DataStore<Preferences> file named "tarot_games" in the app's private storage.
//
// The `by` keyword delegates the getter of `dataStore` to this factory, so every
// call to `context.dataStore` returns the same singleton DataStore instance.
//
// This must be declared at the top level (not inside a class) because the delegate
// uses the Context class as a receiver extension property.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tarot_games")

// The key under which the entire JSON array of saved games is stored.
// `stringPreferencesKey` wraps the name in a type-safe key object for DataStore.
private val GAMES_KEY = stringPreferencesKey("saved_games")

// The key under which the current in-progress game is stored.
// There is at most one in-progress game at a time; it is a single JSON object (not an array).
private val IN_PROGRESS_KEY = stringPreferencesKey("in_progress_game")

// How many past games to keep on the device.
// Older games beyond this limit are dropped when a new game is saved.
private const val MAX_SAVED_GAMES = 20

// The Json instance used for serialization and deserialization.
// `ignoreUnknownKeys = true` makes it tolerant of future schema changes:
// if a new field is added later, old saved data won't throw an error.
private val json = Json { ignoreUnknownKeys = true }

// GameStorage handles all DataStore read and write operations for saved games.
//
// It is a simple class (not a singleton object) so it can be instantiated with a
// Context — DataStore is context-aware and needs the app context to locate its file.
class GameStorage(private val context: Context) {

    // Returns a Flow that emits the current list of saved games whenever it changes.
    //
    // Flow is like a stream of values over time — here it emits the latest list every
    // time the DataStore file is updated (e.g. after a new game is saved).
    //
    // `catch` intercepts I/O exceptions (e.g. corrupted file) and emits empty
    // preferences instead of crashing, so the app degrades gracefully.
    fun loadGames(): Flow<List<SavedGame>> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }   // handle read errors gracefully
            .map { prefs ->
                val raw = prefs[GAMES_KEY] ?: return@map emptyList()
                // `try/catch` guards against JSON parse errors (e.g. corrupted data).
                try {
                    json.decodeFromString<List<SavedGame>>(raw)
                } catch (e: Exception) {
                    emptyList()
                }
            }

    // Returns a Flow that emits the current in-progress game, or null if there is none.
    // The emission pattern is the same as loadGames(): it re-emits whenever DataStore changes.
    fun loadInProgressGame(): Flow<InProgressGame?> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                val raw = prefs[IN_PROGRESS_KEY] ?: return@map null
                try {
                    json.decodeFromString<InProgressGame>(raw)
                } catch (e: Exception) {
                    null
                }
            }

    // Overwrites the in-progress game with the latest state.
    // Called after every round so the saved state is always up to date.
    suspend fun saveInProgressGame(game: InProgressGame) {
        context.dataStore.edit { prefs ->
            prefs[IN_PROGRESS_KEY] = json.encodeToString(game)
        }
    }

    // Removes the in-progress game from DataStore.
    // Called when the game is explicitly ended (New Game) or a fresh game is started.
    suspend fun clearInProgressGame() {
        context.dataStore.edit { prefs ->
            prefs.remove(IN_PROGRESS_KEY)
        }
    }

    // Prepends `game` to the saved games list and persists it to disk.
    //
    // `suspend` means this function must be called from a coroutine (it does I/O).
    // `DataStore.edit` is a suspend function that atomically reads, modifies, and
    // writes the preferences — it is safe to call from multiple coroutines.
    suspend fun addGame(game: SavedGame) {
        context.dataStore.edit { prefs ->
            val raw = prefs[GAMES_KEY] ?: "[]"
            val games = try {
                json.decodeFromString<List<SavedGame>>(raw).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            // Insert the new game at position 0 so the list stays newest-first.
            games.add(0, game)
            // Trim to the limit so the file doesn't grow without bound.
            if (games.size > MAX_SAVED_GAMES) {
                games.subList(MAX_SAVED_GAMES, games.size).clear()
            }
            prefs[GAMES_KEY] = json.encodeToString(games)
        }
    }
}

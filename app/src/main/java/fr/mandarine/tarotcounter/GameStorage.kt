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

// The key under which the user's chosen language is stored ("EN" or "FR").
private val LOCALE_KEY = stringPreferencesKey("app_locale")

// The key under which the user's chosen theme is stored ("LIGHT" or "DARK").
private val THEME_KEY = stringPreferencesKey("app_theme")

// How many past games to keep on the device.
// Older games beyond this limit are dropped when a new game is saved.
private const val MAX_SAVED_GAMES = 20

// The Json instance used for serialization and deserialization.
// `ignoreUnknownKeys = true` makes it tolerant of future schema changes:
// if a new field is added later, old saved data won't throw an error.
private val json = Json { ignoreUnknownKeys = true }

// ── Storage interface ────────────────────────────────────────────────────────
//
// GameStorageInterface defines the contract that both the real DataStore-backed
// implementation and any test fakes must satisfy.
//
// Declaring the abstraction here lets GameViewModel (and any future callers)
// depend only on the interface, making it possible to substitute a simple
// in-memory fake in unit tests — no Android device or DataStore needed.
interface GameStorageInterface {
    fun loadGames(): Flow<List<SavedGame>>
    fun loadInProgressGame(): Flow<InProgressGame?>
    fun loadLocale(): Flow<AppLocale?>
    fun loadTheme(): Flow<AppTheme?>
    suspend fun addGame(game: SavedGame)
    suspend fun saveInProgressGame(game: InProgressGame)
    suspend fun clearInProgressGame()
    suspend fun saveLocale(locale: AppLocale)
    suspend fun saveTheme(theme: AppTheme)
}

// GameStorage handles all DataStore read and write operations for saved games.
//
// It is a simple class (not a singleton object) so it can be instantiated with a
// Context — DataStore is context-aware and needs the app context to locate its file.
class GameStorage(private val context: Context) : GameStorageInterface {

    // Returns a Flow that emits the current list of saved games whenever it changes.
    //
    // Flow is like a stream of values over time — here it emits the latest list every
    // time the DataStore file is updated (e.g. after a new game is saved).
    //
    // `catch` intercepts I/O exceptions (e.g. corrupted file) and emits empty
    // preferences instead of crashing, so the app degrades gracefully.
    override fun loadGames(): Flow<List<SavedGame>> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }   // handle read errors gracefully
            .map { prefs ->
                val raw = prefs[GAMES_KEY] ?: return@map emptyList()
                // `runCatching` is the idiomatic Kotlin alternative to try/catch when
                // you want to convert a thrown exception into a Result value.
                // `getOrDefault` returns the list on success, or the fallback on failure.
                runCatching { json.decodeFromString<List<SavedGame>>(raw) }
                    .getOrDefault(emptyList())
            }

    // Returns a Flow that emits the current in-progress game, or null if there is none.
    // The emission pattern is the same as loadGames(): it re-emits whenever DataStore changes.
    override fun loadInProgressGame(): Flow<InProgressGame?> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                val raw = prefs[IN_PROGRESS_KEY] ?: return@map null
                // `getOrNull` returns the decoded value on success, or null on failure.
                runCatching { json.decodeFromString<InProgressGame>(raw) }.getOrNull()
            }

    // Overwrites the in-progress game with the latest state.
    // Called after every round so the saved state is always up to date.
    override suspend fun saveInProgressGame(game: InProgressGame) {
        context.dataStore.edit { prefs ->
            prefs[IN_PROGRESS_KEY] = json.encodeToString(game)
        }
    }

    // Removes the in-progress game from DataStore.
    // Called when the game is explicitly ended (New Game) or a fresh game is started.
    override suspend fun clearInProgressGame() {
        context.dataStore.edit { prefs ->
            prefs.remove(IN_PROGRESS_KEY)
        }
    }

    // Returns a Flow that emits the user's saved locale preference, or null if none was saved.
    // null means "use the system locale" — MainActivity interprets it that way.
    override fun loadLocale(): Flow<AppLocale?> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                prefs[LOCALE_KEY]?.let { runCatching { AppLocale.valueOf(it) }.getOrNull() }
            }

    // Persists the user's chosen locale to DataStore.
    // `locale.name` stores the enum constant name ("EN" or "FR") as a plain string.
    override suspend fun saveLocale(locale: AppLocale) {
        context.dataStore.edit { prefs ->
            prefs[LOCALE_KEY] = locale.name
        }
    }

    // Returns a Flow that emits the user's saved theme preference, or null if none was saved.
    // null is interpreted as AppTheme.LIGHT by MainActivity (light mode is the default).
    override fun loadTheme(): Flow<AppTheme?> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                // `let` applies the lambda only if the value is non-null.
                // `runCatching` guards against unexpected stored values (e.g. old data).
                prefs[THEME_KEY]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
            }

    // Persists the user's chosen theme to DataStore.
    // `theme.name` stores the enum constant name ("LIGHT" or "DARK") as a plain string.
    override suspend fun saveTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }

    // Upserts `game` into the saved games list and persists the result to disk.
    //
    // "Upsert" means:
    //   - If a game with the same `id` already exists (e.g. the user ended the same
    //     game twice by pressing "End Game" after navigating back from the Final Score
    //     screen), the existing entry is replaced in-place. No duplicate is created.
    //   - If no game with that `id` exists, the new game is prepended (newest-first).
    //
    // In both cases the list is trimmed to MAX_SAVED_GAMES afterwards.
    //
    // `suspend` means this function must be called from a coroutine (it does I/O).
    // `DataStore.edit` is a suspend function that atomically reads, modifies, and
    // writes the preferences — it is safe to call from multiple coroutines.
    override suspend fun addGame(game: SavedGame) {
        context.dataStore.edit { prefs ->
            val raw = prefs[GAMES_KEY] ?: "[]"
            val existing = runCatching { json.decodeFromString<List<SavedGame>>(raw) }
                .getOrDefault(emptyList())

            // Check whether a game with this ID is already stored.
            val alreadyExists = existing.any { it.id == game.id }

            val updated = if (alreadyExists) {
                // Replace the existing entry and keep its current position in the list.
                // `map` returns a new list with the matching element swapped out.
                existing.map { if (it.id == game.id) game else it }
            } else {
                // New game: prepend so the list stays newest-first.
                listOf(game) + existing
            }.take(MAX_SAVED_GAMES)   // always enforce the storage cap

            prefs[GAMES_KEY] = json.encodeToString(updated)
        }
    }
}

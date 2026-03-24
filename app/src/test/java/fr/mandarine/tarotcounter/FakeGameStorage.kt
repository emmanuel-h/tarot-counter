package fr.mandarine.tarotcounter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation of [GameStorageInterface] used exclusively in unit tests.
 *
 * It replaces the real DataStore-backed [GameStorage] with [MutableStateFlow]s so tests
 * run on the JVM without any Android framework or filesystem dependencies.
 *
 * Typical usage:
 *
 *   1. Seed initial state with [seedGames], [seedInProgressGame], or [seedLocale].
 *   2. Create a [GameViewModel] using the internal constructor:
 *        val vm = GameViewModel(Application(), FakeGameStorage())
 *   3. Exercise ViewModel methods, then assert on [addGameCallCount], [lastAddedGame], etc.
 */
class FakeGameStorage : GameStorageInterface {

    // ── In-memory state ───────────────────────────────────────────────────────
    //
    // MutableStateFlow holds the current value and re-emits it whenever it changes,
    // which mirrors the behaviour of the real DataStore-backed Flows.

    private val _games      = MutableStateFlow<List<SavedGame>>(emptyList())
    private val _inProgress = MutableStateFlow<InProgressGame?>(null)
    private val _locale     = MutableStateFlow<AppLocale?>(null)

    // ── Call counters ─────────────────────────────────────────────────────────
    // Each counter increments every time its corresponding suspend function is called.
    // `private set` keeps them read-only from the outside so tests cannot accidentally
    // reset a counter mid-assertion.

    var addGameCallCount         = 0; private set
    var saveInProgressCallCount  = 0; private set
    var clearInProgressCallCount = 0; private set
    var saveLocaleCallCount      = 0; private set

    // ── Last-written values ───────────────────────────────────────────────────
    // Null means the corresponding method has never been called yet.

    var lastAddedGame:       SavedGame?      = null; private set
    var lastSavedInProgress: InProgressGame? = null; private set
    var lastSavedLocale:     AppLocale?      = null; private set

    // ── Test set-up helpers ───────────────────────────────────────────────────
    // Call these before creating the ViewModel to pre-populate the fake's state.

    /** Pre-populate the games list (e.g. to test "Resume" card visibility). */
    fun seedGames(games: List<SavedGame>)       { _games.value = games }

    /** Pre-populate the in-progress game (e.g. to test resume flow). */
    fun seedInProgressGame(game: InProgressGame) { _inProgress.value = game }

    /** Pre-populate the saved locale (e.g. to test locale restoration). */
    fun seedLocale(locale: AppLocale)           { _locale.value = locale }

    // ── GameStorageInterface ──────────────────────────────────────────────────

    override fun loadGames():          Flow<List<SavedGame>>  = _games.asStateFlow()
    override fun loadInProgressGame(): Flow<InProgressGame?>  = _inProgress.asStateFlow()
    override fun loadLocale():         Flow<AppLocale?>       = _locale.asStateFlow()

    override suspend fun addGame(game: SavedGame) {
        addGameCallCount++
        lastAddedGame = game
        // Mirror the production rule: newest game at the front of the list.
        _games.value = listOf(game) + _games.value
    }

    override suspend fun saveInProgressGame(game: InProgressGame) {
        saveInProgressCallCount++
        lastSavedInProgress = game
        _inProgress.value = game
    }

    override suspend fun clearInProgressGame() {
        clearInProgressCallCount++
        _inProgress.value = null
    }

    override suspend fun saveLocale(locale: AppLocale) {
        saveLocaleCallCount++
        lastSavedLocale = locale
        _locale.value = locale
    }
}

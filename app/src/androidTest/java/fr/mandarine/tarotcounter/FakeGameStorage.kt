package fr.mandarine.tarotcounter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation of [GameStorageInterface] used exclusively in instrumented tests.
 *
 * Mirrors the identical class in `src/test/` (unit tests). Both source sets need their own
 * copy because Gradle compiles them independently — the `test` source set is not visible to
 * `androidTest` and vice-versa.
 *
 * Typical usage:
 *
 *   1. Seed initial state with [seedGames], [seedInProgressGame], or [seedLocale].
 *   2. Create a [GameViewModel] using the internal constructor:
 *        val vm = GameViewModel(ApplicationProvider.getApplicationContext(), FakeGameStorage())
 *   3. Exercise the ViewModel or render a composable, then assert on the UI.
 */
class FakeGameStorage : GameStorageInterface {

    // ── In-memory state ───────────────────────────────────────────────────────

    private val _games      = MutableStateFlow<List<SavedGame>>(emptyList())
    private val _inProgress = MutableStateFlow<InProgressGame?>(null)
    private val _locale     = MutableStateFlow<AppLocale?>(null)
    private val _theme      = MutableStateFlow<AppTheme?>(null)

    // ── Call counters ─────────────────────────────────────────────────────────

    var addGameCallCount         = 0; private set
    var saveInProgressCallCount  = 0; private set
    var clearInProgressCallCount = 0; private set
    var saveLocaleCallCount      = 0; private set
    var saveThemeCallCount       = 0; private set

    // ── Last-written values ───────────────────────────────────────────────────

    var lastAddedGame:       SavedGame?      = null; private set
    var lastSavedInProgress: InProgressGame? = null; private set
    var lastSavedLocale:     AppLocale?      = null; private set
    var lastSavedTheme:      AppTheme?       = null; private set

    // ── Test set-up helpers ───────────────────────────────────────────────────

    fun seedGames(games: List<SavedGame>)        { _games.value = games }
    fun seedInProgressGame(game: InProgressGame) { _inProgress.value = game }
    fun seedLocale(locale: AppLocale)            { _locale.value = locale }
    fun seedTheme(theme: AppTheme)               { _theme.value = theme }

    // ── GameStorageInterface ──────────────────────────────────────────────────

    override fun loadGames():          Flow<List<SavedGame>>  = _games.asStateFlow()
    override fun loadInProgressGame(): Flow<InProgressGame?>  = _inProgress.asStateFlow()
    override fun loadLocale():         Flow<AppLocale?>       = _locale.asStateFlow()
    override fun loadTheme():          Flow<AppTheme?>        = _theme.asStateFlow()

    override suspend fun addGame(game: SavedGame) {
        addGameCallCount++
        lastAddedGame = game
        val existing = _games.value
        _games.value = if (existing.any { it.id == game.id }) {
            existing.map { if (it.id == game.id) game else it }
        } else {
            listOf(game) + existing
        }
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

    override suspend fun saveTheme(theme: AppTheme) {
        saveThemeCallCount++
        lastSavedTheme = theme
        _theme.value = theme
    }
}

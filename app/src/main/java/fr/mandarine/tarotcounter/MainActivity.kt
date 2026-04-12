package fr.mandarine.tarotcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme
import java.util.Locale

// Represents which screen is currently shown in the app.
// SETUP    = the player name entry screen.
// GAME     = the active game session.
// SETTINGS = the settings page (theme, language, feedback).
enum class Screen { SETUP, GAME, SETTINGS }

// MainActivity is the entry point of every Android app.
// It extends ComponentActivity, which is the base class for activities
// that use Jetpack Compose for their UI.
class MainActivity : ComponentActivity() {
    // onCreate is called when the app starts (or when the activity is created).
    // savedInstanceState holds data saved from a previous run (e.g. screen rotation).
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Makes the app draw behind the system bars (status bar, navigation bar)
        // for a full-screen "edge-to-edge" look.
        enableEdgeToEdge()

        // setContent replaces the traditional XML layout system.
        // Everything inside this block is Compose UI code.
        setContent {

            // `viewModel()` retrieves (or creates) the GameViewModel for this activity.
            // The ViewModel survives screen rotations and is destroyed only when the
            // activity is permanently finished (e.g. user presses the system back button).
            val gameViewModel: GameViewModel = viewModel()

            // `collectAsState()` subscribes to these StateFlows and converts them into
            // Compose state. Any DataStore update automatically triggers a recomposition.
            val pastGames      by gameViewModel.pastGames.collectAsState()
            val inProgressGame by gameViewModel.inProgressGame.collectAsState()
            val savedLocale    by gameViewModel.locale.collectAsState()
            val savedTheme     by gameViewModel.theme.collectAsState()

            // Determine the active locale:
            //   1. If the user has explicitly chosen a language, use it.
            //   2. Otherwise fall back to the device's system locale.
            //   3. If the system locale is neither French nor English, default to English.
            // `savedLocale` is null only before the first DataStore read completes
            // (typically a few milliseconds); the system fallback prevents any flash.
            val systemLocale = if (Locale.getDefault().language == "fr") AppLocale.FR
                               else AppLocale.EN
            val currentLocale = savedLocale ?: systemLocale

            // Resolve the active theme: null means no preference saved yet → light mode.
            // Light mode is the app default regardless of the device's system setting.
            val currentTheme = savedTheme ?: AppTheme.LIGHT
            val isDarkTheme  = currentTheme == AppTheme.DARK

            // TarotCounterTheme wraps the entire UI with the persisted dark/light palette.
            // It is placed outside CompositionLocalProvider so the theme's background
            // and surface colours apply to the very first frame (no palette flash).
            TarotCounterTheme(darkTheme = isDarkTheme) {

                // CompositionLocalProvider makes both `currentLocale` and `currentTheme`
                // available to every composable in the tree via `.current` accessors.
                // Changing either value triggers a full recomposition of everything below.
                CompositionLocalProvider(
                    LocalAppLocale provides currentLocale,
                    LocalAppTheme  provides currentTheme,
                ) {

                    // Track which screen is visible. `by` delegation means we read/write
                    // `currentScreen` directly instead of `currentScreen.value`.
                    var currentScreen by remember { mutableStateOf(Screen.SETUP) }

                    // Scaffold provides the basic Material Design page structure.
                    // It handles padding so our content doesn't go under system bars.
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                        when (currentScreen) {
                            Screen.SETUP -> LandingScreen(
                                modifier       = Modifier.padding(innerPadding),
                                pastGames      = pastGames,
                                inProgressGame = inProgressGame,
                                // Start a fresh game: resolve display names (blank entries become
                                // "Player N" / "Joueur N"), then initialize the ViewModel session.
                                onStartGame = { rawNames ->
                                    val appStrings = appStrings(currentLocale)
                                    val resolvedNames = rawNames.mapIndexed { i, name ->
                                        name.ifBlank { appStrings.playerFallback(i + 1) }
                                    }
                                    gameViewModel.initGame(resolvedNames, inProgressGame = null)
                                    gameViewModel.clearInProgressGame()
                                    currentScreen = Screen.GAME
                                },
                                // Resume the interrupted game: the stored playerNames are already
                                // resolved (they were saved by initGame on the previous session).
                                onResumeGame = { game ->
                                    gameViewModel.initGame(game.playerNames, game)
                                    currentScreen = Screen.GAME
                                },
                                // Navigate to the settings page when the gear icon is tapped.
                                onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                modifier      = Modifier.padding(innerPadding),
                                // Persist the user's theme choice and re-render the whole UI.
                                onThemeChange = { gameViewModel.setTheme(it) },
                                // Persist the user's language choice and trigger a recomposition
                                // through the StateFlow → collectAsState → CompositionLocalProvider chain.
                                onLocaleChange = { gameViewModel.setLocale(it) },
                                // Navigate back to the setup screen.
                                onBack        = { currentScreen = Screen.SETUP }
                            )
                            Screen.GAME -> GameScreen(
                                viewModel = gameViewModel,
                                // Called when the user presses "New Game" on FinalScoreScreen.
                                // The game is already saved at this point — just navigate away.
                                onEndGame = { currentScreen = Screen.SETUP },
                                modifier  = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

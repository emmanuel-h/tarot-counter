package fr.mandarine.tarotcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme

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
            // Wraps the whole app in our custom theme (colors, fonts, etc.)
            TarotCounterTheme {

                // `viewModel()` retrieves (or creates) the GameViewModel for this activity.
                // The ViewModel survives screen rotations and is destroyed only when the
                // activity is permanently finished (e.g. user presses the system back button).
                val gameViewModel: GameViewModel = viewModel()

                // `collectAsState()` subscribes to these StateFlows and converts them into
                // Compose state. Any DataStore update automatically triggers a recomposition.
                val pastGames      by gameViewModel.pastGames.collectAsState()
                val inProgressGame by gameViewModel.inProgressGame.collectAsState()

                // Track which screen is visible. `by` delegation means we read/write
                // `currentScreen` directly instead of `currentScreen.value`.
                var currentScreen by remember { mutableStateOf(Screen.SETUP) }

                // The finalized player names passed to the game once "Start Game" is pressed
                // (or derived from the saved in-progress state when resuming).
                var confirmedPlayers by remember { mutableStateOf(listOf<String>()) }

                // The in-progress game to restore from, set when the user taps "Resume".
                // Null for a fresh game. Stored in `remember` so it survives recompositions
                // but is not persisted (the ViewModel's StateFlow is the authoritative source).
                var gameToResume by remember { mutableStateOf<InProgressGame?>(null) }

                // Scaffold provides the basic Material Design page structure.
                // It handles padding so our content doesn't go under system bars.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    when (currentScreen) {
                        Screen.SETUP -> LandingScreen(
                            modifier       = Modifier.padding(innerPadding),
                            pastGames      = pastGames,
                            inProgressGame = inProgressGame,
                            // Start a fresh game: clear any leftover in-progress state so
                            // the resume card does not appear after the new game starts.
                            onStartGame = { names ->
                                confirmedPlayers = names
                                gameToResume = null
                                gameViewModel.clearInProgressGame()
                                currentScreen = Screen.GAME
                            },
                            // Resume the interrupted game: restore state and navigate directly
                            // to GameScreen without going through the setup form.
                            onResumeGame = { game ->
                                confirmedPlayers = game.playerNames
                                gameToResume = game
                                currentScreen = Screen.GAME
                            }
                        )
                        Screen.GAME -> GameScreen(
                            playerNames     = confirmedPlayers,
                            inProgressGame  = gameToResume,
                            // Called after every round to keep DataStore in sync.
                            onSaveProgress  = { game -> gameViewModel.saveInProgressGame(game) },
                            // Called when the user presses "End Game" (before FinalScoreScreen).
                            // saveGame() persists the completed entry and clears in-progress.
                            onSaveGame      = { game -> gameViewModel.saveGame(game) },
                            // Called when the user presses "New Game" on FinalScoreScreen.
                            // The game is already saved at this point — just navigate away.
                            onEndGame = {
                                gameToResume = null
                                currentScreen = Screen.SETUP
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

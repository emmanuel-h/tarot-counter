package fr.mandarine.tarotcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

                // Track which screen is visible. `by` delegation means we read/write
                // `currentScreen` directly instead of `currentScreen.value`.
                var currentScreen by remember { mutableStateOf(Screen.SETUP) }

                // The finalized player names passed to the game once "Start Game" is pressed.
                // We keep a separate copy so the game isn't affected if the setup state changes.
                var confirmedPlayers by remember { mutableStateOf(listOf<String>()) }

                // Scaffold provides the basic Material Design page structure.
                // It handles padding so our content doesn't go under system bars.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    when (currentScreen) {
                        Screen.SETUP -> LandingScreen(
                            modifier = Modifier.padding(innerPadding),
                            // Lambda called when the user presses "Start Game".
                            // It receives the player names and triggers the screen switch.
                            onStartGame = { names ->
                                confirmedPlayers = names
                                currentScreen = Screen.GAME
                            }
                        )
                        Screen.GAME -> GameScreen(
                            playerNames = confirmedPlayers,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

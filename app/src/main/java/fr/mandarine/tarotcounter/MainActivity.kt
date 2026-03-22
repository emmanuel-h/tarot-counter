package fr.mandarine.tarotcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme

// Represents which screen is currently shown in the app.
// SETUP = the player name entry screen.
// GAME  = the active game session.
enum class Screen { SETUP, GAME }

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

                    // `when` is Kotlin's switch/match expression.
                    // Each branch renders the appropriate screen based on navigation state.
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

// LandingScreen lets the user configure how many players there are and enter their names.
//
// onStartGame: a callback (lambda) invoked when the user presses "Start Game".
//   It receives the finalized list of player names as a List<String>.
//   `(List<String>) -> Unit` = a function that takes a List<String> and returns nothing.
//   The default `{}` means "do nothing" — useful for the @Preview below.
@Composable
fun LandingScreen(
    modifier: Modifier = Modifier,
    onStartGame: (List<String>) -> Unit = {}
) {
    // `remember` keeps a value alive across recompositions (UI redraws).
    // `mutableIntStateOf` creates an integer that, when changed, triggers a redraw.
    var selectedPlayers by remember { mutableIntStateOf(3) }

    // `mutableStateListOf` creates an observable list: any change triggers a UI redraw.
    // Initialized with 3 empty strings matching the default player count.
    val playerNames = remember { mutableStateListOf("", "", "") }

    // Column stacks children vertically. `verticalScroll` makes it scrollable
    // in case the content (name fields + button) doesn't fit on smaller screens.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Text displays a string. MaterialTheme.typography gives us pre-defined
        // text styles that match Material Design (headlineLarge is a big bold title).
        Text(
            text = "My pretty app",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Number of players",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Row places its children side by side horizontally.
        // `spacedBy(8.dp)` adds 8dp of space between each chip.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Loop from 3 to 5 (inclusive) to create one chip per player count.
            for (n in 3..5) {
                // FilterChip is a selectable chip from Material Design 3.
                // `selected` controls whether this chip appears highlighted.
                FilterChip(
                    selected = selectedPlayers == n,
                    onClick = {
                        selectedPlayers = n
                        // Resize the name list to match the new player count.
                        // If the new count is larger, pad with empty strings.
                        // If smaller, drop the extra entries from the end.
                        while (playerNames.size < n) playerNames.add("")
                        while (playerNames.size > n) playerNames.removeAt(playerNames.lastIndex)
                    },
                    label = { Text(n.toString()) } // chip label: "3", "4", or "5"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Player names",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Loop over each player slot and render a text field for their name.
        // `playerNames.indices` gives us 0, 1, 2 (or up to 4 for 5 players).
        for (i in playerNames.indices) {
            // OutlinedTextField is a Material Design text input with a visible border.
            // `value` is the current text; `onValueChange` updates it when the user types.
            OutlinedTextField(
                value = playerNames[i],
                onValueChange = { playerNames[i] = it }, // `it` is the new string the user typed
                label = { Text("Player ${i + 1}") },
                singleLine = true,                        // prevent multi-line input
                modifier = Modifier
                    .fillMaxWidth(0.8f)                   // 80% of screen width
                    .padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // "Start Game" button. Tapping it calls `onStartGame` with a snapshot of the
        // current names. `toList()` converts the mutable state list to a regular
        // immutable List<String> so it's safe to pass to another screen.
        Button(
            onClick = { onStartGame(playerNames.toList()) },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Start Game")
        }
    }
}

// @Preview lets Android Studio render this composable in the IDE without running the app.
@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
    TarotCounterTheme {
        LandingScreen()
    }
}

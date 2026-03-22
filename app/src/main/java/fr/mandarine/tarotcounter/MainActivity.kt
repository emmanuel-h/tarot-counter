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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
                // Scaffold provides the basic Material Design page structure.
                // It handles padding so our content doesn't go under system bars.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // innerPadding is provided by Scaffold so we don't draw
                    // under the status bar or navigation bar.
                    LandingScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// @Composable marks a function as a UI building block in Jetpack Compose.
// These functions describe *what* the UI looks like — Compose handles rendering.
// Composable functions are reusable and can be nested inside each other.
@Composable
fun LandingScreen(modifier: Modifier = Modifier) {
    // `remember` keeps a value alive across recompositions (UI redraws).
    // `mutableIntStateOf` creates an integer that, when changed, triggers a redraw.
    // `by` is Kotlin delegation syntax — it lets us use `selectedPlayers` directly
    // instead of `selectedPlayers.value`.
    var selectedPlayers by remember { mutableIntStateOf(3) }

    // Column stacks its children vertically.
    // Alignment.CenterHorizontally + Arrangement.Center centers everything on screen.
    Column(
        modifier = modifier.fillMaxSize(), // take up the full screen
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Text displays a string. MaterialTheme.typography gives us pre-defined
        // text styles that match Material Design (headlineLarge is a big bold title).
        Text(
            text = "My pretty app",
            style = MaterialTheme.typography.headlineLarge
        )

        // Spacer adds empty space between elements (like a margin).
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
                // `onClick` is a lambda (anonymous function) called when the user taps it —
                // here it updates selectedPlayers, which triggers a UI redraw.
                FilterChip(
                    selected = selectedPlayers == n,
                    onClick = { selectedPlayers = n },
                    label = { Text(n.toString()) } // chip label: "3", "4", or "5"
                )
            }
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

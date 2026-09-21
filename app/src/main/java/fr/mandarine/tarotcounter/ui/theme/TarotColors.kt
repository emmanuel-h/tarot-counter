package fr.mandarine.tarotcounter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The avatar colour of one seat plus the colour of the initials drawn on it.
 */
@Immutable
class PlayerTone(val container: Color, val content: Color)

/**
 * Colours the Material 3 [androidx.compose.material3.ColorScheme] has no slot for.
 *
 * Material 3 knows "primary", "surface", "error"… but not "brass ornament",
 * "positive score" or "the colour of seat 3". Those live here and are handed to
 * every composable through [LocalTarotColors] — the same mechanism Material uses
 * for its own colour scheme.
 *
 * `@Immutable` tells the Compose compiler the values never change after creation,
 * so composables reading them can skip recomposition safely.
 *
 * @property brass           Brass for non-text ornaments (leader ring, suit marks).
 * @property brassText       Brass that is dark (or light) enough to be used for text.
 * @property positive        Positive scores.
 * @property negative        Negative scores.
 * @property felt            Felt-green ground of the "hero" cards (resume game, winner).
 *                           Stays deep green in dark mode too, unlike `colorScheme.primary`.
 * @property onFelt          Text on [felt].
 * @property brassOnFelt     Brass text on [felt] (e.g. "GAME IN PROGRESS").
 * @property winnerHighlight Background of the winner column in score tables.
 * @property playerTones     One tone per seat, in seat order.
 * @property onPlayerTone    Initials colour drawn on a player tone.
 */
@Immutable
class TarotColors(
    val brass: Color,
    val brassText: Color,
    val positive: Color,
    val negative: Color,
    val felt: Color,
    val onFelt: Color,
    val brassOnFelt: Color,
    val winnerHighlight: Color,
    val playerTones: List<Color>,
    val onPlayerTone: Color,
) {
    init {
        // A theme with no player tones would make playerTone() divide by zero.
        require(playerTones.isNotEmpty()) { "playerTones must not be empty" }
    }

    /**
     * Returns the stable colour of the player sitting at [seatIndex] (0-based).
     *
     * Indices past the last tone wrap around (seat 5 of a 5-tone palette → seat 0),
     * so the call can never crash, even if a future variant allows more players.
     * `mod` (not `%`) keeps the result positive for negative indices too.
     */
    fun playerTone(seatIndex: Int): PlayerTone =
        PlayerTone(
            container = playerTones[seatIndex.mod(playerTones.size)],
            content   = onPlayerTone,
        )
}

/** Light "Salon" extended colours. */
val LightTarotColors = TarotColors(
    brass           = SalonBrass,
    brassText       = SalonBrassText,
    positive        = SalonPositive,
    negative        = SalonNegative,
    felt            = SalonFelt,
    onFelt          = SalonIvory,
    brassOnFelt     = SalonBrassOnFelt,
    winnerHighlight = SalonWinnerTint,
    playerTones     = SalonPlayerTones,
    onPlayerTone    = SalonPaperWhite,
)

/** Dark "Salon at night" extended colours. */
val DarkTarotColors = TarotColors(
    brass           = NightBrass,
    brassText       = NightBrass,
    positive        = NightPositive,
    negative        = NightNegative,
    felt            = SalonFelt,
    onFelt          = SalonIvory,
    brassOnFelt     = SalonBrassOnFelt,
    winnerHighlight = NightWinnerTint,
    playerTones     = NightPlayerTones,
    onPlayerTone    = NightFeltGround,
)

/**
 * CompositionLocal carrying the active [TarotColors].
 *
 * `staticCompositionLocalOf` is the right choice for a theme: the value almost
 * never changes (only when the user toggles light/dark), and when it does the
 * whole tree recomposes anyway. The default is the light palette so previews
 * and tests that forget [TarotCounterTheme] still render sensibly.
 */
val LocalTarotColors = staticCompositionLocalOf { LightTarotColors }

/**
 * Shortcut so screens can write `MaterialTheme.tarotColors.positive`, right next
 * to `MaterialTheme.colorScheme.primary`.
 *
 * This is a Kotlin *extension property*: it adds a read-only property to the
 * existing `MaterialTheme` object without modifying the library.
 */
val MaterialTheme.tarotColors: TarotColors
    @Composable
    @ReadOnlyComposable
    get() = LocalTarotColors.current

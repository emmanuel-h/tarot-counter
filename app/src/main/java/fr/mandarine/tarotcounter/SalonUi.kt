package fr.mandarine.tarotcounter

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.BreakIterator

// ─────────────────────────────────────────────────────────────────────────────
// Pure (non-composable) logic behind the Salon shared components (issue #196).
//
// Everything here runs on a plain JVM, so it is covered by fast unit tests in
// src/test/ (SalonUiTest) and by mutation testing. The composables that use it
// live in UiComponents.kt.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns the letter shown inside a player's avatar circle.
 *
 * - Leading/trailing spaces are ignored (" alice" → "A").
 * - A blank name gives "?" so the circle is never empty.
 * - The first *grapheme* is used, not the first `Char`. A grapheme is what a
 *   reader sees as one character: "É" typed as E + combining accent is two
 *   `Char`s but one grapheme. [BreakIterator] finds grapheme boundaries for us,
 *   so we never cut a character in half.
 * - The result is upper-cased ("élodie" → "É").
 * - A name of several words ending in a number returns that number: the default
 *   names "Player 3" / "Joueur 3" would otherwise all show the same "P"/"J".
 */
fun playerInitial(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"

    // split(Regex("\\s+")) cuts on any run of spaces: "Player  3" → ["Player", "3"].
    val words = trimmed.split(Regex("\\s+"))
    val last  = words.last()
    if (words.size > 1 && last.all { it.isDigit() }) return last

    // getCharacterInstance() iterates over user-perceived characters (graphemes).
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(trimmed)
    // next() moves to the end of the first grapheme and returns its index.
    val end = iterator.next()
    return trimmed.substring(0, end).uppercase()
}

/**
 * The three avatar sizes of the Salon design.
 *
 * An `enum class` is a fixed list of named values; each value here carries its
 * circle diameter and the font size of its initial.
 *
 * @property diameter   Width and height of the circle.
 * @property fontSizeSp Font size of the initial, in sp.
 */
enum class AvatarSize(val diameter: Dp, val fontSizeSp: Float) {
    /** 24 dp — used in overlapping [AvatarStack]s. */
    S(24.dp, 11f),
    /** 36 dp — standard list rows and text-field leading slots. */
    M(36.dp, 14f),
    /** 56 dp — hero spots (leader, winner). */
    L(56.dp, 22f);

    /**
     * How much each avatar slides under its left neighbour in an [AvatarStack]:
     * a third of the diameter, which keeps every initial readable.
     */
    val stackOverlap: Dp get() = diameter / 3

    /** Width of the ring drawn around each avatar in a stack, to separate them. */
    val stackRing: Dp get() = if (this == S) 1.5.dp else 2.dp
}

/**
 * The three sizes of [ScoreText].
 *
 * @property fontSizeSp Font size in sp.
 */
enum class ScoreSize(val fontSizeSp: Float) {
    /** Inline scores in history rows. */
    S(15f),
    /** Scores in standings rows. */
    M(17f),
    /** The leader's or winner's hero score. */
    XL(30f)
}

/**
 * The four card suits shown by [SuitDivider].
 *
 * Each glyph is followed by U+FE0E, the "text presentation" variation selector.
 * Without it, Android may draw ♥ and ♦ with the colour emoji font; with it,
 * they are drawn as plain text glyphs that take the brass text colour.
 */
const val TEXT_PRESENTATION = "︎"
val SUIT_GLYPHS: String = listOf("♠", "♥", "♦", "♣").joinToString(" ") { it + TEXT_PRESENTATION }

/** Maximum number of icon buttons on the right of a [SalonTopBar]. */
const val MAX_TOP_BAR_ACTIONS = 2

/**
 * One icon button shown on the right of a [SalonTopBar].
 *
 * A plain (non-data) class: we never compare actions, so the generated
 * equals/hashCode of a data class would be dead code.
 *
 * @property icon               The icon drawn in the button.
 * @property contentDescription Read aloud by screen readers (the button has no text).
 * @property onClick            Called when the button is tapped.
 */
@Immutable
class TopBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)

/**
 * Throws if more than [MAX_TOP_BAR_ACTIONS] actions are passed to a [SalonTopBar].
 * More than two icons crowd the title on a phone, so this is a programming error.
 */
fun requireValidTopBarActions(actions: List<TopBarAction>) {
    require(actions.size <= MAX_TOP_BAR_ACTIONS) {
        "SalonTopBar supports at most $MAX_TOP_BAR_ACTIONS actions, got ${actions.size}"
    }
}

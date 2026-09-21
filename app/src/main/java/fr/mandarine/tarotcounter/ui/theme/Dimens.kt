package fr.mandarine.tarotcounter.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Salon spacing and sizing tokens.
 *
 * Everything sits on an **8 dp grid**: gaps and paddings are multiples of 8
 * (with 4 dp as a half-step for tight spots). Using named constants instead of
 * raw numbers keeps every screen visually consistent.
 *
 * `object` makes a single shared instance — access as `Dimens.ScreenMargin`.
 */
object Dimens {
    /** Half grid step — tight gaps (icon ↔ label). */
    val SpaceXs = 4.dp
    /** One grid step. */
    val SpaceS = 8.dp
    /** Two grid steps — gap between items in a card. */
    val SpaceM = 16.dp
    /** Gap between sections of a screen. */
    val SpaceL = 24.dp
    /** Four grid steps — large separations. */
    val SpaceXl = 32.dp

    /** Horizontal margin between the screen edge and the content. */
    val ScreenMargin = 20.dp
    /** Inner padding of a card. */
    val CardPadding = 20.dp

    /** Height of a primary call-to-action button ("Start game", "Confirm round"). */
    val PrimaryButtonHeight = 56.dp
    /** Height of a secondary button ("Skip round", "Main menu"). */
    val SecondaryButtonHeight = 48.dp

    /**
     * Maximum width of the content column. On tablets (e.g. a 10" tablet in
     * landscape) the content stays centred at this width instead of stretching.
     */
    val MaxContentWidth = 600.dp
}

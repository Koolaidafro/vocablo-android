// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing steps used between and inside components (FR-16).
 *
 * Design: a plain object of constants rather than a CompositionLocal, because spacing does
 * not change with the theme or the screen size. Named by size, so screens pick a step
 * instead of inventing a number.
 */
object Spacing {
    /** Between an icon and its label. */
    val xs: Dp = 4.dp

    /** Between items in a tight group, such as buttons in a row. */
    val s: Dp = 8.dp

    /** Between lines of a card's content. */
    val m: Dp = 12.dp

    /** Screen edges and the inside of cards. */
    val l: Dp = 16.dp

    /** Inside large surfaces, such as the study card. */
    val xl: Dp = 24.dp

    /** Between unrelated sections. */
    val xxl: Dp = 32.dp
}

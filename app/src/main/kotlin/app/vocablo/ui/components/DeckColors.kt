package app.vocablo.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Deck colors come from the theme, so every deck looks right in every theme and mode. */
@Composable
fun deckColor(index: Int): Color {
    val c = MaterialTheme.colorScheme
    val options = listOf(c.primary, c.tertiary, c.secondary, c.onPrimaryContainer, c.onTertiaryContainer)
    return options[Math.floorMod(index, options.size)]
}

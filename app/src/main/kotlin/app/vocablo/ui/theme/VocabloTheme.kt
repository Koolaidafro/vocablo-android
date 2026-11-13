package app.vocablo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.vocablo.design.Palette
import app.vocablo.design.ThemeId
import app.vocablo.design.ThemeMode
import app.vocablo.design.Themes
import app.vocablo.design.paletteFor

/**
 * Wraps the app in its colours, type and shapes.
 *
 * Design: the single entry point for the visual design. Colours come from the chosen theme
 * in core/design; type and shapes are the same in every theme (FR-14, FR-16).
 */
@Composable
fun VocabloTheme(
    themeId: ThemeId = Themes.default,
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val theme = Themes.byId(themeId)
    val isDark = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = theme.paletteFor(mode, systemDark)
    MaterialTheme(
        colorScheme = palette.toColorScheme(isDark),
        typography = VocabloTypography,
        shapes = VocabloShapes,
        content = content,
    )
}

private fun Palette.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        secondary = Color(secondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(surface),
        onBackground = Color(onSurface),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceContainerHigh),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceContainerLowest = Color(surface),
        surfaceContainerLow = Color(surfaceContainer),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHigh),
        outline = Color(onSurfaceVariant),
        outlineVariant = Color(outline),
    )
}

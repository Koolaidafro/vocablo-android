// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii for every surface in the app (FR-16).
 *
 * Design: rounder than Material's defaults for a softer, current look. Components read these
 * through MaterialTheme.shapes, so no screen hard-codes a radius.
 */
val VocabloShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

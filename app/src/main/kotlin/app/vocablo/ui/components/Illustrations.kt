// Vocablo: Spanish flashcards for Android.
//
// Custom illustrations drawn in the current theme's colours (FR-15).
// Design: drawn with Canvas instead of shipped as images, so each one follows all five
// themes in light and dark without a separate file per theme, and stays sharp at any size.
// Shapes are given as fractions of the drawing's size, so the art scales as a whole.
package app.vocablo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** A small stack of blank cards with a "+" badge, for a deck with no cards yet. */
@Composable
fun EmptyDeckArt(modifier: Modifier = Modifier, size: Dp = 160.dp) {
    val colors = MaterialTheme.colorScheme
    val back = colors.secondaryContainer
    val middle = colors.primaryContainer
    val front = colors.surfaceContainerHigh
    val line = colors.outlineVariant
    val badge = colors.primary
    val onBadge = colors.onPrimary
    val sparkle = colors.tertiary
    Canvas(modifier.size(size).semantics { contentDescription = "An empty deck" }) {
        drawCard(back, tilt = -12f, shift = Offset(-0.08f, 0.02f))
        drawCard(middle, tilt = 8f, shift = Offset(0.07f, 0f))
        drawCard(front, tilt = 0f, shift = Offset.Zero, outline = line)
        // Two blank "text" lines on the front card.
        listOf(0.42f, 0.52f).forEachIndexed { i, y ->
            val width = if (i == 0) 0.30f else 0.20f
            drawLine(line, Offset(this.size.width * (0.5f - width / 2), this.size.height * y), Offset(this.size.width * (0.5f + width / 2), this.size.height * y), strokeWidth = this.size.width * 0.03f, cap = StrokeCap.Round)
        }
        drawPlusBadge(badge, onBadge, center = Offset(this.size.width * 0.72f, this.size.height * 0.74f), radius = this.size.width * 0.13f)
        drawSparkle(sparkle, Offset(this.size.width * 0.16f, this.size.height * 0.18f), this.size.width * 0.05f)
        drawSparkle(sparkle, Offset(this.size.width * 0.86f, this.size.height * 0.24f), this.size.width * 0.035f)
    }
}

/**
 * A ring of confetti and rays behind [content], for the end of a study session.
 *
 * Design: a frame around Tuzo rather than a separate picture, so the mascot stays the hero
 * and the art only adds the celebration.
 */
@Composable
fun CelebrationArt(modifier: Modifier = Modifier, size: Dp = 220.dp, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val glow = colors.primaryContainer
    val confetti = listOf(colors.primary, colors.tertiary, colors.secondary, colors.error)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            drawCircle(glow, radius = this.size.minDimension * 0.36f, center = center)
            repeat(CONFETTI_PIECES) { i ->
                val angle = Math.toRadians(i * 360.0 / CONFETTI_PIECES - 90).toFloat()
                // Alternate two rings so the confetti looks scattered, not lined up.
                val ring = if (i % 2 == 0) 0.44f else 0.40f
                val piece = center + Offset(cos(angle), sin(angle)) * (this.size.minDimension * ring)
                val color = confetti[i % confetti.size]
                if (i % 3 == 0) {
                    drawCircle(color, radius = this.size.minDimension * 0.022f, center = piece)
                } else {
                    rotate(degrees = i * 37f, pivot = piece) {
                        val w = this.size.minDimension * 0.05f
                        drawRoundRect(color, topLeft = piece - Offset(w / 2, w / 4), size = Size(w, w / 2), cornerRadius = CornerRadius(w / 4))
                    }
                }
            }
        }
        content()
    }
}

/** How many confetti pieces ring the celebration. */
private const val CONFETTI_PIECES = 18

/** Draws one card of the stack, tilted and shifted by fractions of the drawing's size. */
private fun DrawScope.drawCard(color: Color, tilt: Float, shift: Offset, outline: Color? = null) {
    val cardSize = Size(size.width * 0.52f, size.height * 0.66f)
    val topLeft = Offset((size.width - cardSize.width) / 2 + shift.x * size.width, (size.height - cardSize.height) / 2 + shift.y * size.height)
    val radius = CornerRadius(size.width * 0.07f)
    rotate(tilt, pivot = topLeft + Offset(cardSize.width / 2, cardSize.height / 2)) {
        drawRoundRect(color, topLeft, cardSize, radius)
        if (outline != null) drawRoundRect(outline, topLeft, cardSize, radius, style = Stroke(width = size.width * 0.012f))
    }
}

/** Draws a filled circle with a "+" in it. */
private fun DrawScope.drawPlusBadge(fill: Color, mark: Color, center: Offset, radius: Float) {
    drawCircle(fill, radius, center)
    val arm = radius * 0.45f
    val stroke = radius * 0.22f
    drawLine(mark, center - Offset(arm, 0f), center + Offset(arm, 0f), stroke, StrokeCap.Round)
    drawLine(mark, center - Offset(0f, arm), center + Offset(0f, arm), stroke, StrokeCap.Round)
}

/** Draws a four-pointed sparkle as two crossed strokes. */
private fun DrawScope.drawSparkle(color: Color, center: Offset, arm: Float) {
    val stroke = arm * 0.45f
    drawLine(color, center - Offset(arm, 0f), center + Offset(arm, 0f), stroke, StrokeCap.Round)
    drawLine(color, center - Offset(0f, arm), center + Offset(0f, arm), stroke, StrokeCap.Round)
}

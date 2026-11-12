package app.vocablo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private val CONFETTI_COLORS = listOf(
    Color(0xFF1E6B58), Color(0xFFF2B33D), Color(0xFFE5584F), Color(0xFF4A90D9), Color(0xFF9B6BD3), Color(0xFF7CC576),
)

private class Piece(val x: Float, val delay: Float, val speed: Float, val drift: Float, val spin: Float, val color: Color)

/** A short burst of falling confetti for wins. Draws nothing once it has fallen. Purely decorative. */
@Composable
fun Confetti(modifier: Modifier = Modifier, pieces: Int = 70, durationMillis: Int = 2600, seed: Int = 0) {
    val parts = remember(seed) {
        val r = Random(seed)
        List(pieces) {
            Piece(r.nextFloat(), r.nextFloat() * 0.35f, 0.7f + r.nextFloat() * 0.6f, (r.nextFloat() - 0.5f) * 0.25f, r.nextFloat() * 720f, CONFETTI_COLORS[it % CONFETTI_COLORS.size])
        }
    }
    val progress = remember(seed) { Animatable(0f) }
    LaunchedEffect(seed) { progress.animateTo(1f, tween(durationMillis, easing = LinearEasing)) }
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val w = size.width
        val h = size.height
        val piece = Size(w * 0.018f, w * 0.032f)
        parts.forEach { p ->
            val local = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = -piece.height + local * p.speed * (h + piece.height * 2)
            val x = (p.x + p.drift * local) * w
            rotate(p.spin * local, Offset(x, y)) {
                drawRect(p.color, Offset(x - piece.width / 2, y - piece.height / 2), piece, alpha = 1f - local * 0.4f)
            }
        }
    }
}

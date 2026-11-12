package app.vocablo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vocablo.R

enum class TuzoMood { IDLE, CHEER, SLEEP, WIN, OOPS }

/**
 * Tuzo the gopher. IDLE breathes gently, CHEER hops (used when a session is won),
 * SLEEP sways slowly (nothing due), WIN does a happy spin-hop after a game or chest, and
 * OOPS gives a small head shake after a miss.
 */
@Composable
fun Tuzo(modifier: Modifier = Modifier, size: Dp = 96.dp, mood: TuzoMood = TuzoMood.IDLE) {
    val motion = rememberInfiniteTransition(label = "tuzo")
    val period = when (mood) {
        TuzoMood.CHEER -> 420
        TuzoMood.IDLE -> 1600
        TuzoMood.SLEEP -> 2600
        TuzoMood.WIN -> 700
        TuzoMood.OOPS -> 140
    }
    val t by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(period, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tuzo-motion",
    )
    Image(
        painter = painterResource(R.drawable.tuzo),
        contentDescription = "Tuzo the gopher",
        modifier = modifier
            .size(size)
            .graphicsLayer {
                when (mood) {
                    TuzoMood.CHEER -> {
                        translationY = -t * this.size.height * 0.12f
                        scaleX = 1f + t * 0.04f
                        scaleY = 1f - t * 0.02f + t * 0.06f
                    }
                    TuzoMood.IDLE -> {
                        scaleX = 1f + t * 0.025f
                        scaleY = 1f + t * 0.025f
                    }
                    TuzoMood.SLEEP -> {
                        rotationZ = (t - 0.5f) * 6f
                        alpha = 0.85f
                    }
                    TuzoMood.WIN -> {
                        translationY = -t * this.size.height * 0.2f
                        rotationZ = (t - 0.5f) * 24f
                        scaleX = 1f + t * 0.1f
                        scaleY = 1f + t * 0.1f
                    }
                    TuzoMood.OOPS -> {
                        translationX = (t - 0.5f) * this.size.width * 0.08f
                        rotationZ = (t - 0.5f) * 8f
                    }
                }
            },
    )
}

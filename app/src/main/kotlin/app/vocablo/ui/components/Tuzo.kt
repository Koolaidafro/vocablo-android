// Vocablo: Spanish flashcards for Android.
//
// Tuzo, the app's mascot, in one pose per mood (FR-17, FR-21).
package app.vocablo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vocablo.R

/**
 * Tuzo's moods. Each one names its own drawing, its motion period and what a screen reader
 * says.
 *
 * Design: the mood owns its pose and motion, so a new mood is one entry here and no screen has
 * to know which picture goes with which feeling. The poses were generated from one master
 * drawing so Tuzo looks the same in all of them.
 */
enum class TuzoMood(@param:DrawableRes val drawable: Int, val periodMillis: Int, val description: String) {
    /** Waving hello; the default. */
    IDLE(R.drawable.tuzo_idle, 1600, "Tuzo the gopher waving hello"),

    /** Holding flash cards, ready to study. */
    READY(R.drawable.tuzo_ready, 1600, "Tuzo holding flash cards, ready to go"),

    /** Jumping for joy after a good answer or a finished session. */
    CHEER(R.drawable.tuzo_cheer, 420, "Tuzo jumping for joy"),

    /** Chin in paw while the learner thinks. */
    THINK(R.drawable.tuzo_think, 2000, "Tuzo thinking"),

    /** A kind, sheepish shrug after a miss. Never sad, never blaming (NFR-7). */
    OOPS(R.drawable.tuzo_oops, 140, "Tuzo with a sheepish smile"),

    /** Asleep on a cushion when nothing is due. */
    SLEEP(R.drawable.tuzo_sleep, 2600, "Tuzo napping"),

    /** Holding up a gold medal after a game, chest or badge. */
    WIN(R.drawable.tuzo_proud, 700, "Tuzo proudly holding a gold medal"),
}

/**
 * Tuzo in the given [mood], with a gentle motion that matches it. When the phone's animations
 * are off, Tuzo is drawn still (NFR-4).
 *
 * Design: the pose changes with a short crossfade so a reaction reads as Tuzo moving, not as a
 * picture being swapped. The motion is a single repeating value shaped per mood, which keeps
 * one animation running instead of one per pose.
 */
@Composable
fun Tuzo(modifier: Modifier = Modifier, size: Dp = 96.dp, mood: TuzoMood = TuzoMood.IDLE) {
    if (rememberReduceMotion()) {
        Image(painterResource(mood.drawable), contentDescription = mood.description, modifier = modifier.size(size))
        return
    }
    val motion = rememberInfiniteTransition(label = "tuzo")
    val t by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(mood.periodMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tuzo-motion",
    )
    Crossfade(targetState = mood, animationSpec = tween(POSE_FADE_MILLIS), label = "tuzo-pose", modifier = modifier.size(size)) { shown ->
        Image(
            painter = painterResource(shown.drawable),
            contentDescription = shown.description,
            modifier = Modifier.size(size).graphicsLayer { move(shown, t) },
        )
    }
}

/** Shapes the shared motion value [t] (0 to 1 and back) into each mood's movement. */
private fun GraphicsLayerScope.move(mood: TuzoMood, t: Float) {
    when (mood) {
        TuzoMood.CHEER -> {
            translationY = -t * size.height * 0.10f
            scaleX = 1f + t * 0.04f
            scaleY = 1f + t * 0.04f
        }
        TuzoMood.IDLE, TuzoMood.READY -> {
            scaleX = 1f + t * 0.02f
            scaleY = 1f + t * 0.02f
        }
        TuzoMood.THINK -> rotationZ = (t - 0.5f) * 4f
        TuzoMood.SLEEP -> {
            scaleX = 1f + t * 0.02f
            scaleY = 1f + t * 0.03f
        }
        TuzoMood.WIN -> {
            translationY = -t * size.height * 0.08f
            rotationZ = (t - 0.5f) * 10f
        }
        TuzoMood.OOPS -> {
            translationX = (t - 0.5f) * size.width * 0.05f
            rotationZ = (t - 0.5f) * 6f
        }
    }
}

/** How long one pose fades into the next. */
private const val POSE_FADE_MILLIS = 180

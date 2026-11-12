// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.study

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.vocablo.cards.CardType
import app.vocablo.cards.Card as VocabCard
import app.vocablo.speech.SpeakButton
import app.vocablo.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The study card: tap to flip, swipe right for "Got it", swipe left for "Again".
 *
 * Design: kept apart from [StudyScreen] so the gesture can be tested on its own (FR-1).
 * The screen only passes in state and callbacks; this composable owns the drag and flip
 * animations, which are pure presentation. The drag is detected on a plain box around the
 * card, not on the card itself, so the flip's mirror rotation never changes its direction.
 *
 * @param turn changes after every answer, so the drag offset resets for the next card.
 * @param onSwiped called with true for a right swipe and false for a left swipe.
 */
@Composable
internal fun SwipeCard(
    card: VocabCard,
    flipped: Boolean,
    turn: Int,
    onFlip: () -> Unit,
    onSwiped: (right: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(turn) { Animatable(0f) }
    val flip by animateFloatAsState(if (flipped) FULL_FLIP else 0f, tween(FLIP_MILLIS), label = "flip")
    Box(modifier.fillMaxWidth()) {
        // What the swipe means, revealed behind the card as it moves.
        val drag = offsetX.value
        Text(
            if (drag > 0) "Got it" else "Again",
            color = if (drag > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(if (drag > 0) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(Spacing.xl)
                .alpha((abs(drag) / HINT_FULL_AT).coerceIn(0f, 1f)),
        )
        // The drag is read here, outside the card's rotation (FR-1). Inside the rotated layer,
        // a flipped card is mirrored, so a right swipe would arrive as a left one.
        Box(Modifier.fillMaxSize().pointerInput(turn) { detectSwipe(offsetX, scope, onSwiped) }) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .graphicsLayer {
                        rotationZ = offsetX.value / TILT_PER_DEGREE
                        rotationY = flip
                        cameraDistance = CAMERA_DISTANCE * density
                    }
                    .clickable(onClick = onFlip),
            ) {
                Box(Modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) {
                    if (flip <= HALF_TURN) Front(card) else Box(Modifier.graphicsLayer { rotationY = FULL_FLIP }) { Back(card) }
                }
            }
        }
    }
}

/**
 * Follows a horizontal drag and decides the swipe when the finger lifts.
 *
 * Design: a drag past [SWIPE_THRESHOLD] of the card's width counts as an answer and throws
 * the card off screen; a shorter drag springs back. Deltas arrive in screen space because the
 * caller attaches this outside the rotated card.
 */
private suspend fun PointerInputScope.detectSwipe(
    offsetX: Animatable<Float, AnimationVector1D>,
    scope: CoroutineScope,
    onSwiped: (right: Boolean) -> Unit,
) {
    val threshold = size.width * SWIPE_THRESHOLD
    detectHorizontalDragGestures(
        onDragEnd = {
            scope.launch {
                val x = offsetX.value
                if (abs(x) > threshold) {
                    offsetX.animateTo(if (x > 0) size.width * THROW_DISTANCE else -size.width * THROW_DISTANCE, tween(THROW_MILLIS))
                    onSwiped(x > 0)
                } else {
                    offsetX.animateTo(0f)
                }
            }
        },
        onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
        onHorizontalDrag = { change, amount ->
            change.consume()
            scope.launch { offsetX.snapTo(offsetX.value + amount) }
        },
    )
}

/** The Spanish side of the card. */
@Composable
private fun Front(card: VocabCard) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        if (card.type != CardType.WORD) {
            Text(card.type.name.lowercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text(card.spanish, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        SpeakButton(card.spanish)
        Text("Tap to flip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The English side of the card, with the example sentence and region note. */
@Composable
private fun Back(card: VocabCard) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(card.spanish, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(card.english, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (card.example.isNotEmpty()) {
            Text("“${card.example}”", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            SpeakButton(card.example)
        }
        if (card.exampleEnglish.isNotEmpty()) {
            Text(card.exampleEnglish, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (card.regionNote.isNotEmpty()) {
            Text(card.regionNote, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

/** Share of the card's width a drag must pass to count as an answer. */
private const val SWIPE_THRESHOLD = 0.3f

/** How far the card is thrown, in card widths, once a swipe counts. */
private const val THROW_DISTANCE = 1.5f

/** How long the throw takes. */
private const val THROW_MILLIS = 180

/** How long the flip takes. */
private const val FLIP_MILLIS = 350

/** The flip rotation: half a turn shows the back. */
private const val FULL_FLIP = 180f

/** Past this angle the back is facing the user. */
private const val HALF_TURN = 90f

/** Pixels of drag per degree of tilt, so the card leans as it moves. */
private const val TILT_PER_DEGREE = 40f

/** Pixels of drag at which the "Got it" or "Again" hint is fully shown. */
private const val HINT_FULL_AT = 300f

/** Camera distance for the flip, in multiples of the screen density, so it doesn't look distorted. */
private const val CAMERA_DISTANCE = 12f

// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import app.vocablo.srs.Rating
import app.vocablo.ui.theme.Spacing

/**
 * The controls under the study card: "Show answer" on the front, four ratings on the back.
 *
 * Design: kept apart from [StudyScreen] so its layout can be tested at narrow widths and
 * large font sizes (FR-2). The four ratings share the row equally, so each button uses
 * slim side padding and a label that never wraps: it shrinks a step at a time instead,
 * which only happens on the narrowest phones with the largest font settings.
 */
@Composable
internal fun RatingBar(flipped: Boolean, onFlip: () -> Unit, onRate: (Rating) -> Unit) {
    if (!flipped) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Button(onClick = onFlip, modifier = Modifier.fillMaxWidth()) { Text("Show answer") }
            Text(
                "Swipe right if you knew it, left if you didn't",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            val tonal = ButtonDefaults.filledTonalButtonColors()
            RatingButton("Again", ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                onRate(Rating.AGAIN)
            }
            RatingButton("Hard", tonal) { onRate(Rating.HARD) }
            RatingButton("Good", ButtonDefaults.buttonColors()) { onRate(Rating.GOOD) }
            RatingButton("Easy", tonal) { onRate(Rating.EASY) }
        }
    }
}

/**
 * One of the four rating buttons, sharing the row's width equally with the others.
 *
 * Design: one function for all four replaces four near-identical button calls, so the
 * no-wrap rule (FR-2) is written once.
 */
@Composable
private fun RowScope.RatingButton(
    label: String,
    colors: ButtonColors,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = colors,
        contentPadding = PaddingValues(horizontal = Spacing.xs, vertical = Spacing.s),
        modifier = Modifier.weight(1f),
    ) {
        val style = LocalTextStyle.current
        BasicText(
            label,
            style = style.copy(color = LocalContentColor.current),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            autoSize = TextAutoSize.StepBased(minFontSize = MIN_LABEL_SIZE.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
        )
    }
}

/** The smallest a rating label may shrink to, in sp, before it would be hard to read. */
private const val MIN_LABEL_SIZE = 10

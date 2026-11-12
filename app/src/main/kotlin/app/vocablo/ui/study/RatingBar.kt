// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.vocablo.srs.Rating

/**
 * The controls under the study card: "Show answer" on the front, four ratings on the back.
 *
 * Design: kept apart from [StudyScreen] so its layout can be tested at narrow widths and
 * large font sizes (FR-2).
 */
@Composable
internal fun RatingBar(flipped: Boolean, onFlip: () -> Unit, onRate: (Rating) -> Unit) {
    if (!flipped) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onFlip, modifier = Modifier.fillMaxWidth()) { Text("Show answer") }
            Text(
                "Swipe right if you knew it, left if you didn't",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = { onRate(Rating.AGAIN) },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.weight(1f),
            ) { Text("Again") }
            FilledTonalButton(onClick = { onRate(Rating.HARD) }, modifier = Modifier.weight(1f)) { Text("Hard") }
            Button(onClick = { onRate(Rating.GOOD) }, modifier = Modifier.weight(1f)) { Text("Good") }
            FilledTonalButton(onClick = { onRate(Rating.EASY) }, modifier = Modifier.weight(1f)) { Text("Easy") }
        }
    }
}

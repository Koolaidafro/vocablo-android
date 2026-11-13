// Vocablo: Spanish flashcards for Android.
//
// The end-of-session screen: celebration, summary, next step and the reminder offer
// (FR-20, FR-23, FR-27).
package app.vocablo.ui.study

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.vocablo.notify.Notifications
import app.vocablo.play.Game
import app.vocablo.play.NextStep
import app.vocablo.ui.Screen
import app.vocablo.ui.components.Scene
import app.vocablo.ui.components.SceneArt
import app.vocablo.ui.theme.AppIcons
import app.vocablo.ui.theme.IconLabel
import java.time.LocalDate

/**
 * The screen shown when a session ends.
 *
 * Design: split out of StudyScreen so each file has one job; it takes plain values and two
 * callbacks, so it can be shown in tests without a ViewModel. One next step is offered, never
 * a list, so the session has a clear end (NFR-7).
 */
@Composable
fun FinishedScreen(
    learned: Int,
    missed: Int,
    summary: Summary,
    onNext: (NextStep) -> Unit,
    onReminderOfferAnswered: () -> Unit,
) {
    val context = LocalContext.current
    val offerReminders = summary.mayOfferReminders &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        Notifications.needsPermission(context)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        SceneArt(Scene.SESSION_COMPLETE, size = 180.dp)
        Text("¡Bien hecho!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "You went through $learned ${if (learned == 1) "card" else "cards"}" +
                if (missed > 0) " and retried $missed." else " without a single miss.",
            textAlign = TextAlign.Center,
        )
        SummaryRow(summary)
        if (offerReminders) ReminderOffer(onAnswered = onReminderOfferAnswered)
        if (summary.next == NextStep.DONE_FOR_TODAY) {
            Text("That's plenty for today. Tuzo is proud of you.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = { onNext(NextStep.DONE_FOR_TODAY) }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        } else {
            Button(onClick = { onNext(summary.next) }, modifier = Modifier.fillMaxWidth()) { Text("Next: ${summary.next.label}") }
            OutlinedButton(onClick = { onNext(NextStep.DONE_FOR_TODAY) }, modifier = Modifier.fillMaxWidth()) { Text("Done for now") }
        }
    }
}

/** XP, quests and streak side by side, the three numbers the learner came to grow. */
@Composable
private fun SummaryRow(summary: Summary) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            SummaryFact("+${summary.xp}", "XP")
            SummaryFact("${summary.questsDone}/${summary.questsTotal}", "quests")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconLabel(AppIcons.Streak, "${summary.streak}", style = MaterialTheme.typography.headlineSmall, tint = MaterialTheme.colorScheme.tertiary)
                Text(if (summary.streak == 1) "day streak" else "days streak", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SummaryFact(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Offers reminders once, after the first finished session, when the learner has just seen
 * what a session feels like (FR-27). Either answer is final: the app never asks again on its
 * own, and saying no changes nothing else.
 */
@Composable
private fun ReminderOffer(onAnswered: () -> Unit) {
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onAnswered() }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SceneArt(Scene.REMINDERS, size = 80.dp)
            Column(Modifier.weight(1f)) {
                Text("Want a nudge from Tuzo?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("One friendly reminder a day when cards are due. Never more than two.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onAnswered) { Text("Not now") }
                    Button(onClick = { ask.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Yes, please") }
                }
            }
        }
    }
}

/**
 * The screen for a next step, or null to just go back. The game rotates by day, so "Play a
 * game" is not always the same one.
 */
fun nextStepScreen(step: NextStep, today: Long = LocalDate.now().toEpochDay()): Screen? = when (step) {
    NextStep.DRILL_VERBS -> Screen.Drill
    NextStep.PLAY_GAME -> Screen.Game(Game.entries[Math.floorMod(today, Game.entries.size.toLong()).toInt()])
    NextStep.BUILD_SENTENCES -> Screen.Sentences
    NextStep.DONE_FOR_TODAY -> null
}

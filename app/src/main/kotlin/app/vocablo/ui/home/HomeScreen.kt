package app.vocablo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vocablo.play.WeeklyChest
import app.vocablo.ui.LibraryViewModel
import app.vocablo.ui.ProgressState
import app.vocablo.ui.components.Confetti
import app.vocablo.ui.Navigator
import app.vocablo.ui.Screen
import app.vocablo.ui.components.Tuzo
import app.vocablo.ui.components.TuzoMood
import app.vocablo.verbs.Conjugator
import app.vocablo.verbs.Person
import app.vocablo.verbs.StarterVerbs
import app.vocablo.verbs.Tense
import java.time.LocalDate

@Composable
fun HomeScreen(library: LibraryViewModel, nav: Navigator, onAddCards: () -> Unit) {
    val state by library.state.collectAsStateWithLifecycle()
    val settings by library.settings.collectAsStateWithLifecycle()
    val progress by library.progress.collectAsStateWithLifecycle()
    var chestXp by remember { mutableStateOf<Int?>(null) }
    val newToday = minOf(state.new, settings.newPerDay)
    val toStudy = state.due + newToday
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tuzo(size = 72.dp, mood = if (state.loaded && toStudy == 0) TuzoMood.SLEEP else TuzoMood.IDLE)
            Column(Modifier.weight(1f)) {
                Text("¡Hola!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        !state.loaded -> ""
                        state.streakAtRisk -> "Study today to keep your ${state.streak}-day streak."
                        state.streak > 0 -> "${state.streak}-day streak. Keep it going."
                        else -> "Let's start a streak today."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StreakBadge(state.streak, state.streakAtRisk)
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Today", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Stat(state.due, "to review")
                    Stat(newToday, "new")
                    Stat(state.answeredToday, "answered")
                }
                when {
                    state.totalCards == 0 && state.loaded -> Button(onClick = onAddCards, Modifier.fillMaxWidth()) { Text("Add your first cards") }
                    toStudy > 0 -> Button(onClick = { nav.open(Screen.Study(null)) }, Modifier.fillMaxWidth()) { Text("Study now") }
                    else -> Text("All caught up. Tuzo is taking a nap.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        LevelCard(progress)
        QuestsCard(progress)
        ChestCard(progress, onOpen = { library.openChest { xp -> chestXp = xp } })

        VerbOfTheDay()

        if (state.decks.isNotEmpty()) {
            OutlinedButton(onClick = onAddCards, Modifier.fillMaxWidth()) { Text("Your decks (${state.decks.size})") }
        }
    }

    chestXp?.let { xp ->
        Box(Modifier.fillMaxSize()) {
            Confetti(seed = xp)
            AlertDialog(
                onDismissRequest = { chestXp = null },
                icon = { Tuzo(size = 96.dp, mood = TuzoMood.WIN) },
                title = { Text(if (xp > 0) "¡Increíble!" else "Not yet") },
                text = { Text(if (xp > 0) "The weekly chest held $xp XP. See you next week." else "Finish your quests on five days this week to open it.") },
                confirmButton = { TextButton(onClick = { chestXp = null }) { Text("¡Vamos!") } },
            )
        }
    }
}

@Composable
private fun LevelCard(progress: ProgressState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Level ${progress.level}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${progress.totalXp} XP · ${progress.today.xp} today", style = MaterialTheme.typography.labelLarge)
            }
            LinearProgressIndicator(
                progress = { progress.levelProgress },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Level ${progress.level}, ${(progress.levelProgress * 100).toInt()} percent to the next" },
            )
            if (progress.freezesHeld > 0) {
                Text(
                    "🧊 ${progress.freezesHeld} streak ${if (progress.freezesHeld == 1) "freeze" else "freezes"} ready. A missed day is covered automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuestsCard(progress: ProgressState) {
    if (progress.quests.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Daily quests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            progress.quests.forEach { q ->
                val done = q.done(progress.today)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (done) "✅" else "⬜")
                    Column(Modifier.weight(1f)) {
                        Text(q.title, fontWeight = if (done) FontWeight.Normal else FontWeight.Medium)
                        LinearProgressIndicator(progress = { q.progress(progress.today).toFloat() / q.target }, modifier = Modifier.fillMaxWidth())
                    }
                    Text("${q.progress(progress.today)}/${q.target}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ChestCard(progress: ProgressState, onOpen: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (progress.chestOpened) "📭" else "🎁", style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f)) {
                Text("Weekly chest", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        progress.chestOpened -> "Opened this week. A new one comes Monday."
                        progress.chestReady -> "Ready to open!"
                        else -> "Quests done on ${progress.questDaysThisWeek} of ${WeeklyChest.DAYS_NEEDED} days this week."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (progress.chestReady) Button(onClick = onOpen) { Text("Open") }
        }
    }
}

@Composable
private fun StreakBadge(streak: Int, atRisk: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (atRisk) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔥 $streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (streak == 1) "day" else "days", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun Stat(value: Int, label: String) {
    Column {
        Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun VerbOfTheDay() {
    val verbs = StarterVerbs.all
    val verb = verbs[Math.floorMod(LocalDate.now().toEpochDay(), verbs.size.toLong()).toInt()]
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Verb of the day", style = MaterialTheme.typography.labelLarge)
            Text("${verb.infinitive} · ${verb.english}", style = MaterialTheme.typography.titleLarge)
            Text("Preterite", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Conjugator.table(verb, Tense.PRETERITE).forEach { (person, form) ->
                if (person != Person.VOSOTROS) {
                    Row {
                        Text(person.label(), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(form, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

fun Person.label() = when (this) {
    Person.YO -> "yo"
    Person.TU -> "tú"
    Person.EL -> "él · ella · usted"
    Person.NOSOTROS -> "nosotros"
    Person.VOSOTROS -> "vosotros"
    Person.ELLOS -> "ellos · ustedes"
}

@Composable
private fun ComingSoon(title: String, body: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Tuzo(size = 120.dp, mood = TuzoMood.SLEEP)
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun TutorScreen() = ComingSoon(
    "Your AI tutor is coming",
    "Bring your own Claude, ChatGPT or Grok key and practice real conversations in your region's Spanish.",
)

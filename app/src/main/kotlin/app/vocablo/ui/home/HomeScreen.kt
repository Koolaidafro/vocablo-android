package app.vocablo.ui.home

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vocablo.ui.LibraryViewModel
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

        VerbOfTheDay()

        if (state.decks.isNotEmpty()) {
            OutlinedButton(onClick = onAddCards, Modifier.fillMaxWidth()) { Text("Your decks (${state.decks.size})") }
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
fun PlayScreen() = ComingSoon(
    "Games are on the way",
    "Verb drills arrive next, then Verb Rain, Tug of War and the sentence builder, all using your own cards.",
)

@Composable
fun TutorScreen() = ComingSoon(
    "Your AI tutor is coming",
    "Bring your own Claude, ChatGPT or Grok key and practice real conversations in your region's Spanish.",
)

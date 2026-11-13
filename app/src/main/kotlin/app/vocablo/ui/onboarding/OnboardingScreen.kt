package app.vocablo.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.vocablo.data.AppSettings
import app.vocablo.data.Region
import app.vocablo.ui.components.Tuzo
import app.vocablo.ui.components.TuzoMood
import app.vocablo.ui.theme.AppIcon
import app.vocablo.ui.theme.AppIcons
import app.vocablo.verbs.Conjugator
import app.vocablo.verbs.Drill
import app.vocablo.verbs.Placement
import app.vocablo.verbs.PracticeSettings
import app.vocablo.verbs.Stage
import app.vocablo.verbs.Tier
import app.vocablo.verbs.VerbList
import app.vocablo.verbs.label
import app.vocablo.verbs.pronoun
import kotlin.random.Random

private enum class Level(val title: String, val body: String) {
    NEW("I'm brand new", "Start with the present tense and the 100 most common verbs."),
    SOME("I know some basics", "You can take a quick check, or start with the top 250 verbs."),
    COMFORTABLE("I'm fairly comfortable", "Take the check to skip what you already know."),
}

private enum class Step { WELCOME, REGION, GOAL, LEVEL, CHECK, DONE }

/**
 * First launch: region, daily goal, level, and an optional eight-question check.
 * Past tenses only switch on if the check shows they're known, so nothing unknown is quizzed.
 */
@Composable
fun OnboardingScreen(onFinish: ((AppSettings) -> AppSettings) -> Unit) {
    var step by rememberSaveable { mutableStateOf(Step.WELCOME) }
    var region by rememberSaveable { mutableStateOf(Region.LATAM) }
    var goal by rememberSaveable { mutableIntStateOf(10) }
    var level by rememberSaveable { mutableStateOf(Level.NEW) }
    var result by rememberSaveable { mutableStateOf<Pair<Stage, Tier>?>(null) }

    fun finish() {
        val (stage, tier) = result ?: when (level) {
            Level.NEW -> Stage.PRESENT to Tier.TOP_100
            Level.SOME -> Stage.PRESENT to Tier.TOP_250
            Level.COMFORTABLE -> Stage.PAST to Tier.TOP_500
        }
        val practice = PracticeSettings(
            unlocked = stage,
            enabledTenses = Stage.tensesThrough(stage).toSet(),
            maxTier = tier,
            includeVosotros = region == Region.SPAIN,
        )
        onFinish { it.copy(region = region, newPerDay = goal, practice = practice, onboarded = true) }
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LinearProgressIndicator(progress = { (step.ordinal + 1f) / Step.entries.size }, modifier = Modifier.fillMaxWidth())
        AnimatedContent(targetState = step, label = "onboarding") { current ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (current) {
                    Step.WELCOME -> {
                        Tuzo(Modifier.align(Alignment.CenterHorizontally), size = 140.dp)
                        Title("¡Hola! I'm Tuzo.")
                        Body("Four quick questions and Vocablo is set up for you. You can change everything later in Me.")
                        Button(onClick = { step = Step.REGION }, Modifier.fillMaxWidth()) { Text("Let's go") }
                        TextButton(onClick = ::finish, Modifier.fillMaxWidth()) { Text("Skip setup") }
                    }
                    Step.REGION -> {
                        Title("Which Spanish do you want?")
                        Body("This sets vocabulary notes and whether vosotros is practiced. Latin American is the default.")
                        Region.entries.forEach { r ->
                            Choice(r.label, if (r == Region.SPAIN) "Includes vosotros" else "Uses ustedes for “you all”", r == region) { region = r }
                        }
                        Next { step = Step.GOAL }
                    }
                    Step.GOAL -> {
                        Title("How many new words a day?")
                        listOf(5 to "Casual", 10 to "Regular", 20 to "Serious", 30 to "Intense").forEach { (n, name) ->
                            Choice("$n a day", name, n == goal) { goal = n }
                        }
                        Next { step = Step.LEVEL }
                    }
                    Step.LEVEL -> {
                        Title("How much Spanish do you know?")
                        Level.entries.forEach { l -> Choice(l.title, l.body, l == level) { level = l } }
                        if (level == Level.NEW) {
                            Next { step = Step.DONE }
                        } else {
                            Button(onClick = { step = Step.CHECK }, Modifier.fillMaxWidth()) { Text("Take the 8-question check") }
                            TextButton(onClick = { step = Step.DONE }, Modifier.fillMaxWidth()) { Text("Skip the check") }
                        }
                    }
                    Step.CHECK -> PlacementCheck { r ->
                        result = r.stage to r.tier
                        step = Step.DONE
                    }
                    Step.DONE -> {
                        val (stage, tier) = result ?: (if (level == Level.COMFORTABLE) Stage.PAST to Tier.TOP_500 else Stage.PRESENT to (if (level == Level.SOME) Tier.TOP_250 else Tier.TOP_100))
                        Tuzo(Modifier.align(Alignment.CenterHorizontally), size = 140.dp, mood = TuzoMood.CHEER)
                        Title("You're all set")
                        Body(
                            "Verb practice starts with: ${Stage.tensesThrough(stage).joinToString(", ") { it.label().lowercase() }}, " +
                                "using the ${tier.label.lowercase()} verbs. New tenses unlock once you're getting 8 in 10 right.",
                        )
                        Button(onClick = ::finish, Modifier.fillMaxWidth()) { Text("Start learning") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlacementCheck(onDone: (Placement.Result) -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val answers = remember { mutableStateListOf<Boolean>() }
    val item = Placement.items[index]
    val verb = VerbList.byInfinitive.getValue(item.infinitive).verb
    val choices = remember(index) { Drill.choices(verb, item.tense, item.person, Random(index)) }
    val answer = remember(index) { Conjugator.conjugate(verb, item.tense, item.person) }

    Text("Question ${index + 1} of ${Placement.items.size}", style = MaterialTheme.typography.labelLarge)
    Title("${item.person.pronoun()} · ${verb.infinitive}")
    Body("${item.tense.label()} tense (${verb.english}). Not sure? Pick “I don't know”, which is a fine answer.")
    choices.forEach { c ->
        OutlinedButton(onClick = {
            answers += c == answer
            if (index + 1 < Placement.items.size) index++ else onDone(Placement.score(answers.toList()))
        }, modifier = Modifier.fillMaxWidth()) { Text(c, style = MaterialTheme.typography.titleMedium) }
    }
    TextButton(onClick = {
        answers += false
        if (index + 1 < Placement.items.size) index++ else onDone(Placement.score(answers.toList()))
    }, modifier = Modifier.fillMaxWidth()) { Text("I don't know") }
}

@Composable
private fun Title(text: String) = Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

@Composable
private fun Body(text: String) = Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun Next(onClick: () -> Unit) {
    Spacer(Modifier.height(4.dp))
    Button(onClick = onClick, Modifier.fillMaxWidth()) { Text("Continue") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(title: String, body: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (selected) {
            CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.outlinedCardColors()
        },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
            }
            if (selected) AppIcon(AppIcons.Selected, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

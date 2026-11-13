package app.vocablo.ui.verbs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vocablo.container
import app.vocablo.data.SettingsStore
import app.vocablo.data.VerbRepository
import app.vocablo.speech.SpeakButton
import app.vocablo.srs.Rating
import app.vocablo.ui.Navigator
import app.vocablo.ui.Screen
import app.vocablo.ui.components.Tuzo
import app.vocablo.ui.components.TuzoMood
import app.vocablo.ui.theme.AppIcon
import app.vocablo.ui.theme.AppIcons
import app.vocablo.ui.theme.IconLabel
import app.vocablo.verbs.AnswerMode
import app.vocablo.verbs.Check
import app.vocablo.verbs.Conjugator
import app.vocablo.verbs.Drill
import app.vocablo.verbs.DrillItem
import app.vocablo.verbs.Explain
import app.vocablo.verbs.PracticeSettings
import app.vocablo.verbs.Question
import app.vocablo.verbs.Stage
import app.vocablo.verbs.StageProgress
import app.vocablo.verbs.hint
import app.vocablo.verbs.label
import app.vocablo.verbs.pronoun
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DrillUi(
    val loading: Boolean = true,
    val question: Question? = null,
    val mode: AnswerMode = AnswerMode.CHOOSE,
    val typed: String = "",
    val result: Check? = null,
    val picked: String? = null,
    val reasons: List<String> = emptyList(),
    val done: Int = 0,
    val total: Int = 0,
    val remaining: Int = 0,
    val right: Int = 0,
    val streakInSession: Int = 0,
    val finished: Boolean = false,
    val nextStage: Stage? = null,
    val stageProgress: Float = 0f,
)

class DrillViewModel(private val verbs: VerbRepository, private val settings: SettingsStore) : ViewModel() {

    private val _ui = MutableStateFlow(DrillUi())
    val ui: StateFlow<DrillUi> = _ui.asStateFlow()

    private val queue = ArrayDeque<DrillItem>()
    private val retried = mutableSetOf<Long>()
    private var current: DrillItem? = null
    private var practice = PracticeSettings()

    init {
        viewModelScope.launch {
            practice = settings.settings.first().practice
            queue.addAll(verbs.plan(practice))
            _ui.update { it.copy(loading = false, total = queue.size, mode = practice.answerMode) }
            nextQuestion()
        }
    }

    fun onTyped(text: String) = _ui.update { it.copy(typed = text) }

    fun appendChar(c: String) = _ui.update { it.copy(typed = it.typed + c) }

    fun submit(choice: String? = null) {
        val q = _ui.value.question ?: return
        if (_ui.value.result != null) return
        val given = choice ?: _ui.value.typed
        if (given.isBlank()) return
        val check = Drill.check(given, q.answer)
        val item = current ?: return
        val correct = check != Check.WRONG
        val rating = when (check) {
            Check.CORRECT -> Rating.GOOD
            Check.ACCENT_ONLY -> Rating.HARD
            Check.WRONG -> Rating.AGAIN
        }
        if (!correct && retried.add(item.id)) queue.addLast(item) // one more try later in this session
        _ui.update {
            it.copy(
                result = check,
                picked = choice,
                reasons = Explain.why(q.verb, q.tense, q.person),
                right = it.right + if (correct) 1 else 0,
                streakInSession = if (correct) it.streakInSession + 1 else 0,
            )
        }
        viewModelScope.launch { verbs.answer(item, rating, correct) }
    }

    fun next() {
        _ui.update { it.copy(done = it.done + 1) }
        nextQuestion()
    }

    private fun nextQuestion() {
        val item = queue.removeFirstOrNull()
        current = item
        if (item == null) {
            viewModelScope.launch {
                val recent = verbs.recentInStage(practice.unlocked)
                val ready = StageProgress.readyForNext(recent)
                _ui.update {
                    it.copy(
                        finished = true,
                        question = null,
                        nextStage = practice.unlocked.next.takeIf { ready },
                        stageProgress = StageProgress.progress(recent),
                    )
                }
            }
            return
        }
        _ui.update {
            it.copy(
                question = Drill.question(item.verb.verb, item.tense, practice.persons),
                remaining = queue.size + 1,
                typed = "",
                result = null,
                picked = null,
                reasons = emptyList(),
            )
        }
    }

    fun unlockNextStage() {
        viewModelScope.launch {
            settings.update { it.copy(practice = it.practice.unlockNext()) }
            _ui.update { it.copy(nextStage = null) }
        }
    }
}

private val ACCENTS = listOf("á", "é", "í", "ó", "ú", "ñ", "ü")

@Composable
fun DrillScreen(nav: Navigator) {
    val app = LocalContext.current.applicationContext
    val vm: DrillViewModel = viewModel { DrillViewModel(app.container.verbs, app.container.settings) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = nav::back) { AppIcon(AppIcons.Close, contentDescription = "End drill") }
            LinearProgressIndicator(
                progress = { if (ui.finished) 1f else ui.done.toFloat() / (ui.done + ui.remaining).coerceAtLeast(1) },
                modifier = Modifier.weight(1f).height(8.dp),
            )
            if (ui.streakInSession >= 3) {
                IconLabel(AppIcons.Streak, "${ui.streakInSession}", style = MaterialTheme.typography.labelLarge, tint = MaterialTheme.colorScheme.tertiary)
            }
        }
        val q = ui.question
        when {
            ui.loading -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ui.finished -> Summary(ui, onUnlock = vm::unlockNextStage, onSettings = { nav.replace(Screen.Practice) }, onDone = nav::back)
            q != null -> {
                QuestionCard(q)
                if (ui.mode == AnswerMode.CHOOSE) {
                    q.choices.forEach { c ->
                        val isAnswer = c == q.answer
                        val colors = when {
                            ui.result == null -> ButtonDefaults.outlinedButtonColors()
                            isAnswer -> ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            c == ui.picked -> ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            else -> ButtonDefaults.outlinedButtonColors()
                        }
                        OutlinedButton(onClick = { vm.submit(c) }, colors = colors, modifier = Modifier.fillMaxWidth()) {
                            Text(c, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = ui.typed,
                        onValueChange = vm::onTyped,
                        label = { Text("Your answer") },
                        singleLine = true,
                        enabled = ui.result == null,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { vm.submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ACCENTS.forEach { a -> AssistChip(onClick = { vm.appendChar(a) }, label = { Text(a) }) }
                    }
                    if (ui.result == null) {
                        Button(onClick = { vm.submit() }, enabled = ui.typed.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Check") }
                    }
                }
                AnimatedVisibility(ui.result != null) { Feedback(ui, q, onNext = vm::next) }
            }
        }
    }
}

@Composable
private fun QuestionCard(q: Question) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${q.tense.label()} · ${q.tense.hint()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(q.person.pronoun(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(q.verb.infinitive, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(q.verb.english, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Feedback(ui: DrillUi, q: Question, onNext: () -> Unit) {
    val (title, color) = when (ui.result) {
        Check.CORRECT -> "¡Correcto!" to MaterialTheme.colorScheme.primary
        Check.ACCENT_ONLY -> "Almost: watch the accent" to MaterialTheme.colorScheme.tertiary
        else -> "Not quite" to MaterialTheme.colorScheme.error
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            SpeakButton("${q.person.pronoun()} ${q.answer}")
        }
        if (ui.result != Check.CORRECT) {
            Text("${q.person.pronoun()} ${q.answer}", style = MaterialTheme.typography.titleMedium)
        }
        ui.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        TenseTable(q)
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

@Composable
private fun TenseTable(q: Question) {
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Conjugator.table(q.verb, q.tense).forEach { (person, form) ->
                Row {
                    Text(person.pronoun(), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(
                        form,
                        fontWeight = if (person == q.person) FontWeight.Bold else FontWeight.Normal,
                        color = if (person == q.person) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun Summary(ui: DrillUi, onUnlock: () -> Unit, onSettings: () -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (ui.total == 0) {
            Tuzo(size = 140.dp, mood = TuzoMood.SLEEP)
            Text("Nothing due right now", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Every verb you've practiced is scheduled for later. Add verbs to your tier or tenses in What to practice.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onSettings) { Text("What to practice") }
        } else {
            Tuzo(size = 150.dp, mood = TuzoMood.CHEER)
            Text("¡Muy bien!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("${ui.right} right out of ${ui.done} answers.", textAlign = TextAlign.Center)
            LinearProgressIndicator(progress = { ui.stageProgress }, modifier = Modifier.fillMaxWidth().height(8.dp))
            Text(
                "Progress toward the next tenses: 8 in 10 right over your last ${StageProgress.WINDOW} answers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        ui.nextStage?.let { stage ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ready for the ${stage.title.lowercase()} tenses?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("You've mastered what's on. Unlock ${stage.tenses.joinToString(" and ") { it.label().lowercase() }} when you've seen them in a lesson.")
                    Button(onClick = onUnlock) { Text("Unlock ${stage.title.lowercase()}") }
                }
            }
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

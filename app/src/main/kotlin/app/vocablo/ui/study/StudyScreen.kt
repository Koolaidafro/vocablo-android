// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vocablo.cards.Card as VocabCard
import app.vocablo.container
import app.vocablo.data.ProgressRepository
import app.vocablo.data.SettingsStore
import app.vocablo.data.StudySession
import app.vocablo.data.VerbRepository
import app.vocablo.data.VocabloRepository
import app.vocablo.play.DailyQuests
import app.vocablo.play.NextStep
import app.vocablo.play.SessionSummary
import app.vocablo.play.StreakFreezes
import app.vocablo.srs.Rating
import app.vocablo.ui.Navigator
import app.vocablo.ui.components.Scene
import app.vocablo.ui.components.SceneArt
import app.vocablo.ui.components.Tuzo
import app.vocablo.ui.components.TuzoMood
import app.vocablo.ui.theme.AppIcon
import app.vocablo.ui.theme.AppIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the finish screen shows once a session ends (FR-23). */
data class Summary(
    val xp: Int,
    val questsDone: Int,
    val questsTotal: Int,
    val streak: Int,
    val next: NextStep,
    /** True when the reminder offer has not been shown yet and reminders are on (FR-27). */
    val mayOfferReminders: Boolean,
)

data class StudyUi(
    val loading: Boolean = true,
    val card: VocabCard? = null,
    val flipped: Boolean = false,
    /** Answers given so far, including repeats. Used to key the card's animations and Tuzo's reaction. */
    val answers: Int = 0,
    val learned: Int = 0,
    val total: Int = 0,
    val missed: Int = 0,
    val finished: Boolean = false,
    /** The most recent rating, so Tuzo can react to it (FR-21). Null before the first answer. */
    val lastRating: Rating? = null,
    /** Answers on new cards and on cards seen before, for the session's XP (FR-23). */
    val newAnswers: Int = 0,
    val reviewAnswers: Int = 0,
    /** Filled in once the last answer is saved. */
    val summary: Summary? = null,
) {
    val progress: Float get() = if (total == 0) 0f else learned.toFloat() / total
}

/**
 * Runs one swipe-study session and builds its summary.
 *
 * Design: the summary's rules live in SessionSummary (core:play); this class only gathers the
 * numbers once the last answer is saved, so the finish screen never shows a half-saved day.
 */
class StudyViewModel(
    private val repo: VocabloRepository,
    private val progress: ProgressRepository,
    private val verbs: VerbRepository,
    private val settings: SettingsStore,
    deckId: Long?,
) : ViewModel() {

    private val _ui = MutableStateFlow(StudyUi())
    val ui: StateFlow<StudyUi> = _ui.asStateFlow()
    private var session: StudySession? = null

    init {
        viewModelScope.launch {
            val s = repo.startStudy(deckId, settings.settings.first().newPerDay)
            session = s
            publish(s)
        }
    }

    fun flip() = _ui.update { it.copy(flipped = !it.flipped) }

    fun answer(rating: Rating) {
        val s = session ?: return
        val item = s.queue.current ?: return
        val wasNew = item.state.isNew
        val updated = s.queue.answer(rating, System.currentTimeMillis())
        _ui.update {
            it.copy(
                answers = it.answers + 1,
                missed = it.missed + if (rating == Rating.AGAIN) 1 else 0,
                lastRating = rating,
                newAnswers = it.newAnswers + if (wasNew) 1 else 0,
                reviewAnswers = it.reviewAnswers + if (wasNew) 0 else 1,
            )
        }
        publish(s)
        viewModelScope.launch {
            repo.saveAnswer(updated, rating, wasNew)
            if (s.queue.isFinished) _ui.update { it.copy(summary = summarize(it)) }
        }
    }

    /** Called when the learner answers the reminder offer, either way, so it is never shown again (FR-27). */
    fun remindersOffered() {
        viewModelScope.launch { settings.update { it.copy(remindersOffered = true) } }
        _ui.update { it.copy(summary = it.summary?.copy(mayOfferReminders = false)) }
    }

    private suspend fun summarize(ui: StudyUi): Summary {
        val today = repo.today()
        val prefs = settings.settings.first()
        val activity = progress.todayActivity().first()
        val quests = DailyQuests.forDay(today, prefs.newPerDay)
        return Summary(
            xp = SessionSummary.xp(ui.newAnswers, ui.reviewAnswers),
            questsDone = SessionSummary.questsDone(quests, activity),
            questsTotal = quests.size,
            streak = StreakFreezes.streak(repo.studyDays().first(), progress.frozenDays().first(), today),
            next = SessionSummary.nextStep(verbs.dueCount().first(), quests, activity),
            mayOfferReminders = prefs.remindersOn && !prefs.remindersOffered,
        )
    }

    private fun publish(s: StudySession) {
        _ui.update {
            it.copy(
                loading = false,
                card = s.queue.current?.let { item -> s.cards[item.cardId] },
                flipped = false,
                learned = s.queue.reviewed,
                total = s.queue.total,
                finished = s.queue.total > 0 && s.queue.isFinished,
            )
        }
    }
}

@Composable
fun StudyScreen(deckId: Long?, nav: Navigator) {
    val app = LocalContext.current.applicationContext
    val vm: StudyViewModel = viewModel {
        StudyViewModel(app.container.repository, app.container.progress, app.container.verbs, app.container.settings, deckId)
    }
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = nav::back) { AppIcon(AppIcons.Close, contentDescription = "End session") }
            LinearProgressIndicator(progress = { ui.progress }, modifier = Modifier.weight(1f).height(8.dp))
            Text("${ui.learned}/${ui.total}", style = MaterialTheme.typography.labelLarge)
            if (!ui.finished && ui.card != null) ReactingTuzo(ui.lastRating, ui.answers, ui.flipped)
        }
        val card = ui.card
        val summary = ui.summary
        when {
            ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ui.finished && summary != null -> FinishedScreen(
                learned = ui.learned,
                missed = ui.missed,
                summary = summary,
                onNext = { step -> openNextStep(step, nav) },
                onReminderOfferAnswered = vm::remindersOffered,
            )
            ui.finished -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            card == null -> Empty(onDone = nav::back)
            else -> {
                SwipeCard(
                    card = card,
                    flipped = ui.flipped,
                    turn = ui.answers,
                    onFlip = vm::flip,
                    onSwiped = { right -> vm.answer(if (right) Rating.GOOD else Rating.AGAIN) },
                    modifier = Modifier.weight(1f),
                )
                RatingBar(flipped = ui.flipped, onFlip = vm::flip, onRate = vm::answer)
            }
        }
    }
}

/**
 * A small Tuzo beside the progress bar that reacts to each answer (FR-21): a cheer after Good
 * or Easy, a kind "oops" after Again, thinking while the answer shows, then back to ready.
 *
 * Design: keyed on the answer count, so two Good answers in a row still replay the cheer.
 */
@Composable
private fun ReactingTuzo(lastRating: Rating?, answers: Int, flipped: Boolean) {
    var reacting by remember { mutableStateOf(false) }
    LaunchedEffect(answers) {
        if (answers == 0) return@LaunchedEffect
        reacting = true
        delay(REACTION_MILLIS)
        reacting = false
    }
    val mood = when {
        reacting && lastRating == Rating.AGAIN -> TuzoMood.OOPS
        reacting && lastRating != null -> TuzoMood.CHEER
        flipped -> TuzoMood.THINK
        else -> TuzoMood.READY
    }
    Tuzo(size = 44.dp, mood = mood)
}

/** Leaves the session for the suggested next step. */
private fun openNextStep(step: NextStep, nav: Navigator) {
    nav.back()
    nextStepScreen(step)?.let(nav::open)
}

@Composable
private fun Empty(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        SceneArt(Scene.ALL_DONE, size = 200.dp)
        Text("Nothing to study right now", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Every card is scheduled for later. Add new words, or come back when Tuzo is out of the hammock.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onDone) { Text("Back") }
    }
}

/** How long Tuzo's reaction to an answer lasts before returning to ready. */
private const val REACTION_MILLIS = 900L

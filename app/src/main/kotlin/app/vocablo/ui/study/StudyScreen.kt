package app.vocablo.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vocablo.cards.Card as VocabCard
import app.vocablo.cards.Streaks
import app.vocablo.container
import app.vocablo.data.SettingsStore
import app.vocablo.data.StudySession
import app.vocablo.data.VocabloRepository
import app.vocablo.srs.Rating
import app.vocablo.ui.Navigator
import app.vocablo.ui.components.Tuzo
import app.vocablo.ui.components.TuzoMood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudyUi(
    val loading: Boolean = true,
    val card: VocabCard? = null,
    val flipped: Boolean = false,
    /** Answers given so far, including repeats. Used to key the card's animations. */
    val answers: Int = 0,
    val learned: Int = 0,
    val total: Int = 0,
    val missed: Int = 0,
    val finished: Boolean = false,
    val streak: Int = 0,
) {
    val progress: Float get() = if (total == 0) 0f else learned.toFloat() / total
}

class StudyViewModel(
    private val repo: VocabloRepository,
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
        _ui.update { it.copy(answers = it.answers + 1, missed = it.missed + if (rating == Rating.AGAIN) 1 else 0) }
        publish(s)
        viewModelScope.launch {
            repo.saveAnswer(updated, rating, wasNew)
            if (s.queue.isFinished) {
                val streak = Streaks.current(repo.studyDays().first(), repo.today())
                _ui.update { it.copy(streak = streak) }
            }
        }
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
    val vm: StudyViewModel = viewModel { StudyViewModel(app.container.repository, app.container.settings, deckId) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = nav::back) { Icon(Icons.Filled.Close, contentDescription = "End session") }
            LinearProgressIndicator(progress = { ui.progress }, modifier = Modifier.weight(1f).height(8.dp))
            Text("${ui.learned}/${ui.total}", style = MaterialTheme.typography.labelLarge)
        }
        val card = ui.card
        when {
            ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ui.finished -> Finished(ui, onDone = nav::back)
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

@Composable
private fun Finished(ui: StudyUi, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Tuzo(size = 160.dp, mood = TuzoMood.CHEER)
        Text("¡Bien hecho!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "You went through ${ui.learned} ${if (ui.learned == 1) "card" else "cards"}" +
                if (ui.missed > 0) " and retried ${ui.missed}." else " without a single miss.",
            textAlign = TextAlign.Center,
        )
        if (ui.streak > 0) {
            Text("🔥 ${ui.streak}-day streak", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary)
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

@Composable
private fun Empty(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Tuzo(size = 140.dp, mood = TuzoMood.SLEEP)
        Text("Nothing to study right now", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Every card is scheduled for later. Add new words, or come back when Tuzo wakes up.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onDone) { Text("Back") }
    }
}

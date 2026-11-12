package app.vocablo.ui.study

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vocablo.cards.CardType
import app.vocablo.cards.Card as VocabCard
import app.vocablo.cards.Streaks
import app.vocablo.container
import app.vocablo.data.SettingsStore
import app.vocablo.data.StudySession
import app.vocablo.data.VocabloRepository
import app.vocablo.speech.SpeakButton
import app.vocablo.srs.Rating
import app.vocablo.ui.Navigator
import app.vocablo.ui.components.Tuzo
import app.vocablo.ui.components.TuzoMood
import kotlin.math.abs
import kotlin.math.roundToInt
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
private fun SwipeCard(
    card: VocabCard,
    flipped: Boolean,
    turn: Int,
    onFlip: () -> Unit,
    onSwiped: (right: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(turn) { Animatable(0f) }
    val flip by animateFloatAsState(if (flipped) 180f else 0f, tween(350), label = "flip")
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
                .padding(24.dp)
                .alpha((abs(drag) / 300f).coerceIn(0f, 1f)),
        )
        Card(
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .graphicsLayer {
                    rotationZ = offsetX.value / 40f
                    rotationY = flip
                    cameraDistance = 12f * density
                }
                .pointerInput(turn) {
                    val threshold = size.width * 0.3f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val x = offsetX.value
                                if (abs(x) > threshold) {
                                    offsetX.animateTo(if (x > 0) size.width * 1.5f else -size.width * 1.5f, tween(180))
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
                .clickable(onClick = onFlip),
        ) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                if (flip <= 90f) Front(card) else Box(Modifier.graphicsLayer { rotationY = 180f }) { Back(card) }
            }
        }
    }
}

@Composable
private fun Front(card: VocabCard) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (card.type != CardType.WORD) {
            Text(card.type.name.lowercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text(card.spanish, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        SpeakButton(card.spanish)
        Text("Tap to flip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Back(card: VocabCard) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

@Composable
private fun RatingBar(flipped: Boolean, onFlip: () -> Unit, onRate: (Rating) -> Unit) {
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

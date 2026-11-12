package app.vocablo.ui.play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vocablo.AppContainer
import app.vocablo.container
import app.vocablo.play.Cuatro
import app.vocablo.play.Game
import app.vocablo.play.ParejasBoard
import app.vocablo.play.Rope
import app.vocablo.play.SiONo
import app.vocablo.play.TugOfWar
import app.vocablo.play.VerbRain
import app.vocablo.ui.Navigator
import app.vocablo.ui.components.Confetti
import app.vocablo.ui.components.Tuzo
import app.vocablo.ui.components.TuzoMood
import app.vocablo.verbs.Conjugator
import app.vocablo.verbs.Drill
import app.vocablo.verbs.label
import app.vocablo.verbs.pronoun
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameResult(val score: Int, val xp: Int)

class GameViewModel(private val game: Game, private val c: AppContainer) : ViewModel() {
    private val _material = MutableStateFlow<PlayMaterial?>(null)
    val material: StateFlow<PlayMaterial?> = _material.asStateFlow()

    private val _result = MutableStateFlow<GameResult?>(null)
    val result: StateFlow<GameResult?> = _result.asStateFlow()

    /** Bumped for "play again" so every game's state starts fresh. */
    private val _session = MutableStateFlow(0)
    val session: StateFlow<Int> = _session.asStateFlow()

    private var finishing = false

    init {
        viewModelScope.launch { _material.value = PlayMaterial.load(c) }
    }

    fun finish(score: Int) {
        if (finishing) return
        finishing = true
        viewModelScope.launch { _result.value = GameResult(score, c.progress.recordGame(game, score)) }
    }

    fun playAgain() {
        finishing = false
        _result.value = null
        _session.value++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(game: Game, nav: Navigator) {
    val app = LocalContext.current.applicationContext
    val vm: GameViewModel = viewModel { GameViewModel(game, app.container) }
    val material by vm.material.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game.title) },
                navigationIcon = { IconButton(onClick = nav::back) { Icon(Icons.Filled.Close, contentDescription = "Quit game") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val m = material
            val r = result
            when {
                m == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                r != null -> Results(r, session, onAgain = vm::playAgain, onDone = nav::back)
                else -> key(session) {
                    when (game) {
                        Game.VERB_RAIN -> VerbRainGame(m, vm::finish)
                        Game.TUG_OF_WAR -> TugOfWarGame(m, vm::finish)
                        Game.SI_O_NO -> SiONoGame(m, vm::finish)
                        Game.PAREJAS -> ParejasGame(m, vm::finish)
                        Game.CUATRO -> CuatroGame(m, vm::finish)
                    }
                }
            }
        }
    }
}

@Composable
private fun Results(result: GameResult, session: Int, onAgain: () -> Unit, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        if (result.score > 0) Confetti(seed = session + 1)
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Tuzo(size = 140.dp, mood = if (result.score > 0) TuzoMood.WIN else TuzoMood.IDLE)
            Text(if (result.score > 0) "¡Bien hecho!" else "¡Otra vez!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Score ${result.score}", style = MaterialTheme.typography.titleLarge)
            Text("+${result.xp} XP", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            Button(onClick = onAgain, modifier = Modifier.fillMaxWidth()) { Text("Play again") }
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}

@Composable
private fun Hud(left: String, score: Int, mood: TuzoMood) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(left, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text("$score", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = "Score $score" })
        Tuzo(size = 48.dp, mood = mood, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun NotEnough(message: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Tuzo(size = 120.dp, mood = TuzoMood.SLEEP)
        Text(message, textAlign = TextAlign.Center)
    }
}

// Verb Rain

private const val RAIN_ROUNDS = 15
private const val RAIN_LIVES = 3

@Composable
private fun VerbRainGame(m: PlayMaterial, onFinish: (Int) -> Unit) {
    var roundNo by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(RAIN_LIVES) }
    var score by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(TuzoMood.IDLE) }
    val round = remember(roundNo) { VerbRain.round(m.verbs, m.tenses, m.persons) }
    val fall = remember(roundNo) { Animatable(0f) }

    fun advance(lostLife: Boolean) {
        if (lostLife) lives--
        if (lives <= 0 || roundNo + 1 >= RAIN_ROUNDS) onFinish(score) else roundNo++
    }

    LaunchedEffect(roundNo) {
        val millis = (7_000 - roundNo * 200).coerceAtLeast(3_500)
        fall.animateTo(1f, tween(millis, easing = LinearEasing))
        mood = TuzoMood.OOPS
        advance(lostLife = true)
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hud("❤️".repeat(lives.coerceAtLeast(0)), score, mood)
        val q = round.question
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${q.person.pronoun()} · ${q.verb.infinitive}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${q.tense.label()} · ${q.verb.english}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp)) {
            val count = round.drops.size
            val laneWidth = maxWidth / count
            val dropHeight = 48.dp
            val travel = constraints.maxHeight - with(androidx.compose.ui.platform.LocalDensity.current) { dropHeight.roundToPx() }
            round.drops.forEachIndexed { i, drop ->
                val stagger = 0.8f + 0.2f * ((i * 37) % 5) / 4f
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .offset { IntOffset(0, (travel * (fall.value * stagger).coerceAtMost(1f)).toInt()) }
                        .offset(x = laneWidth * i)
                        .width(laneWidth)
                        .height(dropHeight)
                        .padding(2.dp)
                        .clickable {
                            if (drop == q.answer) {
                                score += VerbRain.points(fall.value)
                                mood = TuzoMood.WIN
                                advance(lostLife = false)
                            } else {
                                mood = TuzoMood.OOPS
                                advance(lostLife = true)
                            }
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(drop, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// Tug of War

@Composable
private fun TugOfWarGame(m: PlayMaterial, onFinish: (Int) -> Unit) {
    var rope by remember { mutableStateOf(Rope()) }
    var right by remember { mutableIntStateOf(0) }
    var qNo by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(TuzoMood.IDLE) }
    val q = remember(qNo) { Drill.question(m.verbs.random(), m.tenses.random(), m.persons) }

    LaunchedEffect(Unit) {
        val interval = TugOfWar.rivalIntervalMillis(m.level)
        while (!rope.over) {
            delay(interval)
            rope = rope.rivalPull()
        }
    }
    LaunchedEffect(rope.over) {
        if (rope.over) {
            mood = if (rope.won) TuzoMood.WIN else TuzoMood.OOPS
            delay(800)
            onFinish(right * 3 + if (rope.won) 15 else 0)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Hud(if (rope.won) "You win!" else if (rope.lost) "Pulled over!" else "Pull!", right, mood)
        RopeBar(rope)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${q.person.pronoun()} · ${q.verb.infinitive}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(q.tense.label(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        q.choices.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { choice ->
                    Button(
                        onClick = {
                            if (rope.over) return@Button
                            val correct = choice == q.answer
                            rope = rope.answer(correct)
                            if (correct) right++
                            mood = if (correct) TuzoMood.CHEER else TuzoMood.OOPS
                            qNo++
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    ) { Text(choice) }
                }
            }
        }
    }
}

@Composable
private fun RopeBar(rope: Rope) {
    val line = MaterialTheme.colorScheme.outline
    val knot = MaterialTheme.colorScheme.primary
    val goal = MaterialTheme.colorScheme.tertiary
    Column {
        Row {
            Text("Rival", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Text("You", style = MaterialTheme.typography.labelLarge)
        }
        Canvas(
            Modifier.fillMaxWidth().height(32.dp).semantics {
                contentDescription = "Rope at ${rope.position} of ${rope.limit}"
                liveRegion = LiveRegionMode.Polite
            },
        ) {
            val y = size.height / 2
            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 6f)
            drawLine(goal, Offset(size.width * 0.02f, 0f), Offset(size.width * 0.02f, size.height), strokeWidth = 4f)
            drawLine(goal, Offset(size.width * 0.98f, 0f), Offset(size.width * 0.98f, size.height), strokeWidth = 4f)
            val fraction = 0.5f + rope.position / (2f * rope.limit) * 0.96f
            drawCircle(knot, radius = size.height / 2.5f, center = Offset(size.width * fraction, y))
        }
    }
}

// Sí o No

private const val SI_O_NO_MILLIS = 45_000

@Composable
private fun SiONoGame(m: PlayMaterial, onFinish: (Int) -> Unit) {
    var score by remember { mutableIntStateOf(0) }
    var claim by remember { mutableStateOf(SiONo.claim(m.verbs, m.tenses, m.persons)) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var mood by remember { mutableStateOf(TuzoMood.IDLE) }
    val time = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        time.animateTo(0f, tween(SI_O_NO_MILLIS, easing = LinearEasing))
        onFinish(score)
    }

    fun answer(yes: Boolean) {
        if (yes == claim.isTrue) {
            score += 2
            feedback = null
            mood = TuzoMood.CHEER
        } else {
            val right = Conjugator.conjugate(claim.verb, claim.tense, claim.person)
            feedback = "${claim.person.pronoun()} $right (${claim.verb.infinitive}, ${claim.tense.label().lowercase()})"
            mood = TuzoMood.OOPS
        }
        claim = SiONo.claim(m.verbs, m.tenses, m.persons)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Hud("Is it right?", score, mood)
        LinearProgressIndicator(progress = { time.value }, modifier = Modifier.fillMaxWidth().height(8.dp))
        var drag by remember { mutableFloatStateOf(0f) }
        Card(
            Modifier.fillMaxWidth().weight(1f).pointerInput(claim) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (drag > 120f) answer(true) else if (drag < -120f) answer(false)
                        drag = 0f
                    },
                ) { _, delta -> drag += delta }
            }.offset { IntOffset(drag.toInt(), 0) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${claim.verb.infinitive} · ${claim.tense.label()}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${claim.person.pronoun()} ${claim.shown}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Swipe right for sí, left for no", style = MaterialTheme.typography.labelMedium)
            }
        }
        feedback?.let {
            Text("It's $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { answer(false) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f).height(56.dp),
            ) { Text("No") }
            Button(onClick = { answer(true) }, modifier = Modifier.weight(1f).height(56.dp)) { Text("Sí") }
        }
    }
}

// Parejas

@Composable
private fun ParejasGame(m: PlayMaterial, onFinish: (Int) -> Unit) {
    val board = remember { ParejasBoard.from(m.cards) }
    if (board == null) {
        NotEnough("Parejas needs at least 3 cards with different meanings. Add a few words and come back.")
        return
    }
    var tick by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(TuzoMood.IDLE) }
    val matched = tick.let { board.matched }
    val selected = tick.let { board.selected }
    val finished = tick.let { board.finished }
    val score = (matched.size * 5 - board.mistakes).coerceAtLeast(0)

    LaunchedEffect(finished) {
        if (finished) {
            mood = TuzoMood.WIN
            delay(700)
            onFinish(score)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hud("${matched.size} of ${board.tiles.size / 2} pairs", score, mood)
        LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(board.tiles.size) { i ->
                val tile = board.tiles[i]
                val done = tile.pairId in matched
                Card(
                    onClick = {
                        if (board.tap(i)) mood = TuzoMood.CHEER else if (board.selected == null) mood = TuzoMood.OOPS
                        tick++
                    },
                    enabled = !done,
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            done -> MaterialTheme.colorScheme.surfaceVariant
                            selected == i -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
                    modifier = Modifier.heightIn(min = 72.dp),
                ) {
                    Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            tile.text,
                            textAlign = TextAlign.Center,
                            fontWeight = if (tile.spanish) FontWeight.Bold else FontWeight.Normal,
                            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

// Cuatro

private const val CUATRO_QUESTIONS = 10

@Composable
private fun CuatroGame(m: PlayMaterial, onFinish: (Int) -> Unit) {
    var n by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(TuzoMood.IDLE) }
    val q = remember(n) { Cuatro.question(m.cards) }
    if (q == null) {
        NotEnough("Cuatro needs at least 4 cards with different meanings. Add a few words and come back.")
        return
    }
    var picked by remember(n) { mutableStateOf<String?>(null) }

    LaunchedEffect(picked) {
        if (picked != null) {
            delay(900)
            if (n + 1 >= CUATRO_QUESTIONS) onFinish(score) else n++
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hud("${n + 1} of $CUATRO_QUESTIONS", score, mood)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Text(
                q.card.spanish,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(28.dp),
            )
        }
        q.options.forEach { option ->
            val p = picked
            val colors = when {
                p == null -> ButtonDefaults.outlinedButtonColors()
                option == q.answer -> ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                option == p -> ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                else -> ButtonDefaults.outlinedButtonColors()
            }
            OutlinedButton(
                onClick = {
                    if (picked != null) return@OutlinedButton
                    picked = option
                    if (option == q.answer) {
                        score += 3
                        mood = TuzoMood.CHEER
                    } else {
                        mood = TuzoMood.OOPS
                    }
                },
                colors = colors,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(option) }
        }
    }
}

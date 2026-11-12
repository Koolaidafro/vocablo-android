package app.vocablo.ui.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vocablo.AppContainer
import app.vocablo.container
import app.vocablo.play.Noun
import app.vocablo.play.SentenceBuilder
import app.vocablo.play.SentenceExercise
import app.vocablo.ui.Navigator
import app.vocablo.ui.components.Tuzo
import app.vocablo.ui.components.TuzoMood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SentenceUi(
    val loading: Boolean = true,
    val exercise: SentenceExercise? = null,
    /** Indexes into the exercise's tiles, in the order the learner placed them. */
    val placed: List<Int> = emptyList(),
    val result: Boolean? = null,
    val built: Int = 0,
    val right: Int = 0,
    val xp: Int = 0,
    val missing: String? = null,
)

class SentenceViewModel(private val c: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(SentenceUi())
    val ui: StateFlow<SentenceUi> = _ui.asStateFlow()

    private var material: PlayMaterial? = null
    private var nouns: List<Noun> = emptyList()

    init {
        viewModelScope.launch {
            val m = PlayMaterial.load(c)
            material = m
            nouns = SentenceBuilder.nounsFrom(m.cards)
            if (nouns.isEmpty()) {
                _ui.value = SentenceUi(loading = false, missing = "Add a few nouns with their article, like \"el pan\" or \"la mesa\", and Tuzo will build sentences from them.")
            } else {
                next()
            }
        }
    }

    fun place(tile: Int) = _ui.update {
        if (it.result != null || tile in it.placed) it else it.copy(placed = it.placed + tile)
    }

    fun unplace(position: Int) = _ui.update {
        if (it.result != null) it else it.copy(placed = it.placed.filterIndexed { i, _ -> i != position })
    }

    fun check() {
        val s = _ui.value
        val ex = s.exercise ?: return
        if (s.result != null) return
        val correct = SentenceBuilder.check(s.placed.map { ex.tiles[it] }, ex)
        _ui.update { it.copy(result = correct, built = it.built + 1, right = it.right + if (correct) 1 else 0) }
        viewModelScope.launch {
            val xp = c.progress.recordSentence(correct)
            _ui.update { it.copy(xp = it.xp + xp) }
        }
    }

    fun next() {
        val m = material ?: return
        val ex = SentenceBuilder.build(m.sentenceVerbs, nouns, m.tenses, m.persons)
        _ui.update {
            if (ex == null) {
                it.copy(loading = false, missing = "Practice a few verbs like comer, tener or comprar first, then come back to build sentences.")
            } else {
                it.copy(loading = false, exercise = ex, placed = emptyList(), result = null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceScreen(nav: Navigator) {
    val app = LocalContext.current.applicationContext
    val vm: SentenceViewModel = viewModel { SentenceViewModel(app.container) }
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sentence builder") },
                navigationIcon = { IconButton(onClick = nav::back) { Icon(Icons.Filled.Close, contentDescription = "Close") } },
                actions = { if (ui.built > 0) Text("${ui.right}/${ui.built} · +${ui.xp} XP", modifier = Modifier.padding(end = 16.dp)) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val ex = ui.exercise
            when {
                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.missing != null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Tuzo(size = 120.dp, mood = TuzoMood.SLEEP)
                    Text(ui.missing.orEmpty(), textAlign = TextAlign.Center)
                }
                ex != null -> Exercise(ui, ex, vm)
            }
        }
    }
}

@Composable
private fun Exercise(ui: SentenceUi, ex: SentenceExercise, vm: SentenceViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tuzo(
                size = 56.dp,
                mood = when (ui.result) {
                    true -> TuzoMood.WIN
                    false -> TuzoMood.OOPS
                    null -> TuzoMood.IDLE
                },
            )
            Text("Say this in Spanish", style = MaterialTheme.typography.titleMedium)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ex.meaning.forEach { SuggestionChip(onClick = {}, label = { Text(it) }) }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            FlowRow(
                Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (ui.placed.isEmpty()) Text("Tap the tiles below in order", color = MaterialTheme.colorScheme.onSurfaceVariant)
                ui.placed.forEachIndexed { pos, tile ->
                    InputChip(selected = true, onClick = { vm.unplace(pos) }, label = { Text(ex.tiles[tile]) })
                }
            }
        }
        HorizontalDivider()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ex.tiles.forEachIndexed { i, word ->
                if (i !in ui.placed) AssistChip(onClick = { vm.place(i) }, label = { Text(word, style = MaterialTheme.typography.titleMedium) })
            }
        }

        when (val r = ui.result) {
            null -> Button(onClick = vm::check, enabled = ui.placed.size == ex.answer.size, modifier = Modifier.fillMaxWidth()) { Text("Check") }
            else -> {
                Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (r) "¡Perfecto!" else "Not quite.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (r) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Text(ex.sentence, style = MaterialTheme.typography.titleMedium)
                }
                Button(onClick = vm::next, modifier = Modifier.fillMaxWidth()) { Text("Next sentence") }
            }
        }
        if (ui.result == null && ui.placed.isNotEmpty()) {
            OutlinedButton(onClick = { repeat(ui.placed.size) { vm.unplace(0) } }, modifier = Modifier.fillMaxWidth()) { Text("Clear") }
        }
    }
}

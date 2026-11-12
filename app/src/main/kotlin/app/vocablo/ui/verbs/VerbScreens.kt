package app.vocablo.ui.verbs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vocablo.container
import app.vocablo.data.AppSettings
import app.vocablo.data.SettingsStore
import app.vocablo.data.VocabloRepository
import app.vocablo.play.Game
import app.vocablo.play.Medal
import app.vocablo.speech.SpeakButton
import app.vocablo.ui.Navigator
import app.vocablo.ui.Screen
import app.vocablo.verbs.AnswerMode
import app.vocablo.verbs.Conjugator
import app.vocablo.verbs.Person
import app.vocablo.verbs.PracticeSettings
import app.vocablo.verbs.RankedVerb
import app.vocablo.verbs.Stage
import app.vocablo.verbs.Tense
import app.vocablo.verbs.Tier
import app.vocablo.verbs.VerbList
import app.vocablo.verbs.VerbPattern
import app.vocablo.verbs.VerbSource
import app.vocablo.verbs.hint
import app.vocablo.verbs.label
import app.vocablo.verbs.pronoun
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PracticeViewModel(private val store: SettingsStore) : ViewModel() {
    val settings: StateFlow<AppSettings> = store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun change(block: (PracticeSettings) -> PracticeSettings) {
        viewModelScope.launch { store.update { it.copy(practice = block(it.practice)) } }
    }
}

/** "What to practice": organized by stage, so locked tenses are visibly off limits. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSettingsScreen(nav: Navigator) {
    val app = LocalContext.current.applicationContext
    val vm: PracticeViewModel = viewModel { PracticeViewModel(app.container.settings) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val p = settings.practice
    var confirmUnlock by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What to practice") },
                navigationIcon = { IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Tenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Only tenses you've unlocked are ever quizzed. The next stage is suggested once you get 8 in 10 right.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Stage.entries.forEach { stage ->
                val open = stage <= p.unlocked
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (open) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Stage ${stage.number}: ${stage.title}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (!open) Icon(Icons.Filled.Lock, contentDescription = "Locked")
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            stage.tenses.forEach { t ->
                                FilterChip(
                                    selected = open && t in p.enabledTenses,
                                    enabled = open,
                                    onClick = { vm.change { it.toggle(t) } },
                                    label = { Text("${t.label()} · ${t.hint()}") },
                                )
                            }
                        }
                        if (stage == p.unlocked.next) {
                            TextButton(onClick = { confirmUnlock = true }) { Text("I already know these: unlock") }
                        }
                    }
                }
            }

            HorizontalDivider()
            Text("Verbs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Tier.entries.forEach { t ->
                    FilterChip(selected = p.maxTier == t, onClick = { vm.change { it.copy(maxTier = t) } }, label = { Text(t.label) })
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(VerbSource.TOP_LIST to "Most common", VerbSource.MY_CARDS to "My verb cards", VerbSource.BOTH to "Both").forEach { (s, label) ->
                    FilterChip(selected = p.source == s, onClick = { vm.change { it.copy(source = s) } }, label = { Text(label) })
                }
            }

            HorizontalDivider()
            Text("Answers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = p.answerMode == AnswerMode.CHOOSE, onClick = { vm.change { it.copy(answerMode = AnswerMode.CHOOSE) } }, label = { Text("Pick from 4") })
                FilterChip(selected = p.answerMode == AnswerMode.TYPE, onClick = { vm.change { it.copy(answerMode = AnswerMode.TYPE) } }, label = { Text("Type it") })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Include vosotros")
                    Text("Used in Spain. Latin America uses ustedes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = p.includeVosotros, onCheckedChange = { on -> vm.change { it.copy(includeVosotros = on) } })
            }
        }
    }

    if (confirmUnlock) {
        val next = p.unlocked.next
        AlertDialog(
            onDismissRequest = { confirmUnlock = false },
            title = { Text("Unlock ${next?.title?.lowercase()} tenses?") },
            text = { Text("Do this only if you've already learned them. Vocablo never quizzes on tenses that are locked.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.change { it.unlockNext() }
                    confirmUnlock = false
                }) { Text("Unlock") }
            },
            dismissButton = { TextButton(onClick = { confirmUnlock = false }) { Text("Not yet") } },
        )
    }
}

/** Browse and search all 2,000 verbs by tier. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerbListScreen(nav: Navigator) {
    var query by rememberSaveable { mutableStateOf("") }
    var tier by rememberSaveable { mutableStateOf<Tier?>(null) }
    val all = remember { VerbList.all }
    val app = LocalContext.current.applicationContext
    val medals by produceState(emptyMap<String, Medal>()) { value = app.container.progress.medals() }
    val shown = remember(query, tier) {
        val q = query.trim().lowercase()
        all.filter { rv ->
            (tier == null || rv.tier == tier) &&
                (q.isEmpty() || rv.verb.infinitive.contains(q) || rv.verb.english.lowercase().contains(q))
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verbs") },
                navigationIcon = { IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search Spanish or English") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = tier == null, onClick = { tier = null }, label = { Text("All ${all.size}") })
                        Tier.entries.forEach { t -> FilterChip(selected = tier == t, onClick = { tier = t }, label = { Text(t.label) }) }
                    }
                }
            }
            items(shown, key = { it.rank }) { rv ->
                ListItem(
                    leadingContent = { Text("${rv.rank}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    headlineContent = { Text(rv.verb.infinitive, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(rv.verb.english) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (rv.verb.pattern != VerbPattern()) {
                                Text("irregular", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                            medals[rv.verb.infinitive]?.let { MedalBadge(it) }
                        }
                    },
                    modifier = Modifier.clickable { nav.open(Screen.VerbDetail(rv.verb.infinitive)) },
                )
            }
        }
    }
}

class VerbDetailViewModel(private val repo: VocabloRepository) : ViewModel() {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun addToCards(rv: RankedVerb) {
        viewModelScope.launch {
            _message.value = if (repo.addVerbCard(rv.verb.infinitive, rv.verb.english)) {
                "Added ${rv.verb.infinitive} to your Verbos deck."
            } else {
                "You already have a card for ${rv.verb.infinitive}."
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerbDetailScreen(infinitive: String, nav: Navigator) {
    val app = LocalContext.current.applicationContext
    val vm: VerbDetailViewModel = viewModel { VerbDetailViewModel(app.container.repository) }
    val message by vm.message.collectAsStateWithLifecycle()
    val rv = VerbList.byInfinitive[infinitive]
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(infinitive) },
                navigationIcon = { IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (rv == null) {
                Text("This verb isn't in the list.")
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rv.verb.english, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                SpeakButton(rv.verb.infinitive)
            }
            Text("#${rv.rank} most used · ${rv.tier.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.addToCards(rv) }) { Text("Add to my cards") }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Tense.entries.forEach { t ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${t.label()} · ${t.hint()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Conjugator.table(rv.verb, t).forEach { (person, form) ->
                            Row {
                                Text(person.pronoun(), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(form, fontWeight = if (person == Person.VOSOTROS) FontWeight.Normal else FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The Play tab: verb drills now, games in phase 4. */
@Composable
fun PlayHub(nav: Navigator, dueVerbItems: Int, practice: PracticeSettings) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Play", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Conjugation drill", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${practice.practiceTenses.joinToString(", ") { it.label().lowercase() }} · ${practice.maxTier.label.lowercase()} verbs" +
                        if (dueVerbItems > 0) " · $dueVerbItems due" else "",
                )
                Button(onClick = { nav.open(Screen.Drill) }, modifier = Modifier.fillMaxWidth()) { Text("Start drill") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { nav.open(Screen.Practice) }, modifier = Modifier.weight(1f)) { Text("What to practice") }
            OutlinedButton(onClick = { nav.open(Screen.Verbs) }, modifier = Modifier.weight(1f)) { Text("Verb list") }
        }
        Card(
            onClick = { nav.open(Screen.Sentences) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sentence builder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Put tiles in order to make sentences from your own words and verbs.")
            }
        }
        Text("Games", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Game.entries.forEach { game ->
            Card(onClick = { nav.open(Screen.Game(game)) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(gameEmoji(game), style = MaterialTheme.typography.headlineMedium)
                    Column(Modifier.weight(1f)) {
                        Text(game.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(game.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

fun gameEmoji(game: Game): String = when (game) {
    Game.VERB_RAIN -> "🌧️"
    Game.TUG_OF_WAR -> "🪢"
    Game.SI_O_NO -> "👍"
    Game.PAREJAS -> "🃏"
    Game.CUATRO -> "4️⃣"
}

/** Gold: remembered for a month or more in every practiced tense. Silver: a week. Bronze: started. */
@Composable
fun MedalBadge(medal: Medal) {
    val (emoji, name) = when (medal) {
        Medal.GOLD -> "🥇" to "Gold"
        Medal.SILVER -> "🥈" to "Silver"
        Medal.BRONZE -> "🥉" to "Bronze"
        Medal.NONE -> return
    }
    Text(emoji, modifier = Modifier.semantics { contentDescription = "$name medal" })
}

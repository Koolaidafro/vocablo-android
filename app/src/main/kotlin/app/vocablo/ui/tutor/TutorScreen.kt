package app.vocablo.ui.tutor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vocablo.AppContainer
import app.vocablo.container
import app.vocablo.tutor.ChatMessage
import app.vocablo.tutor.Provider
import app.vocablo.tutor.Reply
import app.vocablo.tutor.Role
import app.vocablo.tutor.Scenario
import app.vocablo.tutor.Scenarios
import app.vocablo.tutor.TutorConfig
import app.vocablo.tutor.TutorPrompt
import app.vocablo.tutor.Variety
import app.vocablo.ui.components.Tuzo
import app.vocablo.ui.components.TuzoMood
import app.vocablo.verbs.Stage
import app.vocablo.verbs.label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TutorUi(
    val loading: Boolean = true,
    val hasKey: Boolean = false,
    val editingSetup: Boolean = false,
    val provider: Provider = Provider.CLAUDE,
    val model: String = "",
    val baseUrl: String = "",
    val scenario: Scenario? = null,
    val messages: List<ChatMessage> = emptyList(),
    val sending: Boolean = false,
    val error: String? = null,
)

class TutorViewModel(private val c: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(TutorUi())
    val ui: StateFlow<TutorUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val s = c.settings.settings.first()
            val has = withContext(Dispatchers.IO) { c.keys.has(s.tutorProvider) }
            _ui.update { it.copy(loading = false, hasKey = has, provider = s.tutorProvider, model = s.tutorModel, baseUrl = s.tutorBaseUrl) }
        }
    }

    fun editSetup(on: Boolean) = _ui.update { it.copy(editingSetup = on) }

    fun saveSetup(provider: Provider, key: String, model: String, baseUrl: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { if (key.isNotBlank()) c.keys.save(provider, key) }
            c.settings.update { it.copy(tutorProvider = provider, tutorModel = model.trim(), tutorBaseUrl = baseUrl.trim()) }
            val has = withContext(Dispatchers.IO) { c.keys.has(provider) }
            _ui.update { it.copy(hasKey = has, editingSetup = !has, provider = provider, model = model.trim(), baseUrl = baseUrl.trim(), error = null) }
        }
    }

    fun forgetKey() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { c.keys.forget(_ui.value.provider) }
            _ui.update { it.copy(hasKey = false, editingSetup = true, scenario = null, messages = emptyList()) }
        }
    }

    fun start(scenario: Scenario) =
        _ui.update { it.copy(scenario = scenario, messages = listOf(ChatMessage(Role.ASSISTANT, scenario.opening)), error = null) }

    fun leave() = _ui.update { it.copy(scenario = null, messages = emptyList(), error = null, sending = false) }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _ui.value.sending) return
        _ui.update { it.copy(messages = it.messages + ChatMessage(Role.USER, trimmed), error = null) }
        ask()
    }

    fun retry() = ask()

    private fun ask() {
        val state = _ui.value
        val scenario = state.scenario ?: return
        _ui.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val settings = c.settings.settings.first()
            val key = withContext(Dispatchers.IO) { c.keys.get(settings.tutorProvider) }
            if (key == null) {
                _ui.update { it.copy(sending = false, hasKey = false, editingSetup = true, error = "Add your key again to keep chatting.") }
                return@launch
            }
            val words = c.repository.allCards().sortedByDescending { it.createdMillis }.take(40).map { it.spanish }
            val practice = settings.practice
            val level = when (practice.unlocked) {
                Stage.PRESENT -> "beginner (present tense so far)"
                Stage.PAST -> "lower intermediate (present and past tenses)"
                Stage.FUTURE -> "intermediate"
            }
            val variety = Variety.entries.firstOrNull { it.name == settings.region.name } ?: Variety.LATAM
            val system = TutorPrompt.system(scenario, variety, level, words, practice.practiceTenses.map { it.label().lowercase() })
            val config = TutorConfig(settings.tutorProvider, settings.tutorModel, settings.tutorBaseUrl)
            // Providers expect the conversation to start with the learner, so the tutor's opening line goes in as context.
            val history = _ui.value.messages.dropWhile { it.role == Role.ASSISTANT }
            val withOpening = listOf(ChatMessage(Role.USER, "(Start of the role-play. You opened with: \"${scenario.opening}\")")) +
                listOf(ChatMessage(Role.ASSISTANT, scenario.opening)) + history
            when (val reply = c.tutor.reply(config, key, system, withOpening)) {
                is Reply.Text -> _ui.update { it.copy(sending = false, messages = it.messages + ChatMessage(Role.ASSISTANT, reply.text)) }
                is Reply.Failure -> _ui.update { it.copy(sending = false, error = reply.message) }
            }
        }
    }
}

@Composable
fun TutorScreen() {
    val app = LocalContext.current.applicationContext
    val vm: TutorViewModel = viewModel { TutorViewModel(app.container) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val scenario = ui.scenario
    when {
        ui.loading -> Unit
        !ui.hasKey || ui.editingSetup -> Setup(ui, vm)
        scenario == null -> ScenarioPicker(ui, vm)
        else -> Chat(ui, scenario, vm)
    }
}

@Composable
private fun Setup(ui: TutorUi, vm: TutorViewModel) {
    var provider by rememberSaveable { mutableStateOf(ui.provider) }
    var key by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf(ui.model) }
    var baseUrl by rememberSaveable { mutableStateOf(ui.baseUrl) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tuzo(size = 72.dp)
            Column {
                Text("Your AI tutor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Practice real conversations with Tuzo, using your own AI key.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Service", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Provider.entries.forEach { p -> FilterChip(selected = provider == p, onClick = { provider = p }, label = { Text(p.label) }) }
        }
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text(if (ui.hasKey && provider == ui.provider) "API key (leave empty to keep the saved one)" else "API key") },
            placeholder = { Text(provider.keyHint) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Model") },
            placeholder = { Text(provider.defaultModel.ifEmpty { "model name" }) },
            supportingText = { Text("Leave empty for the default.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (provider == Provider.CUSTOM) {
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Service address") },
                placeholder = { Text("https://…/v1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Text(
                "Your key is encrypted with this phone's secure keystore and is only sent to the service you pick. " +
                    "It is never included in backups or exports. The service bills your account for each message.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
            )
        }
        Button(
            onClick = { vm.saveSetup(provider, key, model, baseUrl) },
            enabled = key.isNotBlank() || (ui.hasKey && provider == ui.provider),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
        if (ui.hasKey) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { vm.editSetup(false) }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                TextButton(onClick = vm::forgetKey, modifier = Modifier.weight(1f)) { Text("Remove saved key") }
            }
        }
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun ScenarioPicker(ui: TutorUi, vm: TutorViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tutor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.editSetup(true) }) { Icon(Icons.Filled.Settings, contentDescription = "Tutor settings") }
        }
        Text("Pick a situation. Tuzo plays the other person and fixes mistakes as you go.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Using ${ui.provider.label}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Scenarios.all.forEach { s ->
            Card(onClick = { vm.start(s) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(s.emoji, style = MaterialTheme.typography.headlineMedium)
                    Column {
                        Text(s.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(s.opening, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Chat(ui: TutorUi, scenario: Scenario, vm: TutorViewModel) {
    var draft by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    LaunchedEffect(ui.messages.size, ui.error) {
        if (ui.messages.isNotEmpty()) list.animateScrollToItem(ui.messages.size)
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::leave) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Leave conversation") }
            Text("${scenario.emoji} ${scenario.title}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Tuzo(size = 40.dp, mood = if (ui.sending) TuzoMood.CHEER else TuzoMood.IDLE, modifier = Modifier.padding(end = 12.dp))
        }
        if (ui.sending) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = list,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(ui.messages) { _, m -> Bubble(m) }
            item {
                ui.error?.let { e ->
                    Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                        Text(e, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = vm::retry) { Text("Try again") }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Escribe en español…") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { vm.send(draft); draft = "" }),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { vm.send(draft); draft = "" }, enabled = draft.isNotBlank() && !ui.sending) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage) {
    val mine = m.role == Role.USER
    val (body, correction) = if (mine) m.text to null else splitCorrection(m.text)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(Modifier.widthIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(body, modifier = Modifier.padding(12.dp))
            }
            correction?.let {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                }
            }
        }
    }
}

/** Pulls Tuzo's "✏️" correction line out of a reply so it can be shown on its own. */
fun splitCorrection(text: String): Pair<String, String?> {
    val lines = text.lines()
    val i = lines.indexOfFirst { it.trimStart().startsWith("✏️") }
    if (i < 0) return text to null
    val body = lines.filterIndexed { j, _ -> j != i }.joinToString("\n").trim()
    return body.ifEmpty { text } to lines[i].trim()
}

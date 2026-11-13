package app.vocablo.ui.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vocablo.cards.Card as VocabCard
import app.vocablo.cards.CardType
import app.vocablo.cards.DuplicateResult
import app.vocablo.container
import app.vocablo.dictionary.DictionaryEntry
import app.vocablo.ui.Navigator
import app.vocablo.ui.Screen
import app.vocablo.ui.home.label
import app.vocablo.ui.theme.AppIcon
import app.vocablo.ui.theme.AppIcons

private val ACCENT_KEYS = listOf("á", "é", "í", "ó", "ú", "ñ", "ü", "¿", "¡")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorScreen(deckId: Long, cardId: Long?, nav: Navigator) {
    val app = LocalContext.current.applicationContext
    val vm: CardEditorViewModel = viewModel {
        val c = app.container
        CardEditorViewModel(c.repository, c.settings, { c.dictionary }, c.onlineDictionary, { c.verbForms }, deckId, cardId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val dupText = liveDuplicateText(state.liveDuplicate, state.deckNames)

    LaunchedEffect(state.done) { if (state.done) nav.back() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editingId == null) "Add card" else "Edit card") },
                navigationIcon = {
                    IconButton(onClick = nav::back) { AppIcon(AppIcons.Close, contentDescription = "Close") }
                },
                actions = {
                    TextButton(onClick = { vm.save() }, enabled = state.canSave) { Text("Save") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.spanish,
                onValueChange = vm::onSpanish,
                label = { Text("Spanish") },
                placeholder = { Text("Start typing, e.g. aguacate") },
                singleLine = true,
                isError = state.liveDuplicate is DuplicateResult.Exact,
                supportingText = if (dupText != null) {
                    { Text(dupText) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ACCENT_KEYS.forEach { key ->
                    AssistChip(onClick = { vm.appendChar(key) }, label = { Text(key) })
                }
            }

            state.formHint?.let { match ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "“${state.spanish.trim()}” is a form of ${match.verb.infinitive}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${match.tense.name.lowercase()}, ${match.person.label()}. One card for ${match.verb.infinitive} " +
                                "covers every form, and the verb drills practice them.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.useInfinitive(match) }) { Text("Add ${match.verb.infinitive}") }
                            TextButton(onClick = vm::dismissFormHint) { Text("Keep as typed") }
                        }
                    }
                }
            }

            if (state.suggestions.isNotEmpty()) {
                EntryList("Dictionary", state.suggestions, vm::pick)
            }
            if (state.spanish.isNotBlank()) {
                OnlineSection(state.online, onLookUp = vm::lookUpOnline, onPick = vm::pick)
            }

            OutlinedTextField(
                value = state.english,
                onValueChange = vm::onEnglish,
                label = { Text("English meaning") },
                supportingText = { Text("Separate meanings with commas") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardType.entries.forEach { t ->
                    FilterChip(
                        selected = state.type == t,
                        onClick = { vm.onType(t) },
                        label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            OutlinedTextField(state.example, vm::onExample, Modifier.fillMaxWidth(), label = { Text("Example sentence (optional)") })
            OutlinedTextField(state.exampleEnglish, vm::onExampleEnglish, Modifier.fillMaxWidth(), label = { Text("Example in English (optional)") })
            OutlinedTextField(state.regionNote, vm::onRegionNote, Modifier.fillMaxWidth(), label = { Text("Region note (optional)") })

            Button(onClick = { vm.save() }, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.editingId == null) "Save and add another" else "Save changes")
            }
            if (state.editingId == null && state.addedThisVisit > 0) {
                OutlinedButton(onClick = nav::back, modifier = Modifier.fillMaxWidth()) {
                    Text("Done (${state.addedThisVisit} added)")
                }
            }
            if (state.editingId != null) {
                TextButton(onClick = vm::delete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete card", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    state.duplicateDialog?.let { dup ->
        DuplicateDialog(
            dup = dup,
            typed = state.spanish.trim(),
            deckNames = state.deckNames,
            editing = state.editingId != null,
            onMerge = vm::mergeIntoExisting,
            onOpen = { existing ->
                vm.dismissDuplicate()
                nav.replace(Screen.EditCard(existing.deckId, existing.id))
            },
            onAddAnyway = { vm.save(force = true) },
            onCancel = vm::dismissDuplicate,
        )
    }
}

private fun liveDuplicateText(dup: DuplicateResult, deckNames: Map<Long, String>): String? = when (dup) {
    is DuplicateResult.Exact -> "Already in ${deckNames[dup.existing.deckId] ?: "a deck"}: ${dup.existing.english}"
    is DuplicateResult.Similar -> dup.candidates.first().let { "You also have “${it.spanish}” (${it.english})" }
    DuplicateResult.None -> null
}

@Composable
private fun EntryList(title: String, entries: List<DictionaryEntry>, onPick: (DictionaryEntry) -> Unit) {
    Card {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
            )
            entries.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider()
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(e) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(e.spanish, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOf(e.partOfSpeech.label, e.gender).filter { it.isNotEmpty() }.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(e.english, style = MaterialTheme.typography.bodyMedium)
                    if (e.example.isNotEmpty()) {
                        Text("“${e.example}”", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (e.regionNote.isNotEmpty()) {
                        Text(e.regionNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineSection(online: OnlineLookup, onLookUp: () -> Unit, onPick: (DictionaryEntry) -> Unit) {
    when (online) {
        OnlineLookup.Idle -> TextButton(onClick = onLookUp) { Text("Not the word you want? Look it up online (free)") }
        OnlineLookup.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text("Asking Wiktionary…")
        }
        is OnlineLookup.Found -> EntryList("Wiktionary (CC BY-SA)", online.entries, onPick)
        OnlineLookup.NotFound -> Text(
            "Nothing found online, or you're offline. Type the meaning yourself.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OnlineLookup.Off -> Text(
            "Online lookup is off. Turn it on in Me → Dictionary.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DuplicateDialog(
    dup: DuplicateResult,
    typed: String,
    deckNames: Map<Long, String>,
    editing: Boolean,
    onMerge: (VocabCard) -> Unit,
    onOpen: (VocabCard) -> Unit,
    onAddAnyway: () -> Unit,
    onCancel: () -> Unit,
) {
    val existing: VocabCard
    val title: String
    val body: String
    var newMeanings: List<String> = emptyList()
    when (dup) {
        is DuplicateResult.Exact -> {
            existing = dup.existing
            newMeanings = dup.newMeanings
            title = "You already have “${existing.spanish}”"
            body = buildString {
                append("It's in ${deckNames[existing.deckId] ?: "another deck"} as “${existing.english}”.")
                if (newMeanings.isNotEmpty()) append("\n\nNew meaning you typed: ${newMeanings.joinToString(", ")}.")
            }
        }
        is DuplicateResult.Similar -> {
            existing = dup.candidates.first()
            title = "Did you mean “${existing.spanish}”?"
            body = "You have “${existing.spanish}” (${existing.english}). Accents can change the meaning, " +
                "so “$typed” is fine if that's what you meant."
        }
        DuplicateResult.None -> return
    }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (!editing && newMeanings.isNotEmpty()) {
                    TextButton(onClick = { onMerge(existing) }) { Text("Add the new meaning to that card") }
                }
                TextButton(onClick = { onOpen(existing) }) { Text("Open that card") }
                TextButton(onClick = onAddAnyway) { Text(if (editing) "Save anyway" else "Add as a separate card") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        },
    )
}

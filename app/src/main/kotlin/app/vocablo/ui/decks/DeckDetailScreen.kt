package app.vocablo.ui.decks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vocablo.cards.Card
import app.vocablo.cards.CardType
import app.vocablo.cards.Deck
import app.vocablo.container
import app.vocablo.data.VocabloRepository
import app.vocablo.ui.Navigator
import app.vocablo.ui.Screen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeckViewModel(private val repo: VocabloRepository, val deckId: Long) : ViewModel() {
    val deck: StateFlow<Deck?> = repo.deck(deckId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val cards: StateFlow<List<Card>?> = repo.cards(deckId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun rename(name: String) {
        if (name.isNotBlank()) viewModelScope.launch { repo.renameDeck(deckId, name) }
    }

    fun delete(then: () -> Unit) {
        viewModelScope.launch {
            repo.deleteDeck(deckId)
            then()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(deckId: Long, nav: Navigator) {
    val app = LocalContext.current.applicationContext
    val vm: DeckViewModel = viewModel(key = "deck-$deckId") { DeckViewModel(app.container.repository, deckId) }
    val deck by vm.deck.collectAsStateWithLifecycle()
    val cards by vm.cards.collectAsStateWithLifecycle()
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deck?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { renaming = true }) { Icon(Icons.Filled.Edit, contentDescription = "Rename deck") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete deck") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.open(Screen.EditCard(deckId, null)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add card") },
            )
        },
    ) { padding ->
        val list = cards.orEmpty()
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { nav.open(Screen.Study(deckId)) }, enabled = list.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text("Study this deck")
                    }
                    if (cards != null && list.isEmpty()) {
                        Text(
                            "No cards yet. Tap Add card and start typing a Spanish word.",
                            Modifier.fillMaxWidth().padding(top = 24.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(list, key = { it.id }) { card ->
                ListItem(
                    headlineContent = { Text(card.spanish) },
                    supportingContent = { Text(card.english) },
                    overlineContent = if (card.type != CardType.WORD) {
                        { Text(card.type.name.lowercase()) }
                    } else {
                        null
                    },
                    modifier = Modifier.clickable { nav.open(Screen.EditCard(deckId, card.id)) },
                )
                HorizontalDivider()
            }
        }
    }

    if (renaming) {
        NameDialog("Rename deck", deck?.name.orEmpty(), "Save", onDismiss = { renaming = false }) {
            vm.rename(it)
            renaming = false
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${deck?.name}?") },
            text = { Text("This deletes its ${cards.orEmpty().size} cards and their progress. Your streak stays.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(then = nav::back)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

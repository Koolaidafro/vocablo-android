package app.vocablo.ui.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vocablo.data.DeckSummary
import app.vocablo.ui.LibraryViewModel
import app.vocablo.ui.Navigator
import app.vocablo.ui.Screen
import app.vocablo.ui.components.deckColor
import app.vocablo.ui.theme.AppIcon
import app.vocablo.ui.theme.AppIcons

@Composable
fun DecksScreen(library: LibraryViewModel, nav: Navigator) {
    val state by library.state.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Decks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${state.totalCards} cards. No limit, all stored on this phone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.decks, key = { it.deck.id }) { summary ->
                DeckRow(
                    summary,
                    onOpen = { nav.open(Screen.Deck(summary.deck.id)) },
                    onStudy = { nav.open(Screen.Study(summary.deck.id)) },
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = { creating = true },
            icon = { AppIcon(AppIcons.Add, contentDescription = null) },
            text = { Text("New deck") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (creating) {
        NameDialog(
            title = "New deck",
            initial = "",
            confirm = "Create",
            onDismiss = { creating = false },
            onConfirm = {
                library.createDeck(it)
                creating = false
            },
        )
    }
}

@Composable
private fun DeckRow(summary: DeckSummary, onOpen: () -> Unit, onStudy: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(deckColor(summary.deck.colorIndex)))
            Column(Modifier.weight(1f)) {
                Text(summary.deck.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${summary.cardCount} cards · ${summary.dueCount} due · ${summary.newCount} new",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (summary.dueCount + summary.newCount > 0) {
                FilledTonalButton(onClick = onStudy) { Text("Study") }
            }
        }
    }
}

@Composable
fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

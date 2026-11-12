package app.vocablo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vocablo.design.ThemeId
import app.vocablo.design.ThemeMode
import app.vocablo.design.Themes
import app.vocablo.ui.theme.VocabloTheme
import app.vocablo.verbs.Conjugator
import app.vocablo.verbs.Person
import app.vocablo.verbs.StarterVerbs
import app.vocablo.verbs.Tense

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    DECKS("Decks", Icons.AutoMirrored.Filled.List),
    PLAY("Play", Icons.Filled.PlayArrow),
    TUTOR("Tutor", Icons.Filled.Face),
    ME("Me", Icons.Filled.Person),
}

/**
 * Phase 1 app shell: the five-tab layout and live theme switching.
 * Appearance choices are kept in memory for now; phase 2 persists them with DataStore.
 */
@Composable
fun VocabloApp() {
    var themeId by rememberSaveable { mutableStateOf(Themes.default) }
    var mode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }

    VocabloTheme(themeId = themeId, mode = mode) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(tab.label, style = MaterialTheme.typography.headlineMedium)
                when (tab) {
                    Tab.HOME -> VerbOfTheDay()
                    Tab.ME -> AppearanceSettings(themeId, mode, onTheme = { themeId = it }, onMode = { mode = it })
                    else -> Text("Coming in a later phase.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun VerbOfTheDay() {
    val verb = StarterVerbs.byInfinitive.getValue("tener")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Verb of the day", style = MaterialTheme.typography.labelLarge)
            Text("${verb.infinitive} · ${verb.english}", style = MaterialTheme.typography.titleLarge)
            Conjugator.table(verb, Tense.PRETERITE).forEach { (person, form) ->
                if (person != Person.VOSOTROS) Text("${person.label()}  $form")
            }
        }
    }
}

private fun Person.label() = when (this) {
    Person.YO -> "yo"
    Person.TU -> "tú"
    Person.EL -> "él · ella · usted"
    Person.NOSOTROS -> "nosotros"
    Person.VOSOTROS -> "vosotros"
    Person.ELLOS -> "ellos · ustedes"
}

@Composable
private fun AppearanceSettings(
    themeId: ThemeId,
    mode: ThemeMode,
    onTheme: (ThemeId) -> Unit,
    onMode: (ThemeMode) -> Unit,
) {
    Text("Mode", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { m ->
            FilterChip(
                selected = m == mode,
                onClick = { onMode(m) },
                label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) },
            )
        }
    }
    Text("Theme", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Themes.all.forEach { theme ->
            FilterChip(
                selected = theme.id == themeId,
                onClick = { onTheme(theme.id) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(theme.light.primary)),
                    )
                },
                label = { Text(theme.displayName) },
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Theme and mode are kept until the app closes. Saving them comes in phase 2.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VocabloAppPreview() {
    VocabloApp()
}

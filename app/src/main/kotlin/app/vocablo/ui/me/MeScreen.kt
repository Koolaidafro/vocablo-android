package app.vocablo.ui.me

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vocablo.data.Region
import app.vocablo.design.ThemeMode
import app.vocablo.design.Themes
import app.vocablo.notify.Notifications
import app.vocablo.ui.LibraryViewModel
import java.time.LocalDate

@Composable
fun MeScreen(library: LibraryViewModel) {
    val context = LocalContext.current
    val settings by library.settings.collectAsStateWithLifecycle()
    val state by library.state.collectAsStateWithLifecycle()
    val message by library.message.collectAsStateWithLifecycle()
    var confirmRestore by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            library.messageShown()
        }
    }

    val sendReminder = {
        val sent = Notifications.showStudyReminder(context, state.due + state.new.coerceAtMost(settings.newPerDay), state.streak)
        if (!sent) Toast.makeText(context, "Notifications are turned off for Vocablo in system settings.", Toast.LENGTH_LONG).show()
    }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sendReminder() else Toast.makeText(context, "Tuzo will stay quiet. You can allow notifications later.", Toast.LENGTH_LONG).show()
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(library::importFrom) }
    val restoreFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(library::restoreFrom) }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(library::exportCsvTo) }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(library::backupTo) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Me", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Fact("${state.streak}", "day streak")
                Fact("${state.longestStreak}", "best streak")
                Fact("${state.totalCards}", "cards")
            }
        }

        Section("Appearance")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { m ->
                FilterChip(
                    selected = m == settings.mode,
                    onClick = { library.updateSettings { it.copy(mode = m) } },
                    label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Themes.all.forEach { theme ->
                FilterChip(
                    selected = theme.id == settings.themeId,
                    onClick = { library.updateSettings { it.copy(themeId = theme.id) } },
                    leadingIcon = { Box(Modifier.size(18.dp).clip(CircleShape).background(Color(theme.light.primary))) },
                    label = { Text(theme.displayName) },
                )
            }
        }

        Section("Your Spanish")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Region.entries.forEach { r ->
                FilterChip(
                    selected = r == settings.region,
                    onClick = { library.updateSettings { it.copy(region = r) } },
                    label = { Text(r.label) },
                )
            }
        }
        Text("New cards per day", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 30, 50).forEach { n ->
                FilterChip(
                    selected = n == settings.newPerDay,
                    onClick = { library.updateSettings { it.copy(newPerDay = n) } },
                    label = { Text("$n") },
                )
            }
        }

        Section("Dictionary")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Free online lookup")
                Text(
                    "When a word isn't in the offline dictionary, ask Wiktionary. No account, no cost.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = settings.onlineLookup, onCheckedChange = { on -> library.updateSettings { it.copy(onlineLookup = on) } })
        }

        Section("Reminders")
        Text(
            "Tuzo can nudge you when cards are due. Daily scheduling arrives with streak quests; for now you can see how it looks.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && Notifications.needsPermission(context)) {
                    askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    sendReminder()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Send a test reminder") }

        Section("Your data")
        OutlinedButton(
            onClick = { importFile.launch(arrayOf("text/*", "application/octet-stream")) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Import cards (CSV or Anki text)") }
        OutlinedButton(onClick = { exportCsv.launch("vocablo-cards.csv") }, modifier = Modifier.fillMaxWidth()) { Text("Export cards as CSV") }
        OutlinedButton(onClick = { backup.launch("vocablo-backup-${LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) {
            Text("Back up everything")
        }
        OutlinedButton(onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) { Text("Restore a backup") }

        Section("About")
        Text(
            "Vocablo is open source. Dictionary text from Wiktionary (CC BY-SA 4.0). Starter dictionary examples written for Vocablo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore a backup?") },
            text = { Text("This replaces every deck and card on this phone with the ones in the backup.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    restoreFile.launch(arrayOf("application/json", "application/octet-stream", "text/*"))
                }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider(Modifier.padding(top = 8.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun Fact(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

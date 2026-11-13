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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vocablo.BuildConfig
import app.vocablo.container
import app.vocablo.data.Region
import app.vocablo.design.ThemeMode
import app.vocablo.design.Themes
import app.vocablo.notify.Notifications
import app.vocablo.play.Badge
import app.vocablo.play.ReminderMessages
import app.vocablo.ui.components.BadgeArt
import app.vocablo.ui.LibraryViewModel
import app.vocablo.update.UpdateState
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun MeScreen(library: LibraryViewModel) {
    val context = LocalContext.current
    val settings by library.settings.collectAsStateWithLifecycle()
    val state by library.state.collectAsStateWithLifecycle()
    val progress by library.progress.collectAsStateWithLifecycle()
    val message by library.message.collectAsStateWithLifecycle()
    var confirmRestore by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            library.messageShown()
        }
    }

    val sendReminder = {
        val due = state.due + state.new.coerceAtMost(settings.newPerDay)
        val kind = ReminderMessages.kindFor(saver = false, dueCount = due, streak = state.streak)
        val sent = Notifications.showStudyReminder(context, ReminderMessages.pick(kind, LocalDate.now().toEpochDay(), due, state.streak))
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
        BadgesCard(progress.badges)

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Daily nudge from Tuzo")
                Text(
                    "At most two a day: one at your time, and an 8 pm streak saver. Quiet once you've studied.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = settings.remindersOn,
                onCheckedChange = { on ->
                    library.updateSettings { it.copy(remindersOn = on) }
                    if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && Notifications.needsPermission(context)) {
                        askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }
        if (settings.remindersOn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 9, 12, 18).forEach { h ->
                    FilterChip(
                        selected = settings.reminderHour == h,
                        onClick = { library.updateSettings { it.copy(reminderHour = h) } },
                        label = { Text(hourLabel(h)) },
                    )
                }
            }
        }
        TextButton(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && Notifications.needsPermission(context)) {
                    askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    sendReminder()
                }
            },
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
        AppVersion()
        Text(
            "Vocablo is open source. Dictionary text from Wiktionary (CC BY-SA 4.0). Verb ranking from FrequencyWords by Hermit Dave (CC BY-SA 4.0). Starter dictionary examples written for Vocablo.",
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

private fun hourLabel(h: Int): String = when {
    h == 0 -> "12 am"
    h < 12 -> "$h am"
    h == 12 -> "12 pm"
    else -> "${h - 12} pm"
}

@Composable
private fun AppVersion() {
    val updates = LocalContext.current.container.updates
    val state by updates.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Version ${BuildConfig.VERSION_NAME}")
            if (updates.enabled) {
                Text(
                    when (val s = state) {
                        UpdateState.Checking -> "Checking…"
                        UpdateState.UpToDate -> "You have the latest version."
                        is UpdateState.Available -> "Version ${s.release.versionName} is ready. See Home to install it."
                        is UpdateState.Downloading -> "Downloading…"
                        is UpdateState.Failed -> s.message
                        UpdateState.Idle -> "Updates come from GitHub."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (updates.enabled) TextButton(onClick = { scope.launch { updates.check() } }) { Text("Check now") }
    }
}

/**
 * Every badge, earned ones in colour and locked ones faded with how to earn them (FR-22).
 *
 * Design: showing locked badges gives a clear next goal; their faded look keeps them from
 * reading as a loss or a debt (NFR-7).
 */
@Composable
private fun BadgesCard(earned: Set<Badge>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Badges · ${earned.size} of ${Badge.entries.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = BADGES_PER_ROW,
            ) {
                Badge.entries.forEach { badge ->
                    val has = badge in earned
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        BadgeArt(badge, earned = has, size = 64.dp)
                        Text(badge.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        if (!has) {
                            Text(
                                badge.howToEarn,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Three badges a row fits a 320 dp phone with readable titles. */
private const val BADGES_PER_ROW = 3

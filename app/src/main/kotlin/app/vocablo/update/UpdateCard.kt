package app.vocablo.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vocablo.container
import kotlinx.coroutines.launch

/** Shown on Home when a newer version is on GitHub. Shows nothing otherwise. */
@Composable
fun UpdateCard() {
    val updates = LocalContext.current.container.updates
    if (!updates.enabled) return
    val state by updates.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val release = when (val s = state) {
        is UpdateState.Available -> s.release
        is UpdateState.Downloading -> s.release
        is UpdateState.Failed -> s.release
        else -> null
    } ?: return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("New version ${release.versionName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (release.notes.isNotEmpty()) Text(release.notes.lines().take(4).joinToString("\n"), style = MaterialTheme.typography.bodySmall)
            when (val s = state) {
                is UpdateState.Downloading -> LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                else -> {
                    if (s is UpdateState.Failed) Text(s.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { scope.launch { updates.install(release) } }, modifier = Modifier.fillMaxWidth()) {
                        Text("Update now")
                    }
                }
            }
            Text("Your cards and progress stay as they are.", style = MaterialTheme.typography.labelSmall)
        }
    }
}

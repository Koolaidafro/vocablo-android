package app.vocablo.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vocablo.cards.Streaks
import app.vocablo.data.AppSettings
import app.vocablo.data.DeckSummary
import app.vocablo.data.SettingsStore
import app.vocablo.data.VerbRepository
import app.vocablo.data.VocabloRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryState(
    val decks: List<DeckSummary> = emptyList(),
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val streakAtRisk: Boolean = false,
    val answeredToday: Int = 0,
    val dueVerbItems: Int = 0,
    val loaded: Boolean = false,
) {
    val due: Int get() = decks.sumOf { it.dueCount }
    val new: Int get() = decks.sumOf { it.newCount }
    val totalCards: Int get() = decks.sumOf { it.cardCount }
}

/** Shared by Home, Decks and Me: deck counts, streaks, settings and file import/export. */
class LibraryViewModel(
    private val repo: VocabloRepository,
    verbs: VerbRepository,
    private val settingsStore: SettingsStore,
    private val resolver: ContentResolver,
) : ViewModel() {

    val state: StateFlow<LibraryState> = combine(
        repo.deckSummaries(),
        repo.studyDays(),
        repo.answersToday(),
        verbs.dueCount(),
    ) { decks, days, answered, dueVerbs ->
        val today = repo.today()
        LibraryState(
            decks = decks,
            streak = Streaks.current(days, today),
            longestStreak = Streaks.longest(days),
            streakAtRisk = Streaks.atRisk(days, today),
            answeredToday = answered,
            dueVerbItems = dueVerbs,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    /** Null until the saved settings have been read, so the first frame never shows defaults. */
    val loadedSettings: StateFlow<AppSettings?> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** One-off messages for the UI to show, e.g. "Imported 40 cards". */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun messageShown() = _message.update { null }

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsStore.update(change) }
    }

    fun createDeck(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.createDeck(name) }
    }

    fun importFrom(uri: Uri) = runFileJob {
        val text = readText(uri)
        val r = repo.importCards(text)
        buildString {
            append("Imported ${r.added} ${if (r.added == 1) "card" else "cards"}.")
            if (r.duplicates > 0) append(" Skipped ${r.duplicates} you already have.")
            if (r.badLines.isNotEmpty()) append(" ${r.badLines.size} lines couldn't be read.")
        }
    }

    fun exportCsvTo(uri: Uri) = runFileJob {
        writeText(uri, repo.exportCsv())
        "Cards exported."
    }

    fun backupTo(uri: Uri) = runFileJob {
        writeText(uri, repo.exportBackup())
        "Backup saved. It has your cards and progress, never your API keys."
    }

    fun restoreFrom(uri: Uri) = runFileJob {
        val count = repo.restoreBackup(readText(uri))
        "Restored $count ${if (count == 1) "card" else "cards"}."
    }

    private fun runFileJob(job: suspend () -> String) {
        viewModelScope.launch {
            _message.value = try {
                job()
            } catch (e: IllegalArgumentException) {
                e.message ?: "That file couldn't be used."
            } catch (e: java.io.IOException) {
                "Couldn't open the file: ${e.message}"
            }
        }
    }

    private suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw java.io.IOException("no access")
    }

    private suspend fun writeText(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        (resolver.openOutputStream(uri, "wt") ?: throw java.io.IOException("no access"))
            .bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }
}

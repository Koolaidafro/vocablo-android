package app.vocablo.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vocablo.cards.Streaks
import app.vocablo.data.AppSettings
import app.vocablo.data.DeckSummary
import app.vocablo.data.ProgressRepository
import app.vocablo.data.SettingsStore
import app.vocablo.play.Badge
import app.vocablo.play.DailyQuests
import app.vocablo.play.DayActivity
import app.vocablo.play.Quest
import app.vocablo.play.StreakFreezes
import app.vocablo.play.WeeklyChest
import app.vocablo.play.WelcomeBack
import app.vocablo.play.Xp
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
    /** Days a streak freeze covered; they keep the chain without adding to it. */
    val frozenDays: Set<Long> = emptySet(),
    /** True after 3 or more days without study, so Home greets the learner (FR-24). */
    val welcomeBack: Boolean = false,
) {
    val due: Int get() = decks.sumOf { it.dueCount }
    val new: Int get() = decks.sumOf { it.newCount }
    val totalCards: Int get() = decks.sumOf { it.cardCount }
}

data class ProgressState(
    val totalXp: Int = 0,
    val today: DayActivity = DayActivity(),
    val quests: List<Quest> = emptyList(),
    val questDaysThisWeek: Int = 0,
    val chestReady: Boolean = false,
    val chestOpened: Boolean = false,
    val freezesHeld: Int = 0,
    /** Badges earned so far (FR-22). */
    val badges: Set<Badge> = emptySet(),
) {
    val level: Int get() = Xp.level(totalXp)
    val levelProgress: Float get() = Xp.levelProgress(totalXp)
}

/** The inputs [LibraryViewModel.progress] combines beyond the first four flows. */
private data class ProgressInputs(
    val settings: AppSettings,
    val studyDays: Set<Long>,
    val frozenDays: Set<Long>,
    val badges: Set<Badge>,
)

/** Shared by Home, Decks and Me: deck counts, streaks, settings and file import/export. */
class LibraryViewModel(
    private val repo: VocabloRepository,
    verbs: VerbRepository,
    private val progressRepo: ProgressRepository,
    private val settingsStore: SettingsStore,
    private val resolver: ContentResolver,
) : ViewModel() {

    val state: StateFlow<LibraryState> = combine(
        repo.deckSummaries(),
        repo.studyDays(),
        repo.answersToday(),
        verbs.dueCount(),
        progressRepo.frozenDays(),
    ) { decks, days, answered, dueVerbs, frozen ->
        val today = repo.today()
        LibraryState(
            decks = decks,
            streak = StreakFreezes.streak(days, frozen, today),
            longestStreak = StreakFreezes.longest(days, frozen),
            streakAtRisk = Streaks.atRisk(days + frozen, today),
            answeredToday = answered,
            dueVerbItems = dueVerbs,
            loaded = true,
            frozenDays = frozen,
            welcomeBack = WelcomeBack.show(days, today),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    /** Null until the saved settings have been read, so the first frame never shows defaults. */
    val loadedSettings: StateFlow<AppSettings?> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val progress: StateFlow<ProgressState> = combine(
        progressRepo.totalXp(),
        progressRepo.todayActivity(),
        progressRepo.questDays(),
        progressRepo.chestOpenedThisWeek(),
        combine(settingsStore.settings, repo.studyDays(), progressRepo.frozenDays(), progressRepo.badges(), ::ProgressInputs),
    ) { xp, day, questDays, opened, (settings, study, frozen, badges) ->
        val today = progressRepo.today()
        ProgressState(
            totalXp = xp,
            today = day,
            quests = DailyQuests.forDay(today, settings.newPerDay),
            questDaysThisWeek = WeeklyChest.questDaysThisWeek(questDays, today),
            chestReady = WeeklyChest.ready(questDays, today, opened),
            chestOpened = opened,
            freezesHeld = StreakFreezes.held(StreakFreezes.longest(study, frozen), frozen.size),
            badges = badges,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    /** Badges earned since Home last celebrated, oldest first (FR-22). Kept in memory only: the badge itself is saved. */
    private val _newBadges = MutableStateFlow<List<Badge>>(emptyList())
    val newBadges: StateFlow<List<Badge>> = _newBadges.asStateFlow()

    /** Called once Home has celebrated the new badges. */
    fun badgesCelebrated() = _newBadges.update { emptyList() }

    private suspend fun settleBadges() {
        val fresh = progressRepo.settleBadges()
        if (fresh.isNotEmpty()) _newBadges.update { it + fresh }
    }

    init {
        // Pay out quests whenever today's activity changes, wherever it happened.
        viewModelScope.launch {
            combine(progressRepo.todayActivity(), settingsStore.settings) { _, s -> s.newPerDay }
                .collect {
                    progressRepo.settleQuests(it)
                    settleBadges()
                }
        }
    }

    /** Opens the weekly chest; the XP shows up through [progress]. */
    fun openChest(onOpened: (Int) -> Unit) {
        viewModelScope.launch {
            onOpened(progressRepo.openChest())
            settleBadges()
        }
    }

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

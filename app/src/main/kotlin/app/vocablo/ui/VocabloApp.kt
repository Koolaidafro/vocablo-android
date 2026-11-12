package app.vocablo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vocablo.container
import app.vocablo.ui.cards.CardEditorScreen
import app.vocablo.ui.decks.DeckDetailScreen
import app.vocablo.ui.decks.DecksScreen
import app.vocablo.ui.home.HomeScreen
import app.vocablo.data.AppSettings
import app.vocablo.ui.onboarding.OnboardingScreen
import app.vocablo.ui.verbs.DrillScreen
import app.vocablo.ui.verbs.PlayHub
import app.vocablo.ui.verbs.PracticeSettingsScreen
import app.vocablo.ui.verbs.VerbDetailScreen
import app.vocablo.ui.verbs.VerbListScreen
import app.vocablo.ui.home.TutorScreen
import app.vocablo.ui.me.MeScreen
import app.vocablo.ui.study.StudyScreen
import app.vocablo.ui.theme.VocabloTheme

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    DECKS("Decks", Icons.AutoMirrored.Filled.List),
    PLAY("Play", Icons.Filled.PlayArrow),
    TUTOR("Tutor", Icons.Filled.Face),
    ME("Me", Icons.Filled.Person),
}

/** Screens above the tabs. Kept as a simple back stack that survives rotation. */
sealed interface Screen {
    data class Deck(val deckId: Long) : Screen
    data class EditCard(val deckId: Long, val cardId: Long?) : Screen
    data class Study(val deckId: Long?) : Screen
    data object Drill : Screen
    data object Practice : Screen
    data object Verbs : Screen
    data class VerbDetail(val infinitive: String) : Screen

    companion object {
        fun encode(s: Screen): String = when (s) {
            is Deck -> "deck:${s.deckId}"
            is EditCard -> "card:${s.deckId}:${s.cardId ?: ""}"
            is Study -> "study:${s.deckId ?: ""}"
            Drill -> "drill"
            Practice -> "practice"
            Verbs -> "verbs"
            is VerbDetail -> "verb:${s.infinitive}"
        }

        fun decode(s: String): Screen {
            val p = s.split(':')
            return when (p[0]) {
                "deck" -> Deck(p[1].toLong())
                "card" -> EditCard(p[1].toLong(), p[2].toLongOrNull())
                "drill" -> Drill
                "practice" -> Practice
                "verbs" -> Verbs
                "verb" -> VerbDetail(p[1])
                else -> Study(p[1].toLongOrNull())
            }
        }
    }
}

/**
 * Each open screen gets its own ViewModelStore, so a screen's state lives exactly as long as
 * the screen: it survives rotation and is cleared when the screen is closed.
 */
class ScreenStores : ViewModel() {
    private val stores = mutableMapOf<String, ViewModelStore>()

    fun storeFor(key: String): ViewModelStore = stores.getOrPut(key) { ViewModelStore() }

    fun clear(key: String) {
        stores.remove(key)?.clear()
    }

    override fun onCleared() {
        stores.values.forEach { it.clear() }
        stores.clear()
    }
}

class Navigator(val stack: SnapshotStateList<Screen>, private val stores: ScreenStores) {
    fun keyAt(index: Int): String = "$index:${Screen.encode(stack[index])}"

    fun open(screen: Screen) {
        stack.add(screen)
    }

    fun back() {
        if (stack.isEmpty()) return
        stores.clear(keyAt(stack.lastIndex))
        stack.removeAt(stack.lastIndex)
    }

    /** Swaps the top screen, e.g. from the add form to the existing card it duplicates. */
    fun replace(screen: Screen) {
        back()
        open(screen)
    }
}

@Composable
fun VocabloApp() {
    val app = LocalContext.current.applicationContext
    val library: LibraryViewModel = viewModel {
        LibraryViewModel(app.container.repository, app.container.verbs, app.container.settings, app.contentResolver)
    }
    VocabloRoot(library)
}

@Composable
private fun VocabloRoot(library: LibraryViewModel) {
    val loaded by library.loadedSettings.collectAsStateWithLifecycle()
    val settings = loaded ?: AppSettings()
    VocabloTheme(themeId = settings.themeId, mode = settings.mode) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (loaded == null) return@Surface
            if (!settings.onboarded) {
                OnboardingScreen(onFinish = library::updateSettings)
                return@Surface
            }
            val stack = rememberSaveable(
                saver = listSaver<SnapshotStateList<Screen>, String>(
                    save = { list -> list.map { Screen.encode(it) } },
                    restore = { saved -> saved.map { Screen.decode(it) }.toMutableStateList() },
                ),
            ) { mutableListOf<Screen>().toMutableStateList() }
            val stores: ScreenStores = viewModel { ScreenStores() }
            val nav = Navigator(stack, stores)
            var tab by rememberSaveable { mutableStateOf(Tab.HOME) }

            BackHandler(enabled = stack.isNotEmpty()) { nav.back() }

            val top = stack.lastOrNull()
            if (top == null) {
                Tabs(tab, onTab = { tab = it }, library, nav)
            } else {
                val key = nav.keyAt(stack.lastIndex)
                val owner = remember(key) {
                    object : ViewModelStoreOwner {
                        override val viewModelStore: ViewModelStore = stores.storeFor(key)
                    }
                }
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    when (top) {
                        is Screen.Deck -> DeckDetailScreen(top.deckId, nav)
                        is Screen.EditCard -> CardEditorScreen(top.deckId, top.cardId, nav)
                        is Screen.Study -> StudyScreen(top.deckId, nav)
                        Screen.Drill -> DrillScreen(nav)
                        Screen.Practice -> PracticeSettingsScreen(nav)
                        Screen.Verbs -> VerbListScreen(nav)
                        is Screen.VerbDetail -> VerbDetailScreen(top.infinitive, nav)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tabs(tab: Tab, onTab: (Tab) -> Unit, library: LibraryViewModel, nav: Navigator) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { onTab(t) },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.HOME -> HomeScreen(library, nav, onAddCards = { onTab(Tab.DECKS) })
                Tab.DECKS -> DecksScreen(library, nav)
                Tab.PLAY -> {
                    val state by library.state.collectAsStateWithLifecycle()
                    val settings by library.settings.collectAsStateWithLifecycle()
                    PlayHub(nav, state.dueVerbItems, settings.practice)
                }
                Tab.TUTOR -> TutorScreen()
                Tab.ME -> MeScreen(library)
            }
        }
    }
}

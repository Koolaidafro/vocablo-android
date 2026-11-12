package app.vocablo

import android.app.Application
import android.content.Context
import app.vocablo.data.ProgressRepository
import app.vocablo.data.SettingsStore
import app.vocablo.data.VocabloDatabase
import app.vocablo.data.VerbRepository
import app.vocablo.data.VocabloRepository
import app.vocablo.data.WiktionaryLookup
import app.vocablo.dictionary.OfflineDictionary
import app.vocablo.notify.Notifications
import app.vocablo.tutor.KeyVault
import app.vocablo.tutor.TutorClient
import app.vocablo.notify.ReminderScheduler
import app.vocablo.verbs.FormIndex
import app.vocablo.verbs.VerbList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Hand-made dependency container: small enough that a DI library would be overkill. */
class AppContainer(context: Context) {
    val database: VocabloDatabase = VocabloDatabase.create(context)
    val repository = VocabloRepository(database.dao())
    val settings = SettingsStore(context)
    val dictionary: OfflineDictionary by lazy { OfflineDictionary.starter() }
    val onlineDictionary = WiktionaryLookup()
    val verbForms: FormIndex by lazy { FormIndex(VerbList.all.map { it.verb }) }
    val verbs = VerbRepository(database.dao())
    val progress = ProgressRepository(database.dao())
    val keys: KeyVault by lazy { KeyVault.create(context) }
    val tutor = TutorClient()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

class VocabloApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        container.scope.launch {
            container.repository.seedIfEmpty()
            container.progress.applyFreezes()
            container.verbForms.lookup("") // builds the conjugated-form index off the main thread
        }
        // Keep the pending reminder in step with the reminder settings.
        container.scope.launch {
            container.settings.settings.map { it.remindersOn to it.reminderHour }.distinctUntilChanged()
                .collect { (on, hour) -> ReminderScheduler.schedule(this@VocabloApplication, on, hour) }
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as VocabloApplication).container

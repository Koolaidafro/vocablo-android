package app.vocablo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.vocablo.design.ThemeId
import app.vocablo.design.ThemeMode
import app.vocablo.design.Themes
import app.vocablo.verbs.AnswerMode
import app.vocablo.verbs.PracticeSettings
import app.vocablo.verbs.Stage
import app.vocablo.verbs.Tense
import app.vocablo.verbs.Tier
import app.vocablo.verbs.VerbSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The Spanish the learner wants. Latin American is the default. */
enum class Region(val label: String) {
    LATAM("Latin America"),
    MEXICO("Mexico"),
    CARIBBEAN("Caribbean"),
    ANDEAN("Andean"),
    RIOPLATENSE("Río de la Plata"),
    SPAIN("Spain"),
}

data class AppSettings(
    val themeId: ThemeId = Themes.default,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val region: Region = Region.LATAM,
    val newPerDay: Int = 20,
    val onlineLookup: Boolean = true,
    val practice: PracticeSettings = PracticeSettings(),
    /** False until the first-launch questions are answered or skipped. */
    val onboarded: Boolean = false,
    /** Tuzo's nudges: a daily one at [reminderHour] and an evening streak saver. Two a day at most. */
    val remindersOn: Boolean = true,
    val reminderHour: Int = 9,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Settings saved on the phone. API keys never go here; they get encrypted storage in phase 5. */
class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<AppSettings> = store.data.map(::read)

    suspend fun update(change: (AppSettings) -> AppSettings) {
        store.edit { p ->
            val new = change(read(p))
            p[THEME] = new.themeId.name
            p[MODE] = new.mode.name
            p[REGION] = new.region.name
            p[NEW_PER_DAY] = new.newPerDay
            p[ONLINE_LOOKUP] = new.onlineLookup
            val pr = new.practice
            p[STAGE] = pr.unlocked.name
            p[TENSES] = pr.enabledTenses.map { it.name }.toSet()
            p[TIER] = pr.maxTier.name
            p[VOSOTROS] = pr.includeVosotros
            p[SOURCE] = pr.source.name
            p[ANSWER_MODE] = pr.answerMode.name
            p[ONBOARDED] = new.onboarded
            p[REMINDERS] = new.remindersOn
            p[REMINDER_HOUR] = new.reminderHour
        }
    }

    private fun read(p: Preferences) = AppSettings(
        themeId = p[THEME].toEnum(Themes.default),
        mode = p[MODE].toEnum(ThemeMode.SYSTEM),
        region = p[REGION].toEnum(Region.LATAM),
        newPerDay = p[NEW_PER_DAY] ?: 20,
        onlineLookup = p[ONLINE_LOOKUP] ?: true,
        practice = PracticeSettings(
            unlocked = p[STAGE].toEnum(Stage.PRESENT),
            enabledTenses = p[TENSES]?.mapNotNull { n -> Tense.entries.firstOrNull { it.name == n } }?.toSet() ?: setOf(Tense.PRESENT),
            maxTier = p[TIER].toEnum(Tier.TOP_100),
            includeVosotros = p[VOSOTROS] ?: false,
            source = p[SOURCE].toEnum(VerbSource.TOP_LIST),
            answerMode = p[ANSWER_MODE].toEnum(AnswerMode.CHOOSE),
        ),
        onboarded = p[ONBOARDED] ?: false,
        remindersOn = p[REMINDERS] ?: true,
        reminderHour = p[REMINDER_HOUR] ?: 9,
    )

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val MODE = stringPreferencesKey("mode")
        val REGION = stringPreferencesKey("region")
        val NEW_PER_DAY = intPreferencesKey("new_per_day")
        val ONLINE_LOOKUP = booleanPreferencesKey("online_lookup")
        val STAGE = stringPreferencesKey("practice_stage")
        val TENSES = stringSetPreferencesKey("practice_tenses")
        val TIER = stringPreferencesKey("practice_tier")
        val VOSOTROS = booleanPreferencesKey("practice_vosotros")
        val SOURCE = stringPreferencesKey("practice_source")
        val ANSWER_MODE = stringPreferencesKey("practice_answer_mode")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val REMINDERS = booleanPreferencesKey("reminders_on")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

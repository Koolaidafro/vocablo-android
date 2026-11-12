package app.vocablo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.vocablo.design.ThemeId
import app.vocablo.design.ThemeMode
import app.vocablo.design.Themes
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
        }
    }

    private fun read(p: Preferences) = AppSettings(
        themeId = p[THEME].toEnum(Themes.default),
        mode = p[MODE].toEnum(ThemeMode.SYSTEM),
        region = p[REGION].toEnum(Region.LATAM),
        newPerDay = p[NEW_PER_DAY] ?: 20,
        onlineLookup = p[ONLINE_LOOKUP] ?: true,
    )

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val MODE = stringPreferencesKey("mode")
        val REGION = stringPreferencesKey("region")
        val NEW_PER_DAY = intPreferencesKey("new_per_day")
        val ONLINE_LOOKUP = booleanPreferencesKey("online_lookup")
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

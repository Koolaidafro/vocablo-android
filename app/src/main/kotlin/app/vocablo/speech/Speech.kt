package app.vocablo.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.vocablo.container
import app.vocablo.data.Region
import java.util.Locale

/** The phone's Spanish voice to use for each region, best match first. */
object Voices {
    fun localesFor(region: Region): List<Locale> = when (region) {
        Region.MEXICO -> listOf(Locale("es", "MX"), Locale("es", "US"))
        Region.LATAM, Region.CARIBBEAN -> listOf(Locale("es", "US"), Locale("es", "MX"))
        Region.ANDEAN -> listOf(Locale("es", "CO"), Locale("es", "US"), Locale("es", "MX"))
        Region.RIOPLATENSE -> listOf(Locale("es", "AR"), Locale("es", "US"))
        Region.SPAIN -> listOf(Locale("es", "ES"))
    } + Locale("es")
}

/**
 * Reads Spanish aloud with the phone's own text-to-speech engine, offline when the voice is
 * installed. Starts the engine on first use and quietly does nothing if no Spanish voice exists.
 */
class Speech(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null
    var region: Region = Region.LATAM
        set(value) {
            field = value
            if (ready) applyLocale()
        }

    fun speak(text: String) {
        if (text.isBlank()) return
        val engine = tts
        if (engine == null) {
            pending = text
            tts = TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    applyLocale()
                    pending?.let { say(it) }
                }
                pending = null
            }
            return
        }
        if (ready) say(text) else pending = text
    }

    private fun say(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vocablo-${text.hashCode()}")
    }

    private fun applyLocale() {
        val engine = tts ?: return
        Voices.localesFor(region).firstOrNull { engine.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
            ?.let { engine.language = it }
        engine.setSpeechRate(0.9f) // a touch slower than normal, for learners
    }
}

/** A small "listen" button for any Spanish text. */
@Composable
fun SpeakButton(text: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    IconButton(
        onClick = { context.container.speech.speak(text) },
        modifier = modifier.size(48.dp).semantics { contentDescription = "Listen to $text" },
    ) { Text("🔊") }
}

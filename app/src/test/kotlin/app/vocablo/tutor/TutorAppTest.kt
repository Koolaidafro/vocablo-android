package app.vocablo.tutor

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.vocablo.ui.tutor.splitCorrection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** Stands in for the Android Keystore, which Robolectric doesn't have. Reverses and tags the text. */
private class FakeCipher : SecretCipher {
    override fun encrypt(plain: String) = "enc:" + plain.reversed()
    override fun decrypt(sealed: String) = if (sealed.startsWith("enc:")) sealed.removePrefix("enc:").reversed() else null
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TutorAppTest {

    private fun prefs() = ApplicationProvider.getApplicationContext<Application>()
        .getSharedPreferences(KeyVault.FILE, Context.MODE_PRIVATE).also { it.edit().clear().commit() }

    @Test
    fun keysAreStoredOnlyAsCiphertext() {
        val prefs = prefs()
        val vault = KeyVault(prefs, FakeCipher())
        vault.save(Provider.CLAUDE, "  sk-ant-secret  ")
        assertEquals("sk-ant-secret", vault.get(Provider.CLAUDE))
        assertFalse(prefs.all.values.any { it.toString().contains("sk-ant-secret") })
        assertFalse(vault.has(Provider.OPENAI))
        vault.forget(Provider.CLAUDE)
        assertNull(vault.get(Provider.CLAUDE))
    }

    @Test
    fun anUnreadableKeyCountsAsMissing() {
        val prefs = prefs()
        prefs.edit().putString(Provider.GROK.name, "garbage").commit()
        assertFalse(KeyVault(prefs, FakeCipher()).has(Provider.GROK))
    }

    @Test
    fun theClientTurnsNetworkTroubleIntoAMessage() = runBlocking<Unit> {
        val history = listOf(ChatMessage(Role.USER, "hola"))
        val ok = TutorClient { 200 to """{"choices":[{"message":{"content":"¡Hola!"}}]}""" }
        assertEquals(Reply.Text("¡Hola!"), ok.reply(TutorConfig(Provider.OPENAI, ""), "k", "s", history))
        val offline = TutorClient { throw IOException("no network") }
        assertTrue(offline.reply(TutorConfig(Provider.OPENAI, ""), "k", "s", history) is Reply.Failure)
        val noAddress = TutorClient { error("must not be called") }
        assertTrue(noAddress.reply(TutorConfig(Provider.CUSTOM, "m", ""), "k", "s", history) is Reply.Failure)
    }

    @Test
    fun correctionsAreShownSeparately() {
        val (body, fix) = splitCorrection("¡Qué bien! ¿Y tú?\n✏️ Estoy bien (estar for how you feel)")
        assertEquals("¡Qué bien! ¿Y tú?", body)
        assertEquals("✏️ Estoy bien (estar for how you feel)", fix)
        assertNull(splitCorrection("Hola").second)
    }
}

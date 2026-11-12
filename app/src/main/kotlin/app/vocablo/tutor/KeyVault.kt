package app.vocablo.tutor

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts short secrets. The real one uses a key that never leaves the Android Keystore. */
interface SecretCipher {
    fun encrypt(plain: String): String
    /** Null when the secret can't be decrypted (for example after the phone's keystore was reset). */
    fun decrypt(sealed: String): String?
}

/** AES-GCM with a non-exportable Android Keystore key. */
class KeystoreCipher(private val alias: String = "vocablo_tutor_keys") : SecretCipher {
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val data = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + data, Base64.NO_WRAP)
    }

    override fun decrypt(sealed: String): String? = runCatching {
        val bytes = Base64.decode(sealed, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_SIZE))
        String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE), Charsets.UTF_8)
    }.getOrNull()

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}

/**
 * API keys, one per provider, stored only as ciphertext in a preferences file that backup
 * rules leave out (see res/xml/backup_rules.xml and data_extraction_rules.xml).
 */
class KeyVault(private val prefs: SharedPreferences, private val cipher: SecretCipher) {

    fun save(provider: Provider, key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return forget(provider)
        prefs.edit().putString(provider.name, cipher.encrypt(trimmed)).apply()
    }

    fun get(provider: Provider): String? = prefs.getString(provider.name, null)?.let(cipher::decrypt)

    fun has(provider: Provider): Boolean = get(provider) != null

    fun forget(provider: Provider) {
        prefs.edit().remove(provider.name).apply()
    }

    companion object {
        const val FILE = "tutor_keys"

        fun create(context: Context): KeyVault =
            KeyVault(context.getSharedPreferences(FILE, Context.MODE_PRIVATE), KeystoreCipher())
    }
}

package app.vocablo.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import app.vocablo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A newer build published on GitHub. */
data class Release(val versionCode: Int, val versionName: String, val apkUrl: String, val notes: String)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class Failed(val message: String, val release: Release? = null) : UpdateState
}

/** Reads GitHub's "latest release" reply. Tags look like v1.0.123, where 123 is the version code. */
object ReleaseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): Release? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val tag = root["tag_name"]?.jsonPrimitive?.contentOrNull ?: return null
        val code = tag.substringAfterLast('.').toIntOrNull() ?: return null
        val apk = (root["assets"] as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }
            .firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk") == true }
            ?.get("browser_download_url")?.jsonPrimitive?.contentOrNull ?: return null
        Release(code, tag.removePrefix("v"), apk, root["body"]?.jsonPrimitive?.contentOrNull.orEmpty().trim())
    }.getOrNull()

    fun isNewer(release: Release, installedCode: Int): Boolean = release.versionCode > installedCode
}

/**
 * Self-updates for the build installed from GitHub releases. The Play Store build never runs
 * this ([BuildConfig.SELF_UPDATE] is false there), because Play handles its own updates.
 */
class Updates(private val context: Context) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val enabled: Boolean get() = BuildConfig.SELF_UPDATE

    private var lastCheckMillis = 0L

    /** Checks when the app comes to the front, at most once an hour (GitHub limits anonymous calls). */
    suspend fun checkIfStale() {
        if (System.currentTimeMillis() - lastCheckMillis >= 60 * 60_000L) check()
    }

    suspend fun check() {
        if (!enabled || _state.value is UpdateState.Downloading) return
        lastCheckMillis = System.currentTimeMillis()
        _state.value = UpdateState.Checking
        _state.value = try {
            val body = withContext(Dispatchers.IO) { get(LATEST) }
            val release = body?.let(ReleaseParser::parse)
            when {
                release == null -> UpdateState.UpToDate // nothing published yet
                ReleaseParser.isNewer(release, BuildConfig.VERSION_CODE) -> UpdateState.Available(release)
                else -> UpdateState.UpToDate
            }
        } catch (e: IOException) {
            UpdateState.Failed("Couldn't check for updates. Check your connection.")
        }
    }

    /** Downloads the new APK and hands it to Android's installer, which asks the learner to confirm. */
    suspend fun install(release: Release) {
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            // Android asks once for permission to install updates from Vocablo.
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            _state.value = UpdateState.Available(release)
            return
        }
        _state.value = UpdateState.Downloading(release, 0f)
        val file = try {
            withContext(Dispatchers.IO) { download(release) }
        } catch (e: IOException) {
            _state.value = UpdateState.Failed("The download didn't finish. Try again.", release)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        _state.value = UpdateState.Available(release)
    }

    private fun download(release: Release): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "vocablo-${release.versionCode}.apk")
        val connection = open(release.apkUrl)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong.takeIf { it > 0 }
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        if (total != null) _state.value = UpdateState.Downloading(release, done.toFloat() / total)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        return target
    }

    private fun get(url: String): String? {
        val connection = open(url)
        return try {
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> connection.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw IOException("HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 60_000
        instanceFollowRedirects = true // release downloads redirect to GitHub's file host
        setRequestProperty("User-Agent", "Vocablo Android app")
    }

    private companion object {
        const val LATEST = "https://api.github.com/repos/Koolaidafro/vocablo-android/releases/latest"
    }
}

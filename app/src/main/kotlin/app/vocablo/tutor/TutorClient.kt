package app.vocablo.tutor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Sends one tutor request straight from the phone to the learner's chosen service. */
class TutorClient(private val post: suspend (HttpCall) -> Pair<Int, String> = ::httpPost) {

    suspend fun reply(config: TutorConfig, apiKey: String, system: String, messages: List<ChatMessage>): Reply {
        if (config.provider == Provider.CUSTOM && config.baseUrl.isBlank()) return Reply.Failure("Add the service's address in the tutor settings.")
        val call = TutorApi.request(config, apiKey, system, messages)
        return try {
            val (status, body) = post(call)
            TutorApi.parse(config.provider, status, body)
        } catch (e: IOException) {
            Reply.Failure("Couldn't reach the service. Check your connection and try again.")
        }
    }
}

private suspend fun httpPost(call: HttpCall): Pair<Int, String> = withContext(Dispatchers.IO) {
    val connection = URL(call.url).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 120_000
        connection.doOutput = true
        call.headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        connection.outputStream.use { it.write(call.body.toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        status to (stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "")
    } finally {
        connection.disconnect()
    }
}

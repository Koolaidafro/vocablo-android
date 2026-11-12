package app.vocablo.tutor

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * The AI services a learner can bring a key for. Models are only defaults: the learner can
 * type any model their key allows.
 */
enum class Provider(val label: String, val defaultModel: String, val defaultBaseUrl: String, val keyHint: String) {
    CLAUDE("Claude (Anthropic)", "claude-opus-5-5", "https://api.anthropic.com/v1", "sk-ant-…"),
    OPENAI("ChatGPT (OpenAI)", "gpt-5-mini", "https://api.openai.com/v1", "sk-…"),
    GROK("Grok (xAI)", "grok-4", "https://api.x.ai/v1", "xai-…"),
    CUSTOM("Other (OpenAI-compatible)", "", "", "key"),
}

enum class Role { USER, ASSISTANT }

data class ChatMessage(val role: Role, val text: String)

data class TutorConfig(val provider: Provider, val model: String, val baseUrl: String = provider.defaultBaseUrl) {
    val effectiveModel: String get() = model.ifBlank { provider.defaultModel }
    val effectiveBaseUrl: String get() = baseUrl.ifBlank { provider.defaultBaseUrl }.trimEnd('/')
}

/** A ready-to-send HTTP request. The app's network code just posts [body] to [url] with [headers]. */
data class HttpCall(val url: String, val headers: Map<String, String>, val body: String)

sealed interface Reply {
    data class Text(val text: String) : Reply
    data class Failure(val message: String) : Reply
}

/** Builds requests and reads replies for each provider. No network code here, so it is fully testable. */
object TutorApi {
    const val MAX_TOKENS = 4_000
    private val json = Json { ignoreUnknownKeys = true }

    /** Claude models that accept the server-side refusal fallback. */
    private val FALLBACK_MODELS = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-opus-5", "claude-fable-5-1", "claude-fable-5-2")

    fun request(config: TutorConfig, apiKey: String, system: String, messages: List<ChatMessage>): HttpCall = when (config.provider) {
        Provider.CLAUDE -> claude(config, apiKey, system, messages)
        else -> openAiStyle(config, apiKey, system, messages)
    }

    private fun claude(config: TutorConfig, apiKey: String, system: String, messages: List<ChatMessage>): HttpCall {
        val model = config.effectiveModel
        val fallback = model in FALLBACK_MODELS
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", MAX_TOKENS)
            put("system", system)
            // A tutor chat is light work: low effort keeps replies quick and cheap.
            put("output_config", buildJsonObject { put("effort", "low") })
            if (fallback) put("fallbacks", "default")
            put("messages", messagesJson(messages))
        }
        val headers = buildMap {
            put("x-api-key", apiKey)
            put("anthropic-version", "2023-06-01")
            put("content-type", "application/json")
            if (fallback) put("anthropic-beta", "server-side-fallback-2026-07-01")
        }
        return HttpCall("${config.effectiveBaseUrl}/messages", headers, body.toString())
    }

    private fun openAiStyle(config: TutorConfig, apiKey: String, system: String, messages: List<ChatMessage>): HttpCall {
        val body = buildJsonObject {
            put("model", config.effectiveModel)
            put(
                "messages",
                buildJsonArray {
                    add(buildJsonObject { put("role", "system"); put("content", system) })
                    messagesJson(messages).forEach { add(it) }
                },
            )
        }
        val headers = mapOf("Authorization" to "Bearer $apiKey", "Content-Type" to "application/json")
        return HttpCall("${config.effectiveBaseUrl}/chat/completions", headers, body.toString())
    }

    private fun messagesJson(messages: List<ChatMessage>): JsonArray = buildJsonArray {
        messages.forEach { m ->
            add(buildJsonObject { put("role", if (m.role == Role.USER) "user" else "assistant"); put("content", m.text) })
        }
    }

    /** Turns an HTTP status and body into the tutor's reply or a message a learner can act on. */
    fun parse(provider: Provider, status: Int, body: String): Reply {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        if (status !in 200..299) return Reply.Failure(errorMessage(status, root))
        root ?: return Reply.Failure("The reply couldn't be read. Try again.")
        return when (provider) {
            Provider.CLAUDE -> parseClaude(root)
            else -> parseOpenAi(root)
        }
    }

    private fun parseClaude(root: JsonObject): Reply {
        if (root["stop_reason"]?.jsonPrimitive?.contentOrNull == "refusal") {
            return Reply.Failure("Claude declined that one. Try saying it another way.")
        }
        val text = (root["content"] as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }
            .filter { it["type"]?.jsonPrimitive?.contentOrNull == "text" }
            .joinToString("") { it["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
            .trim()
        return if (text.isEmpty()) Reply.Failure("The tutor didn't say anything. Try again.") else Reply.Text(text)
    }

    private fun parseOpenAi(root: JsonObject): Reply {
        val text = runCatching {
            root["choices"]!!.jsonArray[0].jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.contentOrNull.orEmpty()
        }.getOrDefault("").trim()
        return if (text.isEmpty()) Reply.Failure("The tutor didn't say anything. Try again.") else Reply.Text(text)
    }

    private fun errorMessage(status: Int, root: JsonObject?): String {
        val detail = (root?.get("error") as? JsonObject)?.get("message")?.let { (it as? JsonPrimitive)?.contentOrNull }
            ?: (root?.get("error") as? JsonPrimitive)?.contentOrNull
        val base = when (status) {
            401, 403 -> "The key was refused. Check it in the tutor settings."
            404 -> "That model wasn't found. Check the model name in the tutor settings."
            429 -> "The service is busy or your key is out of credit. Wait a moment and try again."
            in 500..599 -> "The service is having trouble right now. Try again in a bit."
            else -> "The request didn't work (error $status)."
        }
        return if (detail.isNullOrBlank()) base else "$base ($detail)"
    }
}

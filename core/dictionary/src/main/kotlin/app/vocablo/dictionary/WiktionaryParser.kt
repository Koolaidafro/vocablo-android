package app.vocablo.dictionary

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads the response of Wiktionary's free REST endpoint
 * `https://en.wiktionary.org/api/rest_v1/page/definition/{word}`.
 * Only the Spanish section is used. Wiktionary text is CC BY-SA, credited in the app.
 */
object WiktionaryParser {

    private val json = Json { ignoreUnknownKeys = true }
    private val TAGS = Regex("<[^>]+>")
    private val SPACES = Regex("\\s+")

    fun parse(word: String, body: String): List<DictionaryEntry> {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return emptyList()
        val spanish = root["es"] as? JsonArray ?: return emptyList()
        return spanish.mapNotNull { usage ->
            val obj = usage as? JsonObject ?: return@mapNotNull null
            val pos = obj["partOfSpeech"]?.jsonPrimitive?.content.orEmpty()
            val defs = obj["definitions"]?.jsonArray.orEmpty().mapNotNull { it as? JsonObject }
            val meanings = defs.mapNotNull { d -> d["definition"]?.jsonPrimitive?.content?.let(::clean) }
                .filter { it.isNotEmpty() }
                .take(3)
            if (meanings.isEmpty()) return@mapNotNull null
            val example = defs.firstNotNullOfOrNull { d ->
                (d["examples"] as? JsonArray)?.firstOrNull()?.jsonPrimitive?.content?.let(::clean)
            }.orEmpty()
            DictionaryEntry(
                spanish = word,
                partOfSpeech = PartOfSpeech.parse(pos),
                gender = "",
                english = meanings.joinToString("; "),
                example = example,
                source = Source.ONLINE,
            )
        }
    }

    private fun clean(html: String): String =
        html.replace(TAGS, "").replace("&nbsp;", " ").replace("&amp;", "&").replace(SPACES, " ").trim()
}

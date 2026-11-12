package app.vocablo.data

import app.vocablo.cards.CardText
import app.vocablo.dictionary.DictionaryEntry
import app.vocablo.dictionary.WiktionaryParser
import app.vocablo.dictionary.WordLookup
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Free online fallback: Wiktionary's public REST API. No key and no cost. Blocking, so
 * callers run it on Dispatchers.IO. Any network problem simply returns nothing.
 */
class WiktionaryLookup : WordLookup {
    override fun lookup(word: String): List<DictionaryEntry> {
        val title = CardText.key(word)
        if (title.isEmpty()) return emptyList()
        val encoded = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
        val connection = URL("https://en.wiktionary.org/api/rest_v1/page/definition/$encoded").openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("User-Agent", "Vocablo Android app (open source flashcards)")
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return emptyList()
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            WiktionaryParser.parse(title, body)
        } finally {
            connection.disconnect()
        }
    }
}

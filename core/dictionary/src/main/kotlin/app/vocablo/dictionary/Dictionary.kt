package app.vocablo.dictionary

enum class PartOfSpeech(val label: String) {
    NOUN("noun"), VERB("verb"), ADJECTIVE("adjective"), ADVERB("adverb"),
    CONJUNCTION("conjunction"), PHRASE("phrase"), OTHER("word");

    companion object {
        fun parse(s: String): PartOfSpeech = when (s.trim().lowercase()) {
            "noun", "n" -> NOUN
            "verb", "v" -> VERB
            "adj", "adjective" -> ADJECTIVE
            "adv", "adverb" -> ADVERB
            "conj", "conjunction" -> CONJUNCTION
            "phrase", "phr", "idiom" -> PHRASE
            else -> OTHER
        }
    }
}

enum class Source { OFFLINE, ONLINE, AI }

data class DictionaryEntry(
    val spanish: String,
    val partOfSpeech: PartOfSpeech,
    /** "m" or "f" for nouns, empty otherwise. */
    val gender: String,
    val english: String,
    val example: String = "",
    val exampleEnglish: String = "",
    val regionNote: String = "",
    val source: Source = Source.OFFLINE,
)

/** Something that can look words up: the built-in pack, an online service, or AI. */
fun interface WordLookup {
    /** Blocking call; the app runs it off the main thread. Returns an empty list when nothing is found. */
    fun lookup(word: String): List<DictionaryEntry>
}

/**
 * The offline dictionary. Search is by prefix and ignores accents and articles, so typing
 * "aguac" or "el agu" finds "el aguacate". Results keep the pack's order, which is by
 * how common the word is.
 */
class OfflineDictionary(entries: List<DictionaryEntry>) : WordLookup {

    private class Indexed(val key: String, val rank: Int, val entry: DictionaryEntry)

    private val sorted: List<Indexed> = entries
        .mapIndexed { rank, e -> Indexed(searchKey(e.spanish), rank, e) }
        .sortedBy { it.key }

    val size: Int get() = sorted.size

    fun suggest(typed: String, limit: Int = 5): List<DictionaryEntry> {
        val prefix = searchKey(typed)
        if (prefix.isEmpty()) return emptyList()
        var lo = 0
        var hi = sorted.size
        while (lo < hi) { // first key >= prefix
            val mid = (lo + hi) ushr 1
            if (sorted[mid].key < prefix) lo = mid + 1 else hi = mid
        }
        val hits = mutableListOf<Indexed>()
        var i = lo
        while (i < sorted.size && sorted[i].key.startsWith(prefix)) hits += sorted[i++]
        return hits.sortedWith(compareBy<Indexed>({ it.key != prefix }, { it.rank })).take(limit).map { it.entry }
    }

    override fun lookup(word: String): List<DictionaryEntry> {
        val key = searchKey(word)
        return sorted.filter { it.key == key }.sortedBy { it.rank }.map { it.entry }
    }

    companion object {
        private val ARTICLES = setOf("el", "la", "los", "las", "un", "una")
        private val ACCENTS = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u')

        internal fun searchKey(s: String): String {
            val words = s.lowercase().replace(Regex("[¿?¡!.,]"), " ").trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val trimmed = if (words.size > 1 && words.first() in ARTICLES) words.drop(1) else words
            return trimmed.joinToString(" ").map { ACCENTS[it] ?: it }.joinToString("")
        }

        /** Reads the tab-separated pack format; lines starting with # are comments. */
        fun parse(tsv: String): OfflineDictionary = OfflineDictionary(
            tsv.lineSequence()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { it.split('\t') }
                .filter { it.size >= 4 }
                .map { c ->
                    DictionaryEntry(
                        spanish = c[0].trim(),
                        partOfSpeech = PartOfSpeech.parse(c[1]),
                        gender = c[2].trim(),
                        english = c[3].trim(),
                        example = c.getOrElse(4) { "" }.trim(),
                        exampleEnglish = c.getOrElse(5) { "" }.trim(),
                        regionNote = c.getOrElse(6) { "" }.trim(),
                    )
                }
                .toList(),
        )

        /** The starter pack bundled with the app. */
        fun starter(): OfflineDictionary {
            val text = OfflineDictionary::class.java.getResourceAsStream("/starter_dictionary.tsv")
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("starter_dictionary.tsv is missing from the build")
            return parse(text)
        }
    }
}

/**
 * Tries each source in order and returns the first non-empty answer. Online and AI
 * sources that fail (no connection, no key) are skipped quietly.
 */
class LayeredLookup(private val sources: List<WordLookup>) : WordLookup {
    override fun lookup(word: String): List<DictionaryEntry> {
        for (source in sources) {
            val found = runCatching { source.lookup(word) }.getOrDefault(emptyList())
            if (found.isNotEmpty()) return found
        }
        return emptyList()
    }
}

package app.vocablo.cards

import kotlinx.serialization.Serializable

@Serializable
enum class CardType { WORD, VERB, PHRASE }

@Serializable
data class Deck(
    val id: Long = 0,
    val name: String,
    /** Index into the theme's accent colors, so decks look right in every theme. */
    val colorIndex: Int = 0,
    val createdMillis: Long = 0,
)

@Serializable
data class Card(
    val id: Long = 0,
    val deckId: Long,
    val spanish: String,
    val english: String,
    val type: CardType = CardType.WORD,
    val example: String = "",
    val exampleEnglish: String = "",
    val regionNote: String = "",
    val createdMillis: Long = 0,
)

/** Text rules shared by duplicate detection, search and import. */
object CardText {
    private val ARTICLES = setOf("el", "la", "los", "las", "un", "una", "unos", "unas", "lo")
    private val PUNCTUATION = Regex("[¿?¡!.,;:\"“”«»()]")
    private val SPACES = Regex("\\s+")
    private val ACCENTS = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u')

    /**
     * The identity of a Spanish entry: lowercase, no punctuation or extra spaces, and no
     * leading article. Accents are kept, so "si" (if) and "sí" (yes) stay different.
     */
    fun key(spanish: String): String {
        val words = spanish.lowercase().replace(PUNCTUATION, " ").trim().split(SPACES).filter { it.isNotEmpty() }
        val trimmed = if (words.size > 1 && words.first() in ARTICLES) words.drop(1) else words
        return trimmed.joinToString(" ")
    }

    /** Same as [key] but also ignores accents, for "did you mean" style near matches. */
    fun looseKey(spanish: String): String = key(spanish).map { ACCENTS[it] ?: it }.joinToString("")

    /** Splits "avocado, avocado tree; palta" into its separate meanings. */
    fun meanings(english: String): List<String> =
        english.split(',', ';', '/').map { it.trim() }.filter { it.isNotEmpty() }

    /** Adds meanings the card doesn't have yet, keeping the original order. */
    fun mergeMeanings(existing: String, incoming: String): String {
        val have = meanings(existing)
        val haveLower = have.map { it.lowercase() }.toSet()
        val added = meanings(incoming).filter { it.lowercase() !in haveLower }
        return (have + added).joinToString(", ")
    }
}

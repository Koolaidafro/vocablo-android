package app.vocablo.cards

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Scheduling data saved with each card in a backup. Mirrors the srs module's state. */
@Serializable
data class SavedReview(
    val cardId: Long,
    val stability: Double,
    val difficulty: Double,
    val reps: Int,
    val lapses: Int,
    val lastReviewMillis: Long?,
    val dueMillis: Long,
)

/**
 * A full backup: every deck, card and review state. API keys and settings are never
 * included, so a backup is safe to store anywhere.
 */
@Serializable
data class Backup(
    val format: Int = FORMAT,
    val exportedMillis: Long,
    val decks: List<Deck>,
    val cards: List<Card>,
    val reviews: List<SavedReview>,
) {
    companion object {
        const val FORMAT = 1
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

        fun encode(backup: Backup): String = json.encodeToString(serializer(), backup)

        fun decode(text: String): Backup {
            val backup = json.decodeFromString(serializer(), text)
            require(backup.format <= FORMAT) { "This backup was made by a newer version of Vocablo. Update the app, then try again." }
            return backup
        }
    }
}

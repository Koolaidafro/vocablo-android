package app.vocablo.cards

sealed interface DuplicateResult {
    /** Nothing like it exists. */
    data object None : DuplicateResult

    /** The same word already exists. [newMeanings] lists English meanings the old card lacks. */
    data class Exact(val existing: Card, val newMeanings: List<String>) : DuplicateResult

    /** Only an accent differs (si vs sí). Worth a gentle hint, never a block. */
    data class Similar(val candidates: List<Card>) : DuplicateResult
}

/** Checks a new card against every card the learner already has, across all decks. */
class DuplicateChecker(existing: Iterable<Card>) {
    private val byKey: Map<String, List<Card>> = existing.groupBy { CardText.key(it.spanish) }
    private val byLooseKey: Map<String, List<Card>> = existing.groupBy { CardText.looseKey(it.spanish) }

    fun check(spanish: String, english: String = "", ignoreCardId: Long? = null): DuplicateResult {
        val key = CardText.key(spanish)
        if (key.isEmpty()) return DuplicateResult.None
        byKey[key]?.firstOrNull { it.id != ignoreCardId }?.let { card ->
            val have = CardText.meanings(card.english).map { it.lowercase() }.toSet()
            val new = CardText.meanings(english).filter { it.lowercase() !in have }
            return DuplicateResult.Exact(card, new)
        }
        val similar = byLooseKey[CardText.looseKey(spanish)].orEmpty().filter { it.id != ignoreCardId }
        return if (similar.isEmpty()) DuplicateResult.None else DuplicateResult.Similar(similar)
    }
}

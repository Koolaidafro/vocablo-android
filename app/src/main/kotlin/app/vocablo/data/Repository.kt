package app.vocablo.data

import app.vocablo.cards.Backup
import app.vocablo.cards.Card
import app.vocablo.cards.CardText
import app.vocablo.cards.CardType
import app.vocablo.cards.CsvCards
import app.vocablo.cards.Deck
import app.vocablo.cards.DuplicateChecker
import app.vocablo.cards.DuplicateResult
import app.vocablo.cards.SavedReview
import app.vocablo.srs.QueueItem
import app.vocablo.srs.Rating
import app.vocablo.srs.ReviewState
import app.vocablo.srs.StudyQueue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

/** A study session ready to go: the queue plus the cards it refers to. */
class StudySession(val queue: StudyQueue, val cards: Map<Long, Card>)

data class ImportSummary(val added: Int, val duplicates: Int, val badLines: List<Int>)

/** Everything the screens need from storage. All functions are safe to call from the main thread. */
@OptIn(ExperimentalCoroutinesApi::class)
class VocabloRepository(
    private val dao: VocabloDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {

    fun today(): Long = dayOf(clock())

    fun dayOf(millis: Long): Long = Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate().toEpochDay()

    /** Emits the current time now and every minute, so "due" counts stay fresh. */
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(60_000)
        }
    }

    // Decks

    fun deckSummaries(): Flow<List<DeckSummary>> = ticker.flatMapLatest { dao.observeDeckSummaries(it) }

    fun deck(id: Long): Flow<Deck?> = dao.observeDeck(id).map { it?.toDeck() }

    suspend fun createDeck(name: String): Long {
        val count = dao.decks().size
        return dao.insertDeck(DeckEntity(name = name.trim(), colorIndex = count % DECK_COLORS, createdMillis = clock()))
    }

    suspend fun renameDeck(id: Long, name: String) = dao.renameDeck(id, name.trim())

    suspend fun deleteDeck(id: Long) = dao.deleteDeck(id)

    suspend fun decks(): List<Deck> = dao.decks().map { it.toDeck() }

    // Cards

    fun cards(deckId: Long): Flow<List<Card>> = dao.observeCards(deckId).map { list -> list.map { it.toCard() } }

    suspend fun card(id: Long): Card? = dao.card(id)?.toCard()

    suspend fun allCards(): List<Card> = dao.allCards().map { it.toCard() }

    /** Compares a new or edited card with every card in every deck. */
    suspend fun checkDuplicate(spanish: String, english: String, ignoreCardId: Long? = null): DuplicateResult {
        val sameLetters = dao.cardsWithLooseKey(CardText.looseKey(spanish)).map { it.toCard() }
        return DuplicateChecker(sameLetters).check(spanish, english, ignoreCardId)
    }

    suspend fun addCard(card: Card): Long = dao.insertCard(card.copy(createdMillis = clock()).toEntity())

    suspend fun updateCard(card: Card) = dao.updateCard(card.toEntity())

    /** Adds the meanings (and an example, if the old card had none) to an existing card. */
    suspend fun mergeInto(existingId: Long, english: String, example: String = "", exampleEnglish: String = "") {
        val old = dao.card(existingId)?.toCard() ?: return
        dao.updateCard(
            old.copy(
                english = CardText.mergeMeanings(old.english, english),
                example = old.example.ifEmpty { example },
                exampleEnglish = old.exampleEnglish.ifEmpty { exampleEnglish },
            ).toEntity(),
        )
    }

    suspend fun deleteCard(id: Long) = dao.deleteCard(id)

    /** Adds a verb card to the "Verbos" deck. Returns false if a card for it already exists. */
    suspend fun addVerbCard(infinitive: String, english: String): Boolean {
        if (checkDuplicate(infinitive, english) is DuplicateResult.Exact) return false
        val deckId = dao.decks().firstOrNull { it.name == VERB_DECK }?.id ?: createDeck(VERB_DECK)
        addCard(Card(deckId = deckId, spanish = infinitive, english = english, type = CardType.VERB))
        return true
    }

    // Study

    /** Days with any study: card reviews or verb drills. */
    fun studyDays(): Flow<Set<Long>> =
        combine(dao.observeStudyDays(), dao.observeVerbStudyDays()) { cards, verbs -> (cards + verbs).toSet() }

    fun answersToday(): Flow<Int> = ticker.map { dayOf(it) }.distinctUntilChanged().flatMapLatest { dao.observeAnswersOn(it) }

    /**
     * Builds a session from one deck, or from every deck when [deckId] is null. New cards are
     * capped at [newPerDay] minus the new cards already started today.
     */
    suspend fun startStudy(deckId: Long?, newPerDay: Int): StudySession {
        val rows = if (deckId == null) dao.allStudyCards() else dao.studyCards(deckId)
        val newLeft = (newPerDay - dao.newCardsStudiedOn(today())).coerceAtLeast(0)
        val items = rows.sortedBy { it.card.createdMillis }.map { QueueItem(it.card.id, it.review?.toState() ?: ReviewState()) }
        return StudySession(StudyQueue(items, clock(), newLimit = newLeft), rows.associate { it.card.id to it.card.toCard() })
    }

    suspend fun saveAnswer(updated: QueueItem, rating: Rating, wasNew: Boolean) {
        val now = clock()
        val s = updated.state
        dao.saveAnswer(
            ReviewEntity(updated.cardId, s.stability, s.difficulty, s.reps, s.lapses, s.lastReviewMillis, s.dueMillis),
            ReviewLogEntity(cardId = updated.cardId, rating = rating.value, wasNew = wasNew, reviewedMillis = now, day = dayOf(now)),
        )
    }

    // Import, export and backup

    /** Adds cards from CSV or Anki text. Rows naming a deck go there; the rest go to [fallbackDeckName]. */
    suspend fun importCards(text: String, fallbackDeckName: String = "Imported"): ImportSummary {
        val parsed = CsvCards.parse(text)
        val deckIds = dao.decks().associate { it.name.lowercase() to it.id }.toMutableMap()
        val known = dao.allCards().map { it.searchKey }.toMutableSet()
        val toAdd = mutableListOf<CardEntity>()
        var duplicates = 0
        val now = clock()
        for (row in parsed.rows) {
            val key = CardText.key(row.spanish)
            if (!known.add(key)) {
                duplicates++
                continue
            }
            val deckName = row.deck.ifBlank { fallbackDeckName }
            val deckId = deckIds.getOrPut(deckName.lowercase()) { createDeck(deckName) }
            toAdd += Card(deckId = deckId, spanish = row.spanish, english = row.english, example = row.example, createdMillis = now)
                .toEntity()
        }
        dao.insertCards(toAdd)
        return ImportSummary(toAdd.size, duplicates, parsed.skippedLines)
    }

    suspend fun exportCsv(): String {
        val names = dao.decks().associate { it.id to it.name }
        return CsvCards.write(dao.allCards().map { it.toCard() }, names)
    }

    suspend fun exportBackup(): String = Backup.encode(
        Backup(
            exportedMillis = clock(),
            decks = dao.decks().map { it.toDeck() },
            cards = dao.allCards().map { it.toCard() },
            reviews = dao.allReviews().map {
                SavedReview(it.cardId, it.stability, it.difficulty, it.reps, it.lapses, it.lastReviewMillis, it.dueMillis)
            },
        ),
    )

    /** Replaces everything with the backup. Throws IllegalArgumentException with a readable message if it can't. */
    suspend fun restoreBackup(text: String): Int {
        val backup = try {
            Backup.decode(text)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(e.message?.takeIf { "newer version" in it } ?: "That file isn't a Vocablo backup.", e)
        }
        dao.replaceAll(
            decks = backup.decks.map { DeckEntity(it.id, it.name, it.colorIndex, it.createdMillis) },
            cards = backup.cards.map { it.toEntity() },
            reviews = backup.reviews.map {
                ReviewEntity(it.cardId, it.stability, it.difficulty, it.reps, it.lapses, it.lastReviewMillis, it.dueMillis)
            },
        )
        return backup.cards.size
    }

    /** First launch: a small deck so there is something to swipe right away. */
    suspend fun seedIfEmpty() {
        if (dao.decks().isNotEmpty()) return
        val deckId = createDeck("Primeros pasos")
        val now = clock()
        dao.insertCards(
            STARTER_CARDS.mapIndexed { i, (es, en, type) ->
                Card(deckId = deckId, spanish = es, english = en, type = type, createdMillis = now + i).toEntity()
            },
        )
    }

    companion object {
        const val DECK_COLORS = 5
        const val VERB_DECK = "Verbos"

        private val STARTER_CARDS = listOf(
            Triple("hola", "hello", CardType.WORD),
            Triple("gracias", "thank you", CardType.WORD),
            Triple("por favor", "please", CardType.PHRASE),
            Triple("el agua", "water", CardType.WORD),
            Triple("la casa", "house, home", CardType.WORD),
            Triple("hablar", "to speak", CardType.VERB),
            Triple("comer", "to eat", CardType.VERB),
            Triple("vivir", "to live", CardType.VERB),
            Triple("tener", "to have", CardType.VERB),
            Triple("¿Qué tal?", "how's it going?", CardType.PHRASE),
            Triple("el aguacate", "avocado", CardType.WORD),
            Triple("mañana", "tomorrow, morning", CardType.WORD),
        )
    }
}

internal fun DeckEntity.toDeck() = Deck(id, name, colorIndex, createdMillis)

internal fun CardEntity.toCard() = Card(
    id = id,
    deckId = deckId,
    spanish = spanish,
    english = english,
    type = runCatching { CardType.valueOf(type) }.getOrDefault(CardType.WORD),
    example = example,
    exampleEnglish = exampleEnglish,
    regionNote = regionNote,
    createdMillis = createdMillis,
)

internal fun Card.toEntity() = CardEntity(
    id = id,
    deckId = deckId,
    spanish = spanish.trim(),
    english = english.trim(),
    type = type.name,
    example = example.trim(),
    exampleEnglish = exampleEnglish.trim(),
    regionNote = regionNote.trim(),
    createdMillis = createdMillis,
    searchKey = CardText.key(spanish),
    looseSearchKey = CardText.looseKey(spanish),
)

private fun ReviewEntity.toState() = ReviewState(stability, difficulty, reps, lapses, lastReviewMillis, dueMillis)

package app.vocablo.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int = 0,
    val createdMillis: Long = 0,
)

/**
 * A card. [searchKey] and [looseSearchKey] are derived from [spanish] by the repository
 * (see CardText) so duplicate checks are an indexed lookup, however many cards there are.
 */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(entity = DeckEntity::class, parentColumns = ["id"], childColumns = ["deckId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("deckId"), Index("looseSearchKey")],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val spanish: String,
    val english: String,
    val type: String,
    val example: String,
    val exampleEnglish: String,
    val regionNote: String,
    val createdMillis: Long,
    val searchKey: String,
    val looseSearchKey: String,
)

/** Spaced repetition state. A card without a row here has never been studied. */
@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class ReviewEntity(
    @PrimaryKey val cardId: Long,
    val stability: Double,
    val difficulty: Double,
    val reps: Int,
    val lapses: Int,
    val lastReviewMillis: Long?,
    val dueMillis: Long,
)

/** One answer. Kept even when a card is deleted, so streaks never shrink. */
@Entity(tableName = "review_log", indices = [Index("day")])
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val rating: Int,
    val wasNew: Boolean,
    val reviewedMillis: Long,
    /** Local calendar day (days since 1970-01-01). */
    val day: Long,
)

data class DeckSummary(
    @Embedded val deck: DeckEntity,
    val cardCount: Int,
    val dueCount: Int,
    val newCount: Int,
)

data class CardWithReview(
    @Embedded val card: CardEntity,
    @Relation(parentColumn = "id", entityColumn = "cardId") val review: ReviewEntity?,
)

@Dao
abstract class VocabloDao {

    @Query(
        """
        SELECT d.*,
            (SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id) AS cardCount,
            (SELECT COUNT(*) FROM cards c JOIN reviews r ON r.cardId = c.id
                WHERE c.deckId = d.id AND r.dueMillis <= :now) AS dueCount,
            (SELECT COUNT(*) FROM cards c LEFT JOIN reviews r ON r.cardId = c.id
                WHERE c.deckId = d.id AND r.cardId IS NULL) AS newCount
        FROM decks d ORDER BY d.createdMillis, d.id
        """,
    )
    abstract fun observeDeckSummaries(now: Long): Flow<List<DeckSummary>>

    @Query("SELECT * FROM decks ORDER BY createdMillis, id")
    abstract suspend fun decks(): List<DeckEntity>

    @Query("SELECT * FROM decks WHERE id = :id")
    abstract fun observeDeck(id: Long): Flow<DeckEntity?>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdMillis DESC, id DESC")
    abstract fun observeCards(deckId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    abstract suspend fun card(id: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE looseSearchKey = :looseKey")
    abstract suspend fun cardsWithLooseKey(looseKey: String): List<CardEntity>

    @Query("SELECT * FROM cards")
    abstract suspend fun allCards(): List<CardEntity>

    @Query("SELECT * FROM reviews")
    abstract suspend fun allReviews(): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE cardId = :cardId")
    abstract suspend fun review(cardId: Long): ReviewEntity?

    @Insert
    abstract suspend fun insertDeck(deck: DeckEntity): Long

    @Query("UPDATE decks SET name = :name WHERE id = :id")
    abstract suspend fun renameDeck(id: Long, name: String)

    @Query("DELETE FROM decks WHERE id = :id")
    abstract suspend fun deleteDeck(id: Long)

    @Insert
    abstract suspend fun insertCard(card: CardEntity): Long

    @Insert
    abstract suspend fun insertCards(cards: List<CardEntity>): List<Long>

    @Update
    abstract suspend fun updateCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    abstract suspend fun deleteCard(id: Long)

    @Upsert
    abstract suspend fun upsertReview(review: ReviewEntity)

    @Insert
    abstract suspend fun insertLog(log: ReviewLogEntity)

    @Query("SELECT DISTINCT day FROM review_log")
    abstract fun observeStudyDays(): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM review_log WHERE day = :day")
    abstract fun observeAnswersOn(day: Long): Flow<Int>

    @Query("SELECT COUNT(DISTINCT cardId) FROM review_log WHERE day = :day AND wasNew = 1")
    abstract suspend fun newCardsStudiedOn(day: Long): Int

    @Transaction
    @Query("SELECT * FROM cards WHERE deckId = :deckId")
    abstract suspend fun studyCards(deckId: Long): List<CardWithReview>

    @Transaction
    @Query("SELECT * FROM cards")
    abstract suspend fun allStudyCards(): List<CardWithReview>

    @Query("DELETE FROM decks")
    abstract suspend fun deleteAllDecks()

    @Insert
    abstract suspend fun insertDecks(decks: List<DeckEntity>)

    @Insert
    abstract suspend fun insertReviews(reviews: List<ReviewEntity>)

    @Transaction
    open suspend fun saveAnswer(review: ReviewEntity, log: ReviewLogEntity) {
        upsertReview(review)
        insertLog(log)
    }

    /** Replaces every deck, card and review in one step, for restoring a backup. */
    @Transaction
    open suspend fun replaceAll(decks: List<DeckEntity>, cards: List<CardEntity>, reviews: List<ReviewEntity>) {
        deleteAllDecks() // cards and reviews go with their decks
        insertDecks(decks)
        insertCards(cards)
        insertReviews(reviews)
    }
}

@Database(
    entities = [DeckEntity::class, CardEntity::class, ReviewEntity::class, ReviewLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class VocabloDatabase : RoomDatabase() {
    abstract fun dao(): VocabloDao

    companion object {
        fun create(context: Context): VocabloDatabase =
            Room.databaseBuilder(context, VocabloDatabase::class.java, "vocablo.db").build()
    }
}

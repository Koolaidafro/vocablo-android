package app.vocablo.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.vocablo.cards.Card
import app.vocablo.cards.CardType
import app.vocablo.srs.Rating
import app.vocablo.verbs.PracticeSettings
import app.vocablo.verbs.Stage
import app.vocablo.verbs.Tense
import app.vocablo.verbs.VerbSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class VerbRepositoryTest {

    private lateinit var db: VocabloDatabase
    private lateinit var verbs: VerbRepository
    private lateinit var cards: VocabloRepository
    private var now = 1_700_000_000_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VocabloDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        verbs = VerbRepository(db.dao(), clock = { now }, zone = { ZoneOffset.UTC })
        cards = VocabloRepository(db.dao(), clock = { now }, zone = { ZoneOffset.UTC })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun answeredItemsAreScheduledAndCountTowardTheStage() = runBlocking<Unit> {
        val plan = verbs.plan(PracticeSettings(), newLimit = 3)
        assertEquals(3, plan.size)
        plan.forEach { verbs.answer(it, Rating.GOOD, correct = true) }

        assertTrue("answered items are not due yet", verbs.plan(PracticeSettings(), newLimit = 3).none { it in plan })
        assertEquals(listOf(true, true, true), verbs.recentInStage(Stage.PRESENT))
        assertTrue(verbs.recentInStage(Stage.PAST).isEmpty())
        assertEquals(0, verbs.dueCount().first())
        assertEquals(setOf(cards.today()), cards.studyDays().first())

        now += 60L * 86_400_000
        assertEquals(3, verbs.dueCount().first())
    }

    @Test
    fun myVerbCardsCanBeTheDrillSource() = runBlocking<Unit> {
        val deck = cards.createDeck("Mis verbos")
        cards.addCard(Card(deckId = deck, spanish = "bailar", english = "to dance", type = CardType.VERB))
        cards.addCard(Card(deckId = deck, spanish = "la casa", english = "house"))
        val plan = verbs.plan(PracticeSettings(source = VerbSource.MY_CARDS, enabledTenses = setOf(Tense.PRESENT)))
        assertEquals(listOf("bailar"), plan.map { it.verb.verb.infinitive })
    }

    @Test
    fun addingAVerbCardTwiceIsRefused() = runBlocking<Unit> {
        assertTrue(cards.addVerbCard("comer", "to eat"))
        assertTrue(!cards.addVerbCard("comer", "to eat"))
        assertEquals(listOf(VocabloRepository.VERB_DECK), cards.decks().map { it.name })
    }
}

/** Opens a version 1 database file and checks that it migrates to the current version without losing cards. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class MigrationTest {

    @Test
    fun version1DatabaseMigratesAndKeepsCards() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val name = "migration-test.db"
        context.deleteDatabase(name)

        // Build the current schema, add a card, then turn the file back into version 1.
        val v2 = Room.databaseBuilder(context, VocabloDatabase::class.java, name).allowMainThreadQueries().build()
        val repo = VocabloRepository(v2.dao())
        val deck = repo.createDeck("Comida")
        repo.addCard(Card(deckId = deck, spanish = "el pan", english = "bread"))
        v2.close()
        android.database.sqlite.SQLiteDatabase.openDatabase(
            context.getDatabasePath(name).path,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
        ).apply {
            execSQL("DROP TABLE verb_reviews")
            execSQL("DROP TABLE verb_answers")
            execSQL("DROP TABLE activity")
            version = 1
            close()
        }

        val migrated = Room.databaseBuilder(context, VocabloDatabase::class.java, name)
            .addMigrations(VocabloDatabase.MIGRATION_1_2, VocabloDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        assertEquals(listOf("el pan"), migrated.dao().allCards().map { it.spanish })
        assertTrue(migrated.dao().verbReviews().isEmpty())
        assertTrue(migrated.dao().activityDays(ActivityKind.GAME).isEmpty())
        migrated.close()
        context.deleteDatabase(name)
    }
}

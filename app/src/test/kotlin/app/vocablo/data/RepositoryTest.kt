package app.vocablo.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.vocablo.cards.Card
import app.vocablo.cards.DuplicateResult
import app.vocablo.srs.Rating
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

/** Runs the real Room database in memory, with a clock the test controls. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RepositoryTest {

    private lateinit var db: VocabloDatabase
    private lateinit var repo: VocabloRepository
    private var now = 1_700_000_000_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VocabloDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = VocabloRepository(db.dao(), clock = { now }, zone = { ZoneOffset.UTC })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun duplicatesAreFoundAcrossDecksAndCanBeMerged() = runBlocking<Unit> {
        val food = repo.createDeck("Comida")
        repo.createDeck("Viaje")
        val id = repo.addCard(Card(deckId = food, spanish = "el aguacate", english = "avocado"))

        val dup = repo.checkDuplicate("Aguacate", "avocado tree")
        assertTrue(dup is DuplicateResult.Exact)
        assertEquals(listOf("avocado tree"), (dup as DuplicateResult.Exact).newMeanings)

        repo.mergeInto(id, "avocado tree", example = "Me gusta el aguacate.")
        val merged = repo.card(id)!!
        assertEquals("avocado, avocado tree", merged.english)
        assertEquals("Me gusta el aguacate.", merged.example)

        assertTrue(repo.checkDuplicate("aguacate", "", ignoreCardId = id) == DuplicateResult.None)
        assertTrue(repo.checkDuplicate("aguacáte", "") is DuplicateResult.Similar)
    }

    @Test
    fun answeringSchedulesTheCardAndStartsAStreak() = runBlocking<Unit> {
        val deck = repo.createDeck("Básico")
        repo.addCard(Card(deckId = deck, spanish = "hola", english = "hello"))
        repo.addCard(Card(deckId = deck, spanish = "adiós", english = "goodbye"))

        val session = repo.startStudy(deck, newPerDay = 20)
        assertEquals(2, session.queue.total)
        val first = session.queue.current!!
        repo.saveAnswer(session.queue.answer(Rating.GOOD, now), Rating.GOOD, wasNew = true)

        val again = repo.startStudy(deck, newPerDay = 20)
        assertEquals("the answered card is scheduled for later", 1, again.queue.total)
        assertTrue(again.queue.current!!.cardId != first.cardId)

        val summary = db.dao().observeDeckSummaries(now).first().single()
        assertEquals(2, summary.cardCount)
        assertEquals(0, summary.dueCount)
        assertEquals(1, summary.newCount)
        assertEquals(setOf(repo.today()), repo.studyDays().first())

        now += 30L * 86_400_000 // a month later the card is due again
        assertEquals(1, db.dao().observeDeckSummaries(now).first().single().dueCount)
    }

    @Test
    fun newCardsPerDayLimitCountsCardsAlreadyStartedToday() = runBlocking<Unit> {
        val deck = repo.createDeck("Verbos")
        listOf("hablar", "comer", "vivir").forEach { repo.addCard(Card(deckId = deck, spanish = it, english = "to $it")) }

        val session = repo.startStudy(deck, newPerDay = 2)
        assertEquals(2, session.queue.total)
        while (!session.queue.isFinished) {
            val wasNew = session.queue.current!!.state.isNew
            repo.saveAnswer(session.queue.answer(Rating.GOOD, now), Rating.GOOD, wasNew)
        }
        assertEquals(0, repo.startStudy(deck, newPerDay = 2).queue.total)
        now += 86_400_000 // tomorrow
        assertEquals(1, repo.startStudy(deck, newPerDay = 2).queue.total)
    }

    @Test
    fun importCreatesDecksAndSkipsCardsYouHave() = runBlocking<Unit> {
        val deck = repo.createDeck("Comida")
        repo.addCard(Card(deckId = deck, spanish = "el pan", english = "bread"))
        val csv = "spanish,english,example,deck\n" +
            "pan,bread,,Comida\n" +
            "la leche,milk,,Comida\n" +
            "el tren,train,,Viaje\n" +
            "hola,hello\n" +
            "hola,hi\n" +
            "broken\n"

        val result = repo.importCards(csv)
        assertEquals(3, result.added)
        assertEquals(2, result.duplicates)
        assertEquals(listOf(7), result.badLines)
        assertEquals(listOf("Comida", "Viaje", "Imported"), repo.decks().map { it.name })
    }

    @Test
    fun backupRestoresCardsAndProgress() = runBlocking<Unit> {
        val deck = repo.createDeck("Comida")
        repo.addCard(Card(deckId = deck, spanish = "comer", english = "to eat"))
        val session = repo.startStudy(null, 20)
        repo.saveAnswer(session.queue.answer(Rating.EASY, now), Rating.EASY, wasNew = true)
        val backup = repo.exportBackup()

        repo.addCard(Card(deckId = deck, spanish = "beber", english = "to drink"))
        repo.createDeck("Otra")
        assertEquals(1, repo.restoreBackup(backup))

        assertEquals(listOf("Comida"), repo.decks().map { it.name })
        assertEquals(listOf("comer"), db.dao().allCards().map { it.spanish })
        assertEquals(1, db.dao().allReviews().single().reps)
    }

    @Test
    fun aBadBackupFileGivesAReadableError() = runBlocking<Unit> {
        val error = runCatching { repo.restoreBackup("not a backup") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertEquals("That file isn't a Vocablo backup.", error!!.message)
    }

    @Test
    fun deletingADeckDeletesItsCards() = runBlocking<Unit> {
        val deck = repo.createDeck("Temporal")
        repo.addCard(Card(deckId = deck, spanish = "uno", english = "one"))
        repo.deleteDeck(deck)
        assertTrue(db.dao().allCards().isEmpty())
    }

    @Test
    fun firstLaunchSeedsOneStarterDeckOnlyOnce() = runBlocking<Unit> {
        repo.seedIfEmpty()
        repo.seedIfEmpty()
        assertEquals(1, repo.decks().size)
        assertTrue(db.dao().allCards().size >= 10)
    }
}

package app.vocablo.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.vocablo.cards.Card
import app.vocablo.play.DailyQuests
import app.vocablo.play.Game
import app.vocablo.play.Medal
import app.vocablo.play.Xp
import app.vocablo.srs.Rating
import app.vocablo.verbs.PracticeSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ProgressRepositoryTest {

    private lateinit var db: VocabloDatabase
    private lateinit var cards: VocabloRepository
    private lateinit var verbs: VerbRepository
    private lateinit var progress: ProgressRepository
    private var now = 1_700_000_000_000L // a Tuesday, 2023-11-14 in UTC
    private val dayMillis = 86_400_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VocabloDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cards = VocabloRepository(db.dao(), clock = { now }, zone = { ZoneOffset.UTC })
        verbs = VerbRepository(db.dao(), clock = { now }, zone = { ZoneOffset.UTC })
        progress = ProgressRepository(db.dao(), clock = { now }, zone = { ZoneOffset.UTC })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun xpAddsUpCardsDrillsGamesAndSentences() = runBlocking<Unit> {
        val deck = cards.createDeck("Comida")
        cards.addCard(Card(deckId = deck, spanish = "el pan", english = "bread"))
        val study = cards.startStudy(deck, newPerDay = 20)
        cards.saveAnswer(study.queue.answer(Rating.GOOD, now), Rating.GOOD, wasNew = true)
        verbs.plan(PracticeSettings(), newLimit = 2).let { plan ->
            verbs.answer(plan[0], Rating.GOOD, correct = true)
            verbs.answer(plan[1], Rating.AGAIN, correct = false)
        }
        assertEquals(30, progress.recordGame(Game.CUATRO, 30))
        assertEquals(Xp.SENTENCE_CORRECT, progress.recordSentence(true))
        assertEquals(0, progress.recordSentence(false))

        val expected = Xp.CARD_REVIEW_NEW + Xp.DRILL_CORRECT + Xp.DRILL_WRONG + 30 + Xp.SENTENCE_CORRECT
        assertEquals(expected, progress.totalXp().first())
        val today = progress.todayActivity().first()
        assertEquals(1, today.cardsReviewed)
        assertEquals(1, today.newCards)
        assertEquals(1, today.verbsRight)
        assertEquals(1, today.gamesPlayed)
        assertEquals(1, today.sentencesBuilt)
        assertEquals(expected, today.xp)
    }

    @Test
    fun questsPayOnceAndMarkTheDay() = runBlocking<Unit> {
        // Whatever today's quests are, each one that is done pays exactly once.
        repeat(5) { progress.recordGame(Game.SI_O_NO, 60) }
        repeat(5) { progress.recordSentence(true) }
        val first = progress.settleQuests(newPerDay = 20)
        val second = progress.settleQuests(newPerDay = 20)
        assertEquals("a quest never pays twice", 0, second)
        val activity = progress.todayActivity().first()
        val quests = DailyQuests.forDay(progress.today(), 20)
        assertEquals(quests.count { it.done(activity) }, first)
        val allDone = quests.all { it.done(activity) }
        assertEquals(allDone, progress.today() in progress.questDays().first())
    }

    @Test
    fun theChestOpensOnlyAfterFiveQuestDaysAndOnlyOnce() = runBlocking<Unit> {
        assertEquals(0, progress.openChest())
        // Mark five quest days this week directly, Monday to Friday.
        val monday = 19_674L // 2023-11-13
        (monday until monday + 5).forEach {
            db.dao().insertActivity(ActivityEntity(kind = ActivityKind.QUEST_DAY, xp = 0, day = it, millis = now))
        }
        now = (monday + 4) * dayMillis + 3_600_000 // Friday
        assertFalse(progress.chestOpenedThisWeek().first())
        assertEquals(Xp.CHEST, progress.openChest())
        assertEquals(0, progress.openChest())
        assertTrue(progress.chestOpenedThisWeek().first())
    }

    @Test
    fun aMissedDayIsFrozenOnlyWhenAFreezeWasEarned() = runBlocking<Unit> {
        val deck = cards.createDeck("Comida")
        val id = cards.addCard(Card(deckId = deck, spanish = "el pan", english = "bread"))
        val start = progress.today()
        // Study 7 days in a row to earn a freeze, skip a day, then open the app.
        repeat(7) { d ->
            val at = (start + d) * dayMillis + 3_600_000
            db.dao().insertLog(ReviewLogEntity(cardId = id, rating = 3, wasNew = d == 0, reviewedMillis = at, day = start + d))
        }
        now = (start + 8) * dayMillis + 3_600_000 // day 7 was missed
        assertEquals(1, progress.applyFreezes())
        assertEquals(setOf(start + 7), progress.frozenDays().first())
        assertEquals(0, progress.applyFreezes())
    }

    @Test
    fun medalsComeFromTheWeakestTense() = runBlocking<Unit> {
        val plan = verbs.plan(PracticeSettings(), newLimit = 1)
        verbs.answer(plan[0], Rating.GOOD, correct = true)
        val medals = progress.medals()
        assertEquals(Medal.BRONZE, medals[plan[0].verb.verb.infinitive])
        assertEquals(1, medals.size)
    }
}

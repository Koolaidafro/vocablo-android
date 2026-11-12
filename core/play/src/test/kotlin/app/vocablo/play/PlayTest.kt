package app.vocablo.play

import app.vocablo.cards.Card
import app.vocablo.cards.CardType
import app.vocablo.verbs.Conjugator
import app.vocablo.verbs.Person
import app.vocablo.verbs.Tense
import app.vocablo.verbs.VerbList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

private val verbs = VerbList.all.take(100).map { it.verb }
private val persons = Person.entries - Person.VOSOTROS

class XpTest {
    @Test
    fun `levels grow quadratically and progress stays within the level`() {
        assertEquals(1, Xp.level(0))
        assertEquals(2, Xp.level(50))
        assertEquals(3, Xp.level(200))
        assertEquals(0.5f, Xp.levelProgress(125), 0.001f)
        assertEquals(60, Xp.forGame(500))
    }
}

class QuestTest {
    @Test
    fun `a day has three quests, stable for that day, ending with an XP goal`() {
        val a = DailyQuests.forDay(20_000, 20)
        assertEquals(a, DailyQuests.forDay(20_000, 20))
        assertEquals(3, a.size)
        assertEquals(QuestKind.EARN_XP, a.last().kind)
        assertEquals(2, a.take(2).map { it.kind }.toSet().size)
    }

    @Test
    fun `progress is capped at the target`() {
        val q = Quest(QuestKind.REVIEW_CARDS, 10)
        assertEquals(10, q.progress(DayActivity(cardsReviewed = 25)))
        assertTrue(q.done(DayActivity(cardsReviewed = 10)))
        assertFalse(q.done(DayActivity(cardsReviewed = 9)))
    }

    @Test
    fun `the chest needs five quest days in the Monday-to-Sunday week`() {
        val monday = 20_409L // 2025-11-17 was a Monday
        assertEquals(monday, WeeklyChest.weekStart(monday + 6))
        assertEquals(monday + 7, WeeklyChest.weekStart(monday + 7))
        val days = (monday until monday + 5).toSet()
        assertTrue(WeeklyChest.ready(days, monday + 4, alreadyOpened = false))
        assertFalse(WeeklyChest.ready(days, monday + 4, alreadyOpened = true))
        assertFalse(WeeklyChest.ready(days - monday, monday + 4, alreadyOpened = false))
        assertEquals(0, WeeklyChest.questDaysThisWeek(days, monday + 7)) // new week
    }
}

class StreakFreezeTest {
    @Test
    fun `a missed day is covered when a freeze is held`() {
        val study = setOf(1L, 2L, 3L)
        assertEquals(listOf(4L), StreakFreezes.daysToFreeze(study, emptySet(), held = 1, today = 5))
        assertEquals(3, StreakFreezes.streak(study, setOf(4L), today = 5))
        assertEquals(0, StreakFreezes.streak(study, emptySet(), today = 5))
    }

    @Test
    fun `freezes are not spent when they can't save the streak or aren't needed`() {
        val study = setOf(1L, 2L, 3L)
        assertTrue(StreakFreezes.daysToFreeze(study, emptySet(), held = 1, today = 6).isEmpty()) // two days missed
        assertEquals(listOf(4L, 5L), StreakFreezes.daysToFreeze(study, emptySet(), held = 2, today = 6))
        assertTrue(StreakFreezes.daysToFreeze(study, emptySet(), held = 2, today = 4).isEmpty()) // studied yesterday
        assertTrue(StreakFreezes.daysToFreeze(emptySet(), emptySet(), held = 2, today = 4).isEmpty())
    }

    @Test
    fun `one freeze per week of streak, at most two held`() {
        assertEquals(0, StreakFreezes.held(6, 0))
        assertEquals(1, StreakFreezes.held(13, 0))
        assertEquals(2, StreakFreezes.held(40, 0))
        assertEquals(1, StreakFreezes.held(40, 4))
    }
}

class MedalTest {
    @Test
    fun `the weakest tense decides the medal`() {
        assertEquals(Medal.NONE, Medals.forVerb(emptyList()))
        assertEquals(Medal.BRONZE, Medals.forVerb(listOf(2.0, 40.0)))
        assertEquals(Medal.SILVER, Medals.forVerb(listOf(8.0, 40.0)))
        assertEquals(Medal.GOLD, Medals.forVerb(listOf(31.0, 40.0)))
    }
}

class SentenceTest {
    private val cards = listOf(
        Card(id = 1, deckId = 1, spanish = "el aguacate", english = "avocado"),
        Card(id = 2, deckId = 1, spanish = "la leche", english = "milk, dairy"),
        Card(id = 3, deckId = 1, spanish = "hola", english = "hello"),
        Card(id = 4, deckId = 1, spanish = "comer", english = "to eat", type = CardType.VERB),
    )

    @Test
    fun `nouns need an article`() {
        assertEquals(listOf(Noun("el", "aguacate", "avocado"), Noun("la", "leche", "milk")), SentenceBuilder.nounsFrom(cards))
    }

    @Test
    fun `sentences put subject, verb and object in order with tempting wrong forms`() {
        val comer = VerbList.byInfinitive.getValue("comer").verb
        repeat(30) { seed ->
            val ex = SentenceBuilder.build(listOf(comer), SentenceBuilder.nounsFrom(cards), listOf(Tense.PRESENT, Tense.PRETERITE), persons, Random(seed))!!
            assertEquals(Conjugator.conjugate(comer, ex.tense, ex.person), ex.answer[1])
            assertEquals(6, ex.tiles.size)
            assertTrue(ex.tiles.containsAll(ex.answer))
            assertTrue(SentenceBuilder.check(ex.answer, ex))
            assertFalse(SentenceBuilder.check(ex.answer.reversed(), ex))
            assertTrue(ex.sentence.first().isUpperCase() && ex.sentence.endsWith("."))
            assertTrue(ex.meaning[1].startsWith("eat ("))
        }
    }

    @Test
    fun `no sentence without a usable verb or noun`() {
        val vivir = VerbList.byInfinitive.getValue("vivir").verb
        assertNull(SentenceBuilder.build(listOf(vivir), SentenceBuilder.nounsFrom(cards), listOf(Tense.PRESENT), persons))
        assertNull(SentenceBuilder.build(verbs, emptyList(), listOf(Tense.PRESENT), persons))
    }
}

class GamesTest {
    private val cards = (1..10L).map { Card(id = it, deckId = 1, spanish = "palabra$it", english = "word$it") }

    @Test
    fun `verb rain always drops the answer among five different forms`() {
        repeat(30) { seed ->
            val r = VerbRain.round(verbs, listOf(Tense.PRESENT), persons, Random(seed))
            assertTrue(r.question.answer in r.drops)
            assertEquals(r.drops.size, r.drops.toSet().size)
            assertTrue(r.drops.size in 4..5)
        }
        assertTrue(VerbRain.points(0f) > VerbRain.points(0.9f))
    }

    @Test
    fun `si o no claims are honest about being true`() {
        repeat(50) { seed ->
            val c = SiONo.claim(verbs, listOf(Tense.PRESENT, Tense.PRETERITE), persons, Random(seed))
            assertEquals(c.isTrue, c.shown == Conjugator.conjugate(c.verb, c.tense, c.person))
        }
    }

    @Test
    fun `tug of war ends at either side`() {
        var rope = Rope()
        repeat(5) { rope = rope.answer(true) }
        assertTrue(rope.won)
        assertEquals(rope, rope.rivalPull()) // nothing moves after the game ends
        assertTrue(Rope(position = -4).rivalPull().lost)
        assertTrue(TugOfWar.rivalIntervalMillis(1) > TugOfWar.rivalIntervalMillis(10))
    }

    @Test
    fun `cuatro offers four different meanings including the answer`() {
        val q = Cuatro.question(cards, Random(1))!!
        assertEquals(4, q.options.toSet().size)
        assertTrue(q.answer in q.options)
        assertNull(Cuatro.question(cards.take(3)))
    }

    @Test
    fun `parejas matches only a Spanish tile with its own meaning`() {
        val board = ParejasBoard.from(cards, pairs = 4, random = Random(2))!!
        assertEquals(8, board.tiles.size)
        val es = board.tiles.indexOfFirst { it.spanish }
        val wrong = board.tiles.indexOfFirst { !it.spanish && it.pairId != board.tiles[es].pairId }
        val right = board.tiles.indexOfFirst { !it.spanish && it.pairId == board.tiles[es].pairId }
        assertFalse(board.tap(es))
        assertFalse(board.tap(wrong))
        assertEquals(1, board.mistakes)
        board.tap(es)
        assertTrue(board.tap(right))
        board.tiles.map { it.pairId }.distinct().forEach { id ->
            val (a, b) = board.tiles.indices.filter { board.tiles[it].pairId == id }
            board.tap(a)
            board.tap(b)
        }
        assertTrue(board.finished)
        assertNotNull(ParejasBoard.from(cards.take(3)))
        assertNull(ParejasBoard.from(cards.take(2)))
    }
}

class RemindersAndLongestTest {
    @Test
    fun `reminders come at the chosen hour, then the evening streak saver`() {
        val morning = java.time.LocalDateTime.of(2026, 3, 2, 7, 30)
        assertEquals(Reminders.Slot(Reminders.Kind.DAILY, morning.withHour(9).withMinute(0)), Reminders.next(morning, 9))
        val noon = morning.withHour(12)
        assertEquals(Reminders.Slot(Reminders.Kind.STREAK_SAVER, morning.withHour(20).withMinute(0)), Reminders.next(noon, 9))
        val night = morning.withHour(21)
        assertEquals(Reminders.Slot(Reminders.Kind.DAILY, morning.plusDays(1).withHour(9).withMinute(0)), Reminders.next(night, 9))
    }

    @Test
    fun `an evening daily reminder replaces the streak saver`() {
        val noon = java.time.LocalDateTime.of(2026, 3, 2, 12, 0)
        val first = Reminders.next(noon, 19)
        assertEquals(Reminders.Kind.DAILY, first.kind)
        assertEquals(Reminders.Kind.DAILY, Reminders.next(first.at, 19).kind)
    }

    @Test
    fun `reminders stay quiet once the learner has studied`() {
        assertEquals(false, Reminders.shouldShow(Reminders.Kind.DAILY, studiedToday = true, streak = 4, dueCount = 10))
        assertEquals(true, Reminders.shouldShow(Reminders.Kind.DAILY, studiedToday = false, streak = 0, dueCount = 3))
        assertEquals(false, Reminders.shouldShow(Reminders.Kind.STREAK_SAVER, studiedToday = false, streak = 0, dueCount = 3))
    }

    @Test
    fun `the longest chain bridges frozen days without counting them`() {
        assertEquals(5, StreakFreezes.longest(setOf(1L, 2L, 3L, 5L, 6L, 10L), setOf(4L)))
        assertEquals(0, StreakFreezes.longest(emptySet(), emptySet()))
    }
}

// Vocablo: Spanish flashcards for Android.
//
// Tests for the v1.2 engagement rules: badges, reminder policy and messages, session summary
// and welcome back (FR-22 to FR-26, NFR-7).
package app.vocablo.play

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BadgesTest {
    @Test
    fun `each badge is reached exactly at its milestone`() {
        assertEquals(emptyList<Badge>(), Badges.reached(Milestones()))
        assertEquals(listOf(Badge.FIRST_STEPS), Badges.reached(Milestones(studyDays = 1, longestStreak = 6, cardsLearned = 99)))
        assertEquals(
            Badge.entries.toList(),
            Badges.reached(Milestones(studyDays = 40, longestStreak = 30, cardsLearned = 100, goldVerbs = 1, chestsOpened = 1)),
        )
        assertTrue(Badge.STREAK_7.earned(Milestones(longestStreak = 7)))
        assertFalse(Badge.STREAK_30.earned(Milestones(longestStreak = 29)))
    }

    @Test
    fun `only badges not yet awarded are new, and awarded ones are never taken back`() {
        val now = Milestones(studyDays = 3, longestStreak = 8)
        assertEquals(listOf(Badge.STREAK_7), Badges.newlyReached(now, setOf(Badge.FIRST_STEPS)))
        // Fewer cards than before still leaves an awarded badge out of the "new" list, never revoked.
        assertEquals(emptyList<Badge>(), Badges.newlyReached(Milestones(), setOf(Badge.WORDS_100)))
    }

    @Test
    fun `there are six badges, each with a title and a way to earn it`() {
        assertEquals(6, Badge.entries.size)
        Badge.entries.forEach {
            assertTrue(it.title.isNotBlank())
            assertTrue(it.howToEarn.endsWith("."))
        }
    }
}

class ReminderPolicyTest {
    private fun decide(daysSinceOpened: Long, comebackSent: Boolean = false, studied: Boolean = false, streak: Int = 3, due: Int = 5) =
        Reminders.decide(Reminders.Kind.DAILY, studied, streak, due, daysSinceOpened, comebackSent)

    @Test
    fun `reminders show normally while the app is in use`() {
        assertEquals(Reminders.Decision.SHOW, decide(daysSinceOpened = 0))
        assertEquals(Reminders.Decision.SHOW, decide(daysSinceOpened = 6))
        assertEquals(Reminders.Decision.QUIET, decide(daysSinceOpened = 1, studied = true))
    }

    @Test
    fun `after a week away one comeback message goes out, then silence`() {
        assertEquals(Reminders.Decision.COMEBACK, decide(daysSinceOpened = 7))
        assertEquals(Reminders.Decision.QUIET, decide(daysSinceOpened = 8, comebackSent = true))
        assertEquals(Reminders.Decision.QUIET, decide(daysSinceOpened = 30, comebackSent = true))
    }
}

class ReminderMessagesTest {
    @Test
    fun `every pool has at least ten messages`() {
        ReminderMessages.Kind.entries.forEach { assertTrue(ReminderMessages.pool(it).size >= 10, "$it") }
    }

    @Test
    fun `no message repeats within seven days`() {
        ReminderMessages.Kind.entries.forEach { kind ->
            for (start in 0L until 30L) {
                val week = (start until start + 7).map { ReminderMessages.pick(kind, it, dueCount = 4, streak = 5) }
                assertEquals(7, week.toSet().size, "$kind from day $start")
            }
        }
    }

    @Test
    fun `messages use the real counts and never blame or rush`() {
        val banned = listOf("don't lose", "lose your", "last chance", "hurry", "you failed", "disappointed", "only today", "!!")
        ReminderMessages.Kind.entries.forEach { kind ->
            ReminderMessages.pool(kind).forEach { make ->
                val m = make(1, 12)
                val text = (m.title + " " + m.text).lowercase()
                banned.forEach { assertFalse(it in text, "\"$text\" contains \"$it\"") }
                assertFalse("1 cards" in text)
            }
        }
        assertTrue(ReminderMessages.pool(ReminderMessages.Kind.STREAK).all { "12" in it(0, 12).let { m -> m.title + m.text } || "13" in it(0, 12).let { m -> m.title + m.text } })
    }

    @Test
    fun `the evening saver talks about the streak, otherwise due cards come first`() {
        assertEquals(ReminderMessages.Kind.STREAK, ReminderMessages.kindFor(saver = true, dueCount = 9, streak = 2))
        assertEquals(ReminderMessages.Kind.CARDS_DUE, ReminderMessages.kindFor(saver = false, dueCount = 9, streak = 2))
        assertEquals(ReminderMessages.Kind.STREAK, ReminderMessages.kindFor(saver = false, dueCount = 0, streak = 2))
        assertEquals(ReminderMessages.Kind.FRESH_START, ReminderMessages.kindFor(saver = false, dueCount = 0, streak = 0))
    }
}

class SessionSummaryTest {
    private val quests = listOf(Quest(QuestKind.REVIEW_CARDS, 10), Quest(QuestKind.PLAY_GAME, 1), Quest(QuestKind.EARN_XP, 40))

    @Test
    fun `session XP uses the same weights as the XP total`() {
        assertEquals(4 * Xp.CARD_REVIEW_NEW + 6 * Xp.CARD_REVIEW, SessionSummary.xp(newAnswers = 4, reviewAnswers = 6))
    }

    @Test
    fun `quests done are counted from today's activity`() {
        assertEquals(1, SessionSummary.questsDone(quests, DayActivity(cardsReviewed = 12, xp = 30)))
        assertEquals(3, SessionSummary.questsDone(quests, DayActivity(cardsReviewed = 12, gamesPlayed = 1, xp = 40)))
    }

    @Test
    fun `next step prefers due verbs, then an open game or sentence quest, then rest`() {
        val day = DayActivity(cardsReviewed = 12)
        assertEquals(NextStep.DRILL_VERBS, SessionSummary.nextStep(dueVerbs = 3, quests = quests, today = day))
        assertEquals(NextStep.PLAY_GAME, SessionSummary.nextStep(dueVerbs = 0, quests = quests, today = day))
        val sentenceQuests = listOf(Quest(QuestKind.BUILD_SENTENCES, 3))
        assertEquals(NextStep.BUILD_SENTENCES, SessionSummary.nextStep(0, sentenceQuests, DayActivity()))
        assertEquals(NextStep.DONE_FOR_TODAY, SessionSummary.nextStep(0, quests, day.copy(gamesPlayed = 1)))
    }
}

class WelcomeBackTest {
    @Test
    fun `welcome back shows after three days away, never for a brand new learner`() {
        assertFalse(WelcomeBack.show(emptySet(), today = 100))
        assertFalse(WelcomeBack.show(setOf(98L), today = 100))
        assertTrue(WelcomeBack.show(setOf(90L, 97L), today = 100))
        assertFalse(WelcomeBack.show(setOf(90L, 100L), today = 100))
    }
}

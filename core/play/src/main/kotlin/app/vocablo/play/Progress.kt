package app.vocablo.play

import kotlin.math.sqrt
import kotlin.random.Random

/** What earns XP. Kept small and predictable so XP tracks real practice, not grinding. */
object Xp {
    const val CARD_REVIEW = 2
    const val CARD_REVIEW_NEW = 3
    const val DRILL_CORRECT = 2
    const val DRILL_WRONG = 1 // trying still counts a little
    const val SENTENCE_CORRECT = 5
    const val QUEST_COMPLETE = 20
    const val CHEST = 50

    /** Games pay one XP per point, capped so a long game can't dwarf real study. */
    fun forGame(score: Int): Int = score.coerceIn(0, 60)

    /** Level n needs 50·n² total XP: 50, 200, 450, 800 ... */
    fun level(totalXp: Int): Int = (sqrt(totalXp / 50.0).toInt() + 1).coerceAtLeast(1)

    fun xpForLevel(level: Int): Int = 50 * (level - 1) * (level - 1)

    /** Progress from 0 to 1 through the current level. */
    fun levelProgress(totalXp: Int): Float {
        val l = level(totalXp)
        val start = xpForLevel(l)
        val end = xpForLevel(l + 1)
        return (totalXp - start).toFloat() / (end - start)
    }
}

/** Counters for one day, filled from the database. */
data class DayActivity(
    val cardsReviewed: Int = 0,
    val newCards: Int = 0,
    val verbsRight: Int = 0,
    val gamesPlayed: Int = 0,
    val sentencesBuilt: Int = 0,
    val xp: Int = 0,
)

enum class QuestKind(val title: (Int) -> String) {
    REVIEW_CARDS({ "Review $it cards" }),
    LEARN_NEW({ "Learn $it new cards" }),
    VERBS_RIGHT({ "Get $it verb forms right" }),
    PLAY_GAME({ if (it == 1) "Play a game" else "Play $it games" }),
    BUILD_SENTENCES({ "Build $it sentences" }),
    EARN_XP({ "Earn $it XP" }),
}

data class Quest(val kind: QuestKind, val target: Int) {
    val title: String get() = kind.title(target)

    fun progress(day: DayActivity): Int = when (kind) {
        QuestKind.REVIEW_CARDS -> day.cardsReviewed
        QuestKind.LEARN_NEW -> day.newCards
        QuestKind.VERBS_RIGHT -> day.verbsRight
        QuestKind.PLAY_GAME -> day.gamesPlayed
        QuestKind.BUILD_SENTENCES -> day.sentencesBuilt
        QuestKind.EARN_XP -> day.xp
    }.coerceAtMost(target)

    fun done(day: DayActivity): Boolean = progress(day) >= target
}

/** Three quests a day, the same all day, chosen from the date so nothing needs storing. */
object DailyQuests {
    fun forDay(day: Long, newPerDay: Int): List<Quest> {
        val r = Random(day * 31 + 7)
        val pool = listOf(
            Quest(QuestKind.REVIEW_CARDS, listOf(10, 15, 20).random(r)),
            Quest(QuestKind.LEARN_NEW, (newPerDay / 2).coerceIn(3, 10)),
            Quest(QuestKind.VERBS_RIGHT, listOf(10, 15, 20).random(r)),
            Quest(QuestKind.PLAY_GAME, 1),
            Quest(QuestKind.BUILD_SENTENCES, listOf(3, 5).random(r)),
        ).shuffled(r)
        return pool.take(2) + Quest(QuestKind.EARN_XP, listOf(40, 60, 80).random(r))
    }

    fun allDone(day: Long, newPerDay: Int, activity: DayActivity): Boolean =
        forDay(day, newPerDay).all { it.done(activity) }
}

/** The weekly chest opens when all daily quests were finished on 5 days of the week (Monday start). */
object WeeklyChest {
    const val DAYS_NEEDED = 5

    /** Days since 1970-01-01 of the Monday starting the week that holds [day]. 1970-01-01 was a Thursday. */
    fun weekStart(day: Long): Long = day - Math.floorMod(day + 3, 7L)

    fun questDaysThisWeek(questDays: Set<Long>, today: Long): Int {
        val start = weekStart(today)
        return (start..today).count { it in questDays }
    }

    fun ready(questDays: Set<Long>, today: Long, alreadyOpened: Boolean): Boolean =
        !alreadyOpened && questDaysThisWeek(questDays, today) >= DAYS_NEEDED
}

/**
 * Streak freezes: one is earned for every 7 days of streak, up to 2 held at once. A missed
 * day is covered automatically if a freeze is available, so one busy day doesn't erase weeks.
 */
object StreakFreezes {
    const val MAX_HELD = 2
    const val EARN_EVERY = 7

    /** Streak counting both study days and frozen days, ending today or yesterday. */
    fun streak(studyDays: Set<Long>, frozenDays: Set<Long>, today: Long): Int {
        val covered = studyDays + frozenDays
        var day = if (today in covered) today else today - 1
        var count = 0
        while (day in covered) {
            if (day in studyDays) count++ // frozen days keep the chain but don't add to it
            day--
        }
        return count
    }

    /**
     * Called when the app opens. If the days between the last study day and yesterday were
     * missed, spends freezes to cover them (only if there are enough). Returns the days to
     * mark frozen; empty if nothing changes.
     */
    fun daysToFreeze(studyDays: Set<Long>, frozenDays: Set<Long>, held: Int, today: Long): List<Long> {
        val covered = studyDays + frozenDays
        if (today - 1 in covered) return emptyList()
        val last = covered.filter { it < today }.maxOrNull() ?: return emptyList()
        val gap = ((last + 1) until today).toList()
        return if (gap.size in 1..held) gap else emptyList()
    }

    /** Longest chain ever, with frozen days bridging gaps but not counting as study. */
    fun longest(studyDays: Set<Long>, frozenDays: Set<Long>): Int {
        val covered = studyDays + frozenDays
        var best = 0
        for (d in covered) {
            if (d - 1 in covered) continue
            var day = d
            var n = 0
            while (day in covered) {
                if (day in studyDays) n++
                day++
            }
            best = maxOf(best, n)
        }
        return best
    }

    /** Freezes earned so far: one per completed week of streak, minus those spent, capped. */
    fun held(longestRunEver: Int, spent: Int): Int = (longestRunEver / EARN_EVERY - spent).coerceIn(0, MAX_HELD)
}

enum class Medal { NONE, BRONZE, SILVER, GOLD }

/** A per-verb medal from its drill memory: how long it will stay remembered, in every practiced tense. */
object Medals {
    /** [stabilityDays] has one entry per practiced tense of the verb (missing tenses count as 0). */
    fun forVerb(stabilityDays: List<Double>): Medal {
        if (stabilityDays.isEmpty()) return Medal.NONE
        val weakest = stabilityDays.min()
        return when {
            weakest >= 30 -> Medal.GOLD
            weakest >= 7 -> Medal.SILVER
            weakest > 0 -> Medal.BRONZE
            else -> Medal.NONE
        }
    }
}

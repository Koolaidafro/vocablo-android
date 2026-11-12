package app.vocablo.data

import app.vocablo.play.DailyQuests
import app.vocablo.play.DayActivity
import app.vocablo.play.Game
import app.vocablo.play.Medal
import app.vocablo.play.Medals
import app.vocablo.play.StreakFreezes
import app.vocablo.play.WeeklyChest
import app.vocablo.play.Xp
import app.vocablo.verbs.DrillItem
import app.vocablo.verbs.VerbList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

/** XP, quests, the weekly chest, streak freezes and medals. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressRepository(
    private val dao: VocabloDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    fun today(): Long = Instant.ofEpochMilli(clock()).atZone(zone()).toLocalDate().toEpochDay()

    /** Today's day number now and whenever the date changes. */
    private val days: Flow<Long> = flow {
        while (true) {
            emit(today())
            delay(60_000)
        }
    }.distinctUntilChanged()

    fun totalXp(): Flow<Int> = dao.observeTotalXp(Xp.CARD_REVIEW, Xp.CARD_REVIEW_NEW, Xp.DRILL_CORRECT, Xp.DRILL_WRONG)

    fun todayActivity(): Flow<DayActivity> = days.flatMapLatest { day -> activityOn(day) }

    private fun activityOn(day: Long): Flow<DayActivity> =
        dao.observeDay(day, Xp.CARD_REVIEW, Xp.CARD_REVIEW_NEW, Xp.DRILL_CORRECT, Xp.DRILL_WRONG).map {
            DayActivity(it.cardsReviewed, it.newCards, it.verbsRight, it.gamesPlayed, it.sentencesBuilt, it.xp)
        }

    fun frozenDays(): Flow<Set<Long>> = dao.observeActivityDays(ActivityKind.FREEZE).map { it.toSet() }

    fun questDays(): Flow<Set<Long>> = dao.observeActivityDays(ActivityKind.QUEST_DAY).map { it.toSet() }

    fun chestOpenedThisWeek(): Flow<Boolean> =
        days.flatMapLatest { dao.observeChestsOpened(WeeklyChest.weekStart(it)) }.map { it > 0 }

    /** Records a finished game and returns the XP it earned. */
    suspend fun recordGame(game: Game, score: Int): Int {
        val xp = Xp.forGame(score)
        insert(ActivityKind.GAME, xp, ref = clock() * 10 + game.ordinal)
        return xp
    }

    /** Records one sentence attempt; only right ones earn XP and count for quests. */
    suspend fun recordSentence(correct: Boolean): Int {
        val xp = if (correct) Xp.SENTENCE_CORRECT else 0
        insert(ActivityKind.SENTENCE, xp, ref = clock())
        return xp
    }

    /**
     * Awards XP for each of today's quests that is newly done, and marks the day once all
     * are. Safe to call as often as you like: each quest pays out once.
     */
    suspend fun settleQuests(newPerDay: Int): Int {
        val day = today()
        var awarded = 0
        // Paying a quest can finish the XP quest, so look again until nothing changes.
        repeat(3) {
            val activity = activityOn(day).first()
            val quests = DailyQuests.forDay(day, newPerDay)
            var paidThisRound = 0
            quests.forEachIndexed { i, q ->
                if (q.done(activity) && insert(ActivityKind.QUEST, Xp.QUEST_COMPLETE, ref = i.toLong(), day = day)) {
                    paidThisRound++
                }
            }
            if (quests.all { it.done(activity) }) insert(ActivityKind.QUEST_DAY, 0, day = day)
            awarded += paidThisRound
            if (paidThisRound == 0) return awarded
        }
        return awarded
    }

    /** Opens this week's chest if it's ready. Returns the XP it held, or 0. */
    suspend fun openChest(): Int {
        val today = today()
        val week = WeeklyChest.weekStart(today)
        val opened = dao.observeChestsOpened(week).first() > 0
        val questDays = dao.activityDays(ActivityKind.QUEST_DAY).toSet()
        if (!WeeklyChest.ready(questDays, today, opened)) return 0
        return if (insert(ActivityKind.CHEST, Xp.CHEST, ref = week, day = week)) Xp.CHEST else 0
    }

    /** Called when the app opens: covers missed days with earned freezes. Returns how many were used. */
    suspend fun applyFreezes(): Int {
        val study = (dao.cardStudyDays() + dao.verbStudyDays()).toSet()
        val frozen = dao.activityDays(ActivityKind.FREEZE).toSet()
        val held = StreakFreezes.held(StreakFreezes.longest(study, frozen), frozen.size)
        val toFreeze = StreakFreezes.daysToFreeze(study, frozen, held, today())
        toFreeze.forEach { insert(ActivityKind.FREEZE, 0, day = it) }
        return toFreeze.size
    }

    /** Medal per verb infinitive, from the weakest practiced tense's memory stability. */
    suspend fun medals(): Map<String, Medal> {
        val byRank = VerbList.all.associateBy { it.rank }
        return dao.verbReviews()
            .groupBy { DrillItem.rankOf(it.itemId) }
            .mapNotNull { (rank, rows) ->
                val verb = byRank[rank] ?: return@mapNotNull null
                verb.verb.infinitive to Medals.forVerb(rows.map { it.stability })
            }
            .filter { it.second != Medal.NONE }
            .toMap()
    }

    private suspend fun insert(kind: String, xp: Int, ref: Long = 0, day: Long = today()): Boolean =
        dao.insertActivity(ActivityEntity(kind = kind, xp = xp, day = day, ref = ref, millis = clock())) != -1L
}

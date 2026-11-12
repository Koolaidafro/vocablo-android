package app.vocablo.data

import app.vocablo.cards.CardText
import app.vocablo.srs.Fsrs
import app.vocablo.srs.Rating
import app.vocablo.srs.ReviewState
import app.vocablo.verbs.DrillItem
import app.vocablo.verbs.DrillPlanner
import app.vocablo.verbs.ItemSchedule
import app.vocablo.verbs.PracticeSettings
import app.vocablo.verbs.RankedVerb
import app.vocablo.verbs.Stage
import app.vocablo.verbs.StageProgress
import app.vocablo.verbs.VerbList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/** Verb drill progress: which verb-tense items are due, and how well each stage is going. */
@OptIn(ExperimentalCoroutinesApi::class)
class VerbRepository(
    private val dao: VocabloDao,
    private val verbs: () -> List<RankedVerb> = { VerbList.all },
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val fsrs: Fsrs = Fsrs(),
) {
    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(60_000)
        }
    }

    fun dueCount(): Flow<Int> = ticker.flatMapLatest { dao.observeDueVerbItems(it) }

    fun studyDays(): Flow<List<Long>> = dao.observeVerbStudyDays()

    /** Infinitives of the learner's own verb cards that the engine knows. */
    suspend fun myVerbs(): Set<String> {
        val known = withContext(Dispatchers.Default) { VerbList.byInfinitive }
        return dao.verbCardTexts().map { CardText.key(it) }.filter { it in known }.toSet()
    }

    /** Builds a drill session: due items first, then a few new ones. */
    suspend fun plan(settings: PracticeSettings, size: Int = 20, newLimit: Int = 6): List<DrillItem> {
        val schedules = states().mapValues { (_, s) -> ItemSchedule(s.isNew, s.dueMillis) }
        val mine = myVerbs()
        return withContext(Dispatchers.Default) {
            DrillPlanner.plan(settings, verbs(), mine, schedules, clock(), size, newLimit)
        }
    }

    suspend fun states(): Map<Long, ReviewState> = dao.verbReviews().associate {
        it.itemId to ReviewState(it.stability, it.difficulty, it.reps, it.lapses, it.lastReviewMillis, it.dueMillis)
    }

    /** Saves one answer and returns the item's next due time. */
    suspend fun answer(item: DrillItem, rating: Rating, correct: Boolean): Long {
        val now = clock()
        val old = dao.verbReview(item.id)
            ?.let { ReviewState(it.stability, it.difficulty, it.reps, it.lapses, it.lastReviewMillis, it.dueMillis) }
            ?: ReviewState()
        val s = fsrs.review(old, rating, now)
        dao.saveVerbAnswer(
            VerbReviewEntity(item.id, s.stability, s.difficulty, s.reps, s.lapses, s.lastReviewMillis, s.dueMillis),
            VerbAnswerEntity(
                itemId = item.id,
                tense = item.tense.name,
                correct = correct,
                answeredMillis = now,
                day = Instant.ofEpochMilli(now).atZone(zone()).toLocalDate().toEpochDay(),
            ),
        )
        return s.dueMillis
    }

    /** Recent answers in the current stage's tenses, oldest first. */
    suspend fun recentInStage(stage: Stage): List<Boolean> =
        dao.recentVerbAnswers(stage.tenses.map { it.name }, StageProgress.WINDOW).reversed()
}

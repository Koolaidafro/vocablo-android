package app.vocablo.srs

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/** How well the learner remembered. A left swipe is AGAIN, a right swipe is GOOD. */
enum class Rating(val value: Int) { AGAIN(1), HARD(2), GOOD(3), EASY(4) }

/**
 * Memory state of one card. A card that has never been reviewed has [reps] = 0.
 * Times are epoch milliseconds so this module needs no date library.
 */
data class ReviewState(
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val lastReviewMillis: Long? = null,
    val dueMillis: Long = 0L,
) {
    val isNew: Boolean get() = reps == 0
}

/**
 * FSRS (Free Spaced Repetition Scheduler), version 4.5 formulas with the published
 * default parameters. Targets 90% recall at the moment a card comes due.
 */
class Fsrs(
    private val w: DoubleArray = DEFAULT_WEIGHTS,
    private val targetRetention: Double = 0.9,
    private val maxIntervalDays: Int = 36_500,
) {
    init {
        require(w.size == 17) { "FSRS 4.5 needs 17 weights" }
        require(targetRetention in 0.7..0.99)
    }

    /** Probability of recall [elapsedDays] after the last review. */
    fun retrievability(elapsedDays: Double, stability: Double): Double =
        (1 + FACTOR * elapsedDays / stability).pow(DECAY)

    /** Days until recall probability falls to the target retention. */
    fun intervalDays(stability: Double): Int {
        val days = stability / FACTOR * (targetRetention.pow(1 / DECAY) - 1)
        return days.roundToLong().toInt().coerceIn(1, maxIntervalDays)
    }

    fun review(state: ReviewState, rating: Rating, nowMillis: Long): ReviewState {
        val (stability, difficulty) = if (state.isNew) {
            initialStability(rating) to initialDifficulty(rating)
        } else {
            val elapsed = max(0.0, (nowMillis - (state.lastReviewMillis ?: nowMillis)) / DAY_MS.toDouble())
            val r = retrievability(elapsed, state.stability)
            val d = nextDifficulty(state.difficulty, rating)
            val s = if (rating == Rating.AGAIN) {
                forgetStability(state.difficulty, state.stability, r)
            } else {
                recallStability(state.difficulty, state.stability, r, rating)
            }
            s to d
        }
        val due = if (rating == Rating.AGAIN) {
            nowMillis + RELEARN_MS // see it again later in this session
        } else {
            nowMillis + intervalDays(stability) * DAY_MS
        }
        return ReviewState(
            stability = stability,
            difficulty = difficulty,
            reps = state.reps + 1,
            lapses = state.lapses + if (rating == Rating.AGAIN && !state.isNew) 1 else 0,
            lastReviewMillis = nowMillis,
            dueMillis = due,
        )
    }

    private fun initialStability(r: Rating) = max(w[r.value - 1], 0.1)

    private fun initialDifficulty(r: Rating) = clampD(w[4] - (r.value - 3) * w[5])

    private fun nextDifficulty(d: Double, r: Rating): Double {
        val next = d - w[6] * (r.value - 3)
        return clampD(w[7] * initialDifficulty(Rating.GOOD) + (1 - w[7]) * next)
    }

    private fun recallStability(d: Double, s: Double, r: Double, rating: Rating): Double {
        val hardPenalty = if (rating == Rating.HARD) w[15] else 1.0
        val easyBonus = if (rating == Rating.EASY) w[16] else 1.0
        return s * (1 + exp(w[8]) * (11 - d) * s.pow(-w[9]) * (exp((1 - r) * w[10]) - 1) * hardPenalty * easyBonus)
    }

    private fun forgetStability(d: Double, s: Double, r: Double): Double =
        min(s, w[11] * d.pow(-w[12]) * ((s + 1).pow(w[13]) - 1) * exp((1 - r) * w[14]))

    private fun clampD(d: Double) = d.coerceIn(1.0, 10.0)

    companion object {
        const val DAY_MS = 86_400_000L
        const val RELEARN_MS = 10 * 60_000L
        private const val DECAY = -0.5
        private const val FACTOR = 19.0 / 81.0

        val DEFAULT_WEIGHTS = doubleArrayOf(
            0.4872, 1.4003, 3.7145, 13.8206, 5.1618, 1.2298, 0.8975, 0.031, 1.6474,
            0.1367, 1.0461, 2.1072, 0.0793, 0.3246, 1.587, 0.2272, 2.8755,
        )
    }
}

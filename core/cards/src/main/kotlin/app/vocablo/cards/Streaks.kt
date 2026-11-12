package app.vocablo.cards

/** Streak math on local calendar days (days since 1970-01-01 in the learner's time zone). */
object Streaks {

    /**
     * Consecutive study days ending today. If today has no study yet, the streak
     * that ended yesterday still counts, because the learner can still save it.
     */
    fun current(studyDays: Set<Long>, today: Long): Int {
        var day = if (today in studyDays) today else today - 1
        var count = 0
        while (day in studyDays) {
            count++
            day--
        }
        return count
    }

    /** True when the learner has a streak going but hasn't studied today. */
    fun atRisk(studyDays: Set<Long>, today: Long): Boolean = today !in studyDays && (today - 1) in studyDays

    fun longest(studyDays: Set<Long>): Int {
        var best = 0
        for (d in studyDays) {
            if (d - 1 in studyDays) continue
            var n = 0
            while (d + n in studyDays) n++
            best = maxOf(best, n)
        }
        return best
    }
}

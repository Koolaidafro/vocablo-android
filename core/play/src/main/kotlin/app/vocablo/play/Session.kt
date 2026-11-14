// Vocablo: Spanish flashcards for Android.
//
// The end-of-session summary and the welcome-back check (FR-23, FR-24).
package app.vocablo.play

/** What the finish screen suggests doing next. */
enum class NextStep(val label: String) {
    DRILL_VERBS("Drill your verbs"),
    PLAY_GAME("Play a game"),
    BUILD_SENTENCES("Build a sentence"),
    DONE_FOR_TODAY("Done for today"),
}

/**
 * Builds the session summary and picks one next step.
 *
 * Design: pure functions over plain numbers, so the screen only renders the answer and the
 * choice of next step is tested on its own. One suggestion, not a list, keeps the decision
 * easy and avoids an endless feed (NFR-7).
 */
object SessionSummary {

    /** XP for a session from its answer counts, using the same weights as the XP total. */
    fun xp(newAnswers: Int, reviewAnswers: Int): Int = newAnswers * Xp.CARD_REVIEW_NEW + reviewAnswers * Xp.CARD_REVIEW

    /** How many of today's quests are done. */
    fun questsDone(quests: List<Quest>, today: DayActivity): Int = quests.count { it.done(today) }

    /**
     * Verbs that are due come first, then an unfinished game or sentence quest, otherwise the
     * learner has done enough for today and is told so.
     */
    fun nextStep(dueVerbs: Int, quests: List<Quest>, today: DayActivity): NextStep {
        val open = quests.filterNot { it.done(today) }.map { it.kind }
        return when {
            dueVerbs > 0 -> NextStep.DRILL_VERBS
            QuestKind.PLAY_GAME in open -> NextStep.PLAY_GAME
            QuestKind.BUILD_SENTENCES in open -> NextStep.BUILD_SENTENCES
            else -> NextStep.DONE_FOR_TODAY
        }
    }
}

/** Decides when Home greets a returning learner (FR-24). */
object WelcomeBack {
    /** Days away before the welcome-back card shows. */
    const val AWAY_DAYS = 3

    /**
     * True when the learner has studied before but not in the last [AWAY_DAYS] days. A new
     * learner who never studied sees onboarding instead, so they get false.
     */
    fun show(studyDays: Set<Long>, today: Long): Boolean {
        val last = studyDays.filter { it <= today }.maxOrNull() ?: return false
        return today - last >= AWAY_DAYS
    }
}

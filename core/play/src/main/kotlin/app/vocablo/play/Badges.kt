// Vocablo: Spanish flashcards for Android.
//
// Badges: one-time achievements for reaching a milestone (FR-22).
package app.vocablo.play

/**
 * Everything a badge can depend on, read once from the database.
 *
 * Design: a plain snapshot keeps every badge rule a pure function, so the rules are tested
 * without a database and a new badge only needs a new field here when it needs new data.
 */
data class Milestones(
    val studyDays: Int = 0,
    val longestStreak: Int = 0,
    val cardsLearned: Int = 0,
    val goldVerbs: Int = 0,
    val chestsOpened: Int = 0,
)

/**
 * The six badges. Each entry carries its title, a short line for the Me screen, and its own
 * condition.
 *
 * Design: an enum whose entries hold their rule, like [QuestKind]. Adding a badge is one entry
 * (Open/Closed), and screens list [entries] instead of naming badges one by one.
 * Badges only recognise progress and never give XP, so XP keeps tracking real practice (NFR-7).
 */
enum class Badge(val title: String, val howToEarn: String, val earned: (Milestones) -> Boolean) {
    FIRST_STEPS("First steps", "Finish your first study session.", { it.studyDays >= 1 }),
    STREAK_7("On a roll", "Reach a 7-day streak.", { it.longestStreak >= 7 }),
    STREAK_30("Unstoppable", "Reach a 30-day streak.", { it.longestStreak >= 30 }),
    WORDS_100("Cien palabras", "Learn 100 cards.", { it.cardsLearned >= 100 }),
    GOLD_VERB("Oro", "Earn a gold medal on a verb.", { it.goldVerbs >= 1 }),
    TREASURE("Treasure hunter", "Open a weekly chest.", { it.chestsOpened >= 1 }),
}

/** Which badges a learner has reached. */
object Badges {
    /** Every badge whose milestone is reached, in display order. */
    fun reached(milestones: Milestones): List<Badge> = Badge.entries.filter { it.earned(milestones) }

    /**
     * Badges reached now that were not already awarded, in display order. Badges are never
     * taken away, so a badge in [alreadyAwarded] stays earned even if its milestone no longer
     * holds (for example after cards are deleted).
     */
    fun newlyReached(milestones: Milestones, alreadyAwarded: Set<Badge>): List<Badge> =
        reached(milestones).filter { it !in alreadyAwarded }
}

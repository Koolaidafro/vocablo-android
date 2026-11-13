// Vocablo: Spanish flashcards for Android.
//
// When Tuzo may send a reminder, and whether a reminder that comes due should show (FR-26).
package app.vocablo.play

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * When Tuzo may nudge the learner. Never more than two a day: a daily reminder at the hour
 * they chose, and a streak saver in the evening that only fires if they haven't studied.
 *
 * Design: a pure policy object. The Android receiver asks [decide] and only acts on the answer,
 * so every limit (NFR-7) and the comeback rule (FR-26) is unit-tested without a phone.
 */
object Reminders {
    const val STREAK_SAVER_HOUR = 20

    /** After this many days without the app being opened, reminders stop (FR-26). */
    const val QUIET_AFTER_DAYS = 7

    enum class Kind { DAILY, STREAK_SAVER }

    /** What to do when a reminder slot fires. */
    enum class Decision { SHOW, COMEBACK, QUIET }

    data class Slot(val kind: Kind, val at: LocalDateTime)

    /** The next slot strictly after [now]. The streak saver is skipped when it would land within two hours of the daily one. */
    fun next(now: LocalDateTime, dailyHour: Int): Slot {
        val hour = dailyHour.coerceIn(0, 23)
        val saverToo = kotlin.math.abs(STREAK_SAVER_HOUR - hour) >= 2
        var date: LocalDate = now.toLocalDate()
        while (true) {
            val slots = buildList {
                add(Slot(Kind.DAILY, date.atTime(LocalTime.of(hour, 0))))
                if (saverToo) add(Slot(Kind.STREAK_SAVER, date.atTime(LocalTime.of(STREAK_SAVER_HOUR, 0))))
            }.sortedBy { it.at }
            slots.firstOrNull { it.at.isAfter(now) }?.let { return it }
            date = date.plusDays(1)
        }
    }

    /** Whether a slot that just fired should actually show a notification. */
    fun shouldShow(kind: Kind, studiedToday: Boolean, streak: Int, dueCount: Int): Boolean = when (kind) {
        Kind.DAILY -> !studiedToday && (dueCount > 0 || streak > 0)
        Kind.STREAK_SAVER -> !studiedToday && streak > 0
    }

    /**
     * Decides what a slot that just fired should do. After [QUIET_AFTER_DAYS] days without the
     * app being opened, one comeback message goes out and then nothing until the app is opened
     * again, so a learner who stepped away is never nagged (FR-26).
     */
    fun decide(
        kind: Kind,
        studiedToday: Boolean,
        streak: Int,
        dueCount: Int,
        daysSinceOpened: Long,
        comebackSent: Boolean,
    ): Decision = when {
        daysSinceOpened >= QUIET_AFTER_DAYS -> if (comebackSent) Decision.QUIET else Decision.COMEBACK
        shouldShow(kind, studiedToday, streak, dueCount) -> Decision.SHOW
        else -> Decision.QUIET
    }
}

package app.vocablo.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.vocablo.container
import app.vocablo.play.ReminderMessages
import app.vocablo.play.Reminders
import app.vocablo.play.StreakFreezes
import app.vocablo.widget.TodayWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules Tuzo's reminders with inexact alarms (no exact-alarm permission needed, and kind to
 * the battery). Only one alarm is ever pending; when it fires it posts if it should and books the next.
 */
object ReminderScheduler {
    private const val EXTRA_KIND = "kind"

    fun schedule(context: Context, on: Boolean, hour: Int) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.cancel(pending(context, null))
        if (!on) return
        val zone = ZoneId.systemDefault()
        val slot = Reminders.next(LocalDateTime.now(zone), hour)
        val at = slot.at.atZone(zone).toInstant().toEpochMilli()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context, slot.kind))
    }

    /** Re-reads the saved settings and books the next reminder. */
    suspend fun reschedule(context: Context) {
        val settings = context.container.settings.settings.first()
        schedule(context, settings.remindersOn, settings.reminderHour)
    }

    private fun pending(context: Context, kind: Reminders.Kind?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        if (kind != null) intent.putExtra(EXTRA_KIND, kind.name)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    internal fun kindOf(intent: Intent): Reminders.Kind =
        Reminders.Kind.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_KIND) } ?: Reminders.Kind.DAILY
}

/**
 * Fires at a reminder slot. Asks Reminders.decide what to do, so the receiver holds no rules
 * of its own: it shows a reminder, the one comeback message, or nothing (FR-25, FR-26).
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        context.container.scope.launch {
            try {
                if (context.container.settings.settings.first().remindersOn) remind(context, ReminderScheduler.kindOf(intent))
                ReminderScheduler.reschedule(context)
                TodayWidget.refresh(context) // a new day may have started
            } finally {
                result.finish()
            }
        }
    }

    /** Gathers today's numbers, asks the policy, and posts what it says. */
    private suspend fun remind(context: Context, kind: Reminders.Kind) {
        val c = context.container
        val settings = c.settings.settings.first()
        val today = c.repository.today()
        val study = c.repository.studyDays().first()
        val streak = StreakFreezes.streak(study, c.progress.frozenDays().first(), today)
        val due = c.repository.deckSummaries().first().sumOf { it.dueCount } + c.verbs.dueCount().first()
        // A phone that never recorded an open (first run of v1.2) counts as opened today.
        val sinceOpened = if (settings.lastOpenedDay < 0) 0 else today - settings.lastOpenedDay
        when (Reminders.decide(kind, today in study, streak, due, sinceOpened, settings.comebackSent)) {
            Reminders.Decision.SHOW -> {
                val pool = ReminderMessages.kindFor(kind == Reminders.Kind.STREAK_SAVER, due, streak)
                Notifications.showStudyReminder(context, ReminderMessages.pick(pool, today, due, streak))
            }
            Reminders.Decision.COMEBACK -> {
                val message = ReminderMessages.pick(ReminderMessages.Kind.COMEBACK, today, due, streak)
                if (Notifications.showStudyReminder(context, message)) c.settings.update { it.copy(comebackSent = true) }
            }
            Reminders.Decision.QUIET -> Unit
        }
    }
}

/** Alarms are forgotten on reboot and after the clock or app changes, so book the next one again. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        context.container.scope.launch {
            try {
                ReminderScheduler.reschedule(context)
            } finally {
                result.finish()
            }
        }
    }
}

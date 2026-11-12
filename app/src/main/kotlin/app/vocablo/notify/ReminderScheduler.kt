package app.vocablo.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.vocablo.container
import app.vocablo.play.Reminders
import app.vocablo.play.StreakFreezes
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

/** Fires at a reminder slot: posts the nudge only when the learner hasn't studied yet today. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val c = context.container
        c.scope.launch {
            try {
                val settings = c.settings.settings.first()
                if (settings.remindersOn) {
                    val today = c.repository.today()
                    val study = c.repository.studyDays().first()
                    val frozen = c.progress.frozenDays().first()
                    val streak = StreakFreezes.streak(study, frozen, today)
                    val decks = c.repository.deckSummaries().first()
                    val due = decks.sumOf { it.dueCount } + c.verbs.dueCount().first()
                    val kind = ReminderScheduler.kindOf(intent)
                    if (Reminders.shouldShow(kind, today in study, streak, due)) {
                        Notifications.showStudyReminder(context, due, streak)
                    }
                }
                ReminderScheduler.reschedule(context)
            } finally {
                result.finish()
            }
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

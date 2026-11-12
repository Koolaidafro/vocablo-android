package app.vocablo.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import app.vocablo.MainActivity
import app.vocablo.R

/**
 * Study reminders. Every notification shows Tuzo twice: the white gopher silhouette in the
 * status bar, and the full-color gopher as the large picture beside the text.
 */
object Notifications {

    const val CHANNEL_REMINDERS = "study_reminders"
    private const val ID_REMINDER = 1
    private const val BRAND_COLOR = 0xFF1E6B58.toInt()

    fun createChannels(context: Context) {
        val channel = NotificationChannel(CHANNEL_REMINDERS, "Study reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "A nudge from Tuzo when cards are due or your streak needs saving. At most two a day."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    /** Posts the reminder. Returns false when notifications are not allowed. */
    fun showStudyReminder(context: Context, dueCount: Int, streak: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val tuzo = ContextCompat.getDrawable(context, R.drawable.tuzo)?.toBitmap(256, 256)
        val (title, text) = reminderText(dueCount, streak)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_tuzo)
            .setLargeIcon(tuzo)
            .setColor(BRAND_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_REMINDER, notification)
        return true
    }

    fun reminderText(dueCount: Int, streak: Int): Pair<String, String> = when {
        streak > 0 && dueCount > 0 -> "Keep your $streak-day streak alive" to "Tuzo has $dueCount ${cards(dueCount)} ready. Two minutes is enough."
        dueCount > 0 -> "¡Hola! Tuzo has $dueCount ${cards(dueCount)} for you" to "A quick swipe session keeps them fresh."
        else -> "Tuzo dug up something new" to "Add a few words you saw today and they'll be ready tomorrow."
    }

    private fun cards(n: Int) = if (n == 1) "card" else "cards"
}

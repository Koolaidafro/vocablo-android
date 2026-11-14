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
import app.vocablo.play.Message

/**
 * Study reminders. Every notification shows Tuzo twice: the white gopher silhouette in the
 * status bar, and the full-colour Tuzo art as the large picture beside the text (FR-18).
 *
 * Design: this object only posts what it is given. What to say comes from ReminderMessages
 * and whether to say it from Reminders, both in core:play, so the wording and limits are tested
 * without Android (FR-25, FR-26).
 */
object Notifications {

    const val CHANNEL_REMINDERS = "study_reminders"
    private const val ID_REMINDER = 1
    private const val BRAND_COLOR = 0xFF1E6B58.toInt()

    /** Size of Tuzo's picture beside the text; Android shows it at about 64 dp. */
    private const val LARGE_ICON_PX = 256

    fun createChannels(context: Context) {
        val channel = NotificationChannel(CHANNEL_REMINDERS, "Study reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "A friendly nudge from Tuzo when cards are due or your streak could use a session. At most two a day."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    /** Posts [message] as the reminder. Returns false when notifications are not allowed. */
    fun showStudyReminder(context: Context, message: Message): Boolean {
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
        val tuzo = ContextCompat.getDrawable(context, R.drawable.tuzo_idle)?.toBitmap(LARGE_ICON_PX, LARGE_ICON_PX)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_tuzo)
            .setLargeIcon(tuzo)
            .setColor(BRAND_COLOR)
            .setContentTitle(message.title)
            .setContentText(message.text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_REMINDER, notification)
        return true
    }
}

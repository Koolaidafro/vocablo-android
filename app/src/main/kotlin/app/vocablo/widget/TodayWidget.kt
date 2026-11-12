package app.vocablo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import app.vocablo.MainActivity
import app.vocablo.R
import app.vocablo.container
import app.vocablo.play.DailyQuests
import app.vocablo.play.StreakFreezes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the widget says, worked out from the learner's numbers. Kept separate so it can be tested. */
object WidgetText {
    fun title(streak: Int): String = if (streak > 0) "🔥 $streak-day streak" else "¡Hola! Start a streak"

    fun detail(due: Int, questsDone: Int, questsTotal: Int, studiedToday: Boolean): String {
        val cards = when {
            due > 0 -> "$due ${if (due == 1) "card" else "cards"} ready"
            studiedToday -> "All caught up"
            else -> "Nothing due. Add a new word?"
        }
        return "$cards · quests $questsDone/$questsTotal"
    }
}

/** Home-screen widget: streak, cards ready and quest progress. Tapping it opens the app. */
class TodayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val result = goAsync()
        context.container.scope.launch {
            try {
                render(context, manager, ids)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        /** Asks every placed widget to redraw; cheap and safe to call often. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, TodayWidget::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
            )
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val c = context.container
            val today = c.repository.today()
            val study = c.repository.studyDays().first()
            val frozen = c.progress.frozenDays().first()
            val streak = StreakFreezes.streak(study, frozen, today)
            val due = c.repository.deckSummaries().first().sumOf { it.dueCount } + c.verbs.dueCount().first()
            val settings = c.settings.settings.first()
            val activity = c.progress.todayActivity().first()
            val quests = DailyQuests.forDay(today, settings.newPerDay)

            val open = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val tuzo = ContextCompat.getDrawable(context, R.drawable.tuzo)?.toBitmap(144, 144)
            val views = RemoteViews(context.packageName, R.layout.widget_today).apply {
                setTextViewText(R.id.widget_title, WidgetText.title(streak))
                setTextViewText(R.id.widget_detail, WidgetText.detail(due, quests.count { it.done(activity) }, quests.size, today in study))
                if (tuzo != null) setImageViewBitmap(R.id.widget_tuzo, tuzo)
                setOnClickPendingIntent(R.id.widget_root, open)
            }
            manager.updateAppWidget(ids, views)
        }
    }
}

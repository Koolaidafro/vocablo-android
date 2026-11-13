package app.vocablo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.vocablo.ui.VocabloApp
import app.vocablo.widget.TodayWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { VocabloApp() }
    }

    override fun onStart() {
        super.onStart()
        container.scope.launch { container.updates.checkIfStale() }
        container.scope.launch { recordOpened() }
    }

    /** Remembers today as the last day the app was opened, which restarts reminders after a comeback (FR-26). */
    private suspend fun recordOpened() {
        val today = container.repository.today()
        container.settings.update { if (it.lastOpenedDay == today && !it.comebackSent) it else it.copy(lastOpenedDay = today, comebackSent = false) }
    }

    override fun onStop() {
        super.onStop()
        TodayWidget.refresh(this) // the learner probably just studied
    }
}

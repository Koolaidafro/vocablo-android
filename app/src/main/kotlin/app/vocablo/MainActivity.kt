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
    }

    override fun onStop() {
        super.onStop()
        TodayWidget.refresh(this) // the learner probably just studied
    }
}

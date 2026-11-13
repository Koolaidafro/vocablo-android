// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.study

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.vocablo.play.Game
import app.vocablo.play.NextStep
import app.vocablo.ui.Screen
import app.vocablo.ui.theme.VocabloTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks the end-of-session screen shows the summary and one next step (FR-23).
 *
 * Design: FinishedScreen takes plain values, so it is tested without a database or ViewModel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SessionFinishTest {

    /** Hosts the composable under test. */
    @get:Rule
    val compose = createComposeRule()

    private fun summary(next: NextStep) =
        Summary(xp = 26, questsDone = 2, questsTotal = 3, streak = 5, next = next, mayOfferReminders = false)

    @Test
    fun showsXpQuestsStreakAndTheSuggestedStep() {
        var chosen: NextStep? = null
        compose.setContent {
            VocabloTheme {
                FinishedScreen(learned = 10, missed = 2, summary = summary(NextStep.DRILL_VERBS), onNext = { chosen = it }, onReminderOfferAnswered = {})
            }
        }
        compose.onNodeWithText("+26").assertIsDisplayed()
        compose.onNodeWithText("2/3").assertIsDisplayed()
        compose.onNodeWithText("5").assertIsDisplayed()
        compose.onNodeWithText("Next: Drill your verbs").performClick()
        assertEquals(NextStep.DRILL_VERBS, chosen)
    }

    @Test
    fun aFullDayEndsWithDoneInsteadOfAnotherTask() {
        var chosen: NextStep? = null
        compose.setContent {
            VocabloTheme {
                FinishedScreen(learned = 3, missed = 0, summary = summary(NextStep.DONE_FOR_TODAY), onNext = { chosen = it }, onReminderOfferAnswered = {})
            }
        }
        compose.onNodeWithText("That's plenty for today. Tuzo is proud of you.").assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        assertEquals(NextStep.DONE_FOR_TODAY, chosen)
    }

    @Test
    fun eachNextStepOpensItsScreen() {
        assertEquals(Screen.Drill, nextStepScreen(NextStep.DRILL_VERBS))
        assertEquals(Screen.Sentences, nextStepScreen(NextStep.BUILD_SENTENCES))
        assertEquals(Screen.Game(Game.entries[2]), nextStepScreen(NextStep.PLAY_GAME, today = 2L + Game.entries.size))
        assertNull(nextStepScreen(NextStep.DONE_FOR_TODAY))
    }
}

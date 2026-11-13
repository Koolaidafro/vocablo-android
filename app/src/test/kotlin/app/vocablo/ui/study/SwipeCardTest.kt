// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.study

import android.app.Application
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import app.vocablo.cards.Card
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks that a swipe means the same thing on both sides of the card (FR-11).
 *
 * Design: drives the real [SwipeCard] with Compose's touch injection, because the bug this
 * guards against lived in how the gesture reads coordinates through the flip rotation, which
 * a pure function could not reproduce.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SwipeCardTest {

    /** Hosts the composable under test. */
    @get:Rule
    val compose = createComposeRule()

    /** A plain word card with no example, so the card's content stays small. */
    private val card = Card(deckId = 1, spanish = "hola", english = "hello")

    @Test
    fun swipeRightIsGotItOnTheFront() = assertEquals(true, swipe(flipped = false, right = true))

    @Test
    fun swipeLeftIsAgainOnTheFront() = assertEquals(false, swipe(flipped = false, right = false))

    @Test
    fun swipeRightIsStillGotItAfterFlipping() = assertEquals(true, swipe(flipped = true, right = true))

    @Test
    fun swipeLeftIsStillAgainAfterFlipping() = assertEquals(false, swipe(flipped = true, right = false))

    /**
     * Shows the card on the given side, swipes across it, and returns what the card reported:
     * true for "Got it", false for "Again", null if no swipe was reported.
     */
    private fun swipe(flipped: Boolean, right: Boolean): Boolean? {
        var reported: Boolean? = null
        compose.setContent {
            MaterialTheme {
                SwipeCard(
                    card = card,
                    flipped = flipped,
                    turn = 0,
                    onFlip = {},
                    onSwiped = { reported = it },
                    modifier = Modifier.size(320.dp, 480.dp).testTag(CARD),
                )
            }
        }
        compose.onNodeWithTag(CARD).performTouchInput { if (right) swipeRight() else swipeLeft() }
        compose.waitForIdle()
        return reported
    }

    private companion object {
        /** Test tag on the card's slot. */
        const val CARD = "card"
    }
}

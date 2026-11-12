// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.study

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks that the four rating labels each fit on one line (FR-2).
 *
 * Design: measures the real text layout through the semantics tree, so the test fails on
 * the actual symptom (a word broken over two lines or cut off) rather than on padding values.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RatingBarTest {

    /** Hosts the composable under test. */
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun labelsFitOnANarrowPhone() = assertLabelsFit(rowWidthDp = 328, fontScale = 1f)

    @Test
    fun labelsFitWithALargerSystemFont() = assertLabelsFit(rowWidthDp = 328, fontScale = 1.3f)

    @Test
    fun labelsFitOnTheSmallestSupportedPhone() = assertLabelsFit(rowWidthDp = 288, fontScale = 1.15f)

    /** Lays out the flipped rating bar at the given width and font scale and checks each label. */
    private fun assertLabelsFit(rowWidthDp: Int, fontScale: Float) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    Box(Modifier.width(rowWidthDp.dp)) {
                        RatingBar(flipped = true, onFlip = {}, onRate = {})
                    }
                }
            }
        }
        LABELS.forEach { label ->
            val layout = layoutOf(label)
            assertEquals("\"$label\" should be on one line", 1, layout.lineCount)
            assertFalse("\"$label\" should not be cut off", layout.hasVisualOverflow)
        }
    }

    /** Reads the text layout Compose produced for the node showing [label]. */
    private fun layoutOf(label: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val node = compose.onNodeWithText(label).fetchSemanticsNode()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first()
    }

    private companion object {
        /** The labels on the flipped rating bar, left to right. */
        val LABELS = listOf("Again", "Hard", "Good", "Easy")
    }
}

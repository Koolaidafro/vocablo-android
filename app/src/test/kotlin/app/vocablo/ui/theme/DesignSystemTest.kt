// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.theme

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import app.vocablo.design.ThemeId
import app.vocablo.design.ThemeMode
import app.vocablo.ui.components.CelebrationArt
import app.vocablo.ui.components.EmptyDeckArt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks the app-wide design system: type, shapes, icons and illustrations (FR-13 to FR-16).
 *
 * Design: tests the shared pieces once, so screens that use them don't each need a visual test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class DesignSystemTest {

    /** Hosts composables for the illustration checks. */
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everyTextStyleIsSetInNunito() {
        styles().forEach { (name, style) -> assertEquals("$name should use Nunito", Nunito, style.fontFamily) }
    }

    @Test
    fun headingsAreExtraBold() {
        with(VocabloTypography) {
            listOf(displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall).forEach {
                assertEquals(FontWeight.ExtraBold, it.fontWeight)
            }
        }
    }

    @Test
    fun cornersGetRounderFromSmallToLarge() {
        val radii = with(VocabloShapes) { listOf(extraSmall, small, medium, large, extraLarge) }
            .map { it.topStart.toPx(shapeSize = Size(SHAPE_SIDE, SHAPE_SIDE), density = Density(1f)) }
        assertEquals(radii.sorted(), radii)
        assertEquals(radii.size, radii.toSet().size)
    }

    @Test
    fun everyAppIconLoads() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val ids = AppIcons::class.java.declaredFields
            .filter { it.type == Int::class.javaPrimitiveType }
            .map { it.isAccessible = true; it.getInt(AppIcons) }
        assertTrue("AppIcons should list the app's icons", ids.size >= MIN_ICONS)
        ids.forEach { assertNotNull(ContextCompat.getDrawable(context, it)) }
    }

    @Test
    fun illustrationsDrawInEveryThemeLightAndDark() {
        ThemeId.entries.forEach { theme ->
            listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                compose.setContent {
                    VocabloTheme(theme, mode) {
                        Column {
                            EmptyDeckArt()
                            CelebrationArt { }
                        }
                    }
                }
                compose.onNodeWithContentDescription("An empty deck").assertIsDisplayed()
            }
        }
    }

    /** Every style in the type scale, by name. */
    private fun styles() = with(VocabloTypography) {
        listOf(
            "displayLarge" to displayLarge, "displayMedium" to displayMedium, "displaySmall" to displaySmall,
            "headlineLarge" to headlineLarge, "headlineMedium" to headlineMedium, "headlineSmall" to headlineSmall,
            "titleLarge" to titleLarge, "titleMedium" to titleMedium, "titleSmall" to titleSmall,
            "bodyLarge" to bodyLarge, "bodyMedium" to bodyMedium, "bodySmall" to bodySmall,
            "labelLarge" to labelLarge, "labelMedium" to labelMedium, "labelSmall" to labelSmall,
        )
    }

    private companion object {
        /** The fewest icons the set can have: the five tabs plus the common actions. */
        const val MIN_ICONS = 10

        /** A shape size large enough that no corner is clamped. */
        const val SHAPE_SIDE = 1000f
    }
}

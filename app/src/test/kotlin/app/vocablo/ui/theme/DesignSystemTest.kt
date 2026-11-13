// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.theme

import android.app.Application
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import app.vocablo.play.Badge
import app.vocablo.play.Game
import app.vocablo.ui.components.Scene
import app.vocablo.ui.components.TuzoMood
import app.vocablo.ui.components.badgeArt
import app.vocablo.ui.components.gameArt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks the app-wide design system: type, shapes, icons and the generated art (FR-13, FR-14,
 * FR-16, FR-17, FR-19, FR-20, FR-22).
 *
 * Design: tests the shared pieces once, so screens that use them don't each need a visual test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class DesignSystemTest {

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
            // Skip compiler-made fields such as Compose's "$stable".
            .filter { it.type == Int::class.javaPrimitiveType && !it.name.startsWith("$") }
            .map { it.isAccessible = true; it.getInt(AppIcons) }
        assertTrue("AppIcons should list the app's icons", ids.size >= MIN_ICONS)
        ids.forEach { assertNotNull(ContextCompat.getDrawable(context, it)) }
    }

    @Test
    fun everyTuzoMoodHasItsOwnPose() {
        val poses = TuzoMood.entries.map { it.drawable }
        assertEquals("each mood should show a different drawing", poses.size, poses.toSet().size)
        poses.forEach { assertNotNull(ContextCompat.getDrawable(context(), it)) }
        TuzoMood.entries.forEach { assertTrue(it.description.startsWith("Tuzo")) }
    }

    @Test
    fun everySceneBadgeAndGameTileLoads() {
        val art = Scene.entries.map { it.drawable } + Badge.entries.map(::badgeArt) + Game.entries.map(::gameArt)
        assertEquals("no two pieces of art should share a drawing", art.size, art.toSet().size)
        art.forEach { assertNotNull(ContextCompat.getDrawable(context(), it)) }
        Scene.entries.forEach { assertTrue("${it.name} needs words for screen readers", it.description.isNotBlank()) }
    }

    private fun context() = ApplicationProvider.getApplicationContext<Application>()

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

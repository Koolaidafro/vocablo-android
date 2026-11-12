package app.vocablo.design

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class ThemesTest {

    @Test
    fun `there are exactly five themes and Jade is the default`() {
        assertEquals(ThemeId.entries.toSet(), Themes.all.map { it.id }.toSet())
        assertEquals(5, Themes.all.size)
        assertEquals(ThemeId.JADE, Themes.default)
    }

    @Test
    fun `light modes are white based`() {
        Themes.all.forEach { assertEquals(0xFFFFFFFF, it.light.surface, "${it.displayName} light surface") }
    }

    @Test
    fun `system mode follows the device and explicit modes ignore it`() {
        val jade = Themes.byId(ThemeId.JADE)
        assertSame(jade.dark, jade.paletteFor(ThemeMode.SYSTEM, systemIsDark = true))
        assertSame(jade.light, jade.paletteFor(ThemeMode.SYSTEM, systemIsDark = false))
        assertSame(jade.light, jade.paletteFor(ThemeMode.LIGHT, systemIsDark = true))
        assertSame(jade.dark, jade.paletteFor(ThemeMode.DARK, systemIsDark = false))
    }

    /** Every text-on-color pair must meet WCAG AA (4.5:1) in both modes of every theme. */
    @TestFactory
    fun `text stays readable in every theme and mode`(): List<DynamicTest> =
        Themes.all.flatMap { theme ->
            listOf("light" to theme.light, "dark" to theme.dark).flatMap { (mode, p) ->
                listOf(
                    Triple("onPrimary on primary", p.onPrimary, p.primary),
                    Triple("onPrimaryContainer", p.onPrimaryContainer, p.primaryContainer),
                    Triple("onSecondaryContainer", p.onSecondaryContainer, p.secondaryContainer),
                    Triple("onTertiaryContainer", p.onTertiaryContainer, p.tertiaryContainer),
                    Triple("onSurface on surface", p.onSurface, p.surface),
                    Triple("onSurface on surfaceContainerHigh", p.onSurface, p.surfaceContainerHigh),
                    Triple("onSurfaceVariant on surface", p.onSurfaceVariant, p.surface),
                ).map { (name, fg, bg) ->
                    DynamicTest.dynamicTest("${theme.displayName} $mode: $name") {
                        val ratio = Contrast.ratio(fg, bg)
                        assertTrue(ratio >= 4.5, "contrast %.2f is below 4.5".format(ratio))
                    }
                }
            }
        }

    @Test
    fun `contrast math matches known values`() {
        assertEquals(21.0, Contrast.ratio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, Contrast.ratio(0xFF777777, 0xFF777777), 0.001)
    }
}

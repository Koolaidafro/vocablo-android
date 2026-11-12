package app.vocablo.widget

import app.vocablo.data.Region
import app.vocablo.speech.Voices
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PolishTest {
    @Test
    fun widgetTextReadsNaturally() {
        assertEquals("🔥 12-day streak", WidgetText.title(12))
        assertEquals("¡Hola! Start a streak", WidgetText.title(0))
        assertEquals("1 card ready · quests 0/3", WidgetText.detail(1, 0, 3, studiedToday = false))
        assertEquals("All caught up · quests 3/3", WidgetText.detail(0, 3, 3, studiedToday = true))
    }

    @Test
    fun voicesMatchTheRegionAndFallBackToAnySpanish() {
        assertEquals(Locale("es", "AR"), Voices.localesFor(Region.RIOPLATENSE).first())
        assertEquals(Locale("es", "ES"), Voices.localesFor(Region.SPAIN).first())
        Region.entries.forEach { assertEquals(Locale("es"), Voices.localesFor(it).last()) }
    }
}

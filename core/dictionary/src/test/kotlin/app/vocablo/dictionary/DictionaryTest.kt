package app.vocablo.dictionary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfflineDictionaryTest {

    private val dict = OfflineDictionary.starter()

    @Test
    fun `the starter pack loads with every line well formed`() {
        assertTrue(dict.size >= 150, "starter pack has ${dict.size} entries")
    }

    @Test
    fun `suggests as you type, ignoring accents and articles`() {
        assertEquals("el aguacate", dict.suggest("aguac").first().spanish)
        assertEquals("el aguacate", dict.suggest("el aguac").first().spanish)
        assertEquals(listOf("el agua", "el aguacate"), dict.suggest("agu").map { it.spanish }) // common words first
        assertEquals("el maíz", dict.suggest("maiz").single().spanish)
    }

    @Test
    fun `an exact match is listed before longer words`() {
        val hits = dict.suggest("si").map { it.spanish }
        assertEquals(setOf("sí", "si"), hits.take(2).toSet())
    }

    @Test
    fun `entries carry examples and regional notes`() {
        val entry = dict.lookup("aguacate").single()
        assertEquals(PartOfSpeech.NOUN, entry.partOfSpeech)
        assertEquals("m", entry.gender)
        assertEquals("avocado", entry.english)
        assertTrue(entry.regionNote.contains("palta"))
        assertTrue(entry.example.isNotEmpty() && entry.exampleEnglish.isNotEmpty())
    }

    @Test
    fun `blank input suggests nothing`() {
        assertTrue(dict.suggest("  ").isEmpty())
    }
}

class LayeredLookupTest {

    private val entry = DictionaryEntry("hola", PartOfSpeech.OTHER, "", "hello")

    @Test
    fun `falls through to the next source and skips failures`() {
        val failing = WordLookup { error("no connection") }
        val empty = WordLookup { emptyList() }
        val online = WordLookup { listOf(entry.copy(source = Source.ONLINE)) }
        assertEquals(Source.ONLINE, LayeredLookup(listOf(empty, failing, online)).lookup("hola").single().source)
    }

    @Test
    fun `returns nothing when every source is empty or offline`() {
        assertTrue(LayeredLookup(listOf(WordLookup { error("offline") })).lookup("hola").isEmpty())
    }
}

class WiktionaryParserTest {

    private val sample = """
        {"es":[{"partOfSpeech":"Noun","language":"Spanish","definitions":[
           {"definition":"<a href=\"/wiki/avocado\">avocado</a> (fruit)","examples":["<b>Aguacate</b> con sal."]},
           {"definition":"avocado tree"}]}],
         "en":[{"partOfSpeech":"Noun","language":"English","definitions":[{"definition":"ignored"}]}]}
    """.trimIndent()

    @Test
    fun `reads Spanish meanings and strips html`() {
        val e = WiktionaryParser.parse("aguacate", sample).single()
        assertEquals("avocado (fruit); avocado tree", e.english)
        assertEquals("Aguacate con sal.", e.example)
        assertEquals(PartOfSpeech.NOUN, e.partOfSpeech)
        assertEquals(Source.ONLINE, e.source)
    }

    @Test
    fun `bad or non-Spanish responses give nothing`() {
        assertTrue(WiktionaryParser.parse("x", "not json").isEmpty())
        assertTrue(WiktionaryParser.parse("x", """{"en":[]}""").isEmpty())
    }
}

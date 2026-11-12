package app.vocablo.cards

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class CardTextTest {

    @ParameterizedTest
    @CsvSource(
        "'el aguacate', aguacate",
        "'  El   Aguacate ', aguacate",
        "'¿Qué tal?', 'qué tal'",
        "'la cuenta', cuenta",
        "'las', las", // a lone article is a word in its own right
        "'para llevar', 'para llevar'",
    )
    fun `key ignores articles, case, spaces and punctuation`(input: String, key: String) {
        assertEquals(key, CardText.key(input))
    }

    @Test
    fun `key keeps accents so si and sí stay different`() {
        assertNotEquals(CardText.key("si"), CardText.key("sí"))
        assertEquals(CardText.looseKey("si"), CardText.looseKey("sí"))
    }

    @Test
    fun `merging meanings adds only what is new`() {
        assertEquals("avocado, avocado tree", CardText.mergeMeanings("avocado", "Avocado; avocado tree"))
    }
}

class DuplicateCheckerTest {

    private val cards = listOf(
        Card(id = 1, deckId = 10, spanish = "el aguacate", english = "avocado"),
        Card(id = 2, deckId = 11, spanish = "sí", english = "yes"),
    )
    private val checker = DuplicateChecker(cards)

    @Test
    fun `same word in another deck is an exact duplicate with the new meanings listed`() {
        val result = checker.check("Aguacate", "avocado, avocado tree")
        assertInstanceOf(DuplicateResult.Exact::class.java, result)
        result as DuplicateResult.Exact
        assertEquals(1L, result.existing.id)
        assertEquals(listOf("avocado tree"), result.newMeanings)
    }

    @Test
    fun `an accent difference is only a hint`() {
        val result = checker.check("si", "if")
        assertInstanceOf(DuplicateResult.Similar::class.java, result)
        assertEquals(2L, (result as DuplicateResult.Similar).candidates.single().id)
    }

    @Test
    fun `a new word passes, and editing a card does not flag itself`() {
        assertEquals(DuplicateResult.None, checker.check("la palta", "avocado"))
        assertEquals(DuplicateResult.None, checker.check("el aguacate", "avocado", ignoreCardId = 1))
        assertEquals(DuplicateResult.None, checker.check("   ", ""))
    }
}

class CsvCardsTest {

    @Test
    fun `reads csv with a header, quotes and optional columns`() {
        val text = "spanish,english,example,deck\n" +
            "el aguacate,avocado,\"Pon el aguacate, por favor.\",Comida\n" +
            "para llevar,to go\n" +
            "broken line\n"
        val result = CsvCards.parse(text)
        assertEquals(2, result.rows.size)
        assertEquals(ImportRow("el aguacate", "avocado", "Pon el aguacate, por favor.", "Comida"), result.rows[0])
        assertEquals(ImportRow("para llevar", "to go"), result.rows[1])
        assertEquals(listOf(4), result.skippedLines)
    }

    @Test
    fun `reads tab separated Anki exports and skips their settings lines`() {
        val text = "#separator:tab\n#html:false\nhablar\tto speak\ncomer\tto eat\n"
        val result = CsvCards.parse(text)
        assertEquals(listOf("hablar", "comer"), result.rows.map { it.spanish })
    }

    @Test
    fun `what we write we can read back`() {
        val cards = listOf(
            Card(deckId = 1, spanish = "decir \"hola\"", english = "to say \"hi\", greet", example = "Dice hola."),
        )
        val back = CsvCards.parse(CsvCards.write(cards, mapOf(1L to "Saludos"))).rows.single()
        assertEquals(ImportRow("decir \"hola\"", "to say \"hi\", greet", "Dice hola.", "Saludos"), back)
    }
}

class BackupTest {

    @Test
    fun `round trips decks, cards and review history`() {
        val backup = Backup(
            exportedMillis = 42,
            decks = listOf(Deck(id = 1, name = "Comida")),
            cards = listOf(Card(id = 5, deckId = 1, spanish = "comer", english = "to eat", type = CardType.VERB)),
            reviews = listOf(SavedReview(5, 3.7, 5.1, 1, 0, 1000L, 2000L)),
        )
        assertEquals(backup, Backup.decode(Backup.encode(backup)))
    }

    @Test
    fun `refuses backups from a newer app version with a clear message`() {
        val future = Backup.encode(Backup(exportedMillis = 1, decks = emptyList(), cards = emptyList(), reviews = emptyList()))
            .replace("\"format\":1", "\"format\":99")
        val e = assertThrows(IllegalArgumentException::class.java) { Backup.decode(future) }
        assertTrue(e.message!!.contains("newer version"))
    }
}

class StreaksTest {

    @Test
    fun `counts consecutive days ending today`() {
        assertEquals(3, Streaks.current(setOf(8L, 9L, 10L), today = 10))
    }

    @Test
    fun `yesterday's streak still counts until today ends`() {
        assertEquals(2, Streaks.current(setOf(8L, 9L), today = 10))
        assertTrue(Streaks.atRisk(setOf(8L, 9L), today = 10))
    }

    @Test
    fun `a missed day resets the streak`() {
        assertEquals(0, Streaks.current(setOf(5L, 6L), today = 10))
        assertFalse(Streaks.atRisk(setOf(5L, 6L), today = 10))
        assertEquals(2, Streaks.longest(setOf(5L, 6L, 9L)))
    }
}

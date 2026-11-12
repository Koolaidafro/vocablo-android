package app.vocablo.verbs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SpellingTest {

    @ParameterizedTest
    @CsvSource(
        "rió, 1", "guié, 1", "dió, 1", "vió, 1", "huí, 1", "fuí, 1",
        "leí, 2", "reí, 2", "envío, 3", "hablé, 2", "estáis, 2", "comíamos, 4", "continúo, 4",
    )
    fun `counts syllables the way the RAE does for accent rules`(word: String, syllables: Int) {
        assertEquals(syllables, Spelling.syllableCount(word))
    }

    @ParameterizedTest
    @CsvSource("rió, rio", "guió, guio", "dáis, dais", "véis, veis", "leí, leí", "habló, habló")
    fun `drops the written accent from one-syllable words`(input: String, output: String) {
        assertEquals(output, Spelling.dropMonosyllableAccent(input))
    }
}

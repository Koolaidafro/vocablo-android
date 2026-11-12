package app.vocablo.verbs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FormIndexTest {

    private val index = FormIndex(StarterVerbs.all)

    @Test
    fun `finds the infinitive behind a conjugated form`() {
        val hits = index.lookup("tuve")
        assertEquals(1, hits.size)
        assertEquals("tener", hits.single().verb.infinitive)
        assertEquals(Tense.PRETERITE, hits.single().tense)
        assertEquals(Person.YO, hits.single().person)
    }

    @Test
    fun `ignores case and surrounding spaces but not accents`() {
        assertEquals("hablar", index.lookup("  Hablé ").single().verb.infinitive)
        assertTrue(index.lookup("hable").isEmpty()) // present subjunctive, not indexed yet
    }

    @Test
    fun `shared forms return every match`() {
        val verbs = index.lookup("fuimos").map { it.verb.infinitive }.toSet()
        assertEquals(setOf("ser", "ir"), verbs)
    }

    @Test
    fun `an infinitive is not reported as a conjugated form`() {
        assertTrue(index.lookup("tener").isEmpty())
    }
}

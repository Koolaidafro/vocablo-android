package app.vocablo.verbs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class VerbListTest {

    private val list = VerbList.all

    @Test
    fun `holds at least 2000 unique verbs ranked from 1`() {
        assertTrue(list.size >= 2000, "only ${list.size} verbs")
        assertEquals(list.size, list.map { it.verb.infinitive }.toSet().size)
        assertEquals((1..list.size).toList(), list.map { it.rank })
        assertTrue(list.all { it.verb.english.isNotBlank() })
    }

    @Test
    fun `the most common verbs come first`() {
        val top10 = list.take(10).map { it.verb.infinitive }
        listOf("ser", "ir", "estar", "tener", "hacer").forEach { assertTrue(it in top10, "$it not in $top10") }
        assertEquals(Tier.TOP_100, VerbList.byInfinitive.getValue("hablar").tier)
    }

    @Test
    fun `tiers split the list at 100, 250, 500 and 1000`() {
        val counts = list.groupingBy { it.tier }.eachCount()
        assertEquals(100, counts[Tier.TOP_100])
        assertEquals(150, counts[Tier.TOP_250])
        assertEquals(250, counts[Tier.TOP_500])
        assertEquals(500, counts[Tier.TOP_1000])
    }

    @Test
    fun `every verb conjugates in every tense without blanks`() {
        for (rv in list) for (tense in Tense.entries) for (person in Person.entries) {
            val form = Conjugator.conjugate(rv.verb, tense, person)
            assertTrue(form.isNotBlank() && form.none { it.isWhitespace() }, "${rv.verb.infinitive} $tense $person = '$form'")
        }
    }

    @Test
    fun `agrees with the hand-checked starter verbs`() {
        for (starter in StarterVerbs.all) {
            val listed = VerbList.byInfinitive.getValue(starter.infinitive).verb
            for (tense in Tense.entries) {
                assertEquals(Conjugator.table(starter, tense), Conjugator.table(listed, tense), "${starter.infinitive} $tense")
            }
        }
    }

    @ParameterizedTest(name = "{0} {1} {2} = {3}")
    @CsvSource(
        "detener, PRESENT, YO, detengo",
        "detener, PRETERITE, EL, detuvo",
        "mantener, FUTURE, NOSOTROS, mantendremos",
        "prevenir, PRETERITE, ELLOS, previnieron",
        "suponer, PRETERITE, YO, supuse",
        "deshacer, PRETERITE, EL, deshizo",
        "satisfacer, PRETERITE, EL, satisfizo",
        "contradecir, PRESENT, YO, contradigo",
        "atraer, PRETERITE, ELLOS, atrajeron",
        "sobresalir, FUTURE, YO, sobresaldré",
        "traducir, PRETERITE, YO, traduje",
        "seducir, PRESENT, YO, seduzco",
        "sonreír, PRETERITE, EL, sonrió",
        "reír, PRETERITE, EL, rio",
        "desoír, PRESENT, TU, desoyes",
        "avergonzar, PRESENT, YO, avergüenzo",
        "degollar, PRESENT, ELLOS, degüellan",
        "averiguar, PRETERITE, YO, averigüé",
        "reunir, PRESENT, YO, reúno",
        "prohibir, PRESENT, EL, prohíbe",
        "aislar, PRESENT, YO, aíslo",
        "enviar, PRESENT, ELLOS, envían",
        "guiar, PRETERITE, YO, guie",
        "caber, PRESENT, YO, quepo",
        "saber, PRESENT, YO, sé",
        "haber, PRESENT, EL, ha",
        "maldecir, FUTURE, YO, maldeciré",
        "adquirir, PRESENT, YO, adquiero",
        "errar, PRESENT, YO, yerro",
        "oler, PRESENT, EL, huele",
        "teñir, PRETERITE, EL, tiñó",
        "gruñir, PRETERITE, ELLOS, gruñeron",
        "construir, PRETERITE, ELLOS, construyeron",
        "prever, PRESENT, YO, preveo",
        "equivaler, PRESENT, YO, equivalgo",
    )
    fun `irregular and derived verbs from the data file`(infinitive: String, tense: Tense, person: Person, form: String) {
        assertEquals(form, Conjugator.conjugate(VerbList.byInfinitive.getValue(infinitive).verb, tense, person))
    }

    @Test
    fun `a single-form fix keeps the rule forms around it`() {
        val verb = Verb("caber", "", VerbList.parsePattern("caber", "pret:cup fut:cabr PRESENT.YO:quepo"))
        assertEquals(listOf("quepo", "cabes", "cabe", "cabemos", "cabéis", "caben"), Conjugator.table(verb, Tense.PRESENT).values.toList())
    }
}

package app.vocablo.play

import app.vocablo.cards.Card
import app.vocablo.cards.CardText
import app.vocablo.cards.CardType
import app.vocablo.verbs.Conjugator
import app.vocablo.verbs.Person
import app.vocablo.verbs.Tense
import app.vocablo.verbs.Verb
import kotlin.random.Random

/** A noun with its article, e.g. "el aguacate" / "the avocado". */
data class Noun(val article: String, val word: String, val english: String) {
    val spanish: String get() = "$article $word"
}

/**
 * One sentence to assemble from tiles. The learner sees [meaning] (English chunks) and
 * puts [answer] in order; [tiles] also holds a couple of tempting wrong forms.
 */
data class SentenceExercise(
    val meaning: List<String>,
    val answer: List<String>,
    val tiles: List<String>,
    val verb: Verb,
    val tense: Tense,
    val person: Person,
) {
    val sentence: String get() = answer.joinToString(" ").replaceFirstChar { it.uppercase() } + "."
}

/**
 * Builds simple subject + verb + object sentences from the learner's own cards, so every
 * sentence uses words they're studying. Only verbs that naturally take a direct object are
 * used, which keeps the sentences sensible.
 */
object SentenceBuilder {

    /** Common verbs that take a thing as direct object (comer el pan, comprar la leche). */
    val TRANSITIVE = setOf(
        "tener", "hacer", "querer", "ver", "buscar", "encontrar", "comer", "beber", "comprar", "vender",
        "necesitar", "usar", "tomar", "llevar", "traer", "abrir", "cerrar", "pagar", "pedir", "leer",
        "escribir", "preparar", "cocinar", "limpiar", "lavar", "olvidar", "recordar", "perder", "romper",
        "arreglar", "guardar", "mirar", "probar", "cortar", "dibujar", "pintar", "terminar", "empezar",
        "cambiar", "recibir", "enviar", "mandar", "alquilar", "esconder", "llenar", "tocar", "conocer",
        "preferir", "odiar", "regalar", "devolver", "coger", "agarrar", "sacar", "poner", "dejar", "elegir",
    )

    private val ARTICLES = mapOf("el" to "the", "la" to "the", "los" to "the", "las" to "the", "un" to "a", "una" to "a")

    /** Nouns from word cards written with an article, like "el aguacate". */
    fun nounsFrom(cards: List<Card>): List<Noun> = cards
        .filter { it.type == CardType.WORD }
        .mapNotNull { c ->
            val words = c.spanish.trim().lowercase().split(Regex("\\s+"))
            if (words.size != 2 || words[0] !in ARTICLES) return@mapNotNull null
            val english = CardText.meanings(c.english).firstOrNull() ?: return@mapNotNull null
            Noun(words[0], words[1], english)
        }

    private fun subject(person: Person, random: Random): Pair<String, String> = when (person) {
        Person.YO -> "yo" to "I"
        Person.TU -> "tú" to "you"
        Person.EL -> if (random.nextBoolean()) "ella" to "she" else "él" to "he"
        Person.NOSOTROS -> "nosotros" to "we"
        Person.VOSOTROS -> "vosotros" to "you all"
        Person.ELLOS -> "ellos" to "they"
    }

    private fun tenseTag(tense: Tense) = when (tense) {
        Tense.PRESENT -> "now"
        Tense.PRETERITE -> "finished past"
        Tense.IMPERFECT -> "used to"
        Tense.FUTURE -> "will"
        Tense.CONDITIONAL -> "would"
    }

    fun build(verbs: List<Verb>, nouns: List<Noun>, tenses: List<Tense>, persons: List<Person>, random: Random = Random.Default): SentenceExercise? {
        val usable = verbs.filter { it.infinitive in TRANSITIVE }
        if (usable.isEmpty() || nouns.isEmpty() || tenses.isEmpty() || persons.isEmpty()) return null
        val verb = usable.random(random)
        val noun = nouns.random(random)
        val tense = tenses.random(random)
        val person = persons.random(random)
        val (subject, subjectEnglish) = subject(person, random)
        val form = Conjugator.conjugate(verb, tense, person)
        val answer = listOf(subject, form, noun.article, noun.word)
        val englishObject = (ARTICLES[noun.article] ?: "the") + " " + noun.english.removePrefix("the ").removePrefix("a ")
        val meaning = listOf(subjectEnglish, verb.english.substringBefore(';').removePrefix("to ").trim() + " (${tenseTag(tense)})", englishObject)
        val wrongForms = Person.entries.map { Conjugator.conjugate(verb, tense, it) }.filter { it != form }.distinct().shuffled(random).take(2)
        return SentenceExercise(meaning, answer, (answer + wrongForms).shuffled(random), verb, tense, person)
    }

    fun check(arranged: List<String>, exercise: SentenceExercise): Boolean = arranged == exercise.answer
}

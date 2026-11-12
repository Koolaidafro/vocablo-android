package app.vocablo.play

import app.vocablo.cards.Card
import app.vocablo.cards.CardText
import app.vocablo.verbs.Conjugator
import app.vocablo.verbs.Drill
import app.vocablo.verbs.Person
import app.vocablo.verbs.Question
import app.vocablo.verbs.Tense
import app.vocablo.verbs.Verb
import kotlin.random.Random

/** The five mini-games. All work offline and only use verbs, tenses and cards the learner has. */
enum class Game(val title: String, val blurb: String, val usesVerbs: Boolean) {
    VERB_RAIN("Verb Rain", "Tap the right form before it hits the ground.", true),
    TUG_OF_WAR("Tug of War", "Answer fast to pull the rope to your side.", true),
    SI_O_NO("Sí o No", "Is this form right? Swipe yes or no against the clock.", true),
    PAREJAS("Parejas", "Match Spanish words with their meanings.", false),
    CUATRO("Cuatro", "Pick the right meaning out of four.", false),
}

/** Verb Rain: a prompt plus falling drops, one of which is right. */
data class RainRound(val question: Question, val drops: List<String>)

object VerbRain {
    fun round(verbs: List<Verb>, tenses: List<Tense>, persons: List<Person>, random: Random = Random.Default): RainRound {
        val q = Drill.question(verbs.random(random), tenses.random(random), persons, random)
        val extra = Tense.entries.flatMap { t -> Person.entries.map { p -> Conjugator.conjugate(q.verb, t, p) } }
            .filter { it != q.answer }.distinct().shuffled(random)
        val drops = (q.choices + extra).distinct().take(5)
        return RainRound(q, drops.shuffled(random))
    }

    /** Points for a catch: faster catches (less of the fall used) score more. */
    fun points(fallFraction: Float): Int = (10 * (1f - fallFraction.coerceIn(0f, 1f))).toInt() + 5
}

/** Sí o No: a claim that is right about half the time. */
data class Claim(val verb: Verb, val tense: Tense, val person: Person, val shown: String, val isTrue: Boolean)

object SiONo {
    fun claim(verbs: List<Verb>, tenses: List<Tense>, persons: List<Person>, random: Random = Random.Default): Claim {
        val verb = verbs.random(random)
        val tense = tenses.random(random)
        val person = persons.random(random)
        val right = Conjugator.conjugate(verb, tense, person)
        if (random.nextBoolean()) return Claim(verb, tense, person, right, true)
        val wrong = Drill.choices(verb, tense, person, random).filter { it != right }
        return Claim(verb, tense, person, wrong.firstOrNull() ?: right, wrong.isEmpty())
    }
}

/** Tug of War: the rope moves one step per answer; the rival pulls on a timer. */
data class Rope(val position: Int = 0, val limit: Int = 5) {
    val won: Boolean get() = position >= limit
    val lost: Boolean get() = position <= -limit
    val over: Boolean get() = won || lost

    fun answer(correct: Boolean): Rope = if (over) this else copy(position = position + if (correct) 1 else -1)

    fun rivalPull(): Rope = if (over) this else copy(position = position - 1)
}

object TugOfWar {
    /** Milliseconds between the rival's pulls; it gets quicker as the learner's level rises. */
    fun rivalIntervalMillis(level: Int): Long = (4_000L - (level - 1) * 150L).coerceIn(1_800L, 4_000L)
}

/** Cuatro: one card, four English meanings. */
data class FourQuestion(val card: Card, val options: List<String>, val answer: String)

object Cuatro {
    fun question(cards: List<Card>, random: Random = Random.Default): FourQuestion? {
        val byMeaning = cards.distinctBy { first(it) }
        if (byMeaning.size < 4) return null
        val card = byMeaning.random(random)
        val answer = first(card)
        val others = byMeaning.filter { it.id != card.id }.map(::first).filter { it != answer }.shuffled(random).take(3)
        return FourQuestion(card, (others + answer).shuffled(random), answer)
    }

    private fun first(card: Card) = CardText.meanings(card.english).firstOrNull() ?: card.english
}

/** Parejas: a board of Spanish and English tiles. Two taps reveal a pair. */
data class Tile(val pairId: Int, val text: String, val spanish: Boolean)

class ParejasBoard(val tiles: List<Tile>) {
    var matched: Set<Int> = emptySet()
        private set
    var selected: Int? = null
        private set
    var mistakes: Int = 0
        private set

    val finished: Boolean get() = matched.size * 2 == tiles.size

    /** Taps a tile by index. Returns true when the tap completed a pair. */
    fun tap(index: Int): Boolean {
        val tile = tiles[index]
        if (tile.pairId in matched) return false
        val first = selected
        if (first == null || first == index) {
            selected = index
            return false
        }
        selected = null
        val other = tiles[first]
        return if (other.pairId == tile.pairId && other.spanish != tile.spanish) {
            matched = matched + tile.pairId
            true
        } else {
            mistakes++
            false
        }
    }

    companion object {
        fun from(cards: List<Card>, pairs: Int = 6, random: Random = Random.Default): ParejasBoard? {
            val chosen = cards.distinctBy { CardText.key(it.spanish) }.distinctBy { it.english.lowercase() }.shuffled(random).take(pairs)
            if (chosen.size < 3) return null
            val tiles = chosen.flatMapIndexed { i, c ->
                listOf(Tile(i, c.spanish, true), Tile(i, CardText.meanings(c.english).firstOrNull() ?: c.english, false))
            }
            return ParejasBoard(tiles.shuffled(random))
        }
    }
}

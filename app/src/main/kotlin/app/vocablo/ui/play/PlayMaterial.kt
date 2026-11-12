package app.vocablo.ui.play

import app.vocablo.AppContainer
import app.vocablo.cards.Card
import app.vocablo.play.SentenceBuilder
import app.vocablo.play.Xp
import app.vocablo.verbs.DrillItem
import app.vocablo.verbs.Person
import app.vocablo.verbs.Tense
import app.vocablo.verbs.Verb
import app.vocablo.verbs.VerbList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * What the games may use: only verbs the learner has practiced or saved as cards, only the
 * tenses they have unlocked and switched on, and their own cards. A brand-new learner gets the
 * most common verbs of their chosen list so the games are never empty.
 */
data class PlayMaterial(
    val verbs: List<Verb>,
    val tenses: List<Tense>,
    val persons: List<Person>,
    val cards: List<Card>,
    val level: Int,
    /** The learner's practiced verbs that take an object, for the sentence builder. */
    val sentenceVerbs: List<Verb>,
) {
    companion object {
        private const val STARTER_COUNT = 15

        suspend fun load(c: AppContainer): PlayMaterial {
            val practice = c.settings.settings.first().practice
            val practicedRanks = c.verbs.states().keys.map { DrillItem.rankOf(it) }.toSet()
            val mine = c.verbs.myVerbs()
            val cards = c.repository.allCards().filter { it.english.isNotBlank() }
            val level = Xp.level(c.progress.totalXp().first())
            return withContext(Dispatchers.Default) {
                val inTier = VerbList.all.filter { it.rank <= practice.maxTier.upToRank }
                val known = VerbList.all.filter { it.rank in practicedRanks || it.verb.infinitive in mine }
                val verbs = if (known.size >= 4) known else (known + inTier.take(STARTER_COUNT)).distinctBy { it.rank }
                val knownTransitive = known.filter { it.verb.infinitive in SentenceBuilder.TRANSITIVE }
                val sentenceVerbs = knownTransitive.ifEmpty { inTier.filter { it.verb.infinitive in SentenceBuilder.TRANSITIVE }.take(STARTER_COUNT) }
                PlayMaterial(
                    verbs = verbs.map { it.verb },
                    tenses = practice.practiceTenses,
                    persons = practice.persons,
                    cards = cards,
                    level = level,
                    sentenceVerbs = sentenceVerbs.map { it.verb },
                )
            }
        }
    }
}

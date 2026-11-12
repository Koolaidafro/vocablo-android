package app.vocablo.verbs

/** Where a conjugated form comes from. */
data class FormMatch(val verb: Verb, val tense: Tense, val person: Person)

/**
 * Reverse lookup from a conjugated form to its infinitive, so typing "tuve" when
 * adding a card can suggest "tener" instead.
 */
class FormIndex(verbs: Iterable<Verb>) {

    private val index: Map<String, List<FormMatch>> = buildMap<String, MutableList<FormMatch>> {
        for (verb in verbs) for (tense in Tense.entries) for (person in Person.entries) {
            val form = Conjugator.conjugate(verb, tense, person)
            if (form == verb.infinitive) continue
            val list = getOrPut(form) { mutableListOf() }
            if (list.none { it.verb == verb }) list += FormMatch(verb, tense, person)
        }
    }

    fun lookup(text: String): List<FormMatch> = index[text.trim().lowercase()].orEmpty()
}

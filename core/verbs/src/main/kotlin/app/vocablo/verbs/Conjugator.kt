package app.vocablo.verbs

/**
 * Rules-plus-exceptions conjugation engine.
 *
 * A verb is described by its infinitive and a small [VerbPattern]; the rules here
 * generate every form. A prototype of these rules reproduced 95.6% of a 566-verb
 * reference dataset with no stored forms at all.
 */
object Conjugator {

    private val STRESSED = setOf(Person.YO, Person.TU, Person.EL, Person.ELLOS)

    fun table(verb: Verb, tense: Tense): Map<Person, String> =
        Person.entries.associateWith { conjugate(verb, tense, it) }

    fun conjugate(verb: Verb, tense: Tense, person: Person): String {
        verb.pattern.overrides[tense]?.let { return it[person.ordinal] }
        val raw = when (tense) {
            Tense.PRESENT -> present(verb, person)
            Tense.PRETERITE -> preterite(verb, person)
            Tense.IMPERFECT -> imperfect(verb, person)
            Tense.FUTURE -> future(verb, person, FUTURE_ENDINGS)
            Tense.CONDITIONAL -> future(verb, person, CONDITIONAL_ENDINGS)
        }
        return Spelling.dropMonosyllableAccent(raw)
    }

    // ---- shared helpers -------------------------------------------------------

    private enum class Ending { AR, ER, IR }

    private class Parts(val stem: String, val ending: Ending)

    private fun parts(infinitive: String): Parts {
        val base = Spelling.stripAccents(infinitive) // reír -> reir, oír -> oir
        val ending = when (base.takeLast(2)) {
            "ar" -> Ending.AR
            "er" -> Ending.ER
            "ir" -> Ending.IR
            else -> error("Not an infinitive: $infinitive")
        }
        return Parts(base.dropLast(2), ending)
    }

    private fun changeLast(stem: String, change: StemChange): String {
        val i = stem.lastIndexOf(change.from)
        if (i < 0) return stem
        val changed = stem.substring(0, i) + change.to + stem.substring(i + change.from.length)
        return when {
            change == StemChange.O_UE && changed.startsWith("ue") -> "h$changed" // oler -> huelo
            change == StemChange.E_IE && changed.startsWith("ie") -> "y" + changed.drop(1) // errar -> yerro
            else -> changed
        }
    }

    private fun accentLastWeakVowel(stem: String): String {
        val i = stem.indexOfLast { it == 'i' || it == 'u' }
        return if (i < 0) stem else stem.substring(0, i) + Spelling.accented(stem[i]) + stem.substring(i + 1)
    }

    private fun endsInVowel(s: String) = s.isNotEmpty() && s.last() in "aeo"

    /** construir, huir, oír: stems that insert y before a, e, o. */
    private fun insertsY(p: Parts) =
        p.ending == Ending.IR && (p.stem.endsWith("o") || (p.stem.endsWith("u") && !p.stem.endsWith("gu") && !p.stem.endsWith("qu")))

    /** Keep the stem's sound before -o or -a: escoger -> escojo, seguir -> sigo, vencer -> venzo. */
    private fun keepSoundBeforeBackVowel(stem: String, ending: Ending): String = when {
        ending == Ending.AR -> stem
        ending == Ending.IR && stem.endsWith("gu") -> stem.dropLast(2) + "g"
        ending == Ending.IR && stem.endsWith("qu") -> stem.dropLast(2) + "c"
        stem.endsWith("g") -> stem.dropLast(1) + "j"
        stem.endsWith("c") -> stem.dropLast(1) + "z"
        else -> stem
    }

    /** Keep the stem's sound before -e in -ar verbs: buscar -> busqué, llegar -> llegué, empezar -> empecé. */
    private fun keepSoundBeforeFrontVowel(stem: String): String = when {
        stem.endsWith("c") -> stem.dropLast(1) + "qu"
        stem.endsWith("gu") -> stem.dropLast(2) + "gü"
        stem.endsWith("g") -> stem + "u"
        stem.endsWith("z") -> stem.dropLast(1) + "c"
        else -> stem
    }

    // ---- present ---------------------------------------------------------------

    private val PRESENT_ENDINGS = mapOf(
        Ending.AR to listOf("o", "as", "a", "amos", "áis", "an"),
        Ending.ER to listOf("o", "es", "e", "emos", "éis", "en"),
        Ending.IR to listOf("o", "es", "e", "imos", "ís", "en"),
    )

    private fun present(verb: Verb, person: Person): String {
        val p = parts(verb.infinitive)
        val pattern = verb.pattern
        var ending = PRESENT_ENDINGS.getValue(p.ending)[person.ordinal]

        if (person == Person.YO && pattern.yoForm != YoForm.REGULAR) {
            val base = when (pattern.yoForm) {
                YoForm.G -> {
                    val s = if (pattern.stemChange == StemChange.E_I) changeLast(p.stem, StemChange.E_I) else p.stem
                    (if (s.endsWith("c")) s.dropLast(1) else s) + "g"
                }
                YoForm.IG -> p.stem + "ig"
                YoForm.ZCO -> p.stem.dropLast(1) + "zc"
                YoForm.REGULAR -> error("unreachable")
            }
            return base + ending
        }

        var stem = p.stem
        if (person in STRESSED) {
            pattern.stemChange?.let { stem = changeLast(stem, it) }
            if (pattern.accentStressedStem) stem = accentLastWeakVowel(stem)
            if (insertsY(p)) stem += "y"
        } else if (p.ending == Ending.IR && endsInVowel(stem) && person == Person.NOSOTROS) {
            ending = "ímos" // reímos, oímos
        }
        if (person == Person.YO && !insertsY(p)) stem = keepSoundBeforeBackVowel(stem, p.ending)
        return stem + ending
    }

    // ---- preterite -------------------------------------------------------------

    private fun preterite(verb: Verb, person: Person): String {
        val p = parts(verb.infinitive)
        verb.pattern.strongPreteriteStem?.let { return strongPreterite(it, person) }

        if (p.ending == Ending.AR) {
            val s = p.stem
            return when (person) {
                Person.YO -> keepSoundBeforeFrontVowel(s) + "é"
                Person.TU -> s + "aste"
                Person.EL -> s + "ó"
                Person.NOSOTROS -> s + "amos"
                Person.VOSOTROS -> s + "asteis"
                Person.ELLOS -> s + "aron"
            }
        }

        val s = p.stem
        // -ir stem changers shift e->i and o->u in the third person: pidió, durmió.
        val s3 = verb.pattern.stemChange
            ?.takeIf { p.ending == Ending.IR }
            ?.let { c -> s.lastIndexOf(c.from).takeIf { it >= 0 }?.let { i -> s.substring(0, i) + (if (c.from == "o") "u" else "i") + s.substring(i + 1) } }
            ?: s

        val vowelStem = endsInVowel(s) || insertsY(p)
        if (vowelStem) {
            val i = if (s.endsWith("u")) "i" else "í" // construiste vs leíste
            val third = if (s3.endsWith("i")) "" else "y" // rio/rieron vs leyó/leyeron
            return when (person) {
                Person.YO -> s + "í"
                Person.TU -> s + i + "ste"
                Person.EL -> s3 + third + "ó"
                Person.NOSOTROS -> s + i + "mos"
                Person.VOSOTROS -> s + i + "steis"
                Person.ELLOS -> s3 + third + "eron"
            }
        }
        val dropI = s.endsWith("ñ") || s.endsWith("ll") // gruñó, bulló
        return when (person) {
            Person.YO -> s + "í"
            Person.TU -> s + "iste"
            Person.EL -> s3 + (if (dropI) "ó" else "ió")
            Person.NOSOTROS -> s + "imos"
            Person.VOSOTROS -> s + "isteis"
            Person.ELLOS -> s3 + (if (dropI) "eron" else "ieron")
        }
    }

    private fun strongPreterite(stem: String, person: Person): String {
        val j = stem.endsWith("j")
        return when (person) {
            Person.YO -> stem + "e"
            Person.TU -> stem + "iste"
            Person.EL -> (if (stem.endsWith("c")) stem.dropLast(1) + "z" else stem) + "o" // hizo
            Person.NOSOTROS -> stem + "imos"
            Person.VOSOTROS -> stem + "isteis"
            Person.ELLOS -> stem + if (j) "eron" else "ieron"
        }
    }

    // ---- imperfect, future, conditional ----------------------------------------

    private fun imperfect(verb: Verb, person: Person): String {
        val p = parts(verb.infinitive)
        val endings = if (p.ending == Ending.AR) {
            listOf("aba", "abas", "aba", "ábamos", "abais", "aban")
        } else {
            listOf("ía", "ías", "ía", "íamos", "íais", "ían")
        }
        return p.stem + endings[person.ordinal]
    }

    private val FUTURE_ENDINGS = listOf("é", "ás", "á", "emos", "éis", "án")
    private val CONDITIONAL_ENDINGS = listOf("ía", "ías", "ía", "íamos", "íais", "ían")

    private fun future(verb: Verb, person: Person, endings: List<String>): String {
        val stem = verb.pattern.futureStem ?: Spelling.stripAccents(verb.infinitive)
        return stem + endings[person.ordinal]
    }
}

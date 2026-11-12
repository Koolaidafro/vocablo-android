package app.vocablo.verbs

/**
 * Frequency tiers. A learner unlocks them in order; the numbers are cumulative ranks,
 * so TOP_250 holds verbs ranked 101 to 250.
 */
enum class Tier(val upToRank: Int, val label: String) {
    TOP_100(100, "Top 100"),
    TOP_250(250, "Top 250"),
    TOP_500(500, "Top 500"),
    TOP_1000(1000, "Top 1,000"),
    TOP_2000(Int.MAX_VALUE, "Top 2,000"),
    ;

    companion object {
        fun forRank(rank: Int): Tier = entries.first { rank <= it.upToRank }
    }
}

/** A verb with its place in the frequency list (1 = most used). */
data class RankedVerb(val rank: Int, val verb: Verb) {
    val tier: Tier get() = Tier.forRank(rank)
}

/**
 * The full verb list, about 2,000 verbs ranked by how often their forms appear in
 * everyday Spanish (film and TV subtitles). Each line is
 * `infinitive<TAB>english<TAB>pattern`, where the pattern is a few short tokens:
 *
 * - `ie`, `ue`, `i`, `uue`, `iie`: stem change (pensar, volver, pedir, jugar, adquirir)
 * - `g`, `ig`, `zco`: irregular yo form (tengo, traigo, conozco)
 * - `acc`: accent the stressed i/u of the stem (envío, continúo)
 * - `pret:tuv`: strong preterite stem; `fut:tendr`: irregular future stem
 * - `PRESENT:soy,eres,es,somos,sois,son`: a whole tense spelled out
 * - `PRESENT.YO:quepo`: one form spelled out
 *
 * Regular verbs have an empty pattern, so the whole list is under 100 KB.
 */
object VerbList {

    val all: List<RankedVerb> by lazy { load() }

    val byInfinitive: Map<String, RankedVerb> by lazy { all.associateBy { it.verb.infinitive } }

    fun parse(text: String): List<RankedVerb> = text.lineSequence()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .mapIndexed { index, line ->
            val cells = line.split('\t')
            require(cells.size >= 2) { "Bad verb line: $line" }
            RankedVerb(index + 1, Verb(cells[0], cells[1], parsePattern(cells[0], cells.getOrElse(2) { "" })))
        }
        .toList()

    fun parsePattern(infinitive: String, text: String): VerbPattern {
        var pattern = VerbPattern()
        val singleForms = mutableMapOf<Tense, MutableMap<Person, String>>()
        val wholeTenses = mutableMapOf<Tense, List<String>>()
        for (token in text.split(' ').filter { it.isNotEmpty() }) {
            pattern = when {
                token == "ie" -> pattern.copy(stemChange = StemChange.E_IE)
                token == "ue" -> pattern.copy(stemChange = StemChange.O_UE)
                token == "i" -> pattern.copy(stemChange = StemChange.E_I)
                token == "uue" -> pattern.copy(stemChange = StemChange.U_UE)
                token == "iie" -> pattern.copy(stemChange = StemChange.I_IE)
                token == "g" -> pattern.copy(yoForm = YoForm.G)
                token == "ig" -> pattern.copy(yoForm = YoForm.IG)
                token == "zco" -> pattern.copy(yoForm = YoForm.ZCO)
                token == "acc" -> pattern.copy(accentStressedStem = true)
                token.startsWith("pret:") -> pattern.copy(strongPreteriteStem = token.removePrefix("pret:"))
                token.startsWith("fut:") -> pattern.copy(futureStem = token.removePrefix("fut:"))
                ':' in token -> {
                    val (where, value) = token.split(':', limit = 2)
                    val tense = Tense.valueOf(where.substringBefore('.'))
                    if ('.' in where) {
                        singleForms.getOrPut(tense) { mutableMapOf() }[Person.valueOf(where.substringAfter('.'))] = value
                    } else {
                        wholeTenses[tense] = value.split(',').also { require(it.size == 6) { "$infinitive: $token needs 6 forms" } }
                    }
                    pattern
                }
                else -> error("$infinitive: unknown pattern token '$token'")
            }
        }
        if (singleForms.isEmpty() && wholeTenses.isEmpty()) return pattern
        // Single-form fixes are applied on top of what the rules produce.
        val ruleOnly = Verb(infinitive, "", pattern)
        val overrides = Tense.entries.mapNotNull { tense ->
            val whole = wholeTenses[tense]
            val single = singleForms[tense]
            when {
                whole != null -> tense to whole
                single != null -> tense to Person.entries.map { p -> single[p] ?: Conjugator.conjugate(ruleOnly, tense, p) }
                else -> null
            }
        }.toMap()
        return pattern.copy(overrides = overrides)
    }

    private fun load(): List<RankedVerb> {
        val text = VerbList::class.java.getResourceAsStream("/verbs.tsv")
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("verbs.tsv is missing from the build")
        return parse(text)
    }
}

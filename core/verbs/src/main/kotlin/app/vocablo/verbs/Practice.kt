package app.vocablo.verbs

import kotlin.random.Random

/**
 * Tenses unlock in stages, so a learner is never quizzed on something they haven't met.
 * Stage 1 is the present; stage 2 adds the two past tenses; stage 3 adds the future and
 * conditional.
 */
enum class Stage(val number: Int, val tenses: List<Tense>, val title: String) {
    PRESENT(1, listOf(Tense.PRESENT), "Present"),
    PAST(2, listOf(Tense.PRETERITE, Tense.IMPERFECT), "Past"),
    FUTURE(3, listOf(Tense.FUTURE, Tense.CONDITIONAL), "Future"),
    ;

    val next: Stage? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun of(tense: Tense): Stage = entries.first { tense in it.tenses }

        /** Every tense up to and including [stage]. */
        fun tensesThrough(stage: Stage): List<Tense> = entries.filter { it <= stage }.flatMap { it.tenses }
    }
}

fun Tense.label(): String = when (this) {
    Tense.PRESENT -> "Present"
    Tense.PRETERITE -> "Preterite"
    Tense.IMPERFECT -> "Imperfect"
    Tense.FUTURE -> "Future"
    Tense.CONDITIONAL -> "Conditional"
}

fun Tense.hint(): String = when (this) {
    Tense.PRESENT -> "now, habits"
    Tense.PRETERITE -> "finished past events"
    Tense.IMPERFECT -> "used to, was doing"
    Tense.FUTURE -> "will"
    Tense.CONDITIONAL -> "would"
}

fun Person.pronoun(): String = when (this) {
    Person.YO -> "yo"
    Person.TU -> "tú"
    Person.EL -> "él / ella / usted"
    Person.NOSOTROS -> "nosotros"
    Person.VOSOTROS -> "vosotros"
    Person.ELLOS -> "ellos / ustedes"
}

/** Where drill verbs come from. */
enum class VerbSource { TOP_LIST, MY_CARDS, BOTH }

enum class AnswerMode { TYPE, CHOOSE }

/**
 * What the learner has chosen to practice. [enabledTenses] can only contain tenses of
 * stages already unlocked; [practiceTenses] enforces that even if stored data disagrees.
 */
data class PracticeSettings(
    val unlocked: Stage = Stage.PRESENT,
    val enabledTenses: Set<Tense> = setOf(Tense.PRESENT),
    val maxTier: Tier = Tier.TOP_100,
    val includeVosotros: Boolean = false,
    val source: VerbSource = VerbSource.TOP_LIST,
    val answerMode: AnswerMode = AnswerMode.CHOOSE,
) {
    val practiceTenses: List<Tense>
        get() = Stage.tensesThrough(unlocked).filter { it in enabledTenses }.ifEmpty { listOf(Tense.PRESENT) }

    val persons: List<Person>
        get() = if (includeVosotros) Person.entries else Person.entries - Person.VOSOTROS

    fun toggle(tense: Tense): PracticeSettings {
        if (tense !in Stage.tensesThrough(unlocked)) return this
        val next = if (tense in enabledTenses) enabledTenses - tense else enabledTenses + tense
        return copy(enabledTenses = next.ifEmpty { enabledTenses })
    }

    /** Unlocks the next stage and switches its tenses on. */
    fun unlockNext(): PracticeSettings {
        val next = unlocked.next ?: return this
        return copy(unlocked = next, enabledTenses = enabledTenses + next.tenses)
    }
}

/** Decides when a stage is learned well enough to suggest the next one. */
object StageProgress {
    const val WINDOW = 40
    const val TARGET = 0.8

    /** [recent] holds the latest answers in the current stage's tenses, newest last; true = correct. */
    fun readyForNext(recent: List<Boolean>): Boolean {
        if (recent.size < WINDOW) return false
        val window = recent.takeLast(WINDOW)
        return window.count { it } >= TARGET * WINDOW
    }

    fun progress(recent: List<Boolean>): Float {
        if (recent.isEmpty()) return 0f
        val window = recent.takeLast(WINDOW)
        val accuracy = window.count { it }.toFloat() / WINDOW
        return (accuracy / TARGET.toFloat()).coerceAtMost(1f)
    }
}

/** One drill item: a verb in one tense. Its scheduling id packs both. */
data class DrillItem(val verb: RankedVerb, val tense: Tense) {
    val id: Long get() = idOf(verb.rank, tense)

    companion object {
        fun idOf(rank: Int, tense: Tense): Long = rank * 10L + tense.ordinal
        fun rankOf(id: Long): Int = (id / 10).toInt()
        fun tenseOf(id: Long): Tense = Tense.entries[(id % 10).toInt()]
    }
}

/** What the planner needs to know about an item's schedule. */
data class ItemSchedule(val isNew: Boolean, val dueMillis: Long)

/**
 * Chooses what to drill: due items first (most overdue first), then a few new ones in
 * frequency order, introducing a verb's tenses in stage order.
 */
object DrillPlanner {
    fun plan(
        settings: PracticeSettings,
        verbs: List<RankedVerb>,
        myVerbs: Set<String>,
        schedules: Map<Long, ItemSchedule>,
        nowMillis: Long,
        size: Int = 20,
        newLimit: Int = 6,
    ): List<DrillItem> {
        val pool = verbs.filter { rv ->
            val inList = rv.rank <= settings.maxTier.upToRank
            val mine = rv.verb.infinitive in myVerbs
            when (settings.source) {
                VerbSource.TOP_LIST -> inList
                VerbSource.MY_CARDS -> mine
                VerbSource.BOTH -> inList || mine
            }
        }
        val items = pool.flatMap { rv -> settings.practiceTenses.map { DrillItem(rv, it) } }
        val due = items
            .filter { schedules[it.id]?.let { s -> !s.isNew && s.dueMillis <= nowMillis } == true }
            .sortedBy { schedules.getValue(it.id).dueMillis }
        val fresh = items
            .filter { schedules[it.id]?.isNew ?: true }
            .sortedWith(compareBy({ it.verb.rank }, { it.tense.ordinal }))
            .take(newLimit)
        return (due + fresh).take(size)
    }
}

enum class Check { CORRECT, ACCENT_ONLY, WRONG }

data class Question(
    val verb: Verb,
    val tense: Tense,
    val person: Person,
    val answer: String,
    /** Four options including the answer, for multiple-choice mode. */
    val choices: List<String>,
)

object Drill {

    fun question(verb: Verb, tense: Tense, persons: List<Person>, random: Random = Random.Default): Question {
        val person = persons.random(random)
        val answer = Conjugator.conjugate(verb, tense, person)
        return Question(verb, tense, person, answer, choices(verb, tense, person, random))
    }

    /**
     * The answer plus three believable mistakes: the "regular" form a learner might guess,
     * the same person in another tense, and other persons in this tense.
     */
    fun choices(verb: Verb, tense: Tense, person: Person, random: Random = Random.Default): List<String> {
        val answer = Conjugator.conjugate(verb, tense, person)
        val regular = Conjugator.conjugate(Verb(verb.infinitive, verb.english), tense, person)
        val otherTense = Tense.entries.filter { it != tense }.map { Conjugator.conjugate(verb, it, person) }
        val otherPerson = Person.entries.filter { it != person }.map { Conjugator.conjugate(verb, tense, it) }
        val distractors = (listOf(regular) + otherPerson.shuffled(random).take(2) + otherTense.shuffled(random) + otherPerson)
            .distinct()
            .filter { it != answer && Spelling.stripAccents(it) != Spelling.stripAccents(answer) }
            .take(3)
        return (distractors + answer).shuffled(random)
    }

    /** Case and spaces don't matter. A missing or wrong accent counts, but is pointed out. */
    fun check(typed: String, answer: String): Check {
        val t = typed.trim().lowercase()
        return when {
            t == answer -> Check.CORRECT
            Spelling.stripAccents(t) == Spelling.stripAccents(answer) -> Check.ACCENT_ONLY
            else -> Check.WRONG
        }
    }
}

/** Plain-English reasons a form isn't what the regular rules would give. */
object Explain {
    fun why(verb: Verb, tense: Tense, person: Person): List<String> {
        val p = verb.pattern
        val form = Conjugator.conjugate(verb, tense, person)
        val regular = Conjugator.conjugate(Verb(verb.infinitive, verb.english), tense, person)
        val reasons = mutableListOf<String>()
        if (p.overrides[tense] != null && form != regular) {
            reasons += "${verb.infinitive} is irregular here; this form just has to be learned."
        }
        when (tense) {
            Tense.PRESENT -> {
                if (person == Person.YO && p.yoForm != YoForm.REGULAR && p.overrides[tense] == null) {
                    reasons += when (p.yoForm) {
                        YoForm.G -> "The yo form adds a g: ${form}."
                        YoForm.IG -> "The yo form adds -ig-: ${form}."
                        YoForm.ZCO -> "Verbs ending in a vowel + -cer/-cir take -zco in the yo form."
                        YoForm.REGULAR -> ""
                    }
                } else if (p.stemChange != null && person != Person.NOSOTROS && person != Person.VOSOTROS) {
                    reasons += "Stem change ${p.stemChange.from} → ${p.stemChange.to} when the stem is stressed (all but nosotros and vosotros)."
                }
                if (p.accentStressedStem && person != Person.NOSOTROS && person != Person.VOSOTROS) {
                    reasons += "The stressed i or u takes an accent: $form."
                }
            }
            Tense.PRETERITE -> {
                if (p.strongPreteriteStem != null) {
                    reasons += "Irregular preterite stem ${p.strongPreteriteStem}-, with endings that carry no accent."
                } else if (p.stemChange != null && verb.infinitive.endsWith("ir") && (person == Person.EL || person == Person.ELLOS)) {
                    reasons += "-ir stem-changing verbs shift e → i or o → u in the él and ellos forms."
                }
            }
            Tense.FUTURE, Tense.CONDITIONAL -> if (p.futureStem != null) {
                reasons += "Irregular stem ${p.futureStem}- (same for the future and the conditional)."
            }
            Tense.IMPERFECT -> Unit
        }
        if (reasons.isEmpty() && (form != regular || naive(verb.infinitive, tense, person)?.let { it != form } == true)) {
            reasons += "A spelling change keeps the sound of the stem: $form."
        }
        return reasons
    }

    /** The form a learner would get by gluing the textbook ending on, for the yo forms where spelling shifts. */
    private fun naive(infinitive: String, tense: Tense, person: Person): String? {
        if (person != Person.YO) return null
        val base = Spelling.stripAccents(infinitive)
        val stem = base.dropLast(2)
        return when (tense) {
            Tense.PRESENT -> stem + "o"
            Tense.PRETERITE -> stem + if (base.endsWith("ar")) "é" else "í"
            else -> null
        }
    }
}

/**
 * The optional eight-question check in onboarding. Each question is multiple choice;
 * the score decides which stage and tier the learner starts with.
 */
object Placement {
    data class Item(val infinitive: String, val tense: Tense, val person: Person)

    val items = listOf(
        Item("hablar", Tense.PRESENT, Person.TU),
        Item("poder", Tense.PRESENT, Person.YO),
        Item("tener", Tense.PRESENT, Person.YO),
        Item("ser", Tense.PRESENT, Person.NOSOTROS),
        Item("comer", Tense.PRETERITE, Person.EL),
        Item("hacer", Tense.PRETERITE, Person.YO),
        Item("ir", Tense.IMPERFECT, Person.NOSOTROS),
        Item("tener", Tense.FUTURE, Person.ELLOS),
    )

    data class Result(val stage: Stage, val tier: Tier)

    /** [correct] lists, for each item in order, whether it was answered right. */
    fun score(correct: List<Boolean>): Result {
        require(correct.size == items.size)
        val present = correct.subList(0, 4).count { it }
        val past = correct.subList(4, 7).count { it }
        val future = correct[7]
        val stage = when {
            present >= 3 && past >= 2 && future -> Stage.FUTURE
            present >= 3 -> Stage.PAST
            else -> Stage.PRESENT
        }
        val total = correct.count { it }
        val tier = when {
            total >= 7 -> Tier.TOP_500
            total >= 4 -> Tier.TOP_250
            else -> Tier.TOP_100
        }
        return Result(stage, tier)
    }
}

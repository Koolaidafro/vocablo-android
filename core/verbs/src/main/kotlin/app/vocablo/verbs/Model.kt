package app.vocablo.verbs

/** Grammatical persons in table order. Vosotros is hidden in Latin American mode at the UI level. */
enum class Person { YO, TU, EL, NOSOTROS, VOSOTROS, ELLOS }

/** Indicative tenses supported so far. Compound tenses are built from haber plus the participle. */
enum class Tense { PRESENT, PRETERITE, IMPERFECT, FUTURE, CONDITIONAL }

enum class StemChange(val from: String, val to: String) {
    E_IE("e", "ie"),
    O_UE("o", "ue"),
    E_I("e", "i"),
    U_UE("u", "ue"),
    I_IE("i", "ie"),
}

/** How the present-tense "yo" form is built. */
enum class YoForm {
    /** hablo, como, vivo (with automatic spelling fixes such as escojo, sigo, venzo). */
    REGULAR,

    /** tengo, pongo, salgo, hago, digo. */
    G,

    /** traigo, caigo, oigo. */
    IG,

    /** conozco, parezco, conduzco. */
    ZCO,
}

/**
 * Everything the rules need to know about one verb. Regular verbs use the defaults,
 * so a regular verb costs only its infinitive. Anything the rules still get wrong
 * goes in [overrides].
 */
data class VerbPattern(
    val stemChange: StemChange? = null,
    val yoForm: YoForm = YoForm.REGULAR,
    /** Accent the stressed i or u of the stem: envío, continúo, reúno. */
    val accentStressedStem: Boolean = false,
    /** Stem for "strong" preterites with unstressed -e/-o endings: tuv, pud, hic, dij. */
    val strongPreteriteStem: String? = null,
    /** Irregular future and conditional stem: tendr, podr, har. */
    val futureStem: String? = null,
    val overrides: Map<Tense, List<String>> = emptyMap(),
)

data class Verb(
    val infinitive: String,
    val english: String,
    val pattern: VerbPattern = VerbPattern(),
)

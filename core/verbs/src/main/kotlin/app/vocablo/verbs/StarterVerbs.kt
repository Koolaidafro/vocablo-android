package app.vocablo.verbs

import app.vocablo.verbs.StemChange.E_I
import app.vocablo.verbs.StemChange.E_IE
import app.vocablo.verbs.StemChange.O_UE
import app.vocablo.verbs.StemChange.U_UE
import app.vocablo.verbs.YoForm.G
import app.vocablo.verbs.YoForm.IG
import app.vocablo.verbs.YoForm.ZCO

/**
 * A hand-checked starter set covering every pattern the engine supports.
 * The full frequency-ranked list (top 2,000) arrives as a data file in phase 3.
 */
object StarterVerbs {

    private fun forms(s: String) = s.split(" ")

    val all: List<Verb> = listOf(
        Verb("hablar", "to speak"),
        Verb("comer", "to eat"),
        Verb("vivir", "to live"),
        Verb("llegar", "to arrive"),
        Verb("buscar", "to look for"),
        Verb("leer", "to read"),
        Verb("pensar", "to think", VerbPattern(stemChange = E_IE)),
        Verb("empezar", "to begin", VerbPattern(stemChange = E_IE)),
        Verb("contar", "to count; to tell", VerbPattern(stemChange = O_UE)),
        Verb("volver", "to return", VerbPattern(stemChange = O_UE)),
        Verb("jugar", "to play", VerbPattern(stemChange = U_UE)),
        Verb("pedir", "to ask for", VerbPattern(stemChange = E_I)),
        Verb("seguir", "to follow; to continue", VerbPattern(stemChange = E_I)),
        Verb("dormir", "to sleep", VerbPattern(stemChange = O_UE)),
        Verb("preferir", "to prefer", VerbPattern(stemChange = E_IE)),
        Verb("escoger", "to choose"),
        Verb("conocer", "to know (people, places)", VerbPattern(yoForm = ZCO)),
        Verb("conducir", "to drive", VerbPattern(yoForm = ZCO, strongPreteriteStem = "conduj")),
        Verb("construir", "to build"),
        Verb("enviar", "to send", VerbPattern(accentStressedStem = true)),
        Verb("continuar", "to continue", VerbPattern(accentStressedStem = true)),
        Verb("guiar", "to guide", VerbPattern(accentStressedStem = true)),
        Verb("reír", "to laugh", VerbPattern(stemChange = E_I, accentStressedStem = true)),
        Verb("oír", "to hear", VerbPattern(yoForm = IG)),
        Verb("traer", "to bring", VerbPattern(yoForm = IG, strongPreteriteStem = "traj")),
        Verb("tener", "to have", VerbPattern(stemChange = E_IE, yoForm = G, strongPreteriteStem = "tuv", futureStem = "tendr")),
        Verb("venir", "to come", VerbPattern(stemChange = E_IE, yoForm = G, strongPreteriteStem = "vin", futureStem = "vendr")),
        Verb("poner", "to put", VerbPattern(yoForm = G, strongPreteriteStem = "pus", futureStem = "pondr")),
        Verb("salir", "to leave; to go out", VerbPattern(yoForm = G, futureStem = "saldr")),
        Verb("hacer", "to do; to make", VerbPattern(yoForm = G, strongPreteriteStem = "hic", futureStem = "har")),
        Verb("decir", "to say; to tell", VerbPattern(stemChange = E_I, yoForm = G, strongPreteriteStem = "dij", futureStem = "dir")),
        Verb("poder", "to be able to", VerbPattern(stemChange = O_UE, strongPreteriteStem = "pud", futureStem = "podr")),
        Verb("querer", "to want; to love", VerbPattern(stemChange = E_IE, strongPreteriteStem = "quis", futureStem = "querr")),
        Verb(
            "saber", "to know (facts)",
            VerbPattern(
                strongPreteriteStem = "sup", futureStem = "sabr",
                overrides = mapOf(Tense.PRESENT to forms("sé sabes sabe sabemos sabéis saben")),
            ),
        ),
        Verb(
            "estar", "to be (state, location)",
            VerbPattern(
                strongPreteriteStem = "estuv",
                overrides = mapOf(Tense.PRESENT to forms("estoy estás está estamos estáis están")),
            ),
        ),
        Verb(
            "ser", "to be (identity, traits)",
            VerbPattern(
                overrides = mapOf(
                    Tense.PRESENT to forms("soy eres es somos sois son"),
                    Tense.PRETERITE to forms("fui fuiste fue fuimos fuisteis fueron"),
                    Tense.IMPERFECT to forms("era eras era éramos erais eran"),
                ),
            ),
        ),
        Verb(
            "ir", "to go",
            VerbPattern(
                overrides = mapOf(
                    Tense.PRESENT to forms("voy vas va vamos vais van"),
                    Tense.PRETERITE to forms("fui fuiste fue fuimos fuisteis fueron"),
                    Tense.IMPERFECT to forms("iba ibas iba íbamos ibais iban"),
                ),
            ),
        ),
        Verb(
            "dar", "to give",
            VerbPattern(
                overrides = mapOf(
                    Tense.PRESENT to forms("doy das da damos dais dan"),
                    Tense.PRETERITE to forms("di diste dio dimos disteis dieron"),
                ),
            ),
        ),
        Verb(
            "ver", "to see",
            VerbPattern(
                overrides = mapOf(
                    Tense.PRESENT to forms("veo ves ve vemos veis ven"),
                    Tense.IMPERFECT to forms("veía veías veía veíamos veíais veían"),
                ),
            ),
        ),
    )

    val byInfinitive: Map<String, Verb> = all.associateBy { it.infinitive }
}

package app.vocablo.verbs

import app.vocablo.verbs.Person.EL
import app.vocablo.verbs.Person.ELLOS
import app.vocablo.verbs.Person.NOSOTROS
import app.vocablo.verbs.Person.TU
import app.vocablo.verbs.Person.VOSOTROS
import app.vocablo.verbs.Person.YO
import app.vocablo.verbs.Tense.CONDITIONAL
import app.vocablo.verbs.Tense.FUTURE
import app.vocablo.verbs.Tense.IMPERFECT
import app.vocablo.verbs.Tense.PRESENT
import app.vocablo.verbs.Tense.PRETERITE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Expected forms follow the RAE's current spelling rules (2010 Ortografía),
 * e.g. "rio" and "guie" without an accent.
 */
class ConjugatorTest {

    private val verbs = StarterVerbs.byInfinitive

    /** infinitive, tense, then the six forms yo..ellos. */
    private val expected: List<Triple<String, Tense, String>> = listOf(
        // regular -ar, -er, -ir
        Triple("hablar", PRESENT, "hablo hablas habla hablamos habláis hablan"),
        Triple("hablar", PRETERITE, "hablé hablaste habló hablamos hablasteis hablaron"),
        Triple("hablar", IMPERFECT, "hablaba hablabas hablaba hablábamos hablabais hablaban"),
        Triple("hablar", FUTURE, "hablaré hablarás hablará hablaremos hablaréis hablarán"),
        Triple("hablar", CONDITIONAL, "hablaría hablarías hablaría hablaríamos hablaríais hablarían"),
        Triple("comer", PRESENT, "como comes come comemos coméis comen"),
        Triple("comer", PRETERITE, "comí comiste comió comimos comisteis comieron"),
        Triple("comer", IMPERFECT, "comía comías comía comíamos comíais comían"),
        Triple("comer", FUTURE, "comeré comerás comerá comeremos comeréis comerán"),
        Triple("vivir", PRESENT, "vivo vives vive vivimos vivís viven"),
        Triple("vivir", PRETERITE, "viví viviste vivió vivimos vivisteis vivieron"),
        Triple("vivir", FUTURE, "viviré vivirás vivirá viviremos viviréis vivirán"),
        // stem changes
        Triple("pensar", PRESENT, "pienso piensas piensa pensamos pensáis piensan"),
        Triple("contar", PRESENT, "cuento cuentas cuenta contamos contáis cuentan"),
        Triple("jugar", PRESENT, "juego juegas juega jugamos jugáis juegan"),
        Triple("jugar", PRETERITE, "jugué jugaste jugó jugamos jugasteis jugaron"),
        Triple("pedir", PRESENT, "pido pides pide pedimos pedís piden"),
        Triple("pedir", PRETERITE, "pedí pediste pidió pedimos pedisteis pidieron"),
        Triple("dormir", PRESENT, "duermo duermes duerme dormimos dormís duermen"),
        Triple("dormir", PRETERITE, "dormí dormiste durmió dormimos dormisteis durmieron"),
        Triple("preferir", PRETERITE, "preferí preferiste prefirió preferimos preferisteis prefirieron"),
        Triple("volver", PRESENT, "vuelvo vuelves vuelve volvemos volvéis vuelven"),
        // spelling changes
        Triple("buscar", PRETERITE, "busqué buscaste buscó buscamos buscasteis buscaron"),
        Triple("llegar", PRETERITE, "llegué llegaste llegó llegamos llegasteis llegaron"),
        Triple("empezar", PRESENT, "empiezo empiezas empieza empezamos empezáis empiezan"),
        Triple("empezar", PRETERITE, "empecé empezaste empezó empezamos empezasteis empezaron"),
        Triple("escoger", PRESENT, "escojo escoges escoge escogemos escogéis escogen"),
        Triple("seguir", PRESENT, "sigo sigues sigue seguimos seguís siguen"),
        Triple("conocer", PRESENT, "conozco conoces conoce conocemos conocéis conocen"),
        Triple("leer", PRETERITE, "leí leíste leyó leímos leísteis leyeron"),
        Triple("construir", PRESENT, "construyo construyes construye construimos construís construyen"),
        Triple("construir", PRETERITE, "construí construiste construyó construimos construisteis construyeron"),
        Triple("enviar", PRESENT, "envío envías envía enviamos enviáis envían"),
        Triple("continuar", PRESENT, "continúo continúas continúa continuamos continuáis continúan"),
        Triple("guiar", PRETERITE, "guie guiaste guio guiamos guiasteis guiaron"),
        Triple("guiar", PRESENT, "guío guías guía guiamos guiais guían"),
        Triple("reír", PRESENT, "río ríes ríe reímos reís ríen"),
        Triple("reír", PRETERITE, "reí reíste rio reímos reísteis rieron"),
        Triple("oír", PRESENT, "oigo oyes oye oímos oís oyen"),
        Triple("oír", FUTURE, "oiré oirás oirá oiremos oiréis oirán"),
        // irregular yo, strong preterites, irregular future stems
        Triple("tener", PRESENT, "tengo tienes tiene tenemos tenéis tienen"),
        Triple("tener", PRETERITE, "tuve tuviste tuvo tuvimos tuvisteis tuvieron"),
        Triple("tener", FUTURE, "tendré tendrás tendrá tendremos tendréis tendrán"),
        Triple("tener", CONDITIONAL, "tendría tendrías tendría tendríamos tendríais tendrían"),
        Triple("poner", PRESENT, "pongo pones pone ponemos ponéis ponen"),
        Triple("poner", PRETERITE, "puse pusiste puso pusimos pusisteis pusieron"),
        Triple("salir", PRESENT, "salgo sales sale salimos salís salen"),
        Triple("salir", FUTURE, "saldré saldrás saldrá saldremos saldréis saldrán"),
        Triple("hacer", PRESENT, "hago haces hace hacemos hacéis hacen"),
        Triple("hacer", PRETERITE, "hice hiciste hizo hicimos hicisteis hicieron"),
        Triple("hacer", FUTURE, "haré harás hará haremos haréis harán"),
        Triple("decir", PRESENT, "digo dices dice decimos decís dicen"),
        Triple("decir", PRETERITE, "dije dijiste dijo dijimos dijisteis dijeron"),
        Triple("decir", FUTURE, "diré dirás dirá diremos diréis dirán"),
        Triple("traer", PRESENT, "traigo traes trae traemos traéis traen"),
        Triple("traer", PRETERITE, "traje trajiste trajo trajimos trajisteis trajeron"),
        Triple("venir", PRESENT, "vengo vienes viene venimos venís vienen"),
        Triple("venir", PRETERITE, "vine viniste vino vinimos vinisteis vinieron"),
        Triple("poder", PRESENT, "puedo puedes puede podemos podéis pueden"),
        Triple("poder", PRETERITE, "pude pudiste pudo pudimos pudisteis pudieron"),
        Triple("poder", FUTURE, "podré podrás podrá podremos podréis podrán"),
        Triple("querer", PRETERITE, "quise quisiste quiso quisimos quisisteis quisieron"),
        Triple("querer", FUTURE, "querré querrás querrá querremos querréis querrán"),
        Triple("saber", PRESENT, "sé sabes sabe sabemos sabéis saben"),
        Triple("saber", PRETERITE, "supe supiste supo supimos supisteis supieron"),
        Triple("estar", PRESENT, "estoy estás está estamos estáis están"),
        Triple("estar", PRETERITE, "estuve estuviste estuvo estuvimos estuvisteis estuvieron"),
        Triple("conducir", PRETERITE, "conduje condujiste condujo condujimos condujisteis condujeron"),
        // fully irregular
        Triple("ser", PRESENT, "soy eres es somos sois son"),
        Triple("ser", PRETERITE, "fui fuiste fue fuimos fuisteis fueron"),
        Triple("ser", IMPERFECT, "era eras era éramos erais eran"),
        Triple("ir", PRESENT, "voy vas va vamos vais van"),
        Triple("ir", PRETERITE, "fui fuiste fue fuimos fuisteis fueron"),
        Triple("ir", IMPERFECT, "iba ibas iba íbamos ibais iban"),
        Triple("ir", FUTURE, "iré irás irá iremos iréis irán"),
        Triple("dar", PRESENT, "doy das da damos dais dan"),
        Triple("dar", PRETERITE, "di diste dio dimos disteis dieron"),
        Triple("ver", PRESENT, "veo ves ve vemos veis ven"),
        Triple("ver", PRETERITE, "vi viste vio vimos visteis vieron"),
        Triple("ver", IMPERFECT, "veía veías veía veíamos veíais veían"),
    )

    @TestFactory
    fun `conjugates every person correctly`(): List<DynamicTest> =
        expected.flatMap { (infinitive, tense, forms) ->
            val verb = verbs.getValue(infinitive)
            forms.split(" ").zip(Person.entries).map { (want, person) ->
                DynamicTest.dynamicTest("$infinitive $tense $person = $want") {
                    assertEquals(want, Conjugator.conjugate(verb, tense, person))
                }
            }
        }

    @Test
    fun `every starter verb conjugates in every tense without blanks`() {
        for (verb in StarterVerbs.all) for (tense in Tense.entries) for (person in Person.entries) {
            val form = Conjugator.conjugate(verb, tense, person)
            assert(form.isNotBlank() && form.none { it.isWhitespace() }) {
                "${verb.infinitive} $tense $person gave '$form'"
            }
        }
    }

    @Test
    fun `table returns six forms in person order`() {
        val table = Conjugator.table(verbs.getValue("tener"), PRETERITE)
        assertEquals(listOf(YO, TU, EL, NOSOTROS, VOSOTROS, ELLOS), table.keys.toList())
        assertEquals("tuvieron", table[ELLOS])
    }
}

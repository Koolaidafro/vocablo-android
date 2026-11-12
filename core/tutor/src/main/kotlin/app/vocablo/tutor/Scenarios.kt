package app.vocablo.tutor

/** Which Spanish the tutor speaks. Mirrors the app's region setting by name. */
enum class Variety(val label: String, val guidance: String) {
    LATAM(
        "Latin American Spanish",
        "Use neutral Latin American Spanish: ustedes (never vosotros), and common words like carro, celular, computadora, jugo.",
    ),
    MEXICO("Mexican Spanish", "Use Mexican Spanish: ustedes (never vosotros), and Mexican words like carro, celular, platicar, ¿mande?, chamba (when casual)."),
    CARIBBEAN("Caribbean Spanish", "Use Caribbean Spanish (Puerto Rico, Cuba, Dominican Republic): ustedes (never vosotros), and words like carro, guagua for bus, chévere."),
    ANDEAN("Andean Spanish", "Use Andean Spanish (Peru, Bolivia, Ecuador, Colombia): ustedes (never vosotros), polite usted is common; words like carro, celular, chévere."),
    RIOPLATENSE(
        "Rioplatense Spanish",
        "Use Rioplatense Spanish (Argentina, Uruguay): voseo (vos tenés, vos podés, vení), ustedes (never vosotros), and words like auto, celular, colectivo, che.",
    ),
    SPAIN("Spanish from Spain", "Use Spanish from Spain: vosotros for informal plural you, and words like coche, móvil, ordenador, zumo, vale."),
}

data class Scenario(val id: String, val title: String, val emoji: String, val role: String, val opening: String)

object Scenarios {
    val all = listOf(
        Scenario("chat", "Free chat", "💬", "a friendly conversation partner who asks about the learner's day and interests", "¡Hola! ¿Cómo estás hoy? ¿Qué hiciste esta mañana?"),
        Scenario("cafe", "At the café", "☕", "a waiter at a busy café taking the learner's order", "¡Buenas! Bienvenido. ¿Qué le puedo traer?"),
        Scenario("market", "At the market", "🥑", "a fruit and vegetable seller at an outdoor market", "¡Pase, pase! Tenemos aguacates muy frescos hoy. ¿Qué busca?"),
        Scenario("directions", "Asking directions", "🗺️", "a helpful local the learner stops on the street to ask for directions", "¿Sí? ¿Le puedo ayudar en algo?"),
        Scenario("doctor", "At the doctor", "🩺", "a calm doctor asking what is wrong", "Buenos días. Siéntese, por favor. ¿Qué le pasa?"),
        Scenario("hotel", "Hotel check-in", "🏨", "a hotel receptionist checking the learner in", "Buenas tardes, bienvenido al hotel. ¿Tiene una reservación?"),
        Scenario("friends", "Meeting new people", "🤝", "someone the learner just met at a party, curious about them", "¡Hola! No nos conocemos, ¿verdad? Me llamo Lucía. ¿Y tú?"),
        Scenario("job", "Job interview", "💼", "an interviewer for a job the learner wants, friendly but professional", "Buenos días. Gracias por venir. Cuénteme un poco sobre usted."),
    )

    fun byId(id: String): Scenario = all.firstOrNull { it.id == id } ?: all.first()
}

/** The tutor's instructions: role, region, level, and the words the learner is studying. */
object TutorPrompt {
    fun system(scenario: Scenario, variety: Variety, level: String, studyingWords: List<String>, tenses: List<String>): String = buildString {
        appendLine("You are Tuzo, a cheerful gopher who tutors Spanish inside the Vocablo app.")
        appendLine("In this conversation you play ${scenario.role}. Stay in that role and keep the conversation going.")
        appendLine(variety.guidance)
        appendLine("The learner's level is $level. Write short replies: one to three sentences in Spanish, simple enough for that level.")
        if (tenses.isNotEmpty()) appendLine("Stick mainly to these tenses, which the learner has studied: ${tenses.joinToString(", ")}.")
        if (studyingWords.isNotEmpty()) {
            appendLine("Where it fits naturally, use some of the words the learner is studying: ${studyingWords.take(40).joinToString(", ")}.")
        }
        appendLine("If the learner's last message has a mistake, add one line at the end starting with \"✏️\" that gives the corrected sentence and a very short reason in English. If there was no mistake, leave that line out.")
        appendLine("If the learner writes in English or seems stuck, help briefly in English, then continue in Spanish.")
        append("Never use vosotros unless the variety above says to.")
    }
}

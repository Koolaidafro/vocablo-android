// Vocablo: Spanish flashcards for Android.
//
// What Tuzo says in a reminder notification (FR-25, FR-26).
package app.vocablo.play

/** A notification's title and body. */
data class Message(val title: String, val text: String)

/**
 * Pools of reminder messages in Tuzo's voice, at least ten per kind.
 *
 * Design: the message is chosen from the day number modulo the pool size. With ten or more
 * messages, no message repeats inside a week, and nothing has to be stored to remember what
 * was sent. The pools are lambdas so each message can use the real counts.
 * Every line is written to invite, never to blame or invent urgency (NFR-7).
 */
object ReminderMessages {

    /** Which pool a reminder draws from. */
    enum class Kind { CARDS_DUE, STREAK, FRESH_START, COMEBACK }

    private val cardsDue: List<(Int, Int) -> Message> = listOf(
        { due, _ -> Message("¡Hola! Tuzo has ${cards(due)} for you", "A quick swipe session keeps them fresh.") },
        { due, _ -> Message("${cards(due).cap()} are ready", "Two minutes with Tuzo is all it takes.") },
        { due, _ -> Message("Tuzo dug up ${cards(due)}", "Swipe through them while your coffee cools.") },
        { due, _ -> Message("¿Listo? ${cards(due).cap()} waiting", "Tuzo picked the ones you're about to forget.") },
        { due, _ -> Message("Your words miss you a little", "${cards(due).cap()} are ready for a quick look.") },
        { due, _ -> Message("Tuzo's flash cards are shuffled", "${cards(due).cap()} today. Right is Got it, left is Again.") },
        { due, _ -> Message("Un ratito de español", "${cards(due).cap()} ready whenever you are.") },
        { due, _ -> Message("Perfect timing for a review", "Tuzo has ${cards(due)} at just the right moment to remember.") },
        { due, _ -> Message("Tuzo is waving a card at you", "${cards(due).cap()} to go. You've got this.") },
        { due, _ -> Message("¡Vamos! ${cards(due).cap()} today", "Small and often is how words stick.") },
    )

    private val streak: List<(Int, Int) -> Message> = listOf(
        { _, s -> Message("Keep your $s-day streak going", "One short session tonight keeps the chain. Tuzo's ready.") },
        { _, s -> Message("Day ${s + 1} is one swipe away", "A few cards before bed and the streak grows.") },
        { _, s -> Message("$s days in a row. ¡Qué bien!", "Tuzo would love to make it ${s + 1}.") },
        { _, s -> Message("Tuzo saved you a seat", "Two minutes keeps your $s-day streak alive.") },
        { _, s -> Message("Still time for today", "Your $s-day streak is waiting for one quick session.") },
        { _, s -> Message("Una tarjeta más", "Even a handful of cards counts toward your $s-day streak.") },
        { _, s -> Message("Your streak is cozy at $s days", "Tuck in a quick review and it grows overnight.") },
        { _, s -> Message("Tuzo is on streak watch", "$s days so far. A tiny session tonight makes ${s + 1}.") },
        { _, s -> Message("Evening practice?", "Keep the $s-day run going with a few cards.") },
        { _, s -> Message("¡Sigue así!", "$s days of Spanish. One more short session today?") },
    )

    private val freshStart: List<(Int, Int) -> Message> = listOf(
        { _, _ -> Message("Tuzo dug up something new", "Add a few words you saw today and they'll be ready tomorrow.") },
        { _, _ -> Message("Heard a new word today?", "Save it with Tuzo and learn it this week.") },
        { _, _ -> Message("All caught up. ¡Bien!", "Try a game or a verb drill for a bit of fun.") },
        { _, _ -> Message("Nothing due, Tuzo checked", "A round of Parejas keeps things warm.") },
        { _, _ -> Message("A fresh page", "Add five new words and Tuzo will schedule them for you.") },
        { _, _ -> Message("Verb of the day is waiting", "Open Vocablo to see today's verb with Tuzo.") },
        { _, _ -> Message("Tuzo has a game in mind", "Verb Rain or Tug of War? Your pick.") },
        { _, _ -> Message("Build a sentence?", "Tuzo has a short one ready to put together.") },
        { _, _ -> Message("Your decks look tidy", "Add a word from a song or a sign you saw today.") },
        { _, _ -> Message("Practice, your way", "Cards, verbs or a game. Tuzo is happy with any.") },
    )

    private val comeback: List<(Int, Int) -> Message> = listOf(
        { _, _ -> Message("Tuzo misses you", "Whenever you're ready, your words are right where you left them.") },
        { _, _ -> Message("¡Hola de nuevo!", "Tuzo kept your cards safe. Come back any time.") },
        { _, _ -> Message("Tuzo saved your spot", "No rush. One small session is a great restart.") },
        { _, _ -> Message("It's been a while", "Tuzo would love to see you. Your progress is all still here.") },
        { _, _ -> Message("Tuzo is waving from the burrow", "Pop in for two minutes whenever it suits you.") },
        { _, _ -> Message("Your Spanish is waiting", "Nothing was lost. Pick up exactly where you stopped.") },
        { _, _ -> Message("Te extrañamos", "Tuzo has a gentle warm-up ready for your return.") },
        { _, _ -> Message("A fresh start is easy", "Five cards with Tuzo and you're back in the groove.") },
        { _, _ -> Message("Tuzo kept the lights on", "Come say hola when you have a moment.") },
        { _, _ -> Message("Still here for you", "Your decks, XP and badges are all safe with Tuzo.") },
    )

    /** The pool for [kind]. Exposed so tests can check its size and wording. */
    fun pool(kind: Kind): List<(Int, Int) -> Message> = when (kind) {
        Kind.CARDS_DUE -> cardsDue
        Kind.STREAK -> streak
        Kind.FRESH_START -> freshStart
        Kind.COMEBACK -> comeback
    }

    /** Which pool fits: the evening saver talks about the streak, otherwise due cards come first. */
    fun kindFor(saver: Boolean, dueCount: Int, streak: Int): Kind = when {
        saver && streak > 0 -> Kind.STREAK
        dueCount > 0 -> Kind.CARDS_DUE
        streak > 0 -> Kind.STREAK
        else -> Kind.FRESH_START
    }

    /** The message for [day], so the same day always gives the same message. */
    fun pick(kind: Kind, day: Long, dueCount: Int, streak: Int): Message {
        val messages = pool(kind)
        return messages[Math.floorMod(day, messages.size.toLong()).toInt()](dueCount, streak)
    }

    private fun cards(n: Int) = if (n == 1) "1 card" else "$n cards"

    private fun String.cap() = replaceFirstChar { it.uppercase() }
}

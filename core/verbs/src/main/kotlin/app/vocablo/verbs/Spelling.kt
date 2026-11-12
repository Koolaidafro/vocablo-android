package app.vocablo.verbs

/** Spanish spelling helpers used by the conjugation rules. */
object Spelling {
    private const val STRONG = "aeoáéó"
    private const val WEAK = "iuü"
    private const val WEAK_ACCENTED = "íú"
    private const val VOWELS = STRONG + WEAK + WEAK_ACCENTED

    private val unaccent = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u')
    private val accent = unaccent.entries.associate { (k, v) -> v to k }

    fun stripAccents(s: String): String = s.map { unaccent[it] ?: it }.joinToString("")

    fun accented(c: Char): Char = accent[c] ?: c

    /**
     * Counts syllables using the orthographic rules the RAE applies to written accents:
     * two strong vowels are separate syllables, an accented i/u next to a strong vowel
     * breaks the diphthong, and two weak vowels together (ui, iu) count as one.
     * The u in "gue/gui/que/qui" is silent.
     */
    fun syllableCount(word: String): Int {
        val w = word.lowercase()
        var count = 0
        var i = 0
        while (i < w.length) {
            if (w[i] !in VOWELS || isSilentU(w, i)) {
                i++
                continue
            }
            var j = i
            while (j + 1 < w.length && w[j + 1] in VOWELS) j++
            val group = w.substring(i, j + 1).filterIndexed { k, _ -> !isSilentU(w, i + k) }
            count += nuclei(group)
            i = j + 1
        }
        return count
    }

    /** One-syllable words take no written accent: rio, guie, dio, vio, dais, veis. */
    fun dropMonosyllableAccent(word: String): String =
        if (syllableCount(word) == 1) stripAccents(word) else word

    private fun isSilentU(w: String, i: Int): Boolean =
        w[i] == 'u' && i > 0 && (w[i - 1] == 'q' || w[i - 1] == 'g') &&
            i + 1 < w.length && w[i + 1] in "eiéí"

    private fun nuclei(group: String): Int {
        if (group.isEmpty()) return 0
        var n = 1
        for (k in 1 until group.length) {
            val a = group[k - 1]
            val b = group[k]
            val split = (a in STRONG && b in STRONG) ||
                (a in STRONG && b in WEAK_ACCENTED) ||
                (a in WEAK_ACCENTED && b in STRONG)
            if (split) n++
        }
        return n
    }
}

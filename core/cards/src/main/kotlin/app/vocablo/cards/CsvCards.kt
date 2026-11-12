package app.vocablo.cards

/** One row read from an import file, before it is assigned to a deck. */
data class ImportRow(val spanish: String, val english: String, val example: String = "", val deck: String = "")

data class ImportResult(val rows: List<ImportRow>, val skippedLines: List<Int>)

/**
 * Reads and writes cards as CSV, and also accepts the tab-separated text that Anki and
 * most flashcard apps export. Columns: spanish, english, then optional example and deck.
 * A header row is detected and skipped.
 */
object CsvCards {

    fun parse(text: String): ImportResult {
        val lines = text.removePrefix("﻿").lines()
        val delimiter = if (lines.firstOrNull { it.isNotBlank() && !it.startsWith("#") }?.contains('\t') == true) '\t' else ','
        val rows = mutableListOf<ImportRow>()
        val skipped = mutableListOf<Int>()
        var first = true
        lines.forEachIndexed { index, line ->
            if (line.isBlank() || line.startsWith("#")) return@forEachIndexed // Anki puts settings in # lines
            val cells = splitLine(line, delimiter).map { it.trim() }
            if (first) {
                first = false
                if (cells.firstOrNull()?.lowercase() in setOf("spanish", "español", "front", "word")) return@forEachIndexed
            }
            if (cells.size < 2 || cells[0].isEmpty() || cells[1].isEmpty()) {
                skipped += index + 1
            } else {
                rows += ImportRow(cells[0], cells[1], cells.getOrElse(2) { "" }, cells.getOrElse(3) { "" })
            }
        }
        return ImportResult(rows, skipped)
    }

    fun write(cards: List<Card>, deckNames: Map<Long, String>): String = buildString {
        append("spanish,english,example,deck\n")
        for (c in cards) {
            append(listOf(c.spanish, c.english, c.example, deckNames[c.deckId].orEmpty()).joinToString(",") { quote(it) })
            append('\n')
        }
    }

    private fun quote(s: String) =
        if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    private fun splitLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                quoted && c == '"' && line.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                c == '"' && (cell.isEmpty() || quoted) -> quoted = !quoted
                c == delimiter && !quoted -> { out += cell.toString(); cell.clear() }
                else -> cell.append(c)
            }
            i++
        }
        out += cell.toString()
        return out
    }
}

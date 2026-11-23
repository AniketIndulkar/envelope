package dev.novanest.envelope.privacy

/** Splits a letter into pieces Nano can take (its input limit is ~4000 tokens). Paragraphs stay whole when they fit. */
object Chunker {
    private val PARAGRAPH = Regex("\\n\\s*\\n")
    private val SENTENCE = Regex("(?<=[.!?])\\s+")
    private val SPACE = Regex("\\s+")

    fun chunk(text: String, maxWords: Int): List<String> {
        val units = text.split(PARAGRAPH).map(String::trim).filter(String::isNotEmpty).flatMap { split(it, maxWords) }
        val chunks = mutableListOf<String>()
        val current = mutableListOf<String>()
        var words = 0
        for (unit in units) {
            val n = count(unit)
            if (words + n > maxWords && current.isNotEmpty()) {
                chunks += current.joinToString("\n\n"); current.clear(); words = 0
            }
            current += unit; words += n
        }
        if (current.isNotEmpty()) chunks += current.joinToString("\n\n")
        return chunks
    }

    private fun split(paragraph: String, maxWords: Int): List<String> {
        if (count(paragraph) <= maxWords) return listOf(paragraph)
        return paragraph.split(SENTENCE).flatMap { s ->
            if (count(s) <= maxWords) listOf(s) else s.split(SPACE).chunked(maxWords).map { it.joinToString(" ") }
        }
    }

    private fun count(s: String) = s.split(SPACE).count(String::isNotEmpty)
}

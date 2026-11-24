package dev.novanest.envelope.ocr

/**
 * OCR keeps the printed line breaks, so sentences arrive chopped ("Call our security team\non ...").
 * A line that starts with a lowercase letter continues the previous one and is joined to it; anything else
 * (address lines, headings, new paragraphs) keeps its break.
 */
object Reflow {
    private val CONTINUATION = Regex("[ \\t]*\\n(?=[ \\t]*\\p{Ll})")

    fun lines(text: String): String = CONTINUATION.replace(text, " ").replace(Regex(" {2,}"), " ")
}

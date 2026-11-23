package dev.novanest.envelope.privacy

/**
 * Regex that finds [value] in letter text the way a person would read it:
 * case-insensitive, any whitespace run (incl. OCR line breaks) between words,
 * and never inside a longer word ("Ann" must not hit "Annual").
 */
internal fun valueRegex(value: String): Regex {
    val words = value.trim().split(WHITESPACE).filter { it.isNotEmpty() }.map(Regex::escape)
    return Regex("(?<![\\p{L}\\p{N}])" + words.joinToString("\\s+") + "(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
}

internal fun normalizeValue(value: String): String = value.trim().split(WHITESPACE).joinToString(" ").lowercase()

private val WHITESPACE = Regex("\\s+")

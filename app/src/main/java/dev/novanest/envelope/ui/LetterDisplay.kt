package dev.novanest.envelope.ui

import dev.novanest.envelope.privacy.PiiSpan
import dev.novanest.envelope.privacy.PiiType
import dev.novanest.envelope.privacy.Redaction
import dev.novanest.envelope.privacy.SpanSource

/** A redacted letter ready to draw: readable chip labels in place of `[TYPE_N]`, plus tappable word ranges. */
data class LetterDisplay(
    val text: String,
    val chips: List<Chip>,
    val words: List<IntRange>,
    /** Words that look personal but weren't hidden — shown on phones without Nano so the user can hide them. */
    val suggestions: List<IntRange> = emptyList(),
) {

    data class Chip(val start: Int, val end: Int, val token: String, val value: String)

    /** What tapping [word] hides: the whole suggestion it sits in (e.g. a full name), else just the word. */
    fun tapTarget(word: IntRange): String {
        val range = suggestions.firstOrNull { word.first >= it.first && word.last <= it.last } ?: word
        return text.substring(range.first, range.last + 1)
    }

    companion object {
        private const val PAD = "\u202F" // narrow no-break space: room for the hatch, never a line break
        private val WORD = Regex("[\\p{L}\\p{N}][\\p{L}\\p{N}'’\\-]*")
        private val TOKEN = Regex("\\[([A-Z_]+)_(\\d+)]")

        private val NAMES = mapOf(
            PiiType.NAME to "Name", PiiType.ADDRESS to "Address", PiiType.POSTCODE to "Postcode",
            PiiType.NI_NUMBER to "NI number", PiiType.ACCOUNT to "Account", PiiType.REFERENCE to "Reference",
            PiiType.PHONE to "Phone", PiiType.EMAIL to "Email", PiiType.DOB to "Date of birth",
        )

        fun label(token: String): String {
            val m = TOKEN.matchEntire(token) ?: return token
            val type = runCatching { PiiType.valueOf(m.groupValues[1]) }.getOrNull() ?: return token
            return "${NAMES.getValue(type)} ${m.groupValues[2]}"
        }

        fun of(r: Redaction, suggest: Boolean = false): LetterDisplay {
            val values = r.tokens.associate { it.token to it.value }
            val sb = StringBuilder()
            val chips = mutableListOf<Chip>()
            val words = mutableListOf<IntRange>()
            var cursor = 0
            fun plain(until: Int) {
                val segment = r.redacted.substring(cursor, until)
                val offset = sb.length
                WORD.findAll(segment).forEach { words += (it.range.first + offset)..(it.range.last + offset) }
                sb.append(segment)
            }
            for (p in r.placements) {
                plain(p.start)
                val start = sb.length
                sb.append(PAD).append(label(p.token).replace(' ', '\u00A0')).append(PAD)
                chips += Chip(start, sb.length, p.token, values[p.token].orEmpty())
                cursor = p.end
            }
            plain(r.redacted.length)
            val text = sb.toString()
            return LetterDisplay(text, chips, words, if (suggest) suggestionsIn(text, chips) else emptyList())
        }

        private const val NAME_WORD = "[A-Z][\\p{L}'’\\-]+"
        private val TITLED_NAME = Regex("\\b(?:Mr|Mrs|Ms|Miss|Mx|Dr)\\.?[ \\t]+($NAME_WORD(?:[ \\t]+$NAME_WORD){0,2})")
        private val STREET = Regex(
            "\\b\\d{1,4}[A-Za-z]?[ \\t]+(?:[A-Z][a-z]+[ \\t]+){1,3}" +
                "(?:Road|Street|Avenue|Lane|Close|Way|Drive|Court|Crescent|Place|Gardens|Terrace|Grove|Wharf|Square|Rd|St|Ave)\\b"
        )

        private fun suggestionsIn(text: String, chips: List<Chip>): List<IntRange> {
            val found = TITLED_NAME.findAll(text).mapNotNull { it.groups[1]?.range } + STREET.findAll(text).map { it.range }
            return found.filter { r -> chips.none { it.start <= r.last && r.first < it.end } }.sortedBy { it.first }.toList()
        }

        /** What the user taps to hide: "14 Elm Road" is an address, "RGH2287731" a reference, anything else a name. */
        fun manualSpan(word: String) = PiiSpan(
            text = word,
            type = when {
                word.any(Char::isDigit) && word.trim().contains(' ') -> PiiType.ADDRESS
                word.any(Char::isDigit) -> PiiType.REFERENCE
                else -> PiiType.NAME
            },
            source = SpanSource.MANUAL,
        )
    }
}

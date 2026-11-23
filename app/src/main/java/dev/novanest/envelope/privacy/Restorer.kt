package dev.novanest.envelope.privacy

data class RestoreResult(val text: String, val unknownTokens: List<String>)

/** Puts real values back into model output, tolerating the ways models mangle placeholders. */
object Restorer {

    private val TOKEN = Regex(
        "\\[\\s*(${PiiType.entries.joinToString("|") { it.name }})_(\\d+)\\s*]",
        RegexOption.IGNORE_CASE,
    )

    fun restore(text: String, tokens: List<TokenEntry>): RestoreResult {
        val byToken = tokens.associateBy { it.token }
        val unknown = mutableListOf<String>()
        val out = TOKEN.replace(text) { m ->
            val canonical = "[${m.groupValues[1].uppercase()}_${m.groupValues[2]}]"
            byToken[canonical]?.value ?: canonical.also { if (it !in unknown) unknown += it }
        }
        return RestoreResult(out, unknown)
    }
}

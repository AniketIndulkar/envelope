package dev.novanest.envelope.privacy

import kotlinx.serialization.Serializable

/** One personal detail and the placeholder that replaced it. Never leaves the phone. */
@Serializable
data class TokenEntry(val token: String, val value: String, val type: PiiType, val source: SpanSource)

@Serializable
data class Redaction(val original: String, val redacted: String, val tokens: List<TokenEntry>) {
    fun countsByType(): Map<PiiType, Int> = tokens.groupingBy { it.type }.eachCount()
}

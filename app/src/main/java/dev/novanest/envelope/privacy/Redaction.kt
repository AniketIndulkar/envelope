package dev.novanest.envelope.privacy

import kotlinx.serialization.Serializable

/** One personal detail and the placeholder that replaced it. Never leaves the phone. */
@Serializable
data class TokenEntry(val token: String, val value: String, val type: PiiType, val source: SpanSource)

/** Where a token sits: [start, end) in the redacted text, [originalStart, originalEnd) in the original. */
@Serializable
data class Placement(val token: String, val start: Int, val end: Int, val originalStart: Int, val originalEnd: Int)

@Serializable
data class Redaction(
    val original: String,
    val redacted: String,
    val tokens: List<TokenEntry>,
    val placements: List<Placement> = emptyList(),
) {
    fun countsByType(): Map<PiiType, Int> = tokens.groupingBy { it.type }.eachCount()
}

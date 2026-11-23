package dev.novanest.envelope.privacy

import android.util.Log

@kotlinx.serialization.Serializable
enum class RedactionMode { NANO_AND_RULES, RULES_ONLY }

data class RedactionOutcome(val redaction: Redaction, val mode: RedactionMode)

/**
 * Runs every on-device layer and merges them. If Nano is missing or fails, falls back to rules only —
 * never to the cloud — and says so through [RedactionMode] so the UI can warn the user.
 */
class RedactionService(
    private val patterns: PatternRedactor,
    private val entity: SpanFinder,
    private val nano: SpanFinder?,
) {
    private var cachedText: String? = null
    private var cachedAuto: Pair<List<PiiSpan>, RedactionMode>? = null

    suspend fun redact(text: String, extra: List<PiiSpan> = emptyList(), ignore: Set<String> = emptySet()): RedactionOutcome {
        val (auto, mode) = autoSpans(text)
        val ignored = ignore.map(::normalizeValue).toSet()
        val spans = (auto + extra).filter { normalizeValue(it.text) !in ignored }
        return RedactionOutcome(RedactionMerger.merge(text, spans), mode)
    }

    /** Model layers are slow; reuse them while the user toggles chips on the same letter. */
    private suspend fun autoSpans(text: String): Pair<List<PiiSpan>, RedactionMode> {
        cachedAuto?.takeIf { cachedText == text }?.let { return it }
        val base = patterns.find(text) + runCatching { entity.find(text) }.getOrElse { emptyList() }
        val nanoSpans = nano?.let { finder ->
            try { finder.find(text) } catch (e: Exception) { Log.w(TAG, "Nano redaction failed, rules only", e); null }
        }
        val result = if (nanoSpans != null) (base + nanoSpans) to RedactionMode.NANO_AND_RULES
                     else base to RedactionMode.RULES_ONLY
        cachedText = text; cachedAuto = result
        return result
    }

    private companion object { const val TAG = "RedactionService" }
}

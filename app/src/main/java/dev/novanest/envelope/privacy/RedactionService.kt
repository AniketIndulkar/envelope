package dev.novanest.envelope.privacy

import android.util.Log
import kotlinx.coroutines.CancellationException

@kotlinx.serialization.Serializable
enum class RedactionMode { NANO_AND_RULES, RULES_ONLY }

/** [entityLayerWorked] is false when ML Kit's address/phone layer couldn't run (e.g. its model isn't downloaded yet). */
data class RedactionOutcome(val redaction: Redaction, val mode: RedactionMode, val entityLayerWorked: Boolean = true)

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
    private var cachedAuto: Auto? = null

    private data class Auto(val spans: List<PiiSpan>, val mode: RedactionMode, val entityOk: Boolean)

    suspend fun redact(text: String, extra: List<PiiSpan> = emptyList(), ignore: Set<String> = emptySet()): RedactionOutcome {
        val auto = autoSpans(text)
        val ignored = ignore.map(::normalizeValue).toSet()
        val spans = (auto.spans + extra).filter { normalizeValue(it.text) !in ignored }
        return RedactionOutcome(RedactionMerger.merge(text, spans), auto.mode, auto.entityOk)
    }

    /** Model layers are slow; reuse them while the user toggles chips on the same letter. */
    private suspend fun autoSpans(text: String): Auto {
        cachedAuto?.takeIf { cachedText == text }?.let { return it }
        val entitySpans = orNull("Entity extraction") { entity.find(text) }
        val base = patterns.find(text) + entitySpans.orEmpty()
        val nanoSpans = nano?.let { finder -> orNull("Nano redaction") { finder.find(text) } }
        val result = if (nanoSpans != null) Auto(base + nanoSpans, RedactionMode.NANO_AND_RULES, entitySpans != null)
                     else Auto(base, RedactionMode.RULES_ONLY, entitySpans != null)
        cachedText = text; cachedAuto = result
        return result
    }

    /** A failed layer is skipped (Nano → rules only). Cancellation is not a failure: it propagates. Logs never carry letter text. */
    private suspend fun <T> orNull(layer: String, block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "$layer failed (${e.javaClass.simpleName}); continuing without it")
        null
    }

    private companion object { const val TAG = "RedactionService" }
}

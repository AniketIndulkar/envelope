package dev.novanest.envelope.privacy

import android.util.Log
import kotlinx.coroutines.CancellationException
import dev.novanest.envelope.ai.Engine
import dev.novanest.envelope.ai.ModelJson
import dev.novanest.envelope.ai.NanoEntities
import dev.novanest.envelope.ai.Prompts
import dev.novanest.envelope.ai.TextModel

/**
 * Gemini Nano layer: finds names, street addresses and free-text references that rules can't.
 * [model] must be the ONLY_ON_DEVICE redactor; it throws when Nano is unavailable.
 */
class NanoSpanFinder(
    private val model: TextModel,
    private val prompts: Prompts,
    private val maxWords: Int,
) : SpanFinder {

    override suspend fun find(text: String): List<PiiSpan> =
        Chunker.chunk(text, maxWords).flatMapIndexed { index, chunk ->
            try {
                findInChunk(chunk)
            } catch (e: CancellationException) {
                throw e
            } catch (e: NanoOffDeviceException) {
                throw e // never tolerate a reply that didn't come from the phone
            } catch (e: Exception) {
                retryOnce(index, chunk, e)
            }
        }

    private suspend fun findInChunk(chunk: String): List<PiiSpan> {
        val reply = model.generate(prompts.redact(chunk))
        if (reply.engine != Engine.ON_DEVICE) throw NanoOffDeviceException()
        return ModelJson.decode<NanoEntities>(reply.text).entities.mapNotNull { e ->
            val type = runCatching { PiiType.valueOf(e.type.trim().uppercase()) }.getOrNull()
            if (type == null || e.text.isBlank()) null else PiiSpan(e.text, type, SpanSource.NANO)
        }
    }

    /**
     * One chunk failing (bad or truncated JSON, Nano busy) gets one more try, then is split in two: Nano's answer
     * is capped at ~256 tokens, so a dense chunk can overflow where each half fits. If that fails too, throw so the
     * whole letter drops to rules-only mode: the banner must never claim Nano checked text it didn't.
     */
    private suspend fun retryOnce(index: Int, chunk: String, first: Exception): List<PiiSpan> {
        Log.w(TAG, "Chunk $index failed (${first.javaClass.simpleName}), retrying once")
        try {
            return findInChunk(chunk)
        } catch (e: CancellationException) {
            throw e
        } catch (e: NanoOffDeviceException) {
            throw e
        } catch (e: Exception) {
            val words = chunk.split(Regex("\\s+")).count { it.isNotEmpty() }
            val halves = Chunker.chunk(chunk, maxOf(1, (words + 1) / 2))
            if (halves.size < 2) { e.addSuppressed(first); throw e }
            Log.w(TAG, "Chunk $index failed again (${e.javaClass.simpleName}); trying ${halves.size} smaller pieces")
            return halves.flatMap { findInChunk(it) }
        }
    }

    private companion object { const val TAG = "NanoSpanFinder" }
}

class NanoOffDeviceException : IllegalStateException("Redaction reply did not come from the on-device model")

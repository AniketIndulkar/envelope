package dev.novanest.envelope.pipeline

import dev.novanest.envelope.ai.Engine
import dev.novanest.envelope.ai.Explanation
import dev.novanest.envelope.ai.ModelJson
import dev.novanest.envelope.ai.ModelParseException
import dev.novanest.envelope.ai.Prompts
import dev.novanest.envelope.ai.QuickCheck
import dev.novanest.envelope.ai.TextModel
import dev.novanest.envelope.privacy.LeakDetectedException
import dev.novanest.envelope.privacy.LeakGuard
import dev.novanest.envelope.privacy.PiiSpan
import dev.novanest.envelope.privacy.RedactionMode
import dev.novanest.envelope.privacy.RedactionService
import dev.novanest.envelope.privacy.Restorer
import dev.novanest.envelope.privacy.TokenEntry
import java.io.IOException
import java.time.LocalDate

/**
 * Letter → redacted text (phone) → quick check (Nano, or cloud on redacted text) → explanation (cloud, redacted)
 * → real details restored (phone). Every prompt that may leave the phone passes [LeakGuard] first.
 */
class LetterPipeline(
    private val redaction: RedactionService,
    private val guard: LeakGuard,
    private val quick: TextModel,
    private val explainer: TextModel,
    private val prompts: Prompts = Prompts(),
    private val today: () -> LocalDate = LocalDate::now,
) {

    suspend fun prepare(ocrText: String, manual: List<PiiSpan> = emptyList(), ignore: Set<String> = emptySet()): Prepared {
        if (ocrText.trim().length < MIN_LETTER_CHARS) throw PipelineError.NotALetter()
        val outcome = redaction.redact(ocrText, manual, ignore)
        val findEngine = if (outcome.mode == RedactionMode.NANO_AND_RULES) Engine.ON_DEVICE else Engine.RULES
        return Prepared(outcome, listOf(StepRecord(Step.READ, Engine.ON_DEVICE), StepRecord(Step.FIND_DETAILS, findEngine)))
    }

    suspend fun quickCheck(prepared: Prepared): Pair<QuickCheck, StepRecord> {
        val prompt = prompts.quickCheck(prepared.outcome.redaction.redacted, today().toString())
        val (qc, engine) = callJson<QuickCheck>(quick, prompt, prepared.outcome.redaction.tokens, Step.QUICK_CHECK)
        return qc to StepRecord(Step.QUICK_CHECK, engine)
    }

    suspend fun explain(prepared: Prepared, language: String): Explained {
        val tokens = prepared.outcome.redaction.tokens
        val prompt = prompts.explain(prepared.outcome.redaction.redacted, language, today().toString())
        val (tokenised, engine) = callJson<Explanation>(explainer, prompt, tokens, Step.EXPLAIN)
        val unknown = linkedSetOf<String>()
        val restored = tokenised.mapText { text ->
            Restorer.restore(text, tokens).also { unknown += it.unknownTokens }.text
        }
        return Explained(tokenised, restored, unknown.toList(), prompt, prepared.steps + StepRecord(Step.EXPLAIN, engine))
    }

    /** Guard → call → decode; one retry on unreadable JSON. */
    private suspend inline fun <reified T> callJson(model: TextModel, prompt: String, tokens: List<TokenEntry>, step: Step): Pair<T, Engine> {
        try {
            guard.assertSafeToSend(prompt, tokens)
        } catch (e: LeakDetectedException) {
            throw PipelineError.Leak(e.leaks)
        }
        repeat(2) { attempt ->
            val reply = try { model.generate(prompt) } catch (e: Exception) { throw mapFailure(e) }
            try {
                return ModelJson.decode<T>(reply.text) to reply.engine
            } catch (e: ModelParseException) {
                if (attempt == 1) throw PipelineError.BadModelOutput(step)
            }
        }
        throw PipelineError.BadModelOutput(step)
    }

    private fun mapFailure(e: Exception): Exception =
        if (generateSequence<Throwable>(e) { it.cause }.any { it is IOException }) PipelineError.Offline() else e

    private fun Explanation.mapText(f: (String) -> String) = copy(
        summary = f(summary),
        whatItMeans = f(whatItMeans),
        actions = actions.map { it.copy(step = f(it.step)) },
        consequences = f(consequences),
        redFlags = redFlags.map(f),
        draftReply = f(draftReply),
        helpLinks = helpLinks.map { it.copy(label = f(it.label)) },
    )

    private companion object { const val MIN_LETTER_CHARS = 40 }
}

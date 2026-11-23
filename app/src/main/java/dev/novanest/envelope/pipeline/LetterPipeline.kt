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
import dev.novanest.envelope.privacy.Redaction
import java.io.IOException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ServerException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    /** Redaction is regex-heavy; keep it off the main thread so the screen never stalls. */
    private val compute: CoroutineDispatcher = Dispatchers.Default,
) {

    suspend fun prepare(ocrText: String, manual: List<PiiSpan> = emptyList(), ignore: Set<String> = emptySet()): Prepared {
        if (ocrText.trim().length < MIN_LETTER_CHARS) throw PipelineError.NotALetter()
        val outcome = withContext(compute) { redaction.redact(ocrText, manual, ignore) }
        val findEngine = if (outcome.mode == RedactionMode.NANO_AND_RULES) Engine.ON_DEVICE else Engine.RULES
        return Prepared(outcome, listOf(StepRecord(Step.READ, Engine.ON_DEVICE), StepRecord(Step.FIND_DETAILS, findEngine)))
    }

    /**
     * PREFER_ON_DEVICE may fall back to the cloud. That is only safe when Nano found the names and addresses;
     * in rules-only mode nothing leaves the phone until the user has reviewed the letter and tapped Explain.
     */
    suspend fun quickCheck(prepared: Prepared): Pair<QuickCheck, StepRecord>? {
        if (prepared.outcome.mode == RedactionMode.RULES_ONLY) return null
        val prompt = prompts.quickCheck(prepared.outcome.redaction.redacted, today().toString())
        val (qc, engine) = callJson<QuickCheck>(quick, prompt, prepared.outcome.redaction, Step.QUICK_CHECK)
        return qc to StepRecord(Step.QUICK_CHECK, engine)
    }

    suspend fun explain(prepared: Prepared, language: String): Explained {
        val tokens = prepared.outcome.redaction.tokens
        val prompt = prompts.explain(prepared.outcome.redaction.redacted, language, today().toString())
        val (tokenised, engine) = callJson<Explanation>(explainer, prompt, prepared.outcome.redaction, Step.EXPLAIN)
        val unknown = linkedSetOf<String>()
        val restored = tokenised.mapText { text ->
            Restorer.restore(text, tokens).also { unknown += it.unknownTokens }.text
        }
        return Explained(tokenised, restored, unknown.toList(), prompt, prepared.outcome.redaction, prepared.steps + StepRecord(Step.EXPLAIN, engine))
    }

    /**
     * Guard → call → decode; one retry on unreadable JSON. The guard checks the letter text, the only part of the
     * prompt that comes from the user; our own template words ("address", "number") must not block sending.
     */
    private suspend inline fun <reified T> callJson(model: TextModel, prompt: String, letter: Redaction, step: Step): Pair<T, Engine> {
        check(prompt.contains(letter.redacted)) { "Prompt must embed the guarded letter text" }
        try {
            guard.assertSafeToSend(letter.redacted, letter.tokens)
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

    private fun mapFailure(e: Exception): Exception {
        val chain = generateSequence<Throwable>(e) { it.cause }.toList()
        return when {
            // Free-tier quota (e.g. 5 a minute) or the model overloaded ('high demand'): both mean try again later.
            chain.any { it is QuotaExceededException || it is ServerException } -> PipelineError.Busy(e)
            chain.any { it is IOException } -> PipelineError.Offline()
            else -> e
        }
    }

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

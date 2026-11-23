package dev.novanest.envelope.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.novanest.envelope.EnvelopeApp
import dev.novanest.envelope.ai.Engine
import dev.novanest.envelope.ai.QuickCheck
import dev.novanest.envelope.data.LetterRecord
import dev.novanest.envelope.data.Prefs
import dev.novanest.envelope.data.SampleLetter
import dev.novanest.envelope.pipeline.Explained
import dev.novanest.envelope.pipeline.LetterPipeline
import dev.novanest.envelope.pipeline.PipelineError
import dev.novanest.envelope.pipeline.Prepared
import dev.novanest.envelope.pipeline.Step
import dev.novanest.envelope.pipeline.StepRecord
import dev.novanest.envelope.privacy.PiiSpan
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class Busy { READING, FINDING, EXPLAINING }

data class SessionUi(
    val busy: Busy? = null,
    val prepared: Prepared? = null,
    val display: LetterDisplay? = null,
    val quick: QuickCheck? = null,
    val quickEngine: Engine? = null,
    val quickPending: Boolean = false,
    val quickUnavailable: Boolean = false,
    val explained: Explained? = null,
    val error: String? = null,
    val leaks: List<String> = emptyList(),
    val language: String = "English",
    val notConfigured: Boolean = false,
) {
    /** Every step in order, with the quick check slotted in after finding details — for the receipt and history. */
    val steps: List<StepRecord>
        get() {
            val base = explained?.steps ?: prepared?.steps.orEmpty()
            val quickStep = quickEngine?.let { StepRecord(Step.QUICK_CHECK, it) } ?: return base
            val at = base.indexOfFirst { it.step == Step.FIND_DETAILS } + 1
            return base.take(at) + quickStep + base.drop(at)
        }
}

/** One letter's journey, shared by the review, result and receipt screens. */
class LetterSessionViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as EnvelopeApp).container
    private val prefs = Prefs(app)
    private var pipeline: LetterPipeline? = null
    private var ocrText = ""
    private val manual = mutableListOf<PiiSpan>()
    private val ignore = mutableSetOf<String>()
    private var job: Job? = null

    private val _ui = MutableStateFlow(SessionUi())
    val ui: StateFlow<SessionUi> = _ui.asStateFlow()

    fun startFromUris(uris: List<Uri>) = start { container.textReader.read(uris) }

    fun startFromSample(sample: SampleLetter) = start {
        // Real OCR on the rendered sample, so the demo shows the actual on-device read; text file only as a fallback.
        runCatching { container.textReader.read(container.samples.image(sample)) }
            .getOrNull()?.takeIf { it.length > 40 }
            ?: getApplication<Application>().assets.open(sample.textAsset).bufferedReader().readText()
    }

    private fun start(read: suspend () -> String) {
        job?.cancel()
        manual.clear(); ignore.clear()
        pipeline = container.newPipeline(prefs.forceRulesOnly)
        _ui.value = SessionUi(busy = Busy.READING, language = prefs.language, notConfigured = !container.firebaseReady)
        job = viewModelScope.launch {
            try {
                ocrText = read()
                _ui.update { it.copy(busy = Busy.FINDING) }
                redact()
                runQuickCheck()
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    private suspend fun redact() {
        val p = pipeline ?: throw IllegalStateException("Add your Firebase config (google-services.json) to use Envelope")
        val prepared = p.prepare(ocrText, manual, ignore)
        _ui.update { it.copy(busy = null, prepared = prepared, display = LetterDisplay.of(prepared.outcome.redaction, suggest = prepared.outcome.mode == dev.novanest.envelope.privacy.RedactionMode.RULES_ONLY), error = null, leaks = emptyList()) }
    }

    private fun runQuickCheck() {
        val p = pipeline ?: return
        val prepared = _ui.value.prepared ?: return
        _ui.update { it.copy(quickPending = true) }
        viewModelScope.launch {
            try {
                val (qc, rec) = p.quickCheck(prepared)
                _ui.update { it.copy(quick = qc, quickEngine = rec.engine, quickPending = false) }
            } catch (e: Exception) {
                Log.w(TAG, "Quick check unavailable", e)
                _ui.update { it.copy(quickPending = false, quickUnavailable = true) }
            }
        }
    }

    fun hideWord(word: String) = reRedact { manual += LetterDisplay.manualSpan(word); ignore.removeAll { it.equals(word, true) } }

    fun showValue(value: String) = reRedact { ignore += value; manual.removeAll { it.text.equals(value, true) } }

    private fun reRedact(change: () -> Unit) {
        if (_ui.value.busy != null) return
        change()
        viewModelScope.launch { runCatching { redact() }.onFailure(::fail) }
    }

    fun setLanguage(language: String) {
        prefs.language = language
        _ui.update { it.copy(language = language) }
    }

    fun explain(onDone: () -> Unit = {}) {
        val p = pipeline ?: return
        val prepared = _ui.value.prepared ?: return
        _ui.update { it.copy(busy = Busy.EXPLAINING, error = null) }
        viewModelScope.launch {
            try {
                val explained = p.explain(prepared, _ui.value.language)
                _ui.update { it.copy(busy = null, explained = explained) }
                onDone()
                save(prepared, explained)
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    private suspend fun save(prepared: Prepared, explained: Explained) {
        val repo = container.letters ?: return
        val ui = _ui.value
        val record = LetterRecord(
            id = UUID.randomUUID().toString(),
            redactedText = prepared.outcome.redaction.redacted,
            quickCheck = ui.quick,
            explanation = explained.tokenised,
            redactionMode = prepared.outcome.mode,
            engines = ui.copy(explained = explained).steps,
            createdAt = System.currentTimeMillis(),
        )
        runCatching { repo.save(record, prepared.outcome.redaction.tokens) }.onFailure { Log.w(TAG, "History save failed", it) }
    }

    fun clearError() = _ui.update { it.copy(error = null) }

    private fun fail(e: Throwable) {
        Log.w(TAG, "Letter failed", e)
        _ui.update {
            it.copy(
                busy = null,
                leaks = (e as? PipelineError.Leak)?.leaks.orEmpty(),
                error = when (e) {
                    is PipelineError -> e.message
                    is IllegalStateException -> e.message
                    else -> "Something went wrong reading that letter. Try again."
                },
            )
        }
    }

    private companion object { const val TAG = "LetterSession" }
}

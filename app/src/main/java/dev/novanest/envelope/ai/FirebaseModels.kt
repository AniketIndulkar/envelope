package dev.novanest.envelope.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.InferenceSource
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FirebaseAIOnDeviceUnknownException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.delay
import android.os.SystemClock
import android.util.Log
import dev.novanest.envelope.BuildConfig
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.thinkingConfig

/**
 * The three hybrid models. Each step uses the inference mode its privacy needs:
 * - redactor: ONLY_ON_DEVICE — raw letter text must never reach the cloud; throws if Nano is missing.
 * - quickCheck: PREFER_ON_DEVICE — input is already redacted, so a cloud fallback is safe.
 * - explainer: ONLY_IN_CLOUD — deep reasoning on redacted text.
 */
@OptIn(PublicPreviewAPI::class)
class FirebaseModels(val cloudModelName: String, val fallbacks: List<String> = emptyList()) {

    private val ai = Firebase.ai(backend = GenerativeBackend.googleAI())
    private val json = generationConfig { responseMimeType = "application/json"; temperature = 0.2f }

    val redactorModel: GenerativeModel = ai.generativeModel(
        modelName = cloudModelName,
        generationConfig = json,
        onDeviceConfig = OnDeviceConfig(mode = InferenceMode.ONLY_ON_DEVICE, maxOutputTokens = 256, temperature = 0f),
    )

    private val quickCheckModel: GenerativeModel = ai.generativeModel(
        modelName = cloudModelName,
        generationConfig = json,
        requestOptions = CLOUD_TIMEOUT,
        onDeviceConfig = OnDeviceConfig(mode = InferenceMode.PREFER_ON_DEVICE, maxOutputTokens = 200, temperature = 0.1f),
    )

    private fun explainerFor(name: String): GenerativeModel = ai.generativeModel(
        modelName = name,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            temperature = 0.3f
            // A plain-English explanation doesn't need long reasoning; low thinking cuts seconds off every answer.
            thinkingConfig = thinkingConfig { thinkingLevel = ThinkingLevel.LOW }
        },
        requestOptions = CLOUD_TIMEOUT,
        onDeviceConfig = OnDeviceConfig(mode = InferenceMode.ONLY_IN_CLOUD),
    )

    val redactor: TextModel = adapt(redactorModel)
    val quickCheck: TextModel = adapt(quickCheckModel)
    /** Primary model first, then the backups; a busy or out-of-quota model hands over to the next. */
    val explainer: TextModel = FallbackTextModel(
        (listOf(cloudModelName) + fallbacks).distinct().map { name -> adapt(explainerFor(name), attempts = 2, label = name) },
        retryable = ::isBusy,
    )

    private fun adapt(model: GenerativeModel, attempts: Int = 3, label: String = "") = TextModel { prompt ->
        val started = SystemClock.elapsedRealtime()
        val response = withRetry(attempts) { model.generateContent(prompt) }
        if (BuildConfig.DEBUG) Log.d("FirebaseModels", "$label ${response.inferenceSource} reply in ${SystemClock.elapsedRealtime() - started} ms, " +
            "tokens in/out/thinking=${response.usageMetadata?.promptTokenCount}/${response.usageMetadata?.candidatesTokenCount}/${response.usageMetadata?.thoughtsTokenCount}")
        ModelReply(
            text = response.text.orEmpty(),
            engine = if (response.inferenceSource == InferenceSource.ON_DEVICE) Engine.ON_DEVICE else Engine.IN_CLOUD,
        )
    }

    /**
     * Nano reports a busy state through unknown on-device errors; the cloud through ServerException, and a stalled
     * connection through RequestTimeoutException (we cap cloud calls at 30 s instead of the SDK's 180 s default).
     */
    private suspend fun <T> withRetry(attempts: Int, block: suspend () -> T): T {
        // Overload spikes last seconds, not milliseconds: back off 1 s, 2 s, 4 s between attempts.
        var wait = 1_000L
        repeat(attempts - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (e !is FirebaseAIOnDeviceUnknownException && e !is ServerException && e !is RequestTimeoutException) throw e
                if (BuildConfig.DEBUG) Log.d("FirebaseModels", "Attempt ${attempt + 1} failed (${e.javaClass.simpleName}); retrying")
                delay(wait); wait *= 2
            }
        }
        return block()
    }

    private fun isBusy(e: Throwable): Boolean = generateSequence(e) { it.cause }
        .any { it is ServerException || it is QuotaExceededException || it is RequestTimeoutException }

    private companion object {
        val CLOUD_TIMEOUT = RequestOptions(timeoutInMillis = 30_000)
    }
}

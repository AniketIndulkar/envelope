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
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.delay

/**
 * The three hybrid models. Each step uses the inference mode its privacy needs:
 * - redactor: ONLY_ON_DEVICE — raw letter text must never reach the cloud; throws if Nano is missing.
 * - quickCheck: PREFER_ON_DEVICE — input is already redacted, so a cloud fallback is safe.
 * - explainer: ONLY_IN_CLOUD — deep reasoning on redacted text.
 */
@OptIn(PublicPreviewAPI::class)
class FirebaseModels(cloudModelName: String) {

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
        onDeviceConfig = OnDeviceConfig(mode = InferenceMode.PREFER_ON_DEVICE, maxOutputTokens = 200, temperature = 0.1f),
    )

    private val explainerModel: GenerativeModel = ai.generativeModel(
        modelName = cloudModelName,
        generationConfig = generationConfig { responseMimeType = "application/json"; temperature = 0.3f },
        onDeviceConfig = OnDeviceConfig(mode = InferenceMode.ONLY_IN_CLOUD),
    )

    val redactor: TextModel = adapt(redactorModel)
    val quickCheck: TextModel = adapt(quickCheckModel)
    val explainer: TextModel = adapt(explainerModel)

    private fun adapt(model: GenerativeModel) = TextModel { prompt ->
        val response = withRetry { model.generateContent(prompt) }
        ModelReply(
            text = response.text.orEmpty(),
            engine = if (response.inferenceSource == InferenceSource.ON_DEVICE) Engine.ON_DEVICE else Engine.IN_CLOUD,
        )
    }

    /** Nano reports a busy/quota state through unknown on-device errors; the cloud through ServerException. */
    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var wait = 500L
        repeat(2) {
            try {
                return block()
            } catch (e: FirebaseAIOnDeviceUnknownException) {
                delay(wait); wait *= 2
            } catch (e: ServerException) {
                delay(wait); wait *= 2
            }
        }
        return block()
    }
}

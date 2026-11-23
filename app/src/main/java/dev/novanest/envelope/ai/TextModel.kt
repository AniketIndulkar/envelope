package dev.novanest.envelope.ai

import kotlinx.serialization.Serializable

/** Which engine actually handled a step. RULES = deterministic on-device code, no model. */
@Serializable
enum class Engine { ON_DEVICE, IN_CLOUD, RULES }

data class ModelReply(val text: String, val engine: Engine)

/** Seam between the pipeline and Firebase AI Logic, so the pipeline is testable with fakes. */
fun interface TextModel {
    suspend fun generate(prompt: String): ModelReply
}

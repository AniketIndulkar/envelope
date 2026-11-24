package dev.novanest.envelope.ai

/**
 * Tries each cloud model in turn. Every Gemini model has its own capacity and free quota, so when one is
 * overloaded or out of quota ([retryable]) the next usually answers. Other errors fail straight away.
 */
class FallbackTextModel(
    private val models: List<TextModel>,
    private val retryable: (Throwable) -> Boolean,
) : TextModel {
    init { require(models.isNotEmpty()) }

    override suspend fun generate(prompt: String): ModelReply {
        var last: Throwable? = null
        for (model in models) {
            try {
                return model.generate(prompt)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!retryable(e)) throw e
                last = e
            }
        }
        throw last!!
    }
}

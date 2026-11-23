package dev.novanest.envelope.pipeline

import dev.novanest.envelope.ai.Engine
import dev.novanest.envelope.ai.Explanation
import dev.novanest.envelope.privacy.Redaction
import dev.novanest.envelope.privacy.RedactionOutcome
import kotlinx.serialization.Serializable

@Serializable
enum class Step { READ, FIND_DETAILS, QUICK_CHECK, EXPLAIN }

/** Which engine handled which step — shown on the privacy receipt. */
@Serializable
data class StepRecord(val step: Step, val engine: Engine)

sealed class PipelineError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotALetter : PipelineError("That doesn't look like a letter — try a clearer photo")
    class Offline : PipelineError("You're offline")
    class Busy(cause: Throwable? = null) : PipelineError("The AI service is busy or has reached its limit for now. Nothing was sent. Try again in a little while.", cause)
    class Leak(val leaks: List<String>) : PipelineError("Personal details still present; nothing was sent")
    class BadModelOutput(val step: Step) : PipelineError("The AI gave an answer we couldn't read")
}

data class Prepared(val outcome: RedactionOutcome, val steps: List<StepRecord>)

data class Explained(
    val tokenised: Explanation,
    val restored: Explanation,
    val unknownTokens: List<String>,
    /** Exact text sent to the cloud model, for the privacy receipt. */
    val sentText: String,
    /** The redaction as it was when sent; the receipt shows this even if the user hides more words afterwards. */
    val sentRedaction: Redaction,
    val steps: List<StepRecord>,
)

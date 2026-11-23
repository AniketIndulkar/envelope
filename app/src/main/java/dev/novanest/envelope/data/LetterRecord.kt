package dev.novanest.envelope.data

import dev.novanest.envelope.ai.Explanation
import dev.novanest.envelope.ai.QuickCheck
import dev.novanest.envelope.pipeline.StepRecord
import dev.novanest.envelope.privacy.RedactionMode
import kotlinx.serialization.Serializable

/** What gets saved to Firestore: redacted text and tokenised AI output only. Real details stay in [TokenStore]. */
@Serializable
data class LetterRecord(
    val id: String,
    val redactedText: String,
    val quickCheck: QuickCheck?,
    val explanation: Explanation?,
    val redactionMode: RedactionMode,
    val engines: List<StepRecord>,
    val createdAt: Long,
) {
    val deadline: String? get() = explanation?.deadline ?: quickCheck?.deadline

    /** Every human-readable field that leaves the phone, for the leak guard. */
    fun outgoingText(): String = buildList {
        add(redactedText)
        quickCheck?.let { addAll(listOf(it.sender, it.oneLine)) }
        explanation?.let { e ->
            addAll(listOf(e.summary, e.whatItMeans, e.consequences, e.draftReply))
            addAll(e.actions.map { it.step }); addAll(e.redFlags); addAll(e.helpLinks.map { it.label })
        }
    }.joinToString("\n")
}

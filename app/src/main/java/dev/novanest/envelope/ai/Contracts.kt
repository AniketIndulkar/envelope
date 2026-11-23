package dev.novanest.envelope.ai

import kotlinx.serialization.Serializable

/** Nano's redaction answer: spans only, never rewritten text (keeps output under the ~256-token on-device budget). */
@Serializable data class NanoEntities(val entities: List<NanoEntity> = emptyList())
@Serializable data class NanoEntity(val text: String, val type: String)

/** urgency ∈ LOW|MEDIUM|HIGH; category ∈ TAX|COUNCIL|HEALTH|BANK|HOUSING|BENEFITS|OTHER; deadline ISO yyyy-MM-dd. */
@Serializable data class QuickCheck(
    val sender: String,
    val category: String = "OTHER",
    val urgency: String = "MEDIUM",
    val deadline: String? = null,
    val oneLine: String,
)

@Serializable data class Action(val step: String, val by: String? = null)
@Serializable data class HelpLink(val label: String, val url: String)

@Serializable data class Explanation(
    val summary: String,
    val whatItMeans: String,
    val actions: List<Action> = emptyList(),
    val deadline: String? = null,
    val consequences: String = "",
    val redFlags: List<String> = emptyList(),
    val draftReply: String = "",
    val helpLinks: List<HelpLink> = emptyList(),
)

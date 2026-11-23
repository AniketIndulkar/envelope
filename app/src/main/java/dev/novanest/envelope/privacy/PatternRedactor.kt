package dev.novanest.envelope.privacy

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One detection rule. [group] selects the capture group holding the value; [validator] is "luhn" or "nhs_mod11". */
@Serializable
data class PatternRule(val type: PiiType, val regex: String, val group: Int = 0, val validator: String? = null)

/** Deterministic layer: finds well-formed UK identifiers. Rules are data so Remote Config can update them. */
class PatternRedactor(val rules: List<PatternRule>) {

    private val compiled = rules.map { it to Regex(it.regex) }

    val ruleCount: Int get() = compiled.size

    fun find(text: String): List<PiiSpan> = compiled.flatMap { (rule, regex) ->
        regex.findAll(text).mapNotNull { m ->
            val value = m.groups[rule.group]?.value?.trim().orEmpty()
            if (value.isEmpty() || !passes(rule.validator, value)) null
            else PiiSpan(value, rule.type, SpanSource.PATTERN)
        }
    }

    private fun passes(validator: String?, value: String) = when (validator) {
        null -> true
        "luhn" -> Checksums.luhn(value)
        "nhs_mod11" -> Checksums.nhsMod11(value)
        else -> false
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun rulesFromJson(text: String): List<PatternRule> = json.decodeFromString<List<PatternRule>>(text)
        fun fromJson(text: String): PatternRedactor = PatternRedactor(rulesFromJson(text))
    }
}

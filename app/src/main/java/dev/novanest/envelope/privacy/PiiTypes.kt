package dev.novanest.envelope.privacy

import kotlinx.serialization.Serializable

@Serializable
enum class PiiType { NAME, ADDRESS, POSTCODE, NI_NUMBER, ACCOUNT, REFERENCE, PHONE, EMAIL, DOB }

/** Where a span came from. Order is also merge priority (earlier wins on duplicates). */
@Serializable
enum class SpanSource { PATTERN, ENTITY, NANO, MANUAL }

data class PiiSpan(val text: String, val type: PiiType, val source: SpanSource)

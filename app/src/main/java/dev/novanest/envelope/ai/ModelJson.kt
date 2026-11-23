package dev.novanest.envelope.ai

import kotlinx.serialization.json.Json

class ModelParseException(val raw: String, cause: Throwable?) :
    Exception("Model returned something that isn't the expected JSON", cause)

/** Models wrap JSON in fences or chatter; take the outermost {...} and decode leniently. */
object ModelJson {
    val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; explicitNulls = false }

    inline fun <reified T> decode(raw: String): T {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) throw ModelParseException(raw, null)
        return try {
            json.decodeFromString<T>(raw.substring(start, end + 1))
        } catch (e: Exception) {
            throw ModelParseException(raw, e)
        }
    }
}

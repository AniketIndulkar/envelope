package dev.novanest.envelope.privacy

/** One redaction layer (ML Kit entities, Gemini Nano, ...). Must run entirely on the phone. */
fun interface SpanFinder {
    suspend fun find(text: String): List<PiiSpan>
}

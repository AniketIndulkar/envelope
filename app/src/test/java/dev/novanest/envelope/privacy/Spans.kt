package dev.novanest.envelope.privacy

fun name(text: String, source: SpanSource = SpanSource.NANO) = PiiSpan(text, PiiType.NAME, source)
fun ni(text: String) = PiiSpan(text, PiiType.NI_NUMBER, SpanSource.PATTERN)
fun address(text: String) = PiiSpan(text, PiiType.ADDRESS, SpanSource.NANO)
fun merge(text: String, vararg spans: PiiSpan) = RedactionMerger.merge(text, spans.toList())

package dev.novanest.envelope.privacy

/** Unions spans from every layer and replaces each distinct value with a stable `[TYPE_N]` token. */
object RedactionMerger {

    fun merge(text: String, spans: List<PiiSpan>): Redaction {
        // 1. One candidate per distinct value; the highest-priority source decides its type.
        val candidates = spans
            .filter { it.text.isNotBlank() }
            .sortedBy { it.source.ordinal }
            .distinctBy { normalizeValue(it.text) }

        // 2. Longest values claim text first so "Margaret O'Neill" beats "Margaret".
        val claims = mutableListOf<Pair<IntRange, PiiSpan>>()
        for (span in candidates.sortedByDescending { normalizeValue(it.text).length }) {
            valueRegex(span.text).findAll(text).forEach { m ->
                if (claims.none { (r, _) -> r.first <= m.range.last && m.range.first <= r.last }) {
                    claims += m.range to span
                }
            }
        }
        claims.sortBy { it.first.first }

        // 3. Number per type by first appearance in the letter (values absent from the text never get a token).
        val counters = mutableMapOf<PiiType, Int>()
        val tokenFor = linkedMapOf<PiiSpan, TokenEntry>()
        for ((_, span) in claims) {
            tokenFor.getOrPut(span) {
                val n = counters.merge(span.type, 1, Int::plus)!!
                TokenEntry("[${span.type}_$n]", span.text.trim(), span.type, span.source)
            }
        }

        // 4. Rebuild the text left to right.
        val out = StringBuilder()
        val placements = mutableListOf<Placement>()
        var cursor = 0
        for ((range, span) in claims) {
            out.append(text, cursor, range.first)
            val token = tokenFor.getValue(span).token
            placements += Placement(token, out.length, out.length + token.length, range.first, range.last + 1)
            out.append(token)
            cursor = range.last + 1
        }
        out.append(text, cursor, text.length)

        return Redaction(original = text, redacted = out.toString(), tokens = tokenFor.values.toList(), placements = placements)
    }
}

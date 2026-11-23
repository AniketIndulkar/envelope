package dev.novanest.envelope.privacy

/** Unions spans from every layer and replaces each distinct value with a stable `[TYPE_N]` token. */
object RedactionMerger {

    fun merge(text: String, spans: List<PiiSpan>): Redaction {
        // 1. One candidate per distinct value; the highest-priority source decides its type.
        val candidates = spans
            .flatMap(::withNameParts)
            .filter { it.text.isNotBlank() && !tooVague(it) }
            .sortedBy { it.source.ordinal }
            .distinctBy { normalizeValue(it.text) }

        // 2. Longest values claim text first so "Margaret O'Neill" beats "Margaret".
        val claims = mutableListOf<Pair<IntRange, PiiSpan>>()
        for (span in candidates.sortedByDescending { normalizeValue(it.text).length }) {
            valueRegex(span.text).findAll(text).forEach { m -> claim(claims, m.range, span, text) }
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

    private val TITLES = setOf("mr", "mrs", "ms", "miss", "mx", "dr", "sir", "dame", "lord", "lady", "rev", "prof")

    /**
     * Letters name you in full once ("Mrs Margaret O'Neill") and then by surname ("Dear Mrs O'Neill"). Nano tags the
     * full name, so each real part of it (not titles, not initials) is hidden wherever it appears alone too.
     */
    private fun withNameParts(span: PiiSpan): List<PiiSpan> {
        if (span.type != PiiType.NAME) return listOf(span)
        val parts = span.text.trim().split(Regex("\\s+"))
            .map { it.trim(',', '.', ';', ':') }
            .filter { it.length >= 3 && it.first().isUpperCase() && it.lowercase().trimEnd('.') !in TITLES }
        return if (parts.size < 2) listOf(span) else listOf(span) + parts.map { PiiSpan(it, PiiType.NAME, span.source) }
    }

    /**
     * Model and entity layers sometimes tag a bare "1" or "12", which would also hit "£1,000" or a date. Short
     * number-only values are only trusted from the deterministic rules.
     */
    private fun tooVague(span: PiiSpan): Boolean {
        if (span.source == SpanSource.PATTERN) return false
        val t = span.text.trim()
        return t.length < 2 || (t.none(Char::isLetter) && t.count(Char::isDigit) < 4)
    }

    /**
     * Adds [range] unless an existing claim already covers it. When two details partly overlap
     * ("Flat 2, 14 Elm Road" and "14 Elm Road, Leeds") they become one block over their union,
     * so no tail of either is left showing.
     */
    private fun claim(claims: MutableList<Pair<IntRange, PiiSpan>>, range: IntRange, span: PiiSpan, text: String) {
        var union = range
        var owner = span
        while (true) {
            val hit = claims.firstOrNull { (r, _) -> r.first <= union.last && union.first <= r.last } ?: break
            val (r, s) = hit
            if (r.first <= union.first && union.last <= r.last && union == range) return // already covered
            claims.remove(hit)
            owner = s
            union = minOf(r.first, union.first)..maxOf(r.last, union.last)
        }
        val merged = if (union == range) span else PiiSpan(text.substring(union.first, union.last + 1), owner.type, owner.source)
        claims += union to merged
    }
}

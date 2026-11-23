package dev.novanest.envelope.privacy

class LeakDetectedException(val leaks: List<String>) :
    Exception("Refusing to send: ${leaks.size} personal detail(s) still present")

/**
 * The single choke point every cloud call and Firestore write goes through.
 * Re-runs the deterministic rules on outgoing text and checks no known real value survived.
 */
class LeakGuard(private val patterns: PatternRedactor) {

    fun findLeaks(outgoing: String, tokens: List<TokenEntry>): List<String> {
        val patternHits = patterns.find(outgoing).map { it.text }
        val valueHits = tokens.filter { valueRegex(it.value).containsMatchIn(outgoing) }.map { it.value }
        return (patternHits + valueHits).distinct()
    }

    fun assertSafeToSend(outgoing: String, tokens: List<TokenEntry>) {
        val leaks = findLeaks(outgoing, tokens)
        if (leaks.isNotEmpty()) throw LeakDetectedException(leaks)
    }
}

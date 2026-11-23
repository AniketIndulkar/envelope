package dev.novanest.envelope.privacy

class LeakDetectedException(val leaks: List<String>) :
    Exception("Refusing to send: ${leaks.size} personal detail(s) still present")

/**
 * The single choke point every cloud call and Firestore write goes through.
 * Re-runs the deterministic rules on outgoing text and checks no known real value survived.
 */
class LeakGuard(private val patterns: PatternRedactor) {

    fun findLeaks(outgoing: String, tokens: List<TokenEntry>): List<String> =
        (patterns.find(outgoing).map { it.text } + findValues(outgoing, tokens)).distinct()

    /**
     * Only the user's own values. For AI output: the model never saw them, so a pattern-shaped number in its answer
     * (an official helpline, say) is not personal data — but a real value appearing there would be.
     */
    fun findValues(outgoing: String, tokens: List<TokenEntry>): List<String> {
        // Placeholders like [NI_NUMBER_1] are ours, not the user's: a hidden word such as "number" must not match inside them.
        val withoutPlaceholders = PLACEHOLDER.replace(outgoing, " ")
        return tokens.filter { valueRegex(it.value).containsMatchIn(withoutPlaceholders) }.map { it.value }.distinct()
    }

    private companion object { val PLACEHOLDER = Regex("\\[[A-Z_]+_\\d+]") }

    fun assertSafeToSend(outgoing: String, tokens: List<TokenEntry>) {
        val leaks = findLeaks(outgoing, tokens)
        if (leaks.isNotEmpty()) throw LeakDetectedException(leaks)
    }

    fun assertNoRealValues(outgoing: String, tokens: List<TokenEntry>) {
        val leaks = findValues(outgoing, tokens)
        if (leaks.isNotEmpty()) throw LeakDetectedException(leaks)
    }
}

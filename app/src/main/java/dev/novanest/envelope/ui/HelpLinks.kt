package dev.novanest.envelope.ui

import java.net.URI

/**
 * Help links come from the model, which may echo a phishing URL from a scam letter. Only open official UK sources.
 */
object HelpLinks {
    private val TRUSTED = listOf(
        "gov.uk", "nhs.uk", "citizensadvice.org.uk", "shelter.org.uk", "moneyhelper.org.uk",
        "stepchange.org", "nationaldebtline.org", "actionfraud.police.uk", "ofgem.gov.uk", "ageuk.org.uk",
    )

    fun isTrusted(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase() ?: return false
        return TRUSTED.any { host == it || host.endsWith(".$it") }
    }
}

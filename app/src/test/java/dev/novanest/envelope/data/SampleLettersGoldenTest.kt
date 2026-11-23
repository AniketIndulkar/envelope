package dev.novanest.envelope.data

import dev.novanest.envelope.privacy.LeakGuard
import dev.novanest.envelope.privacy.PiiSpan
import dev.novanest.envelope.privacy.PiiType
import dev.novanest.envelope.privacy.RedactionMerger
import dev.novanest.envelope.privacy.SpanSource
import dev.novanest.envelope.privacy.TestRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Rules + the known names alone must hide every identifier in the bundled samples, and keep what the explanation needs. */
class SampleLettersGoldenTest {

    private val hidden = mapOf(
        "hmrc_penalty" to listOf("QQ 12 34 56 C", "12345 67890", "1234567890K", "LS6 2QT", "Margaret O'Neill"),
        "council_tax" to listOf("8001234567", "RV1 4HD", "Daniel Okafor"),
        "nhs_referral" to listOf("943 476 5919", "03/07/1951", "RGH2287731", "RV3 9PL", "Joan Whitfield"),
        "bank_fraud_scam" to listOf("40-12-34", "31926819", "07700 900123", "security-northgate@mail-verify.example.com", "RV2 7XN", "Priya Shah"),
        "landlord_notice" to listOf("HPL4471920", "RV1 2AB", "Tom Brennan"),
    )

    private val kept = mapOf(
        "hmrc_penalty" to listOf("18 December 2026", "£100", "HM Revenue & Customs"),
        "council_tax" to listOf("25 November 2026", "£286.40", "Riverside City Council"),
        "nhs_referral" to listOf("9 December 2026", "2 December 2026"),
        "bank_fraud_scam" to listOf("£250", "24 hours", "gift cards"),
        "landlord_notice" to listOf("1 February 2027", "£1,040.00"),
    )

    private fun load(id: String) = File("src/main/assets/samples/$id.txt").readText()

    private fun redact(id: String, names: List<String>) = RedactionMerger.merge(
        load(id),
        TestRules.redactor.find(load(id)) + names.map { PiiSpan(it, PiiType.NAME, SpanSource.NANO) },
    )

    @Test fun everySampleIdentifierHidden() = hidden.forEach { (id, values) ->
        val r = redact(id, values.takeLast(1))
        values.forEach { assertFalse("$id leaked '$it'", r.redacted.contains(it)) }
        assertTrue("$id guard", LeakGuard(TestRules.redactor).findLeaks(r.redacted, r.tokens).isEmpty())
    }

    @Test fun datesAmountsAndOrganisationsKept() = kept.forEach { (id, values) ->
        val r = redact(id, hidden.getValue(id).takeLast(1))
        values.forEach { assertTrue("$id lost '$it'", r.redacted.contains(it)) }
    }
}

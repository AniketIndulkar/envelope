package dev.novanest.envelope.privacy

import org.junit.Assert.assertTrue
import org.junit.Test

class PatternRedactorTest {
    private val redactor = TestRules.redactor

    private fun assertSpan(text: String, value: String, type: PiiType) {
        val spans = redactor.find(text)
        assertTrue("expected $type '$value' in $spans", spans.any { it.text == value && it.type == type && it.source == SpanSource.PATTERN })
    }

    @Test fun findsNiNumber() = assertSpan("NI number: QQ 12 34 56 C", "QQ 12 34 56 C", PiiType.NI_NUMBER)
    @Test fun findsCompactNiNumber() = assertSpan("ref QQ123456C.", "QQ123456C", PiiType.NI_NUMBER)

    @Test fun findsSortCodeOnlyWhenLabelled() {
        assertSpan("Sort code: 12-34-56", "12-34-56", PiiType.ACCOUNT)
        assertTrue(redactor.find("Date: 12-04-26").none { it.type == PiiType.ACCOUNT })
    }

    @Test fun findsAccountNumberWhenLabelled() = assertSpan("Account number: 12345678", "12345678", PiiType.ACCOUNT)
    @Test fun findsIban() = assertSpan("IBAN GB29 NWBK 6016 1331 9268 19", "GB29 NWBK 6016 1331 9268 19", PiiType.ACCOUNT)
    @Test fun findsPostcode() = assertSpan("Leeds LS1 4AB", "LS1 4AB", PiiType.POSTCODE)
    @Test fun findsPhone() = assertSpan("Call 0113 496 0000 today", "0113 496 0000", PiiType.PHONE)
    @Test fun findsEmail() = assertSpan("Email ann.lee@example.co.uk now", "ann.lee@example.co.uk", PiiType.EMAIL)

    @Test fun cardRequiresLuhn() {
        assertSpan("Card 4111 1111 1111 1111", "4111 1111 1111 1111", PiiType.ACCOUNT)
        assertTrue(redactor.find("Card 4111 1111 1111 1112").none { it.type == PiiType.ACCOUNT })
    }

    @Test fun nhsNumberRequiresMod11() {
        assertSpan("NHS number 943 476 5919", "943 476 5919", PiiType.REFERENCE)
        assertTrue(redactor.find("Ref 943 476 5918").isEmpty())
    }

    @Test fun labelledDobRedactedButOtherDatesKept() {
        assertSpan("Date of birth: 03/07/1951", "03/07/1951", PiiType.DOB)
        assertTrue(redactor.find("Pay by 12/12/2026").isEmpty())
    }

    @Test fun moneyAndOrgNotRedacted() = assertTrue(redactor.find("HMRC says you owe £412.00").isEmpty())
    @Test fun utrFound() = assertSpan("UTR 12345 67890", "12345 67890", PiiType.REFERENCE)
    @Test fun councilTaxReferenceFound() = assertSpan("Council tax account: 8001234567", "8001234567", PiiType.REFERENCE)

    @Test fun yourReferenceNumberCapturesTheValue() = assertSpan("Your reference number: RGH2287731", "RGH2287731", PiiType.REFERENCE)
    @Test fun customerReferenceNumberCapturesTheValue() = assertSpan("Customer reference number: AB123456", "AB123456", PiiType.REFERENCE)
    @Test fun labelFollowedByAPlainWordIsNotAReference() =
        assertTrue(redactor.find("Case reference - please keep this letter").none { it.type == PiiType.REFERENCE })
}

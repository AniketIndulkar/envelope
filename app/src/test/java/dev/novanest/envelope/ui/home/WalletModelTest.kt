package dev.novanest.envelope.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class WalletModelTest {
    private val redacted = "Dear [NAME_1], [ADDRESS_1] [POSTCODE_1]. UTR [REFERENCE_1]. Dear [NAME_2], ref [REFERENCE_2], again [NAME_1]."

    @Test fun tokensAreDistinctInLetterOrder() = assertEquals(
        listOf("[NAME_1]", "[ADDRESS_1]", "[POSTCODE_1]", "[REFERENCE_1]", "[NAME_2]", "[REFERENCE_2]"),
        WalletLetter.tokensIn(redacted),
    )

    @Test fun ledgerCountsKindsInPrototypeOrderWithPlurals() = assertEquals(
        listOf("Names" to 2, "Home address" to 1, "Postcode" to 1, "Tax and payment references" to 2),
        WalletLetter.ledgerOf(WalletLetter.tokensIn(redacted)),
    )

    @Test fun niNumbersAreNotMistakenForNames() = assertEquals(
        listOf("Name" to 1, "National Insurance number" to 1),
        WalletLetter.ledgerOf(listOf("[NI_NUMBER_1]", "[NAME_1]")),
    )

    private fun letter(days: Int?, scam: Boolean = false) =
        WalletLetter("id", "HMRC", days, scam, null, "", emptyList(), emptyList())

    @Test fun stripLabels() {
        assertEquals("24 days", letter(24).stripLabel)
        assertEquals("Today", letter(0).stripLabel)
        assertEquals("1 day", letter(1).stripLabel)
        assertEquals("Overdue", letter(-3).stripLabel)
        assertEquals("–", letter(null).stripLabel)
        assertEquals("Scam?", letter(null, scam = true).stripLabel)
    }

    @Test fun units() {
        assertEquals("days\nleft", letter(5).unit)
        assertEquals("day\noverdue", letter(-1).unit)
    }
}

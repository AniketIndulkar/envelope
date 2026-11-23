package dev.novanest.envelope.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpLinksTest {
    @Test fun officialSitesAreTrusted() = listOf(
        "https://www.gov.uk/tax-appeals/penalty", "https://gov.uk", "https://www.nhs.uk/x", "https://111.nhs.uk",
        "https://www.citizensadvice.org.uk", "https://england.shelter.org.uk/housing_advice", "https://www.moneyhelper.org.uk",
    ).forEach { assertTrue(it, HelpLinks.isTrusted(it)) }

    @Test fun lookalikesAndPlainHttpAreNot() = listOf(
        "https://gov.uk.refund-claim.com", "http://www.gov.uk", "https://hmrc-refund.co.uk", "https://evilgov.uk", "not a url",
    ).forEach { assertFalse(it, HelpLinks.isTrusted(it)) }
}

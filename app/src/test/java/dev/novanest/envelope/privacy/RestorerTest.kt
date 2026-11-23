package dev.novanest.envelope.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestorerTest {
    private val t = listOf(
        TokenEntry("[NAME_1]", "Ann Lee", PiiType.NAME, SpanSource.NANO),
        TokenEntry("[NI_NUMBER_1]", "QQ123456C", PiiType.NI_NUMBER, SpanSource.PATTERN),
    )

    @Test fun restoresExactTokens() = assertEquals("Dear Ann Lee", Restorer.restore("Dear [NAME_1]", t).text)
    @Test fun restoresMangledTokens() = assertEquals("Ann Lee's account", Restorer.restore("[ name_1 ]'s account", t).text)
    @Test fun restoresMultiWordType() = assertEquals("NI: QQ123456C", Restorer.restore("NI: [NI_NUMBER_1]", t).text)

    @Test fun reportsUnknownTokens() {
        val r = Restorer.restore("[NAME_7] and [NAME_1]", t)
        assertEquals("[NAME_7] and Ann Lee", r.text)
        assertEquals(listOf("[NAME_7]"), r.unknownTokens)
    }

    @Test fun textWithoutTokensUnchanged() {
        val r = Restorer.restore("Pay £412 [by 12 Dec]", t)
        assertEquals("Pay £412 [by 12 Dec]", r.text)
        assertTrue(r.unknownTokens.isEmpty())
    }
}

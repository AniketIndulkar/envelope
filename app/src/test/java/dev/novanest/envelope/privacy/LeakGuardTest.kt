package dev.novanest.envelope.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LeakGuardTest {
    private val guard = LeakGuard(TestRules.redactor)
    private val tokens = listOf(TokenEntry("[NAME_1]", "Ann Lee", PiiType.NAME, SpanSource.NANO))

    @Test fun cleanTextPasses() = guard.assertSafeToSend("Dear [NAME_1], pay £412 by 12 Dec", tokens)

    @Test fun patternHitBlocks() {
        val e = assertThrows(LeakDetectedException::class.java) { guard.assertSafeToSend("NI QQ123456C", emptyList()) }
        assertEquals(listOf("QQ123456C"), e.leaks)
    }

    @Test fun tokenValueBlocks() {
        val e = assertThrows(LeakDetectedException::class.java) { guard.assertSafeToSend("Hello ANN\nLEE", tokens) }
        assertEquals(listOf("Ann Lee"), e.leaks)
    }

    @Test fun tokenValueInsideLongerWordIsNotALeak() =
        assertTrue(guard.findLeaks("Annual Leeway", listOf(TokenEntry("[NAME_1]", "Ann", PiiType.NAME, SpanSource.NANO))).isEmpty())
}

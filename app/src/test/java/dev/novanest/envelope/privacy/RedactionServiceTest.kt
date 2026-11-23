package dev.novanest.envelope.privacy

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RedactionServiceTest {
    private fun fakeFinder(vararg spans: PiiSpan) = SpanFinder { _ -> spans.toList() }
    private fun throwingFinder() = SpanFinder { _ -> throw IllegalStateException("Nano unavailable") }
    private val noEntities = SpanFinder { _ -> emptyList() }
    private fun service(nano: SpanFinder?) = RedactionService(TestRules.redactor, noEntities, nano)

    @Test fun usesNanoWhenAvailable() = runTest {
        val o = service(nano = fakeFinder(name("Ann Lee"))).redact("Dear Ann Lee")
        assertEquals(RedactionMode.NANO_AND_RULES, o.mode)
        assertEquals("Dear [NAME_1]", o.redaction.redacted)
    }

    @Test fun fallsBackToRulesWhenNanoThrows() = runTest {
        val o = service(nano = throwingFinder()).redact("Dear Ann, NI QQ123456C")
        assertEquals(RedactionMode.RULES_ONLY, o.mode)
        assertEquals("Dear Ann, NI [NI_NUMBER_1]", o.redaction.redacted)
    }

    @Test fun rulesOnlyWhenNoNano() = runTest {
        assertEquals(RedactionMode.RULES_ONLY, service(nano = null).redact("x y z").mode)
    }

    @Test fun manualSpansApplied() = runTest {
        val o = service(nano = null).redact("Dear Ann", listOf(PiiSpan("Ann", PiiType.NAME, SpanSource.MANUAL)))
        assertEquals("Dear [NAME_1]", o.redaction.redacted)
    }

    @Test fun ignoredValueNotRedacted() = runTest {
        val o = service(nano = fakeFinder(name("Ann Lee"))).redact("Dear Ann Lee", ignore = setOf("ann lee"))
        assertEquals("Dear Ann Lee", o.redaction.redacted)
    }

    @Test fun nanoCalledOncePerText() = runTest {
        var calls = 0
        val svc = service(nano = SpanFinder { _ -> calls++; listOf(name("Ann Lee")) })
        svc.redact("Dear Ann Lee")
        svc.redact("Dear Ann Lee", ignore = setOf("ann lee"))
        assertEquals(1, calls)
    }

    @Test fun entityLayerSpansMerged() = runTest {
        val entity = SpanFinder { _ -> listOf(PiiSpan("14 Elm Road", PiiType.ADDRESS, SpanSource.ENTITY)) }
        val o = RedactionService(TestRules.redactor, entity, null).redact("Lives at 14 Elm Road")
        assertEquals("Lives at [ADDRESS_1]", o.redaction.redacted)
    }
}

package dev.novanest.envelope.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactionMergerTest {

    @Test fun numbersTokensPerTypeInOrder() {
        val r = merge("Dear Ann Lee, ref QQ123456C. Ann Lee", name("Ann Lee"), ni("QQ123456C"))
        assertEquals("Dear [NAME_1], ref [NI_NUMBER_1]. [NAME_1]", r.redacted)
    }

    @Test fun numbersByFirstAppearanceNotSpanOrder() {
        val r = merge("Bob Ray wrote to Ann Lee", name("Ann Lee"), name("Bob Ray"))
        assertEquals("[NAME_1] wrote to [NAME_2]", r.redacted)
        assertEquals("Bob Ray", r.tokens.first { it.token == "[NAME_1]" }.value)
    }

    @Test fun longestValueWins() {
        val r = merge("Margaret O'Neill and Margaret", name("Margaret"), name("Margaret O'Neill"))
        assertEquals("[NAME_1] and [NAME_2]", r.redacted)
        assertEquals("Margaret O'Neill", r.tokens.first { it.token == "[NAME_1]" }.value)
    }

    @Test fun caseInsensitiveSameToken() =
        assertEquals("[NAME_1] / [NAME_1]", merge("SMITH / Smith", name("Smith")).redacted)

    @Test fun matchesAcrossLineBreak() =
        assertEquals("To [NAME_1]", merge("To Margaret\nO'Neill", name("Margaret O'Neill")).redacted)

    @Test fun wordBoundaryOnly() =
        assertEquals("[NAME_1]: Annual statement", merge("Ann: Annual statement", name("Ann")).redacted)

    @Test fun dropsSpansNotInText() {
        val r = merge("Hello there", name("John Smith"))
        assertEquals("Hello there", r.redacted)
        assertTrue(r.tokens.isEmpty())
    }

    @Test fun duplicateSpansFromSourcesCollapse() =
        assertEquals(1, merge("Ann Lee", name("Ann Lee", SpanSource.NANO), name("ann lee", SpanSource.ENTITY)).tokens.size)

    @Test fun countsByType() = assertEquals(
        mapOf(PiiType.NAME to 1, PiiType.NI_NUMBER to 1),
        merge("Ann QQ123456C", name("Ann"), ni("QQ123456C")).countsByType(),
    )

    @Test fun keepsOriginalText() = assertEquals("Dear Ann", merge("Dear Ann", name("Ann")).original)

    @Test fun valuesWithRegexCharactersAreLiteral() =
        assertEquals("Flat [ADDRESS_1]", merge("Flat 2 (rear), 14 Elm Rd.", address("2 (rear), 14 Elm Rd.")).redacted)

    @Test fun exposesTokenPositionsInRedactedText() {
        val r = merge("Dear Ann Lee, NI QQ123456C", name("Ann Lee"), ni("QQ123456C"))
        assertEquals(listOf("[NAME_1]", "[NI_NUMBER_1]"), r.placements.map { r.redacted.substring(it.start, it.end) })
        assertEquals(listOf("Ann Lee", "QQ123456C"), r.placements.map { r.original.substring(it.originalStart, it.originalEnd) })
    }

    @Test fun shortBareNumbersFromModelsAreIgnored() {
        val r = merge("Pay £1,000 by flat 1", PiiSpan("1", PiiType.REFERENCE, SpanSource.NANO))
        assertEquals("Pay £1,000 by flat 1", r.redacted)
    }

    @Test fun shortPatternMatchesAreStillHonoured() =
        assertEquals("ref [REFERENCE_1]", merge("ref 77", PiiSpan("77", PiiType.REFERENCE, SpanSource.PATTERN)).redacted)

    @Test fun partlyOverlappingSpansMergeIntoOneBlock() {
        val r = merge("Flat 2, 14 Elm Road, Leeds LS6", address("Flat 2, 14 Elm Road"), address("14 Elm Road, Leeds"))
        assertEquals("[ADDRESS_1] LS6", r.redacted)
        assertEquals("Flat 2, 14 Elm Road, Leeds", r.tokens.single().value)
    }

    @Test fun surnameAloneIsHiddenWhenTheFullNameIs() {
        val r = merge("Mrs Margaret O'Neill\nDear Mrs O'Neill, and Margaret too.", name("Margaret O'Neill"))
        assertEquals("Mrs [NAME_1]\nDear Mrs [NAME_2], and [NAME_3] too.", r.redacted)
        assertEquals(listOf("Margaret O'Neill", "O'Neill", "Margaret"), r.tokens.map { it.value })
    }

    @Test fun titlesAndShortPartsOfANameAreNotHiddenAlone() {
        val r = merge("Mrs Jo Li wrote. Mrs Smith replied.", name("Mrs Jo Li"))
        assertEquals("[NAME_1] wrote. Mrs Smith replied.", r.redacted)
    }
}

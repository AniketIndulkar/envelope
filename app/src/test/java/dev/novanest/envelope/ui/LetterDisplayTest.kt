package dev.novanest.envelope.ui

import dev.novanest.envelope.privacy.PiiSpan
import dev.novanest.envelope.privacy.PiiType
import dev.novanest.envelope.privacy.RedactionMerger
import dev.novanest.envelope.privacy.SpanSource
import org.junit.Assert.assertEquals
import org.junit.Test

class LetterDisplayTest {
    private val r = RedactionMerger.merge(
        "Dear Ann Lee, NI QQ123456C.",
        listOf(PiiSpan("Ann Lee", PiiType.NAME, SpanSource.NANO), PiiSpan("QQ123456C", PiiType.NI_NUMBER, SpanSource.PATTERN)),
    )

    @Test fun labelsAreReadable() {
        assertEquals("Name 1", LetterDisplay.label("[NAME_1]"))
        assertEquals("NI number 2", LetterDisplay.label("[NI_NUMBER_2]"))
        assertEquals("Date of birth 1", LetterDisplay.label("[DOB_1]"))
    }

    @Test fun displayReplacesTokensWithLabels() {
        val d = LetterDisplay.of(r)
        assertEquals("Dear  Name 1 , NI  NI number 1 .", d.text)
        assertEquals(listOf(" Name 1 ", " NI number 1 "), d.chips.map { d.text.substring(it.start, it.end) })
        assertEquals(listOf("Ann Lee", "QQ123456C"), d.chips.map { it.value })
    }

    @Test fun wordsOutsideChipsAreTappable() {
        val d = LetterDisplay.of(r)
        assertEquals(listOf("Dear", "NI"), d.words.map { d.text.substring(it.first, it.last + 1) })
    }

    @Test fun manualTypeGuess() {
        assertEquals(PiiType.NAME, LetterDisplay.manualSpan("Margaret").type)
        assertEquals(PiiType.REFERENCE, LetterDisplay.manualSpan("RGH2287731").type)
        assertEquals(SpanSource.MANUAL, LetterDisplay.manualSpan("x").source)
    }

    @Test fun suggestsNamesAfterTitlesAndAddresses() {
        val plain = RedactionMerger.merge("Mrs Margaret O'Neill\n14 Elm Road\nDear Mrs O'Neill, pay by Friday.", emptyList())
        val d = LetterDisplay.of(plain, suggest = true)
        assertEquals(listOf("Margaret O'Neill", "14 Elm Road", "O'Neill"), d.suggestions.map { d.text.substring(it.first, it.last + 1) })
    }

    @Test fun noSuggestionsUnlessAsked() =
        assertEquals(emptyList<IntRange>(), LetterDisplay.of(RedactionMerger.merge("Dear Mr Smith", emptyList())).suggestions)
}

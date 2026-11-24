package dev.novanest.envelope.ui.components

import dev.novanest.envelope.privacy.PiiType
import dev.novanest.envelope.privacy.SpanSource
import dev.novanest.envelope.privacy.TokenEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LineHelpersTest {
    private fun tok(value: String) = TokenEntry("[NAME_1]", value, PiiType.NAME, SpanSource.PATTERN)

    @Test fun scrambleStartsAsGlyphsOfTheStandInLengthAndEndsAsTheRealValue() {
        assertEquals(6, scramble("Name 1", "Margaret O’Neill", 0f, Random(1)).length)
        assertEquals("Margaret O’Neill", scramble("Name 1", "Margaret O’Neill", 1f, Random(1)))
    }

    @Test fun scrambleHalfwayKeepsTheFirstHalfOfTheTarget() {
        val s = scramble("Ref 1", "1234567890", 0.5f, Random(1))
        assertTrue(s.startsWith("12345"))
    }

    @Test fun rangesPreferTheLongerValueWhenOneContainsAnother() {
        val text = "Yours, Margaret O'Neill. Mrs O'Neill"
        val r = realDetailRanges(text, listOf(tok("O'Neill"), tok("Margaret O'Neill")))
        assertEquals(listOf("Margaret O'Neill", "O'Neill"), r.map { text.substring(it) })
    }

    @Test fun rangesIgnoreBlankValuesAndFindRepeats() {
        val text = "LS6 2QT and LS6 2QT"
        val r = realDetailRanges(text, listOf(tok(""), tok("LS6 2QT")))
        assertEquals(listOf(0..6, 12..18), r)
    }

    @Test fun warmColourIsWhiteWithTimeAndPeachNearTheDeadline() {
        assertEquals(dev.novanest.envelope.ui.theme.OnSky, warmColor(30))
        assertEquals(dev.novanest.envelope.ui.theme.Accent, warmColor(2))
    }
}

class DecodeScheduleTest {
    @Test fun shortLettersUseThePrototypeTiming() {
        val s = DecodeSchedule(3)
        assertEquals(450L, s.startAt(0))
        assertEquals(790L, s.startAt(1))
        assertEquals(790L + DecodeSchedule.DecodeMillis, s.lockAt(1))
    }

    @Test fun longLettersFinishDecodingWithinAboutThreeSeconds() {
        val s = DecodeSchedule(40)
        assertTrue(s.lockAt(39) <= 450L + 2400L + DecodeSchedule.DecodeMillis)
    }

    @Test fun theStageRunsAtLeastAsLongAsTheBeam() {
        assertEquals(DecodeSchedule.BeamEnd, DecodeSchedule(0).endMillis)
        assertEquals(DecodeSchedule.BeamEnd, DecodeSchedule(2).endMillis)
    }
}

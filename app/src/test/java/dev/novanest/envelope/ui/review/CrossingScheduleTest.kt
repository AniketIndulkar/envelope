package dev.novanest.envelope.ui.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossingScheduleTest {
    @Test fun fewStandInsUseThePrototypeTiming() {
        val s = CrossingSchedule(3)
        assertEquals(250L, s.leaveAt(0))
        assertEquals(510L, s.leaveAt(1))
        assertEquals(540L, s.crossAt(1))
        assertEquals(770L + CrossingSchedule.RiseMillis, s.doneMillis)
    }

    @Test fun manyStandInsStillFinishCrossingInAboutThreeAndAHalfSeconds() {
        assertTrue(CrossingSchedule(flyingCount(30)).doneMillis <= 250L + 2100L + CrossingSchedule.RiseMillis)
    }

    @Test fun nothingToCrossIsDoneAtOnce() = assertEquals(0L, CrossingSchedule(0).doneMillis)

    @Test fun moreThanTwelveStandInsAddOnePlusMoreChip() {
        assertEquals(12, flyingCount(12))
        assertEquals(13, flyingCount(30))
    }
}

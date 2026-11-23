package dev.novanest.envelope.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DatesTest {
    private val today = LocalDate.of(2026, 11, 23)
    @Test fun daysLeft() = assertEquals(25, Dates.daysLeft("2026-12-18", today))
    @Test fun pastIsNegative() = assertEquals(-3, Dates.daysLeft("2026-11-20", today))
    @Test fun badDateIsNull() = assertNull(Dates.daysLeft("soon", today))
    @Test fun nullIsNull() = assertNull(Dates.daysLeft(null, today))
    @Test fun pretty() = assertEquals("18 December", Dates.pretty("2026-12-18", today))
    @Test fun prettyOtherYear() = assertEquals("1 February 2027", Dates.pretty("2027-02-01", today))
}

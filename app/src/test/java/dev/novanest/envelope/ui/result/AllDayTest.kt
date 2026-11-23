package dev.novanest.envelope.ui.result

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AllDayTest {
    /** CalendarContract wants all-day events at UTC midnight; local midnight shifts the day east or west of UTC. */
    @Test fun allDayEventIsUtcMidnightToMidnight() {
        val (start, end) = allDayMillis(LocalDate.of(2026, 12, 18))
        assertEquals(1797552000000L, start) // 2026-12-18T00:00:00Z
        assertEquals(start + 86_400_000L, end)
    }
}

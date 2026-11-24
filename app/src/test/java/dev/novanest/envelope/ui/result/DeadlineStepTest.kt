package dev.novanest.envelope.ui.result

import dev.novanest.envelope.ai.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeadlineStepTest {
    private val steps = listOf(
        Action("Send your tax return"),
        Action("Pay £100", by = "2026-12-18"),
        Action("Appeal, if you had a good reason", by = "2026-12-20"),
    )

    @Test fun picksTheStepDueOnTheDeadline() = assertEquals(1, deadlineStepIndex(steps, "2026-12-18"))

    @Test fun toleratesStraySpacesAroundDates() =
        assertEquals(2, deadlineStepIndex(steps.map { it.copy(by = it.by?.let { b -> " $b " }) }, "2026-12-20 "))

    @Test fun noDeadlineMeansNoHighlight() = assertNull(deadlineStepIndex(steps, null))

    @Test fun noStepOnTheDeadlineMeansNoHighlight() = assertNull(deadlineStepIndex(steps, "2027-01-31"))

    @Test fun unparseableDeadlineMeansNoHighlight() = assertNull(deadlineStepIndex(steps, "18 December"))

    @Test fun twoStepsOnTheSameDateHighlightsTheFirst() =
        assertEquals(1, deadlineStepIndex(steps + Action("Tell your bank", by = "2026-12-18"), "2026-12-18"))
}

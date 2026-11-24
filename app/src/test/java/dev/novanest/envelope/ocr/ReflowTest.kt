package dev.novanest.envelope.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class ReflowTest {
    @Test fun joinsALineThatContinuesASentence() =
        assertEquals("Call our security team on 0113 496 0000.", Reflow.lines("Call our security team\non 0113 496 0000."))

    @Test fun keepsAddressLines() =
        assertEquals("Mrs Ann Lee\n14 Elm Road\nLeeds\nLS6 2QT", Reflow.lines("Mrs Ann Lee\n14 Elm Road\nLeeds\nLS6 2QT"))

    @Test fun keepsParagraphBreaks() =
        assertEquals("Dear Ann,\n\nWe have not received it.", Reflow.lines("Dear Ann,\n\nWe have not\nreceived it."))

    @Test fun trimsTrailingSpacesBeforeJoining() =
        assertEquals("paid using gift cards", Reflow.lines("paid using   \ngift cards"))
}

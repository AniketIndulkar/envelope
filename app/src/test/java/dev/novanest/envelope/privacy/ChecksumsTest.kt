package dev.novanest.envelope.privacy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecksumsTest {
    @Test fun luhnAcceptsValidCard() = assertTrue(Checksums.luhn("4111 1111 1111 1111"))
    @Test fun luhnRejectsInvalidCard() = assertFalse(Checksums.luhn("4111 1111 1111 1112"))
    @Test fun nhsMod11AcceptsValid() = assertTrue(Checksums.nhsMod11("943 476 5919"))
    @Test fun nhsMod11RejectsInvalid() = assertFalse(Checksums.nhsMod11("943 476 5918"))
    @Test fun nhsMod11RejectsWrongLength() = assertFalse(Checksums.nhsMod11("943 476 591"))
}

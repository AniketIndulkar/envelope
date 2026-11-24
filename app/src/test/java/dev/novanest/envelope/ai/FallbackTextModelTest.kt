package dev.novanest.envelope.ai

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FallbackTextModelTest {
    private class Busy : RuntimeException("high demand")
    private val retryable: (Throwable) -> Boolean = { it is Busy }
    private fun ok(name: String) = TextModel { _ -> ModelReply(name, Engine.IN_CLOUD) }
    private val busy = TextModel { _ -> throw Busy() }

    @Test fun usesTheFirstModelWhenItWorks() = runTest {
        assertEquals("a", FallbackTextModel(listOf(ok("a"), ok("b")), retryable).generate("p").text)
    }

    @Test fun movesToTheNextModelWhenOneIsBusy() = runTest {
        assertEquals("b", FallbackTextModel(listOf(busy, ok("b")), retryable).generate("p").text)
    }

    @Test fun throwsTheLastErrorWhenAllAreBusy() {
        assertThrows(Busy::class.java) { runBlocking { FallbackTextModel(listOf(busy, busy), retryable).generate("p") } }
    }

    @Test fun otherErrorsAreNotRetriedOnAnotherModel() {
        var secondCalled = false
        val broken = TextModel { _ -> throw IllegalStateException("bad request") }
        val second = TextModel { _ -> secondCalled = true; ModelReply("b", Engine.IN_CLOUD) }
        assertThrows(IllegalStateException::class.java) { runBlocking { FallbackTextModel(listOf(broken, second), retryable).generate("p") } }
        assertEquals(false, secondCalled)
    }
}

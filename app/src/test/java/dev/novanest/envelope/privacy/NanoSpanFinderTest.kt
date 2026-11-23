package dev.novanest.envelope.privacy

import dev.novanest.envelope.ai.Engine
import dev.novanest.envelope.ai.ModelReply
import dev.novanest.envelope.ai.Prompts
import dev.novanest.envelope.ai.TextModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NanoSpanFinderTest {
    private val ok = """{"entities":[{"text":"Ann Lee","type":"NAME"}]}"""

    private fun scripted(vararg replies: () -> ModelReply): Pair<TextModel, () -> Int> {
        var i = 0
        return TextModel { _ -> replies[i++].invoke() } to { i }
    }

    private fun finder(model: TextModel) = NanoSpanFinder(model, Prompts(), maxWords = 2500)

    @Test fun returnsNanoSpans() = runTest {
        val (model, _) = scripted({ ModelReply(ok, Engine.ON_DEVICE) })
        assertEquals(listOf(PiiSpan("Ann Lee", PiiType.NAME, SpanSource.NANO)), finder(model).find("Dear Ann Lee"))
    }

    @Test fun retriesFailedChunkOnce() = runTest {
        val (model, calls) = scripted({ ModelReply("garbage", Engine.ON_DEVICE) }, { ModelReply(ok, Engine.ON_DEVICE) })
        assertEquals(1, finder(model).find("Dear Ann Lee").size)
        assertEquals(2, calls())
    }

    @Test fun throwsWhenRetryAlsoFails() = runTest {
        val (model, calls) = scripted({ throw IllegalStateException("busy") }, { throw IllegalStateException("busy") })
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { finder(model).find("Dear Ann Lee") } }
        assertEquals(2, calls())
    }

    @Test fun offDeviceReplyThrowsWithoutRetry() = runTest {
        val (model, calls) = scripted({ ModelReply(ok, Engine.IN_CLOUD) })
        assertThrows(NanoOffDeviceException::class.java) { kotlinx.coroutines.runBlocking { finder(model).find("Dear Ann Lee") } }
        assertEquals(1, calls())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun cancellationIsNotRetried() = runTest {
        var calls = 0
        val model = TextModel { _ -> calls++; awaitCancellation() }
        val job = launch { finder(model).find("Dear Ann Lee") }
        runCurrent()
        job.cancel(); job.join()
        assertEquals(1, calls)
    }
}

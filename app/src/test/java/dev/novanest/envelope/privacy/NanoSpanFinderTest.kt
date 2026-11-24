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

    @Test fun throwsWhenRetryAndSmallerPiecesAlsoFail() = runTest {
        val busy: () -> ModelReply = { throw IllegalStateException("busy") }
        val (model, calls) = scripted(busy, busy, busy, busy)
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { finder(model).find("Dear Ann Lee") } }
        assertEquals(3, calls()) // whole chunk, retry, then the first half fails and the letter goes rules-only
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

    @Test fun aChunkThatKeepsFailingIsRetriedAsTwoHalves() = runTest {
        // Nano's output budget is small: a dense chunk truncates its JSON twice, but each half fits.
        val model = TextModel { prompt ->
            val letter = prompt.substringAfterLast("Letter:")
            if (letter.contains("Ann Lee") && letter.contains("Bob Ray")) ModelReply("""{"entities":[{"text":"Ann""", Engine.ON_DEVICE)
            else if (letter.contains("Ann Lee")) ModelReply("""{"entities":[{"text":"Ann Lee","type":"NAME"}]}""", Engine.ON_DEVICE)
            else ModelReply("""{"entities":[{"text":"Bob Ray","type":"NAME"}]}""", Engine.ON_DEVICE)
        }
        val spans = finder(model).find("Dear Ann Lee, thanks.\n\nRegards, Bob Ray.")
        assertEquals(setOf("Ann Lee", "Bob Ray"), spans.map { it.text }.toSet())
    }
}

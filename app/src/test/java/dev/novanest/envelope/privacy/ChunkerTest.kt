package dev.novanest.envelope.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkerTest {
    private fun para(words: Int) = (1..words).joinToString(" ") { "w$it" }
    private fun wordCount(s: String) = s.split(Regex("\\s+")).count { it.isNotEmpty() }

    @Test fun chunkKeepsParagraphsUnderLimit() {
        val c = Chunker.chunk(para(100) + "\n\n" + para(100) + "\n\n" + para(100), 250)
        assertEquals(2, c.size)
        assertTrue(c.all { wordCount(it) <= 250 })
    }

    @Test fun shortTextIsOneChunk() = assertEquals(listOf("Dear Ann"), Chunker.chunk("Dear Ann", 250))

    @Test fun oversizeParagraphSplitsOnSentences() {
        val sentence = para(30) + "."
        val c = Chunker.chunk(List(10) { sentence }.joinToString(" "), 100)
        assertTrue(c.size >= 3)
        assertTrue(c.all { wordCount(it) <= 100 })
    }

    @Test fun noWordsLost() {
        val text = para(100) + "\n\n" + para(100) + "\n\n" + para(100)
        assertEquals(300, Chunker.chunk(text, 120).sumOf(::wordCount))
    }
}

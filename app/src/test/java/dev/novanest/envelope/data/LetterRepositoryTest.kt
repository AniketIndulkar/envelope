package dev.novanest.envelope.data

import dev.novanest.envelope.ai.Explanation
import dev.novanest.envelope.ai.QuickCheck
import dev.novanest.envelope.privacy.LeakDetectedException
import dev.novanest.envelope.privacy.LeakGuard
import dev.novanest.envelope.privacy.PiiType
import dev.novanest.envelope.privacy.RedactionMode
import dev.novanest.envelope.privacy.SpanSource
import dev.novanest.envelope.privacy.TestRules
import dev.novanest.envelope.privacy.TokenEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeRemote : RemoteLetterStore {
    val docs = MutableStateFlow<List<LetterRecord>>(emptyList())
    override suspend fun put(record: LetterRecord) { docs.value = docs.value + record }
    override fun observe(): Flow<List<LetterRecord>> = docs
}

class FakeTokens : TokenStore {
    val saved = mutableMapOf<String, List<TokenEntry>>()
    override suspend fun put(letterId: String, tokens: List<TokenEntry>) { saved[letterId] = tokens }
    override suspend fun get(letterId: String) = saved[letterId].orEmpty()
}

class LetterRepositoryTest {
    private val remote = FakeRemote()
    private val tokenStore = FakeTokens()
    private val repo = LetterRepository(remote, tokenStore, LeakGuard(TestRules.redactor))
    private val ann = TokenEntry("[NAME_1]", "Ann Lee", PiiType.NAME, SpanSource.NANO)

    private fun record(id: String, redacted: String = "Dear [NAME_1]", deadline: String? = null, createdAt: Long = 0, reply: String = "From [NAME_1]") =
        LetterRecord(
            id = id, redactedText = redacted,
            quickCheck = QuickCheck(sender = "HMRC", deadline = deadline, oneLine = "Pay"),
            explanation = Explanation(summary = "s", whatItMeans = "w", draftReply = reply),
            redactionMode = RedactionMode.NANO_AND_RULES, engines = emptyList(), createdAt = createdAt,
        )

    @Test fun saveWritesRedactedRemotelyAndTokensLocally() = runTest {
        repo.save(record("a"), listOf(ann))
        assertEquals("Dear [NAME_1]", remote.docs.value.single().redactedText)
        assertEquals(listOf(ann), tokenStore.get("a"))
    }

    @Test fun saveRefusesLeakInLetterAndStoresNothing() {
        assertThrows(LeakDetectedException::class.java) { runBlocking { repo.save(record("b", redacted = "Dear Ann Lee"), listOf(ann)) } }
        assertTrue(remote.docs.value.isEmpty())
        assertTrue(tokenStore.saved.isEmpty())
    }

    @Test fun saveRefusesRealValueInExplanation() {
        assertThrows(LeakDetectedException::class.java) { runBlocking { repo.save(record("c", reply = "From Ann Lee"), listOf(ann)) } }
    }

    @Test fun saveAllowsNumbersTheModelAddedItself() = runTest {
        repo.save(record("d", reply = "Call HMRC on 0300 200 3300"), listOf(ann))
        assertEquals(1, remote.docs.value.size)
    }

    @Test fun saveStillRefusesPatternsInTheLetterText() {
        assertThrows(LeakDetectedException::class.java) { runBlocking { repo.save(record("e", redacted = "NI QQ123456C"), listOf(ann)) } }
    }

    @Test fun historySortedByDeadlineThenNewest() = runTest {
        repo.save(record("none-old", createdAt = 1), listOf(ann))
        repo.save(record("late", deadline = "2026-12-20", createdAt = 2), listOf(ann))
        repo.save(record("soon", deadline = "2026-12-01", createdAt = 3), listOf(ann))
        repo.save(record("none-new", createdAt = 4), listOf(ann))
        assertEquals(listOf("soon", "late", "none-new", "none-old"), repo.history().first().map { it.id })
    }

    @Test fun findReturnsTheRecordOnceItArrives() = runTest {
        repo.save(record("x"), listOf(ann))
        assertEquals("x", repo.find("x", timeoutMs = 1_000)?.id)
    }

    @Test fun findGivesUpInsteadOfWaitingForever() = runTest {
        assertEquals(null, repo.find("missing", timeoutMs = 1_000))
    }
}

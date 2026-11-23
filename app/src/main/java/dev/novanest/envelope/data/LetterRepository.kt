package dev.novanest.envelope.data

import dev.novanest.envelope.privacy.LeakGuard
import dev.novanest.envelope.privacy.TokenEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

/** Cloud copy of letters (redacted only). */
interface RemoteLetterStore {
    suspend fun put(record: LetterRecord)
    fun observe(): Flow<List<LetterRecord>>
}

/** On-phone token maps (real details). Never synced or backed up. */
interface TokenStore {
    suspend fun put(letterId: String, tokens: List<TokenEntry>)
    suspend fun get(letterId: String): List<TokenEntry>
}

class LetterRepository(
    private val remote: RemoteLetterStore,
    private val tokenStore: TokenStore,
    private val guard: LeakGuard,
) {
    /** Throws [dev.novanest.envelope.privacy.LeakDetectedException] and saves nothing if a real detail would leave the phone. */
    suspend fun save(record: LetterRecord, tokens: List<TokenEntry>) {
        guard.assertSafeToSend(record.redactedText, tokens)
        guard.assertNoRealValues(record.modelText(), tokens)
        tokenStore.put(record.id, tokens)
        remote.put(record)
    }

    /** Soonest deadline first; letters without a deadline after, newest first. */
    fun history(): Flow<List<LetterRecord>> = remote.observe().map { list ->
        list.sortedWith(compareBy<LetterRecord, String?>(nullsLast()) { it.deadline }.thenByDescending { it.createdAt })
    }

    suspend fun tokens(id: String): List<TokenEntry> = tokenStore.get(id)

    /** Waits for [id] to appear in history (the first snapshot can be an empty cache), but never forever. */
    suspend fun find(id: String, timeoutMs: Long = 8_000): LetterRecord? =
        withTimeoutOrNull(timeoutMs) { remote.observe().first { list -> list.any { it.id == id } }.first { it.id == id } }
}

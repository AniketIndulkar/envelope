package dev.novanest.envelope.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dev.novanest.envelope.ai.ModelJson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** users/{uid}/letters/{id} — the record as JSON plus fields for ordering. Security rules restrict to the owner. */
class FirestoreLetterStore(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : RemoteLetterStore {

    private suspend fun uid(): String =
        auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid ?: error("Not signed in")

    private fun letters(uid: String) = firestore.collection("users").document(uid).collection("letters")

    override suspend fun put(record: LetterRecord) {
        letters(uid()).document(record.id).set(
            mapOf(
                "payload" to ModelJson.json.encodeToString(LetterRecord.serializer(), record),
                "deadline" to record.deadline,
                "createdAt" to record.createdAt,
            )
        ).await()
    }

    override fun observe(): Flow<List<LetterRecord>> = callbackFlow {
        var registration: ListenerRegistration? = null
        launch {
            val id = runCatching { uid() }.getOrElse { trySend(emptyList()); return@launch }
            registration = letters(id).addSnapshotListener { snap, err ->
                if (err != null) { Log.w("FirestoreLetterStore", "history failed", err); return@addSnapshotListener }
                val records = snap?.documents.orEmpty().mapNotNull { doc ->
                    doc.getString("payload")?.let { runCatching { ModelJson.json.decodeFromString(LetterRecord.serializer(), it) }.getOrNull() }
                }
                trySend(records)
            }
        }
        awaitClose { registration?.remove() }
    }
}

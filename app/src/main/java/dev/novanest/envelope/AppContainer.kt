package dev.novanest.envelope

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.remoteconfig.remoteConfig
import dev.novanest.envelope.ai.FirebaseModels
import dev.novanest.envelope.ai.NanoStatus
import dev.novanest.envelope.ai.TextModel
import dev.novanest.envelope.data.EnvelopeDb
import dev.novanest.envelope.data.FirestoreLetterStore
import dev.novanest.envelope.data.LetterRepository
import dev.novanest.envelope.data.RemoteSettings
import dev.novanest.envelope.data.RoomTokenStore
import dev.novanest.envelope.data.SampleLetters
import dev.novanest.envelope.ocr.TextReader
import dev.novanest.envelope.pipeline.LetterPipeline
import dev.novanest.envelope.privacy.EntitySpanFinder
import dev.novanest.envelope.privacy.LeakGuard
import dev.novanest.envelope.privacy.NanoSpanFinder
import dev.novanest.envelope.privacy.RedactionService

/** Single place where dependencies are built. Cloud pieces are null when Firebase isn't configured. */
class AppContainer(
    val context: Context,
    val firebaseReady: Boolean,
) {
    val settings = RemoteSettings(
        rc = if (firebaseReady) Firebase.remoteConfig else null,
        bundledPatterns = context.assets.open("redaction_patterns.json").bufferedReader().readText(),
    )
    val textReader by lazy { TextReader(context) }
    val samples by lazy { SampleLetters(context.assets) }
    private val db by lazy { EnvelopeDb.create(context) }

    val models: FirebaseModels? by lazy { if (firebaseReady) FirebaseModels(settings.cloudModelName) else null }
    val nanoStatus by lazy { NanoStatus(models?.redactorModel) }

    val letters: LetterRepository? by lazy {
        if (!firebaseReady) null
        else LetterRepository(FirestoreLetterStore(Firebase.firestore, Firebase.auth), RoomTokenStore(db.tokenMaps()), guard())
    }

    fun guard() = LeakGuard(settings.patternRedactor)

    /** Builds a pipeline for one letter. [forceRulesOnly] lets the demo show the no-Nano fallback on a Nano phone. */
    fun newPipeline(forceRulesOnly: Boolean): LetterPipeline {
        val m = models
        val prompts = settings.prompts
        val patterns = settings.patternRedactor
        val nano = if (forceRulesOnly || m == null) null else NanoSpanFinder(m.redactor, prompts, settings.nanoChunkWords)
        return LetterPipeline(
            redaction = RedactionService(patterns, EntitySpanFinder(), nano),
            guard = LeakGuard(patterns),
            quick = m?.quickCheck ?: notConfigured,
            explainer = m?.explainer ?: notConfigured,
            prompts = prompts,
        )
    }

    /** Without google-services.json the app still scans and hides details on the phone; only cloud steps are off. */
    private val notConfigured = TextModel { throw IllegalStateException("Add your Firebase config (google-services.json) to explain letters") }
}

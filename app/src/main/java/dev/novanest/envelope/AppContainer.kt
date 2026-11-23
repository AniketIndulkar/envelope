package dev.novanest.envelope

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    /** One ML Kit entity client for the app's lifetime (it holds a loaded model; one per letter leaked them). */
    private val entityFinder by lazy { EntitySpanFinder() }
    val samples by lazy { SampleLetters(context.assets) }
    private val db by lazy { EnvelopeDb.create(context) }

    /** App-wide scope for work that must outlive a screen, like the Nano model download. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var cachedModels: FirebaseModels? = null

    /**
     * Rebuilt when Remote Config's cloud_model_name changes, so a new value applies to the next letter rather
     * than the next app launch (Remote Config activates after start-up).
     */
    val models: FirebaseModels?
        get() {
            if (!firebaseReady) return null
            val name = settings.cloudModelName
            return cachedModels?.takeIf { it.cloudModelName == name } ?: FirebaseModels(name).also { cachedModels = it }
        }

    val nanoStatus by lazy { NanoStatus(models?.redactorModel) }

    fun downloadNano() { appScope.launch { nanoStatus.download() } }

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
            redaction = RedactionService(patterns, entityFinder, nano),
            guard = LeakGuard(patterns),
            quick = m?.quickCheck ?: notConfigured,
            explainer = m?.explainer ?: notConfigured,
            prompts = prompts,
        )
    }

    /** Without google-services.json the app still scans and hides details on the phone; only cloud steps are off. */
    private val notConfigured = TextModel { throw IllegalStateException("Add your Firebase config (google-services.json) to explain letters") }
}

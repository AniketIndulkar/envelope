package dev.novanest.envelope.data

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dev.novanest.envelope.ai.Prompts
import dev.novanest.envelope.privacy.PatternRedactor

/** Remote Config values with bundled fallbacks, so the app works offline and on first run. */
class RemoteSettings(private val rc: FirebaseRemoteConfig?, private val bundledPatterns: String) {

    val cloudModelName: String get() = rc?.getString("cloud_model_name")?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
    val nanoChunkWords: Int get() = rc?.getLong("nano_chunk_words")?.toInt()?.takeIf { it > 0 } ?: DEFAULT_CHUNK_WORDS
    val patternRedactor: PatternRedactor get() = patterns(rc?.getString("redaction_patterns_json").orEmpty(), bundledPatterns)

    val prompts: Prompts get() = Prompts(
        redactTemplate = prompt("prompt_redact") ?: Prompts.REDACT,
        quickCheckTemplate = prompt("prompt_quickcheck") ?: Prompts.QUICK_CHECK,
        explainTemplate = prompt("prompt_explain") ?: Prompts.EXPLAIN,
    )

    private fun prompt(key: String) = rc?.getString(key)?.takeIf { it.contains("{text}") }

    companion object {
        const val DEFAULT_MODEL = "gemini-3.8-flash"

        /** Nano's ~256-token output budget, not its 4000-token input, is the limit: keep chunks small enough that the span list fits. */
        const val DEFAULT_CHUNK_WORDS = 400

        /**
         * Remote rules can only ADD to the bundled ones: a broken, empty or partial remote list never weakens
         * redaction. Each remote rule must compile, or the whole remote list is ignored.
         */
        fun patterns(remote: String, bundled: String): PatternRedactor {
            val base = PatternRedactor.rulesFromJson(bundled)
            val extra = remote.takeIf { it.isNotBlank() }
                ?.let { runCatching { PatternRedactor.rulesFromJson(it).onEach { r -> Regex(r.regex) } }.getOrNull() }
                .orEmpty()
            return PatternRedactor((base + extra).distinct())
        }
    }
}

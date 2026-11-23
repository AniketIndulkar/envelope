package dev.novanest.envelope.data

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dev.novanest.envelope.ai.Prompts
import dev.novanest.envelope.privacy.PatternRedactor

/** Remote Config values with bundled fallbacks, so the app works offline and on first run. */
class RemoteSettings(private val rc: FirebaseRemoteConfig?, private val bundledPatterns: String) {

    val cloudModelName: String get() = rc?.getString("cloud_model_name")?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
    val nanoChunkWords: Int get() = rc?.getLong("nano_chunk_words")?.toInt()?.takeIf { it > 0 } ?: 2500
    val patternRedactor: PatternRedactor get() = patterns(rc?.getString("redaction_patterns_json").orEmpty(), bundledPatterns)

    val prompts: Prompts get() = Prompts(
        redactTemplate = prompt("prompt_redact") ?: Prompts.REDACT,
        quickCheckTemplate = prompt("prompt_quickcheck") ?: Prompts.QUICK_CHECK,
        explainTemplate = prompt("prompt_explain") ?: Prompts.EXPLAIN,
    )

    private fun prompt(key: String) = rc?.getString(key)?.takeIf { it.contains("{text}") }

    companion object {
        const val DEFAULT_MODEL = "gemini-3.8-flash"

        /** A broken or empty remote rule set must never weaken redaction: fall back to the bundled rules. */
        fun patterns(remote: String, bundled: String): PatternRedactor =
            remote.takeIf { it.isNotBlank() }
                ?.let { runCatching { PatternRedactor.fromJson(it) }.getOrNull() }
                ?.takeIf { it.ruleCount > 0 }
                ?: PatternRedactor.fromJson(bundled)
    }
}

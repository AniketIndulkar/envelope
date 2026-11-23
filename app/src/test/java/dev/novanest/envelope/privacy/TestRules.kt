package dev.novanest.envelope.privacy

import java.io.File

/** Loads the bundled default rules exactly as the app ships them. */
object TestRules {
    val json: String by lazy { File("src/main/assets/redaction_patterns.json").readText() }
    val redactor: PatternRedactor by lazy { PatternRedactor.fromJson(json) }
}

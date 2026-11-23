package dev.novanest.envelope.data

import android.content.Context

/** Small user settings. Nothing personal is stored here. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("envelope_prefs", Context.MODE_PRIVATE)

    var language: String
        get() = sp.getString("language", "English") ?: "English"
        set(v) = sp.edit().putString("language", v).apply()

    /** Debug-only: film the no-Nano fallback on a Nano phone. */
    var forceRulesOnly: Boolean
        get() = sp.getBoolean("force_rules_only", false)
        set(v) = sp.edit().putBoolean("force_rules_only", v).apply()

    companion object {
        val LANGUAGES = listOf("English", "Polish", "Urdu", "Punjabi", "Romanian", "Bengali", "Arabic", "Spanish")
    }
}

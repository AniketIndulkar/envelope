package dev.novanest.envelope

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.auth
import com.google.firebase.remoteconfig.remoteConfig

class EnvelopeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val firebaseReady = initFirebase()
        container = AppContainer(this, firebaseReady)
    }

    /** Returns false when no google-services.json was bundled; AI and cloud features then show "not configured". */
    private fun initFirebase(): Boolean {
        val app = FirebaseApp.initializeApp(this) ?: run {
            Log.w(TAG, "Firebase not configured: add app/google-services.json")
            return false
        }
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) debugAppCheckFactory() ?: PlayIntegrityAppCheckProviderFactory.getInstance()
            else PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        Firebase.remoteConfig.apply {
            setDefaultsAsync(R.xml.remote_config_defaults)
            fetchAndActivate()
        }
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signInAnonymously()
                .addOnFailureListener { Log.w(TAG, "Anonymous sign-in failed", it) }
        }
        Log.i(TAG, "Firebase ready: ${app.name}")
        return true
    }

    /**
     * The debug provider ships only in debug builds (debugImplementation), so release builds can't contain it.
     * Loaded by name because main code is compiled for both.
     */
    private fun debugAppCheckFactory(): AppCheckProviderFactory? = runCatching {
        Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
            .getMethod("getInstance").invoke(null) as AppCheckProviderFactory
    }.getOrNull()

    private companion object { const val TAG = "EnvelopeApp" }
}

package dev.novanest.envelope

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
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
            if (BuildConfig.DEBUG) DebugAppCheckProviderFactory.getInstance()
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

    private companion object { const val TAG = "EnvelopeApp" }
}

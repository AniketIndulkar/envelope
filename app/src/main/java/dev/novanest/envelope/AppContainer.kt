package dev.novanest.envelope

import android.content.Context

/** Single place where dependencies are built. Later tasks add fields here. */
class AppContainer(
    val context: Context,
    val firebaseReady: Boolean,
)

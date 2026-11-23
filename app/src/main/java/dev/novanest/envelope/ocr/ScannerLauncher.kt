package dev.novanest.envelope.ocr

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

private val options = GmsDocumentScannerOptions.Builder()
    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
    .setGalleryImportAllowed(true)
    .setPageLimit(5)
    .build()

/**
 * The Document Scanner is a Play services module downloaded on first use. Ask for it at launch so it's
 * usually ready by the time someone taps Scan.
 */
fun prefetchScanner(context: Context) {
    ModuleInstall.getClient(context)
        .installModules(ModuleInstallRequest.newBuilder().addApi(GmsDocumentScanning.getClient(options)).build())
        .addOnFailureListener { Log.w("Scanner", "Scanner module not installed yet (${it.javaClass.simpleName})") }
}

/**
 * ML Kit Document Scanner: edge detection, crop, multi-page, gallery import, and no camera permission.
 * If the scanner can't start (module not downloaded, emulator without it), falls back to the system photo picker
 * so a letter can always be read. Returns a function that starts scanning; [onResult] gets one image per page.
 */
@Composable
fun rememberScannerLauncher(onResult: (List<Uri>) -> Unit): () -> Unit {
    val activity: Activity? = LocalActivity.current
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages.orEmpty()
            if (pages.isNotEmpty()) onResult(pages.map { it.imageUri })
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        if (uris.isNotEmpty()) onResult(uris)
    }
    val client = remember { GmsDocumentScanning.getClient(options) }
    return {
        activity?.let { a ->
            client.getStartScanIntent(a)
                .addOnSuccessListener { scanner.launch(IntentSenderRequest.Builder(it).build()) }
                .addOnFailureListener {
                    Log.w("Scanner", "Scanner unavailable (${it.javaClass.simpleName}); using photo picker")
                    Toast.makeText(a, "The scanner isn’t ready on this phone yet, so choose a photo of your letter.", Toast.LENGTH_LONG).show()
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
        }
    }
}

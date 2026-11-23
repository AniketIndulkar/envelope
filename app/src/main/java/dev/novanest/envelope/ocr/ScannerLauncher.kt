package dev.novanest.envelope.ocr

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * ML Kit Document Scanner: edge detection, crop, multi-page, gallery import — and no camera permission.
 * Returns a function that opens the scanner; [onResult] gets one image per page.
 */
@Composable
fun rememberScannerLauncher(onError: (Exception) -> Unit = {}, onResult: (List<Uri>) -> Unit): () -> Unit {
    val activity: Activity? = LocalActivity.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages.orEmpty()
            if (pages.isNotEmpty()) onResult(pages.map { it.imageUri })
        }
    }
    val client = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setGalleryImportAllowed(true)
                .setPageLimit(5)
                .build()
        )
    }
    return {
        activity?.let { a ->
            client.getStartScanIntent(a)
                .addOnSuccessListener { launcher.launch(IntentSenderRequest.Builder(it).build()) }
                .addOnFailureListener(onError)
        }
    }
}

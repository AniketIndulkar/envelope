package dev.novanest.envelope.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** ML Kit Text Recognition v2 (bundled Latin model) — runs on the phone, works offline. */
class TextReader(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Pages are joined with a blank line so the chunker treats them as separate paragraphs. */
    suspend fun read(uris: List<Uri>): String = withContext(Dispatchers.IO) {
        uris.map { recognize(InputImage.fromFilePath(context, it)) }.joinToString("\n\n")
    }

    /** Off the main thread: the first call loads the recognition model from disk. */
    suspend fun read(bitmap: Bitmap): String = withContext(Dispatchers.Default) { recognize(InputImage.fromBitmap(bitmap, 0)) }

    private suspend fun recognize(image: InputImage): String = recognizer.process(image).await().text
}

package dev.novanest.envelope.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** ML Kit Text Recognition v2 (bundled Latin model) — runs on the phone, works offline. */
class TextReader(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Pages are joined with a blank line so the chunker treats them as separate paragraphs. */
    suspend fun read(uris: List<Uri>): String =
        uris.map { recognize(InputImage.fromFilePath(context, it)) }.joinToString("\n\n")

    suspend fun read(bitmap: Bitmap): String = recognize(InputImage.fromBitmap(bitmap, 0))

    private suspend fun recognize(image: InputImage): String = recognizer.process(image).await().text
}

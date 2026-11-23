package dev.novanest.envelope.data

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SampleLetter(val id: String, val title: String) {
    val imageAsset get() = "samples/$id.png"
    val textAsset get() = "samples/$id.txt"
}

/** Bundled fictional letters so judges (and the demo video) can try Envelope without real personal data. */
class SampleLetters(private val assets: AssetManager) {

    val all: List<SampleLetter> by lazy {
        json.decodeFromString(assets.open("samples/index.json").bufferedReader().readText())
    }

    private companion object { val json = Json { ignoreUnknownKeys = true } }

    fun image(sample: SampleLetter): Bitmap = assets.open(sample.imageAsset).use(BitmapFactory::decodeStream)
}

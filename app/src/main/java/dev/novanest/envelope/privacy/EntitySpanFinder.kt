package dev.novanest.envelope.privacy

import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import kotlinx.coroutines.tasks.await

/** ML Kit Entity Extraction (on-device): catches address/phone/email/IBAN/card formats regex misses. Dates and money are kept on purpose. */
class EntitySpanFinder : SpanFinder {

    private val extractor = EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
    )

    override suspend fun find(text: String): List<PiiSpan> {
        extractor.downloadModelIfNeeded().await()
        return extractor.annotate(text).await().flatMap { annotation ->
            annotation.entities.mapNotNull { entity -> TYPES[entity.type] }
                .distinct()
                .map { PiiSpan(annotation.annotatedText, it, SpanSource.ENTITY) }
        }
    }

    private companion object {
        val TYPES = mapOf(
            Entity.TYPE_ADDRESS to PiiType.ADDRESS,
            Entity.TYPE_PHONE to PiiType.PHONE,
            Entity.TYPE_EMAIL to PiiType.EMAIL,
            Entity.TYPE_IBAN to PiiType.ACCOUNT,
            Entity.TYPE_PAYMENT_CARD to PiiType.ACCOUNT,
        )
    }
}

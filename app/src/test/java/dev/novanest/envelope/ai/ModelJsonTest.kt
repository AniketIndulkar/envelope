package dev.novanest.envelope.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

const val QC_JSON = """{"sender":"HMRC","category":"TAX","urgency":"HIGH","deadline":"2026-12-12","oneLine":"You owe £412; pay by 12 Dec."}"""

class ModelJsonTest {
    @Test fun decodesPlainJson() = assertEquals("HMRC", ModelJson.decode<QuickCheck>(QC_JSON).sender)
    @Test fun decodesFencedJson() = assertEquals("HMRC", ModelJson.decode<QuickCheck>("```json\n$QC_JSON\n```").sender)
    @Test fun decodesJsonWithProse() = assertEquals("HMRC", ModelJson.decode<QuickCheck>("Here you go: $QC_JSON Thanks").sender)
    @Test fun ignoresUnknownKeys() = assertEquals("HMRC", ModelJson.decode<QuickCheck>(QC_JSON.replaceFirst("{", "{\"extra\":1,")).sender)
    @Test fun missingOptionalFieldsDefault() = assertEquals(emptyList<String>(), ModelJson.decode<Explanation>(
        """{"summary":"s","whatItMeans":"w","actions":[],"consequences":"c","draftReply":"d"}""").redFlags)
    @Test fun badJsonThrowsTyped() { assertThrows(ModelParseException::class.java) { ModelJson.decode<QuickCheck>("not json") } }
    @Test fun emptyEntitiesDecode() = assertEquals(0, ModelJson.decode<NanoEntities>("{\"entities\":[]}").entities.size)
}

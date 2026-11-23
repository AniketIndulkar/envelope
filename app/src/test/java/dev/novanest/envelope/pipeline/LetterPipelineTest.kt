package dev.novanest.envelope.pipeline

import dev.novanest.envelope.ai.Engine
import dev.novanest.envelope.ai.ModelReply
import dev.novanest.envelope.ai.TextModel
import dev.novanest.envelope.privacy.LeakGuard
import dev.novanest.envelope.privacy.PiiSpan
import dev.novanest.envelope.privacy.PiiType
import dev.novanest.envelope.privacy.RedactionService
import dev.novanest.envelope.privacy.SpanFinder
import dev.novanest.envelope.privacy.SpanSource
import dev.novanest.envelope.privacy.TestRules
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException
import java.time.LocalDate

private const val LETTER = "Dear Ann Lee,\nYour NI number QQ123456C is on file. Please pay £412 by 12 December 2026.\nHMRC"
private const val QC_JSON = """{"sender":"HMRC","category":"TAX","urgency":"HIGH","deadline":"2026-12-12","oneLine":"Pay £412 by 12 Dec."}"""
private const val EXPLANATION_JSON = """{"summary":"You owe £412.","whatItMeans":"Tax bill for [NAME_1].","actions":[{"step":"Pay online","by":"2026-12-12"}],"deadline":"2026-12-12","consequences":"Penalty.","redFlags":[],"draftReply":"From [NAME_1], NI [NI_NUMBER_1], ref [NAME_9]","helpLinks":[]}"""

class RecordingModel(private val reply: String, private val engine: Engine = Engine.IN_CLOUD) : TextModel {
    val prompts = mutableListOf<String>()
    override suspend fun generate(prompt: String): ModelReply { prompts += prompt; return ModelReply(reply, engine) }
}

class LetterPipelineTest {
    private val nano = SpanFinder { _ -> listOf(PiiSpan("Ann Lee", PiiType.NAME, SpanSource.NANO)) }

    private fun pipeline(
        quick: TextModel = RecordingModel(QC_JSON, Engine.ON_DEVICE),
        explainer: TextModel = RecordingModel(EXPLANATION_JSON),
        nanoFinder: SpanFinder? = nano,
    ) = LetterPipeline(
        redaction = RedactionService(TestRules.redactor, { _ -> emptyList() }, nanoFinder),
        guard = LeakGuard(TestRules.redactor),
        quick = quick,
        explainer = explainer,
        today = { LocalDate.of(2026, 11, 23) },
    )

    @Test fun shortTextIsNotALetter() {
        assertThrows(PipelineError.NotALetter::class.java) { runBlocking { pipeline().prepare("   hi  ") } }
    }

    @Test fun prepareRecordsReadAndFindSteps() = runTest {
        val p = pipeline().prepare(LETTER)
        assertEquals(listOf(StepRecord(Step.READ, Engine.ON_DEVICE), StepRecord(Step.FIND_DETAILS, Engine.ON_DEVICE)), p.steps)
    }

    @Test fun rulesOnlyRecordedAsRulesEngine() = runTest {
        val p = pipeline(nanoFinder = null).prepare(LETTER)
        assertEquals(StepRecord(Step.FIND_DETAILS, Engine.RULES), p.steps.last())
    }

    @Test fun explainSendsOnlyRedactedText() = runTest {
        val cloud = RecordingModel(EXPLANATION_JSON)
        val pl = pipeline(explainer = cloud)
        pl.explain(pl.prepare(LETTER), "English")
        val sent = cloud.prompts.single()
        assertFalse(sent.contains("QQ123456C"))
        assertFalse(sent.contains("Ann Lee"))
        assertTrue(sent.contains("[NAME_1]"))
        assertTrue(sent.contains("12 December 2026"))
    }

    @Test fun explainRestoresTokens() = runTest {
        val pl = pipeline()
        val e = pl.explain(pl.prepare(LETTER), "English")
        assertEquals("From Ann Lee, NI QQ123456C, ref [NAME_9]", e.restored.draftReply)
        assertEquals("Tax bill for Ann Lee.", e.restored.whatItMeans)
        assertEquals("From [NAME_1], NI [NI_NUMBER_1], ref [NAME_9]", e.tokenised.draftReply)
        assertEquals(listOf("[NAME_9]"), e.unknownTokens)
    }

    @Test fun explainRecordsCloudStepAndSentText() = runTest {
        val cloud = RecordingModel(EXPLANATION_JSON)
        val pl = pipeline(explainer = cloud)
        val e = pl.explain(pl.prepare(LETTER), "Polish")
        assertEquals(StepRecord(Step.EXPLAIN, Engine.IN_CLOUD), e.steps.last())
        assertEquals(cloud.prompts.single(), e.sentText)
        assertTrue(e.sentText.contains("Polish"))
    }

    @Test fun quickCheckRecordsEngine() = runTest {
        val pl = pipeline(quick = RecordingModel(QC_JSON, Engine.IN_CLOUD))
        val (qc, rec) = pl.quickCheck(pl.prepare(LETTER))
        assertEquals("HMRC", qc.sender)
        assertEquals(StepRecord(Step.QUICK_CHECK, Engine.IN_CLOUD), rec)
    }

    @Test fun quickCheckSendsOnlyRedactedText() = runTest {
        val quick = RecordingModel(QC_JSON, Engine.IN_CLOUD)
        val pl = pipeline(quick = quick)
        pl.quickCheck(pl.prepare(LETTER))
        assertFalse(quick.prompts.single().contains("QQ123456C"))
    }

    @Test fun leakBlocksCloudCall() = runTest {
        val cloud = RecordingModel(EXPLANATION_JSON)
        val pl = pipeline(explainer = cloud)
        val prepared = pl.prepare(LETTER)
        val bad = prepared.copy(outcome = prepared.outcome.copy(redaction = prepared.outcome.redaction.copy(redacted = "NI QQ123456C")))
        val e = assertThrows(PipelineError.Leak::class.java) { runBlocking { pl.explain(bad, "English") } }
        assertEquals(listOf("QQ123456C"), e.leaks)
        assertTrue(cloud.prompts.isEmpty())
    }

    @Test fun badJsonRetriedOnceThenTypedError() = runTest {
        val cloud = RecordingModel("nope")
        val pl = pipeline(explainer = cloud)
        val prepared = pl.prepare(LETTER)
        val e = assertThrows(PipelineError.BadModelOutput::class.java) { runBlocking { pl.explain(prepared, "English") } }
        assertEquals(Step.EXPLAIN, e.step)
        assertEquals(2, cloud.prompts.size)
    }

    @Test fun networkFailureIsOffline() = runTest {
        val pl = pipeline(explainer = { _ -> throw RuntimeException("wrapped", UnknownHostException("no dns")) })
        val prepared = pl.prepare(LETTER)
        assertThrows(PipelineError.Offline::class.java) { runBlocking { pl.explain(prepared, "English") } }
    }
}

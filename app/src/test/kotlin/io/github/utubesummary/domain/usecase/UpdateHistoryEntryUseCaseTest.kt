package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.provider.SourceTextStore
import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.llm.LlmGateway
import io.github.utubesummary.llm.SummarizationOutcome
import io.github.utubesummary.llm.SummarizationRequest
import io.github.utubesummary.model.CRITICAL_ANALYST_PROFILE_ID
import io.github.utubesummary.model.ChatExchange
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import io.github.utubesummary.model.ExtractedContent
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.QUICK_BRIEF_PROFILE_ID
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput
import io.github.utubesummary.model.SummarySource
import io.github.utubesummary.model.SummaryType
import io.github.utubesummary.model.contentFingerprint
import io.github.utubesummary.testutil.FakeUserPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

class UpdateHistoryEntryUseCaseTest {

    private val link = "https://example.com/post"

    private class FakeGateway : LlmGateway {
        var content = ExtractedContent(title = "T", author = "A", content = "new text")
        var failure: Throwable? = null
        val extracted = mutableListOf<SummarySource>()

        override suspend fun extractContent(source: SummarySource): ExtractedContent {
            extracted += source
            failure?.let { throw it }
            return content
        }

        override suspend fun summarize(request: SummarizationRequest, source: SummarySource): SummarizationOutcome =
            error("not used")

        override suspend fun askQuestion(
            provider: AIProvider,
            apiKey: String,
            baseUrl: String?,
            model: String?,
            appLanguage: Locale,
            sourceText: String,
            history: List<ChatExchange>,
            question: String,
        ): String = error("not used")
    }

    private class MapStore : SourceTextStore {
        val contents = mutableMapOf<String, ExtractedContent>()
        override fun put(key: String, text: String) { contents[key] = ExtractedContent(content = text) }
        override fun get(key: String): String? = contents[key]?.content
        override fun putContent(key: String, content: ExtractedContent) { contents[key] = content }
        override fun getContent(key: String): ExtractedContent? = contents[key]
    }

    private data class Call(
        val text: String,
        val length: SummaryLength?,
        val profileId: String?,
        val language: String?,
        val storedAtCall: ExtractedContent?,
    )

    private inner class FakeSummarize : SummarizeContentUseCase {
        val calls = mutableListOf<Call>()
        var behaviour: (Int) -> Result<SummaryOutput> = { Result.success(output()) }

        override suspend fun invoke(
            text: String,
            overrideLength: SummaryLength?,
            profileId: String?,
            language: String?,
        ): Result<SummaryOutput> {
            calls += Call(text, overrideLength, profileId, language, store.getContent(text))
            return behaviour(calls.size - 1)
        }
    }

    private fun output() = SummaryOutput(
        title = "t",
        author = "a",
        summary = "s",
        isYoutubeLink = false,
        isBiliBiliLink = false,
        length = SummaryLength.MEDIUM,
    )

    private val gateway = FakeGateway()
    private val store = MapStore()
    private val summarize = FakeSummarize()
    private val prefs = FakeUserPreferencesRepository()
    private val useCase = UpdateHistoryEntryUseCaseImpl(gateway, summarize, store, prefs)

    private fun result(
        profile: String?,
        createdOn: Long,
        fingerprint: String? = "old",
        length: SummaryLength = SummaryLength.MEDIUM,
        language: String? = "content",
    ) = HistoryLengthResult(
        length = length,
        summary = "summary",
        profileId = profile,
        language = language,
        createdOn = createdOn,
        sourceFingerprint = fingerprint,
    )

    private fun entry(
        current: HistoryLengthResult,
        vararg alternatives: HistoryLengthResult,
        type: SummaryType = SummaryType.ARTICLE,
        sourceLink: String? = link,
    ) = HistorySummary(
        title = "t",
        type = type,
        sourceLink = sourceLink,
        length = current.length,
        lengthResults = mapOf(current.length to current),
        alternatives = alternatives.toList(),
    )

    private val threeProfiles = entry(
        current = result(CRITICAL_ANALYST_PROFILE_ID, 3),
        result(DEFAULT_PROFILE_ID, 1),
        result(QUICK_BRIEF_PROFILE_ID, 2),
    )

    @Test
    fun `a changed page is fetched once and every profile is summarized again, oldest first`() = runTest {
        val outcome = useCase(threeProfiles).getOrThrow()

        assertEquals(1, gateway.extracted.size)
        assertEquals(SummarySource.Article(link), gateway.extracted.single())
        assertEquals(
            listOf(DEFAULT_PROFILE_ID, QUICK_BRIEF_PROFILE_ID, CRITICAL_ANALYST_PROFILE_ID),
            summarize.calls.map { it.profileId }
        )
        assertEquals(UpdateOutcome(updated = 3, upToDate = 0, skipped = 0, failed = 0), outcome)
    }

    @Test
    fun `the new text is in the store when the summaries are made, so it is not fetched again`() = runTest {
        useCase(threeProfiles)

        assertTrue(summarize.calls.all { it.storedAtCall == gateway.content && it.text == link })
    }

    @Test
    fun `each summary keeps its profile, language and length`() = runTest {
        val entry = entry(
            current = result(QUICK_BRIEF_PROFILE_ID, 2, length = SummaryLength.LONG, language = "de"),
            result(DEFAULT_PROFILE_ID, 1, length = SummaryLength.SHORT, language = "content"),
        )

        useCase(entry)

        assertEquals(SummaryLength.SHORT, summarize.calls[0].length)
        assertEquals("content", summarize.calls[0].language)
        assertEquals(SummaryLength.LONG, summarize.calls[1].length)
        assertEquals("de", summarize.calls[1].language)
    }

    @Test
    fun `an old summary without profile, language or length uses the defaults`() = runTest {
        useCase(entry(result(null, 1, length = SummaryLength.NONE, language = null)))

        val call = summarize.calls.single()
        assertEquals(DEFAULT_PROFILE_ID, call.profileId)
        assertEquals(null, call.length)
        assertEquals(null, call.language)
    }

    @Test
    fun `unchanged content makes no model call`() = runTest {
        val same = contentFingerprint(gateway.content.content)
        val entry = entry(result(QUICK_BRIEF_PROFILE_ID, 2, fingerprint = same), result(DEFAULT_PROFILE_ID, 1, fingerprint = same))

        val outcome = useCase(entry).getOrThrow()

        assertTrue(summarize.calls.isEmpty())
        assertEquals(UpdateOutcome(updated = 0, upToDate = 2, skipped = 0, failed = 0), outcome)
    }

    @Test
    fun `only outdated summaries are made again`() = runTest {
        val same = contentFingerprint(gateway.content.content)
        val entry = entry(result(QUICK_BRIEF_PROFILE_ID, 2, fingerprint = same), result(DEFAULT_PROFILE_ID, 1, fingerprint = "old"))

        val outcome = useCase(entry).getOrThrow()

        assertEquals(listOf(DEFAULT_PROFILE_ID), summarize.calls.map { it.profileId })
        assertEquals(UpdateOutcome(updated = 1, upToDate = 1, skipped = 0, failed = 0), outcome)
    }

    @Test
    fun `a deleted profile is skipped`() = runTest {
        val entry = entry(result("deleted_custom_profile", 2), result(DEFAULT_PROFILE_ID, 1))

        val outcome = useCase(entry).getOrThrow()

        assertEquals(listOf(DEFAULT_PROFILE_ID), summarize.calls.map { it.profileId })
        assertEquals(1, outcome.skipped)
    }

    @Test
    fun `a failed fetch changes nothing`() = runTest {
        gateway.failure = SummaryException.NoInternetException()

        val result = useCase(threeProfiles)

        assertTrue(result.exceptionOrNull() is SummaryException.NoInternetException)
        assertTrue(summarize.calls.isEmpty())
    }

    @Test
    fun `a summary that fails is counted and the others are still made`() = runTest {
        summarize.behaviour = { index ->
            if (index == 1) Result.failure(SummaryException.RateLimitException()) else Result.success(output())
        }

        val outcome = useCase(threeProfiles).getOrThrow()

        assertEquals(3, summarize.calls.size)
        assertEquals(UpdateOutcome(updated = 2, upToDate = 0, skipped = 0, failed = 1), outcome)
    }

    @Test
    fun `when nothing can be made the reason is reported`() = runTest {
        summarize.behaviour = { Result.failure(SummaryException.NoContentException()) }

        val result = useCase(threeProfiles)

        assertTrue(result.exceptionOrNull() is SummaryException.NoContentException)
    }

    @Test
    fun `progress counts the summaries made`() = runTest {
        val seen = mutableListOf<UpdateProgress>()

        useCase(threeProfiles) { seen += it }

        assertEquals(
            listOf(UpdateProgress(0, 3), UpdateProgress(1, 3), UpdateProgress(2, 3), UpdateProgress(3, 3)),
            seen
        )
    }

    @Test
    fun `text and document entries cannot be updated`() = runTest {
        val text = useCase(entry(result(DEFAULT_PROFILE_ID, 1), type = SummaryType.TEXT, sourceLink = null))
        val document = useCase(entry(result(DEFAULT_PROFILE_ID, 1), type = SummaryType.DOCUMENT, sourceLink = "content://a"))

        assertTrue(text.exceptionOrNull() is SummaryException.InvalidLinkException)
        assertTrue(document.exceptionOrNull() is SummaryException.InvalidLinkException)
        assertTrue(gateway.extracted.isEmpty())
    }

    @Test
    fun `cancellation is passed on, not turned into a result`() {
        summarize.behaviour = { throw CancellationException("cancelled") }

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking { useCase(threeProfiles) }
        }

        assertEquals("cancelled", thrown.message)
    }
}

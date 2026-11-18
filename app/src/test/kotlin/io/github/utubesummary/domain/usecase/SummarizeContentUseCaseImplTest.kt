package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.provider.AppLocaleProvider
import io.github.utubesummary.domain.provider.Clock
import io.github.utubesummary.domain.provider.DocumentMetadataProvider
import io.github.utubesummary.domain.provider.SourceTextStore
import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.llm.LlmGateway
import io.github.utubesummary.llm.SummarizationOutcome
import io.github.utubesummary.llm.SummarizationProgress
import io.github.utubesummary.llm.SummarizationRequest
import io.github.utubesummary.llm.quickBriefPrompt
import io.github.utubesummary.model.ChatExchange
import io.github.utubesummary.model.ProviderConfig
import io.github.utubesummary.model.SummarizationPhase
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput
import io.github.utubesummary.model.SummarySource
import io.github.utubesummary.model.UserPreferences
import io.github.utubesummary.testutil.FakeHistoryRepository
import io.github.utubesummary.testutil.FakeProviderConfigRepository
import io.github.utubesummary.testutil.FakeUserPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime

class SummarizeContentUseCaseImplTest {

    private class FakeGateway(private val progress: SummarizationProgress) : LlmGateway {
        var result: () -> SummarizationOutcome = { error("no outcome set") }
        var lastRequest: SummarizationRequest? = null
        var lastSource: SummarySource? = null
        var phaseDuringCall: SummarizationPhase? = null
        var calls = 0

        /** Virtual time the call takes, so tests can run a slow request instantly. */
        var delayMillis = 0L

        override suspend fun summarize(request: SummarizationRequest, source: SummarySource): SummarizationOutcome {
            calls++
            lastRequest = request
            lastSource = source
            phaseDuringCall = progress.phase.value
            if (delayMillis > 0) delay(delayMillis)
            return result()
        }

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

    private class MapSourceTextStore : SourceTextStore {
        val texts = mutableMapOf<String, String>()
        override fun put(key: String, text: String) { texts[key] = text }
        override fun get(key: String): String? = texts[key]
    }

    private val progress = SummarizationProgress()
    private val gateway = FakeGateway(progress)
    private val history = FakeHistoryRepository()
    private val store = MapSourceTextStore()
    private val prefs = FakeUserPreferencesRepository()
    private val configs = FakeProviderConfigRepository(
        mapOf(AIProvider.OPENAI.name to ProviderConfig(apiKey = "sk-secret", activeModel = "gpt-test"))
    )

    private class FixedClock(var now: Long) : Clock {
        override fun nowMillis(): Long = now
    }

    private val clock = FixedClock(1_700_000_000_000L)

    private fun newUseCase(dispatcher: CoroutineDispatcher = Dispatchers.IO) = SummarizeContentUseCaseImpl(
        userPreferencesRepository = prefs,
        aiProviderConfigRepository = configs,
        historyRepository = history,
        llmGateway = gateway,
        documentMetadataProvider = object : DocumentMetadataProvider {
            override suspend fun getFileName(uriString: String): String? = "doc.pdf"
        },
        appLocaleProvider = object : AppLocaleProvider {
            override fun getCurrentLocale(): Locale = Locale.ENGLISH
        },
        sourceTextStore = store,
        progress = progress,
        clock = clock,
        ioDispatcher = dispatcher,
    )

    private val useCase = newUseCase()

    private fun outcome(text: String = "The summary") = SummarizationOutcome(
        summary = SummaryOutput(
            title = "Title",
            author = "Author",
            summary = text,
            isYoutubeLink = false,
            isBiliBiliLink = false,
            length = SummaryLength.MEDIUM,
        ),
        sourceText = "source text",
    )

    private fun selectOpenAi() {
        prefs.prefs.value = UserPreferences(activeProvider = AIProvider.OPENAI.name)
    }

    @Test
    fun `a slow request takes virtual time on the injected dispatcher`() = runTest {
        selectOpenAi()
        gateway.delayMillis = 3_600_000L
        gateway.result = { outcome() }

        val result = newUseCase(StandardTestDispatcher(testScheduler))("https://example.com/slow")

        assertTrue(result.isSuccess)
        assertEquals(3_600_000L, currentTime)
    }

    @Test
    fun `cancelling a slow request saves nothing`() = runTest {
        selectOpenAi()
        gateway.delayMillis = 3_600_000L
        gateway.result = { outcome() }
        val job = launch { newUseCase(StandardTestDispatcher(testScheduler))("https://example.com/slow") }

        advanceTimeBy(1_000L)
        job.cancel()
        advanceUntilIdle()

        assertTrue(job.isCancelled)
        assertTrue(history.added.isEmpty())
    }

    @Test
    fun `the saved result is stamped with the time of the clock`() = runTest {
        selectOpenAi()
        gateway.result = { outcome() }

        useCase("https://example.com/first")
        clock.now = 1_800_000_000_000L
        useCase("https://example.com/second")

        assertEquals(
            listOf(1_700_000_000_000L, 1_800_000_000_000L),
            history.added.map { it.activeResult!!.createdOn }
        )
    }

    @Test
    fun `a summary is returned, kept for questions and saved to the history`() = runTest {
        selectOpenAi()
        gateway.result = { outcome("Body") }

        val result = useCase("https://example.com/post")

        assertEquals("Body", result.getOrThrow().summary)
        assertTrue(gateway.lastSource is SummarySource.Article)
        assertEquals("source text", store.texts["https://example.com/post"])

        val saved = history.added.single()
        assertEquals("https://example.com/post", saved.sourceLink)
        val stored = saved.activeResult!!
        assertEquals("default", stored.profileId)
        assertEquals("content", stored.language)
        assertEquals("OPENAI", stored.provider)
        assertEquals("gpt-test", stored.model)
        assertTrue(stored.createdOn > 0L)
    }

    @Test
    fun `the default profile keeps the structured card and the legacy prompt`() = runTest {
        selectOpenAi()
        gateway.result = { outcome() }

        useCase("https://example.com/post")

        val request = gateway.lastRequest!!
        assertEquals(AIProvider.OPENAI, request.provider)
        assertEquals("sk-secret", request.apiKey)
        assertNull(request.profileBasePrompt)
        assertTrue(request.structuredOutput)
        assertTrue(request.useContentLanguage)
        assertNull(request.summaryLanguage)
    }

    @Test
    fun `another profile sends its prompt, answers free-form and is recorded`() = runTest {
        selectOpenAi()
        gateway.result = { outcome() }

        useCase("https://example.com/post", profileId = "quick_brief")

        val request = gateway.lastRequest!!
        assertEquals(quickBriefPrompt, request.profileBasePrompt)
        assertFalse(request.structuredOutput)
        assertEquals("quick_brief", history.added.single().activeResult!!.profileId)
    }

    @Test
    fun `a chosen language is named in the prompt and recorded`() = runTest {
        selectOpenAi()
        gateway.result = { outcome() }

        useCase("https://example.com/post", language = "fr")

        val request = gateway.lastRequest!!
        assertFalse(request.useContentLanguage)
        assertEquals("French", request.summaryLanguage!!.getDisplayLanguage(Locale.ENGLISH))
        assertEquals("fr", history.added.single().activeResult!!.language)
    }

    @Test
    fun `length options off or a chosen length are passed on`() = runTest {
        selectOpenAi()
        gateway.result = { outcome() }

        useCase("https://example.com/post", overrideLength = SummaryLength.LONG)
        assertEquals(SummaryLength.LONG, gateway.lastRequest!!.summaryLength)

        prefs.prefs.value = prefs.prefs.value.copy(showLength = false)
        useCase("https://example.com/post", overrideLength = SummaryLength.LONG)
        assertEquals(SummaryLength.NONE, gateway.lastRequest!!.summaryLength)
    }

    @Test
    fun `without a selected provider nothing is sent`() = runTest {
        val result = useCase("https://example.com/post")

        assertTrue(result.exceptionOrNull() is SummaryException.NoKeyException)
        assertEquals(0, gateway.calls)
    }

    @Test
    fun `blank input has no content`() = runTest {
        selectOpenAi()

        val result = useCase("   ")

        assertTrue(result.exceptionOrNull() is SummaryException.NoContentException)
        assertEquals(0, gateway.calls)
        assertTrue(history.added.isEmpty())
    }

    @Test
    fun `a failure carries the request context and no secret and saves nothing`() = runTest {
        selectOpenAi()
        gateway.result = { throw RuntimeException("boom") }

        val error = useCase("https://example.com/post").exceptionOrNull() as SummaryException

        val details = error.technicalDetails.orEmpty()
        assertTrue(details.contains("Provider: OPENAI"), details)
        assertTrue(details.contains("gpt-test"), details)
        assertFalse(details.contains("sk-secret"), details)
        assertTrue(history.added.isEmpty())
    }

    @Test
    fun `cancellation is passed on and not turned into an error result`() = runTest {
        selectOpenAi()
        gateway.result = { throw CancellationException("stop") }

        try {
            useCase("https://example.com/post")
            fail("cancellation should propagate")
        } catch (expected: CancellationException) {
            assertNotNull(expected)
        }
        assertTrue(history.added.isEmpty())
    }

    @Test
    fun `the phase is extracting during the call and cleared afterwards, also after a failure`() = runTest {
        selectOpenAi()
        gateway.result = { outcome() }
        useCase("https://example.com/post")
        assertEquals(SummarizationPhase.EXTRACTING, gateway.phaseDuringCall)
        assertNull(progress.phase.value)

        gateway.result = { throw RuntimeException("boom") }
        useCase("https://example.com/post")
        assertNull(progress.phase.value)
    }
}

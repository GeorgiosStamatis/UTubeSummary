package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.provider.AppLocaleProvider
import io.github.utubesummary.domain.provider.SourceTextStore
import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.llm.LlmGateway
import io.github.utubesummary.llm.SummarizationOutcome
import io.github.utubesummary.llm.SummarizationRequest
import io.github.utubesummary.model.ChatExchange
import io.github.utubesummary.model.ProviderConfig
import io.github.utubesummary.model.SummarySource
import io.github.utubesummary.model.UserPreferences
import io.github.utubesummary.testutil.FakeProviderConfigRepository
import io.github.utubesummary.testutil.FakeUserPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale
import io.github.utubesummary.model.ExtractedContent

class AskAboutContentUseCaseImplTest {

    private class FakeGateway : LlmGateway {
        var answer: () -> String = { "An answer" }
        var sourceText: String? = null
        var history: List<ChatExchange>? = null
        var question: String? = null

        override suspend fun summarize(request: SummarizationRequest, source: SummarySource): SummarizationOutcome =
            error("not used")

        override suspend fun extractContent(source: SummarySource): ExtractedContent = error("not used")

        override suspend fun askQuestion(
            provider: AIProvider,
            apiKey: String,
            baseUrl: String?,
            model: String?,
            appLanguage: Locale,
            sourceText: String,
            history: List<ChatExchange>,
            question: String,
        ): String {
            this.sourceText = sourceText
            this.history = history
            this.question = question
            return answer()
        }
    }

    private val store = object : SourceTextStore {
        val texts = mutableMapOf<String, String>()
        override fun put(key: String, text: String) { texts[key] = text }
        override fun get(key: String): String? = texts[key]
        override fun putContent(key: String, content: ExtractedContent) { texts[key] = content.content }
        override fun getContent(key: String): ExtractedContent? = texts[key]?.let { ExtractedContent(content = it) }
    }
    private val gateway = FakeGateway()
    private val prefs = FakeUserPreferencesRepository().apply {
        prefs.value = UserPreferences(activeProvider = AIProvider.OPENAI.name)
    }
    private val configs = FakeProviderConfigRepository(
        mapOf(AIProvider.OPENAI.name to ProviderConfig(apiKey = "sk-secret", activeModel = "gpt-test"))
    )
    private val useCase = AskAboutContentUseCaseImpl(
        userPreferencesRepository = prefs,
        aiProviderConfigRepository = configs,
        llmGateway = gateway,
        appLocaleProvider = object : AppLocaleProvider {
            override fun getCurrentLocale(): Locale = Locale.ENGLISH
        },
        sourceTextStore = store,
    )

    @Test
    fun `a question is answered from the stored text with the earlier exchanges`() = runTest {
        store.put("https://example.com/post", "the stored article")
        val earlier = listOf(ChatExchange("First?", "Yes."))

        val result = useCase("https://example.com/post", earlier, "And then?")

        assertEquals("An answer", result.getOrThrow())
        assertEquals("the stored article", gateway.sourceText)
        assertEquals(earlier, gateway.history)
        assertEquals("And then?", gateway.question)
    }

    @Test
    fun `availability follows the stored text`() {
        assertFalse(useCase.isAvailable("https://example.com/post"))

        store.put("https://example.com/post", "text")

        assertTrue(useCase.isAvailable("https://example.com/post"))
    }

    @Test
    fun `without stored text there is no content to ask about`() = runTest {
        val result = useCase("https://example.com/unknown", emptyList(), "Question?")

        assertTrue(result.exceptionOrNull() is SummaryException.NoContentException)
        assertEquals(null, gateway.question)
    }

    @Test
    fun `a blank answer counts as no content`() = runTest {
        store.put("input", "text")
        gateway.answer = { "   " }

        assertTrue(useCase("input", emptyList(), "Q?").exceptionOrNull() is SummaryException.NoContentException)
    }

    @Test
    fun `a failure carries the request context`() = runTest {
        store.put("input", "text")
        gateway.answer = { throw RuntimeException("boom") }

        val error = useCase("input", emptyList(), "Q?").exceptionOrNull() as SummaryException

        assertTrue(error.technicalDetails.orEmpty().contains("Provider: OPENAI"))
    }
}

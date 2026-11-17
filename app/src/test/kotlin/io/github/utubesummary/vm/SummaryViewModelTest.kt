package io.github.utubesummary.vm

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import io.github.utubesummary.domain.usecase.AskAboutContentUseCase
import io.github.utubesummary.domain.usecase.ObserveSummarizationPhaseUseCase
import io.github.utubesummary.llm.SummarizationProgress
import io.github.utubesummary.model.SummarizationPhase
import io.github.utubesummary.domain.usecase.SummarizeContentUseCase
import io.github.utubesummary.model.ChatExchange
import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeSummarizeContentUseCase : SummarizeContentUseCase {
        var summaryToReturn: SummaryOutput? = null
        var errorToThrow: Throwable? = null
        var lastInput: String? = null
        var lastOverrideLength: SummaryLength? = null
        var lastProfileId: String? = null
        var lastLanguage: String? = null

        /** When set, every call suspends until it is completed (simulates a slow request). */
        var gate: CompletableDeferred<Unit>? = null
        val inputs = mutableListOf<String>()

        override suspend operator fun invoke(
            text: String,
            overrideLength: SummaryLength?,
            profileId: String?,
            language: String?,
        ): Result<SummaryOutput> {
            lastProfileId = profileId
            lastLanguage = language
            lastInput = text
            inputs += text
            gate?.await()
            lastOverrideLength = overrideLength
            errorToThrow?.let { return Result.failure(it) }
            val length = overrideLength ?: SummaryLength.MEDIUM
            val output = summaryToReturn ?: createSummaryOutput(length)
            return Result.success(output)
        }
    }

    private class FakeAskAboutContentUseCase : AskAboutContentUseCase {
        val availableInputs = mutableSetOf<String>()
        var answer: Result<String> = Result.success("The answer")
        var lastHistory: List<ChatExchange> = emptyList()
        var lastQuestion: String? = null

        override fun isAvailable(input: String) = input in availableInputs

        override suspend fun invoke(
            input: String,
            history: List<ChatExchange>,
            question: String,
        ): Result<String> {
            lastHistory = history
            lastQuestion = question
            return answer
        }
    }

    private lateinit var fakeAsk: FakeAskAboutContentUseCase
    private lateinit var fakeUseCase: FakeSummarizeContentUseCase
    private lateinit var viewModel: SummaryViewModel
    private lateinit var progress: SummarizationProgress

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeUseCase = FakeSummarizeContentUseCase()
        fakeAsk = FakeAskAboutContentUseCase()
        progress = SummarizationProgress()
        viewModel = SummaryViewModel(fakeUseCase, fakeAsk, ObserveSummarizationPhaseUseCase(progress))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test summarize success updates state and caches result`() = runTest(testDispatcher) {
        val output = createSummaryOutput(SummaryLength.MEDIUM, "Medium summary")
        fakeUseCase.summaryToReturn = output

        viewModel.summarize("https://example.com/article")

        assertEquals("https://example.com/article", fakeUseCase.lastInput)
        val state = viewModel.summarizationState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(output, state.summaryResult)
        assertEquals(output, state.lengthResults[SummaryLength.MEDIUM])
    }

    @Test
    fun `test summarize failure updates error state`() = runTest(testDispatcher) {
        fakeUseCase.errorToThrow = SummaryException.NoKeyException()

        viewModel.summarize("https://example.com")

        val state = viewModel.summarizationState.value
        assertFalse(state.isLoading)
        assertNull(state.summaryResult)
        assertTrue(state.error is SummaryException.NoKeyException)
    }

    @Test
    fun `cancelSummarize stops loading and a late result is ignored`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        fakeUseCase.gate = gate

        viewModel.summarize("https://example.com/slow")
        assertTrue(viewModel.summarizationState.value.isLoading)

        viewModel.cancelSummarize()
        assertFalse(viewModel.summarizationState.value.isLoading)

        gate.complete(Unit)
        val state = viewModel.summarizationState.value
        assertFalse(state.isLoading)
        assertNull(state.summaryResult)
        assertNull(state.error)
    }

    @Test
    fun `a new summarize replaces the running one`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        fakeUseCase.gate = gate
        val output = createSummaryOutput(SummaryLength.MEDIUM, "Second")
        fakeUseCase.summaryToReturn = output

        viewModel.summarize("https://example.com/first")
        viewModel.summarize("https://example.com/second")
        gate.complete(Unit)

        val state = viewModel.summarizationState.value
        assertFalse(state.isLoading)
        assertEquals(output, state.summaryResult)
        assertEquals(listOf("https://example.com/first", "https://example.com/second"), fakeUseCase.inputs)
    }

    @Test
    fun `changing the language drops cached results and is passed to the use case`() = runTest(testDispatcher) {
        fakeUseCase.summaryToReturn = createSummaryOutput(SummaryLength.SHORT, "Short summary")
        viewModel.summarize("https://example.com", SummaryLength.SHORT, language = "content")

        assertEquals("content", viewModel.summarizationState.value.language)

        fakeUseCase.summaryToReturn = createSummaryOutput(SummaryLength.LONG, "Long summary")
        viewModel.summarize("https://example.com", SummaryLength.LONG, language = "fr")

        assertEquals("fr", fakeUseCase.lastLanguage)
        val state = viewModel.summarizationState.value
        assertEquals("fr", state.language)
        assertEquals(setOf(SummaryLength.LONG), state.lengthResults.keys)
    }

    @Test
    fun `the phase of the running summarization is exposed in the state`() = runTest(testDispatcher) {
        assertNull(viewModel.summarizationState.value.phase)

        progress.report(SummarizationPhase.EXTRACTING)
        assertEquals(SummarizationPhase.EXTRACTING, viewModel.summarizationState.value.phase)

        progress.report(SummarizationPhase.SUMMARIZING)
        assertEquals(SummarizationPhase.SUMMARIZING, viewModel.summarizationState.value.phase)

        progress.report(null)
        assertNull(viewModel.summarizationState.value.phase)
    }

    @Test
    fun `clearCurrentSummary also stops loading`() = runTest(testDispatcher) {
        fakeUseCase.gate = CompletableDeferred()

        viewModel.summarize("https://example.com/slow")
        viewModel.clearCurrentSummary()

        val state = viewModel.summarizationState.value
        assertFalse(state.isLoading)
        assertNull(state.summaryResult)
    }

    @Test
    fun `test switchLength returns early when no summaries generated yet`() {
        viewModel.switchLength(SummaryLength.SHORT)
        assertNull(viewModel.summarizationState.value.summaryResult)
        assertTrue(viewModel.summarizationState.value.lengthResults.isEmpty())
    }

    @Test
    fun `test switchLength switches between cached results`() = runTest(testDispatcher) {
        val shortOutput = createSummaryOutput(SummaryLength.SHORT, "Short summary")
        val longOutput = createSummaryOutput(SummaryLength.LONG, "Long summary")

        fakeUseCase.summaryToReturn = shortOutput
        viewModel.summarize("https://example.com", SummaryLength.SHORT)

        fakeUseCase.summaryToReturn = longOutput
        viewModel.summarize("https://example.com", SummaryLength.LONG)

        // Switch to SHORT
        viewModel.switchLength(SummaryLength.SHORT)
        assertEquals(shortOutput, viewModel.summarizationState.value.summaryResult)

        // Switch to LONG
        viewModel.switchLength(SummaryLength.LONG)
        assertEquals(longOutput, viewModel.summarizationState.value.summaryResult)

        // Switch to MEDIUM (not yet cached)
        viewModel.switchLength(SummaryLength.MEDIUM)
        assertNull(viewModel.summarizationState.value.summaryResult)
    }

    @Test
    fun `test switchLength preserves un-lengthed summary when specific length not cached`() =
        runTest(testDispatcher) {
            val noneOutput = createSummaryOutput(SummaryLength.NONE, "Un-lengthed summary")
            viewModel.setExistingSummary(output = noneOutput, input = "https://example.com")

            // Switch to MEDIUM (not yet cached) - should fall back to NONE output
            viewModel.switchLength(SummaryLength.MEDIUM)
            assertEquals(noneOutput, viewModel.summarizationState.value.summaryResult)

            // Switch to NONE directly
            viewModel.switchLength(SummaryLength.NONE)
            assertEquals(noneOutput, viewModel.summarizationState.value.summaryResult)
        }

    @Test
    fun `test clearCurrentSummary clears all results and cache`() = runTest(testDispatcher) {
        fakeUseCase.summaryToReturn = createSummaryOutput(SummaryLength.SHORT)
        viewModel.summarize("https://example.com", SummaryLength.SHORT)

        assertNotNull(viewModel.summarizationState.value.summaryResult)

        viewModel.clearCurrentSummary()
        assertNull(viewModel.summarizationState.value.summaryResult)
        assertNull(viewModel.summarizationState.value.error)
        assertTrue(viewModel.summarizationState.value.lengthResults.isEmpty())
    }

    @Test
    fun `test setExistingSummary directly updates state without invoking useCase`() {
        val output = createSummaryOutput(SummaryLength.MEDIUM, "Direct medium summary")

        viewModel.setExistingSummary(output = output, input = "https://example.com/existing")

        assertNull(fakeUseCase.lastInput)
        val state = viewModel.summarizationState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(output, state.summaryResult)
        assertEquals(output, state.lengthResults[SummaryLength.MEDIUM])
    }

    @Test
    fun `test setExistingSummary with lengthResults allows switchLength immediately`() {
        val mediumOutput = createSummaryOutput(SummaryLength.MEDIUM, "Medium summary")
        val shortOutput = createSummaryOutput(SummaryLength.SHORT, "Short summary")
        val lengthResults = mapOf(
            SummaryLength.MEDIUM to mediumOutput,
            SummaryLength.SHORT to shortOutput,
        )

        viewModel.setExistingSummary(
            output = mediumOutput,
            input = "https://example.com",
            lengthResults = lengthResults
        )

        assertEquals(mediumOutput, viewModel.summarizationState.value.summaryResult)

        // Switch to SHORT
        viewModel.switchLength(SummaryLength.SHORT)
        assertEquals(shortOutput, viewModel.summarizationState.value.summaryResult)

        // Switch back to MEDIUM
        viewModel.switchLength(SummaryLength.MEDIUM)
        assertEquals(mediumOutput, viewModel.summarizationState.value.summaryResult)
    }

    @Test
    fun `test serialization and deserialization of summaryResult and lengthResults`() {
        val original = createSummaryOutput(SummaryLength.MEDIUM, "Serialized summary")
        val shortOutput = createSummaryOutput(SummaryLength.SHORT, "Short summary")
        val lengthResults = mapOf(
            SummaryLength.MEDIUM to original,
            SummaryLength.SHORT to shortOutput
        )

        val json = Json { ignoreUnknownKeys = true }
        val encodedSummary = json.encodeToString(original)
        val encodedLengths = json.encodeToString(lengthResults.mapKeys { it.key.name })

        val decodedSummary = json.decodeFromString<SummaryOutput>(encodedSummary)
        val decodedLengths = json.decodeFromString<Map<String, SummaryOutput>>(encodedLengths)
            .mapNotNull { (k, v) -> runCatching { SummaryLength.valueOf(k) to v }.getOrNull() }
            .toMap()

        assertEquals(original, decodedSummary)
        assertEquals(2, decodedLengths.size)
        assertEquals(original, decodedLengths[SummaryLength.MEDIUM])
        assertEquals(shortOutput, decodedLengths[SummaryLength.SHORT])
    }

    companion object {
        private fun createSummaryOutput(
            length: SummaryLength,
            summary: String = "Summary text for $length",
        ): SummaryOutput {
            return SummaryOutput(
                title = "Test Title",
                author = "Test Author",
                summary = summary,
                sourceLink = "https://example.com",
                isYoutubeLink = false,
                isBiliBiliLink = false,
                length = length,
            )
        }
    }

    @Test
    fun `test questions are available only when the text is in memory`() = runTest(testDispatcher) {
        viewModel.summarize("https://example.com/a")
        assertFalse(viewModel.chatState.value.canAsk)

        fakeAsk.availableInputs += "https://example.com/b"
        viewModel.summarize("https://example.com/b")
        assertTrue(viewModel.chatState.value.canAsk)
    }

    @Test
    fun `test asking appends turns and sends earlier answers as history`() = runTest(testDispatcher) {
        fakeAsk.availableInputs += "https://example.com/a"
        viewModel.summarize("https://example.com/a")

        viewModel.askQuestion("  What is it about?  ")
        var chat = viewModel.chatState.value
        assertEquals(listOf(ChatTurn("What is it about?", "The answer")), chat.turns)
        assertFalse(chat.isAsking)
        assertEquals(emptyList<ChatExchange>(), fakeAsk.lastHistory)

        fakeAsk.answer = Result.success("Second answer")
        viewModel.askQuestion("And then?")
        chat = viewModel.chatState.value
        assertEquals(2, chat.turns.size)
        assertEquals("Second answer", chat.turns.last().answer)
        assertEquals(listOf(ChatExchange("What is it about?", "The answer")), fakeAsk.lastHistory)
    }

    @Test
    fun `test failed question keeps the turn with its error`() = runTest(testDispatcher) {
        fakeAsk.availableInputs += "https://example.com/a"
        viewModel.summarize("https://example.com/a")
        fakeAsk.answer = Result.failure(RuntimeException("boom"))

        viewModel.askQuestion("Why?")

        val turn = viewModel.chatState.value.turns.single()
        assertNull(turn.answer)
        assertNotNull(turn.error)
        assertFalse(viewModel.chatState.value.isAsking)
    }

    @Test
    fun `test blank questions and unavailable content are ignored`() = runTest(testDispatcher) {
        viewModel.summarize("https://example.com/a") // not available
        viewModel.askQuestion("Hello?")
        assertTrue(viewModel.chatState.value.turns.isEmpty())

        fakeAsk.availableInputs += "https://example.com/b"
        viewModel.summarize("https://example.com/b")
        viewModel.askQuestion("   ")
        assertTrue(viewModel.chatState.value.turns.isEmpty())
        assertNull(fakeAsk.lastQuestion)
    }

    @Test
    fun `test chat is cleared for a new input`() = runTest(testDispatcher) {
        fakeAsk.availableInputs += "https://example.com/a"
        viewModel.summarize("https://example.com/a")
        viewModel.askQuestion("Question")
        assertEquals(1, viewModel.chatState.value.turns.size)

        viewModel.summarize("https://example.com/other")
        assertTrue(viewModel.chatState.value.turns.isEmpty())
        assertFalse(viewModel.chatState.value.canAsk)
    }
}

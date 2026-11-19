package io.github.utubesummary.llm

import io.github.utubesummary.model.ChatExchange
import io.github.utubesummary.model.ExtractedContent
import io.github.utubesummary.model.SummarySource
import io.github.utubesummary.model.SummaryLength
import java.util.Locale

/** Everything that decides how one summarization request is made. */
data class SummarizationRequest(
    val provider: AIProvider,
    val apiKey: String,
    val baseUrl: String?,
    val model: String?,
    val summaryLength: SummaryLength,
    val showLength: Boolean,
    /** True when the summary follows the language of the content instead of [summaryLanguage]. */
    val useContentLanguage: Boolean,
    /** The app's locale; it also selects regional endpoints of some providers. */
    val appLanguage: Locale,
    /** The language named in the prompt when the user chose one. */
    val summaryLanguage: Locale?,
    val isAppendMode: Boolean,
    val customBasePrompt: String,
    val additionalSystemPrompt: String,
    /** The base prompt of a non-default profile; null keeps the legacy base/custom prompt. */
    val profileBasePrompt: String?,
    val structuredOutput: Boolean,
)

/**
 * The language model side of the use cases. [LLMHandler] is the real implementation; use cases
 * depend on this interface so they can be tested without network or Android.
 */
interface LlmGateway {
    /** Extracts the content of [source] and summarizes it. */
    suspend fun summarize(request: SummarizationRequest, source: SummarySource): SummarizationOutcome

    /** Fetches the content of a video, article or document again, without asking a model. */
    suspend fun extractContent(source: SummarySource): ExtractedContent

    /** Answers [question] about [sourceText] with a plain request (no agent, no tools). */
    suspend fun askQuestion(
        provider: AIProvider,
        apiKey: String,
        baseUrl: String?,
        model: String?,
        appLanguage: Locale,
        sourceText: String,
        history: List<ChatExchange>,
        question: String,
    ): String
}

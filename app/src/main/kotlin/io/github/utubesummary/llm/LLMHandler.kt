package io.github.utubesummary.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import android.content.Context
import io.ktor.client.HttpClient
import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.llm.tools.ArticleExtractorTool
import io.github.utubesummary.llm.tools.BiliBiliSubtitleTool
import io.github.utubesummary.llm.tools.FileExtractorTool
import io.github.utubesummary.llm.tools.YouTubeTranscriptTool
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.ChatExchange
import io.github.utubesummary.model.SummarySource
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val MAX_AGENT_CACHE_SIZE = 20

private data class AgentCacheKey(
    val provider: AIProvider,
    val apiKey: String,
    val baseUrl: String?,
    val model: String?,
    val summaryLength: SummaryLength,
    val systemPrompt: String,
    val structuredOutput: Boolean,
)

class LLMHandler(
    context: Context,
    httpClient: HttpClient,
    userPreferencesRepository: UserPreferencesRepository,
) {
    private val executorFactory = LLMExecutorFactory(httpClient)

    private val fileExtractorTool = FileExtractorTool(context)
    private val articleExtractorTool = ArticleExtractorTool(httpClient)
    private val youTubeTranscriptTool = YouTubeTranscriptTool(httpClient)
    private val bilibiliSubtitleTool = BiliBiliSubtitleTool(httpClient, userPreferencesRepository)

    private val toolRegistry = ToolRegistry {
        tool(articleExtractorTool)
        tool(youTubeTranscriptTool)
        tool(bilibiliSubtitleTool)
        tool(fileExtractorTool)
    }

    private val agentCache =
        ConcurrentHashMap<AgentCacheKey, AIAgent<SummarySource, SummarizationOutcome>>()

    fun getSummarizationAgent(
        provider: AIProvider,
        apiKey: String,
        baseUrl: String? = null,
        model: String? = null,
        summaryLength: SummaryLength = SummaryLength.MEDIUM,
        showLength: Boolean = true,
        useContentLanguage: Boolean,
        appLanguage: Locale,
        isAppendMode: Boolean = true,
        customBasePrompt: String = "",
        additionalSystemPrompt: String = "",
        profileBasePrompt: String? = null,
        structuredOutput: Boolean = true,
    ): AIAgent<SummarySource, SummarizationOutcome> {
        val promptString = generateFinalPromptString(
            length = summaryLength,
            showLength = showLength,
            useContentLanguage = useContentLanguage,
            appLanguage = appLanguage.getDisplayLanguage(Locale.ENGLISH),
            isAppendMode = isAppendMode,
            customBasePrompt = customBasePrompt,
            additionalSystemPrompt = additionalSystemPrompt,
            profileBasePrompt = profileBasePrompt,
        )

        val cacheKey = AgentCacheKey(
            provider = provider,
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = model,
            summaryLength = summaryLength,
            systemPrompt = promptString,
            structuredOutput = structuredOutput,
        )

        return agentCache.computeIfAbsent(cacheKey) {
            if (agentCache.size > MAX_AGENT_CACHE_SIZE) {
                agentCache.clear()
            }
            val executor =
                executorFactory.getOrCreateExecutor(provider, apiKey, baseUrl, appLanguage)
            val agentConfig = AgentConfigFactory.createAgentConfig(provider, model, promptString)

            AIAgent(
                promptExecutor = executor,
                strategy = createSummarizationStrategy(
                    provider = provider,
                    summaryLength = summaryLength,
                    model = agentConfig.model,
                    articleExtractorTool = articleExtractorTool,
                    youTubeTranscriptTool = youTubeTranscriptTool,
                    bilibiliSubtitleTool = bilibiliSubtitleTool,
                    fileExtractorTool = fileExtractorTool,
                    structuredOutput = structuredOutput,
                ),
                agentConfig = agentConfig,
                toolRegistry = toolRegistry,
            )
        }
    }

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
    ): String {
        val executor = executorFactory.getOrCreateExecutor(provider, apiKey, baseUrl, appLanguage)
        val llModel = AgentConfigFactory.resolveModel(provider, model)
        val response = executor.execute(buildQuestionPrompt(sourceText, history, question), llModel)
        return response.textContent().trim()
    }
}

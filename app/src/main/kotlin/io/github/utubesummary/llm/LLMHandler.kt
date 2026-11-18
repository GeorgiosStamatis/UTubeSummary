package io.github.utubesummary.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import android.content.Context
import io.ktor.client.HttpClient
import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.llm.tools.preferredTranscriptLanguage
import io.github.utubesummary.llm.tools.ArticleExtractorTool
import io.github.utubesummary.llm.tools.BiliBiliSubtitleTool
import io.github.utubesummary.llm.tools.FileExtractorTool
import io.github.utubesummary.llm.tools.YouTubeTranscriptTool
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.ChatExchange
import io.github.utubesummary.model.SummarySource
import java.util.Locale

private const val MAX_AGENT_CACHE_SIZE = 20

private data class AgentCacheKey(
    val provider: AIProvider,
    val apiKeyDigest: String,
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
    private val progress: SummarizationProgress,
) : LlmGateway {
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
        BoundedLruCache<AgentCacheKey, AIAgent<SummarySource, SummarizationOutcome>>(MAX_AGENT_CACHE_SIZE)

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
        summaryLanguage: Locale? = null,
    ): AIAgent<SummarySource, SummarizationOutcome> {
        val promptString = generateFinalPromptString(
            length = summaryLength,
            showLength = showLength,
            useContentLanguage = useContentLanguage,
            // appLanguage is the app locale (it also selects regional endpoints); the language named
            // in the prompt is the chosen summary language when there is one.
            appLanguage = (summaryLanguage ?: appLanguage).getDisplayLanguage(Locale.ENGLISH),
            isAppendMode = isAppendMode,
            customBasePrompt = customBasePrompt,
            additionalSystemPrompt = additionalSystemPrompt,
            profileBasePrompt = profileBasePrompt,
        )

        val cacheKey = AgentCacheKey(
            provider = provider,
            apiKeyDigest = apiKey.sha256Hex(),
            baseUrl = baseUrl,
            model = model,
            summaryLength = summaryLength,
            systemPrompt = promptString,
            structuredOutput = structuredOutput,
        )

        return agentCache.getOrPut(cacheKey) {
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
                    preferredTranscriptLanguage = preferredTranscriptLanguage(
                        useContentLanguage, summaryLanguage, appLanguage
                    ),
                    onPhase = progress::report,
                ),
                agentConfig = agentConfig,
                toolRegistry = toolRegistry,
            )
        }
    }

    override suspend fun summarize(
        request: SummarizationRequest,
        source: SummarySource,
    ): SummarizationOutcome = getSummarizationAgent(
        provider = request.provider,
        apiKey = request.apiKey,
        baseUrl = request.baseUrl,
        model = request.model,
        summaryLength = request.summaryLength,
        showLength = request.showLength,
        useContentLanguage = request.useContentLanguage,
        appLanguage = request.appLanguage,
        isAppendMode = request.isAppendMode,
        customBasePrompt = request.customBasePrompt,
        additionalSystemPrompt = request.additionalSystemPrompt,
        profileBasePrompt = request.profileBasePrompt,
        structuredOutput = request.structuredOutput,
        summaryLanguage = request.summaryLanguage,
    ).run(source)

    /** Answers [question] about [sourceText] with a plain request (no agent, no tools). */
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
        val executor = executorFactory.getOrCreateExecutor(provider, apiKey, baseUrl, appLanguage)
        val llModel = AgentConfigFactory.resolveModel(provider, model)
        val response = executor.execute(buildQuestionPrompt(sourceText, history, question), llModel)
        return response.textContent().trim()
    }
}

package io.github.utubesummary.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import io.github.utubesummary.domain.provider.AppLocaleProvider
import io.github.utubesummary.domain.provider.DocumentMetadataProvider
import io.github.utubesummary.domain.provider.SourceTextStore
import io.github.utubesummary.domain.repository.AIProviderConfigRepository
import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.exception.toSummaryException
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.llm.LLMHandler
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput
import io.github.utubesummary.model.SummaryProfile
import io.github.utubesummary.model.SummarySource
import io.github.utubesummary.model.SummaryType
import io.github.utubesummary.model.VideoSubtype
import javax.inject.Inject

interface SummarizeContentUseCase {
    suspend operator fun invoke(
        text: String,
        overrideLength: SummaryLength? = null,
        profileId: String? = null,
    ): Result<SummaryOutput>
}

class SummarizeContentUseCaseImpl @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiProviderConfigRepository: AIProviderConfigRepository,
    private val historyRepository: HistoryRepository,
    private val llmHandler: LLMHandler,
    private val documentMetadataProvider: DocumentMetadataProvider,
    private val appLocaleProvider: AppLocaleProvider,
    private val sourceTextStore: SourceTextStore,
) : SummarizeContentUseCase {
    override suspend operator fun invoke(
        text: String,
        overrideLength: SummaryLength?,
        profileId: String?,
    ): Result<SummaryOutput> = withContext(Dispatchers.IO) {
        var requestContext = ""
        runCatching {
            val prefs = userPreferencesRepository.preferencesFlow.first()
            val providerConfigs = aiProviderConfigRepository.providerConfigsFlow.first()

            val active = resolveActiveProvider(prefs, providerConfigs)
            requestContext = active.description

            val source = resolveSummarySource(text, prefs.autoExtractUrl)
            if (source is SummarySource.None) {
                throw SummaryException.NoContentException()
            }

            val targetLength = if (prefs.showLength) {
                overrideLength
                    ?: SummaryLength.entries.find { it.name == prefs.summaryLength && it != SummaryLength.NONE }
                    ?: SummaryLength.MEDIUM
            } else {
                SummaryLength.NONE
            }

            val profile = resolveProfile(prefs, profileId)

            val agent = llmHandler.getSummarizationAgent(
                provider = active.provider,
                apiKey = active.apiKey,
                baseUrl = active.baseUrl,
                model = active.model,
                summaryLength = targetLength,
                showLength = prefs.showLength,
                useContentLanguage = prefs.useOriginalLanguage,
                appLanguage = appLocaleProvider.getCurrentLocale(),
                isAppendMode = prefs.isAppendMode,
                customBasePrompt = prefs.customBasePrompt,
                additionalSystemPrompt = prefs.additionalSystemPrompt,
                // Default profile keeps the legacy base/custom prompt behavior.
                profileBasePrompt = profile.basePrompt.takeIf { profile.id != DEFAULT_PROFILE_ID },
            )

            val outcome = agent.run(source)
            val summaryOutput = outcome.summary
            // Kept in memory only, so questions can be asked about this content.
            sourceTextStore.put(text, outcome.sourceText)

            saveSummaryToHistory(
                summaryOutput = summaryOutput,
                summaryLength = targetLength,
                source = source,
                provider = active.provider.name,
                model = active.model,
                profile = profile,
            )

            summaryOutput
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                val summaryException = error.toSummaryException()
                if (requestContext.isNotEmpty()) {
                    summaryException.technicalDetails =
                        requestContext + "\n" + summaryException.technicalDetails.orEmpty()
                }
                Result.failure(summaryException)
            }
        )
    }

    private suspend fun resolveSummarySource(text: String, autoExtractUrl: Boolean): SummarySource {
        if (text.startsWith("content://") || text.startsWith("file://")) {
            val filename = documentMetadataProvider.getFileName(text) ?: "Document"
            return SummarySource.Document(filename, text)
        }

        val processedText = if (autoExtractUrl) extractHttpUrl(text) else text
        return when {
            processedText.startsWith("http://", ignoreCase = true) ||
                    processedText.startsWith("https://", ignoreCase = true) -> {
                if (VideoSubtype.fromUrl(processedText) != null) {
                    SummarySource.Video(processedText)
                } else {
                    SummarySource.Article(processedText)
                }
            }

            processedText.isNotBlank() -> SummarySource.Text(processedText)
            else -> SummarySource.None
        }
    }

    private fun extractHttpUrl(text: String): String {
        val urlRegex = Regex(
            "(?:^|\\W)((http|https)://)" +
                    "([\\w\\-]+\\.)+" +
                    "([\\w\\-]+)" +
                    "([^\\s<>\"#%{}|\\\\^`]*)"
        )
        return urlRegex.find(text)?.value?.trim() ?: text
    }

    private suspend fun saveSummaryToHistory(
        summaryOutput: SummaryOutput,
        summaryLength: SummaryLength,
        source: SummarySource,
        provider: String,
        model: String?,
        profile: SummaryProfile,
    ) {
        if (source is SummarySource.None) return

        val type: SummaryType
        var subtype: VideoSubtype? = null
        var sourceLink: String? = null
        var sourceText: String? = null

        when (source) {
            is SummarySource.Article -> {
                type = SummaryType.ARTICLE
                sourceLink = source.url
            }

            is SummarySource.Document -> {
                type = SummaryType.DOCUMENT
                sourceLink = source.uri
            }

            is SummarySource.Text -> {
                type = SummaryType.TEXT
                sourceText = source.content
            }

            is SummarySource.Video -> {
                type = SummaryType.VIDEO
                sourceLink = source.url
                subtype = VideoSubtype.fromUrl(source.url)
            }

            is SummarySource.None -> return
        }

        val initialResult = HistoryLengthResult(
            length = summaryLength,
            summary = summaryOutput.summary.trim(),
            provider = provider,
            model = model,
            overview = summaryOutput.overview,
            keyPoints = summaryOutput.keyPoints,
            tags = summaryOutput.tags,
            profileId = profile.id,
            profileName = profile.name,
        )

        val summary = HistorySummary(
            title = summaryOutput.title,
            author = summaryOutput.author,
            length = summaryLength,
            type = type,
            subtype = subtype,
            sourceLink = sourceLink,
            sourceText = sourceText,
            lengthResults = mapOf(summaryLength to initialResult),
        )
        if (summary.summary.isNotBlank() && summary.summary != "invalid link") {
            historyRepository.addSummary(summary)
        }
    }
}

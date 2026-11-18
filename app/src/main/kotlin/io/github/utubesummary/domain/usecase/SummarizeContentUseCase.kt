package io.github.utubesummary.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import io.github.utubesummary.domain.provider.AppLocaleProvider
import io.github.utubesummary.domain.provider.Clock
import io.github.utubesummary.domain.provider.SystemClock
import io.github.utubesummary.domain.provider.DocumentMetadataProvider
import io.github.utubesummary.domain.provider.SourceTextStore
import io.github.utubesummary.domain.repository.AIProviderConfigRepository
import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.exception.toSummaryException
import io.github.utubesummary.llm.LlmGateway
import io.github.utubesummary.llm.SummarizationRequest
import io.github.utubesummary.llm.SummarizationProgress
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.effectiveSummaryLanguage
import io.github.utubesummary.model.summaryPromptLocale
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
        language: String? = null,
    ): Result<SummaryOutput>
}

class SummarizeContentUseCaseImpl @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiProviderConfigRepository: AIProviderConfigRepository,
    private val historyRepository: HistoryRepository,
    private val llmGateway: LlmGateway,
    private val documentMetadataProvider: DocumentMetadataProvider,
    private val appLocaleProvider: AppLocaleProvider,
    private val sourceTextStore: SourceTextStore,
    private val progress: SummarizationProgress,
    private val clock: Clock = SystemClock,
) : SummarizeContentUseCase {
    override suspend operator fun invoke(
        text: String,
        overrideLength: SummaryLength?,
        profileId: String?,
        language: String?,
    ): Result<SummaryOutput> = withContext(Dispatchers.IO) {
        var requestContext = ""
        runCatching {
            val prefs = userPreferencesRepository.preferencesFlow.first()
            val providerConfigs = aiProviderConfigRepository.providerConfigsFlow.first()

            val active = resolveActiveProvider(prefs, providerConfigs)
            requestContext = active.description

            val source = classifySummarySource(
                text = text,
                autoExtractUrl = prefs.autoExtractUrl,
                documentName = if (isDocumentUri(text)) documentMetadataProvider.getFileName(text) else null,
            )
            if (source is SummarySource.None) {
                throw SummaryException.NoContentException()
            }

            val targetLength = resolveTargetLength(prefs.showLength, overrideLength, prefs.summaryLength)

            val profile = resolveProfile(prefs, profileId)
            val appLocale = appLocaleProvider.getCurrentLocale()
            val promptLocale = summaryPromptLocale(language ?: effectiveSummaryLanguage(prefs), appLocale)

            val request = SummarizationRequest(
                provider = active.provider,
                apiKey = active.apiKey,
                baseUrl = active.baseUrl,
                model = active.model,
                summaryLength = targetLength,
                showLength = prefs.showLength,
                useContentLanguage = promptLocale == null,
                appLanguage = appLocale,
                summaryLanguage = promptLocale,
                isAppendMode = prefs.isAppendMode,
                customBasePrompt = prefs.customBasePrompt,
                additionalSystemPrompt = prefs.additionalSystemPrompt,
                // Default profile keeps the legacy base/custom prompt behavior.
                profileBasePrompt = profile.basePrompt.takeIf { profile.id != DEFAULT_PROFILE_ID },
                structuredOutput = usesStructuredOutput(profile),
            )

            val progressId = progress.start()
            val outcome = try {
                llmGateway.summarize(request, source)
            } finally {
                progress.finish(progressId)
            }
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
                language = language ?: effectiveSummaryLanguage(prefs),
            )

            summaryOutput
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                val summaryException = error.toSummaryException()
                if (requestContext.isNotEmpty()) {
                    summaryException.technicalDetails =
                        requestContext + "\n" + summaryException.technicalDetails.orEmpty()
                }
                Result.failure(summaryException)
            }
        )
    }

    private suspend fun saveSummaryToHistory(
        summaryOutput: SummaryOutput,
        summaryLength: SummaryLength,
        source: SummarySource,
        provider: String,
        model: String?,
        profile: SummaryProfile,
        language: String,
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
            language = language,
            createdOn = clock.nowMillis(),
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

private fun isDocumentUri(text: String): Boolean =
    text.startsWith("content://") || text.startsWith("file://")

/**
 * Decides what the user wants summarized: a document/image URI, a video or article link (the first
 * link found in the text when [autoExtractUrl] is on) or plain text.
 */
internal fun classifySummarySource(
    text: String,
    autoExtractUrl: Boolean,
    documentName: String?,
): SummarySource {
    if (isDocumentUri(text)) {
        return SummarySource.Document(documentName ?: "Document", text)
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

/** The length to ask for: none when length options are off, else the override or the saved one. */
internal fun resolveTargetLength(
    showLength: Boolean,
    overrideLength: SummaryLength?,
    savedLength: String,
): SummaryLength {
    if (!showLength) return SummaryLength.NONE
    return overrideLength
        ?: SummaryLength.entries.find { it.name == savedLength && it != SummaryLength.NONE }
        ?: SummaryLength.MEDIUM
}

private val urlRegex = Regex(
    "(?:^|\\W)((http|https)://)" +
            "([\\w\\-]+\\.)+" +
            "([\\w\\-]+)" +
            "([^\\s<>\"#%{}|\\\\^`]*)"
)

private fun extractHttpUrl(text: String): String =
    urlRegex.find(text)?.value?.trim() ?: text

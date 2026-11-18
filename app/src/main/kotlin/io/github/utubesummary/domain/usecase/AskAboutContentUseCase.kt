package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.provider.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import io.github.utubesummary.domain.provider.AppLocaleProvider
import io.github.utubesummary.domain.provider.SourceTextStore
import io.github.utubesummary.domain.repository.AIProviderConfigRepository
import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.exception.toSummaryException
import io.github.utubesummary.llm.LlmGateway
import io.github.utubesummary.model.ChatExchange
import javax.inject.Inject

/** Asks the active LLM a question about content that was just summarized. */
interface AskAboutContentUseCase {
    /** True if the text of [input] is still in memory (e.g. it was summarized in this session). */
    fun isAvailable(input: String): Boolean

    suspend operator fun invoke(
        input: String,
        history: List<ChatExchange>,
        question: String,
    ): Result<String>
}

class AskAboutContentUseCaseImpl @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiProviderConfigRepository: AIProviderConfigRepository,
    private val llmGateway: LlmGateway,
    private val appLocaleProvider: AppLocaleProvider,
    private val sourceTextStore: SourceTextStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AskAboutContentUseCase {

    override fun isAvailable(input: String): Boolean = sourceTextStore.get(input) != null

    override suspend fun invoke(
        input: String,
        history: List<ChatExchange>,
        question: String,
    ): Result<String> = withContext(ioDispatcher) {
        var requestContext = ""
        runCatching {
            val sourceText = sourceTextStore.get(input) ?: throw SummaryException.NoContentException()
            val prefs = userPreferencesRepository.preferencesFlow.first()
            val configs = aiProviderConfigRepository.providerConfigsFlow.first()
            val active = resolveActiveProvider(prefs, configs)
            requestContext = active.description

            val answer = llmGateway.askQuestion(
                provider = active.provider,
                apiKey = active.apiKey,
                baseUrl = active.baseUrl,
                model = active.model,
                appLanguage = appLocaleProvider.getCurrentLocale(),
                sourceText = sourceText,
                history = history,
                question = question,
            )
            if (answer.isBlank()) throw SummaryException.NoContentException()
            answer
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                val exception = error.toSummaryException()
                if (requestContext.isNotEmpty()) {
                    exception.technicalDetails =
                        requestContext + "\n" + exception.technicalDetails.orEmpty()
                }
                Result.failure(exception)
            }
        )
    }
}

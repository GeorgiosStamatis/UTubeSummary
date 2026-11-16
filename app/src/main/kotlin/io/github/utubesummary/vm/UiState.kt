package io.github.utubesummary.vm

import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ImportResult
import io.github.utubesummary.model.ProviderConfig
import io.github.utubesummary.model.ProviderTestResult
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput

data class AppStartAction(
    val content: String? = null,
    val autoTrigger: Boolean = false,
    val initialSummary: SummaryOutput? = null,
    val lengthResults: Map<SummaryLength, SummaryOutput> = emptyMap(),
)

data class SettingsUiState(
    val useOriginalLanguage: Boolean = true,
    val dynamicColor: Boolean = true,
    val theme: Int = 0,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val activeProvider: AIProvider? = null,
    val providerConfigs: Map<String, ProviderConfig> = emptyMap(),
    val activeModel: String? = null,
    val autoExtractUrl: Boolean = true,
    val showLength: Boolean = true,
    val summaryLength: SummaryLength = SummaryLength.MEDIUM,
    val bilibiliSessData: String = "",
    val bilibiliSessDataExpires: Long = 0L,
    val isAppendMode: Boolean = true,
    val customBasePrompt: String = "",
    val additionalSystemPrompt: String = "",
    val providerOrder: List<String> = emptyList(),
)

data class SummarizationState(
    val isLoading: Boolean = false,
    val summaryResult: SummaryOutput? = null,
    val error: SummaryException? = null,
    val lengthResults: Map<SummaryLength, SummaryOutput> = emptyMap(),
)

/** State of the "Test" button in the AI provider dialog. */
sealed interface ProviderTestState {
    data object Idle : ProviderTestState
    data object Testing : ProviderTestState
    data class Done(val result: ProviderTestResult) : ProviderTestState
}

/** One question in the conversation about the current content. */
data class ChatTurn(
    val question: String,
    val answer: String? = null,
    val error: SummaryException? = null,
)

data class ChatState(
    /** The extracted text is still in memory, so questions can be asked. */
    val canAsk: Boolean = false,
    val turns: List<ChatTurn> = emptyList(),
    val isAsking: Boolean = false,
)

enum class ImportFailure { INVALID_FILE, TOO_LARGE, UNREADABLE }

/** Progress of importing video summaries from a backup file (Settings). */
sealed interface ImportState {
    data object Idle : ImportState
    data object Importing : ImportState
    data class Done(val result: ImportResult) : ImportState
    data class Failed(val reason: ImportFailure) : ImportState
}

package io.github.utubesummary.vm

import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ProviderConfig
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

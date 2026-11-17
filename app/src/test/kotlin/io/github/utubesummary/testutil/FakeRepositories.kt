package io.github.utubesummary.testutil

import androidx.paging.PagingData
import io.github.utubesummary.domain.repository.AIProviderConfigRepository
import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.model.HistoryFilter
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.ProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

/** Provider settings held in memory. */
class FakeProviderConfigRepository(initial: Map<String, ProviderConfig> = emptyMap()) : AIProviderConfigRepository {
    val configs = MutableStateFlow(initial)

    override val providerConfigsFlow: Flow<Map<String, ProviderConfig>> = configs

    override fun getConfigFlow(provider: String): Flow<ProviderConfig?> = configs.map { it[provider] }

    override suspend fun getConfig(provider: String): ProviderConfig? = configs.value[provider]

    override suspend fun saveConfig(provider: String, config: ProviderConfig) {
        configs.value += (provider to config)
    }

    override suspend fun updateApiKey(provider: String, apiKey: String) {
        saveConfig(provider, (getConfig(provider) ?: ProviderConfig()).copy(apiKey = apiKey))
    }

    override suspend fun updateBaseUrl(provider: String, baseUrl: String) {
        saveConfig(provider, (getConfig(provider) ?: ProviderConfig()).copy(baseUrl = baseUrl))
    }

    override suspend fun updateModel(provider: String, model: String) {
        saveConfig(provider, (getConfig(provider) ?: ProviderConfig()).copy(activeModel = model))
    }
}

/** History held in memory; [added] records what the use cases saved. */
class FakeHistoryRepository : HistoryRepository {
    val added = mutableListOf<HistorySummary>()

    override fun getSummaries(query: String, filter: HistoryFilter): Flow<PagingData<HistorySummary>> = emptyFlow()
    override suspend fun setPinned(id: String, pinned: Boolean) {}
    override suspend fun addSummary(summary: HistorySummary) { added += summary }
    override suspend fun replaceSummary(summary: HistorySummary) {}
    override suspend fun addSummaries(summaries: List<HistorySummary>) { added += summaries }
    override suspend fun getAllSourceLinks(): List<String> = added.mapNotNull { it.sourceLink }
    override suspend fun getAllSummaries(): List<HistorySummary> = added.toList()
    override suspend fun getSummariesByIds(ids: List<String>): List<HistorySummary> = added.filter { it.id in ids }
    override suspend fun deleteSummary(id: String) {}
}

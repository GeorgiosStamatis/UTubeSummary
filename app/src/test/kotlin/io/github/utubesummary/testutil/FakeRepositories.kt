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
import io.github.utubesummary.util.SecureStorageException

/** Provider settings held in memory. */
class FakeProviderConfigRepository(initial: Map<String, ProviderConfig> = emptyMap()) : AIProviderConfigRepository {
    val configs = MutableStateFlow(initial)

    /** Makes [saveConfig] fail like a broken secure storage would. */
    var failSave = false

    override val providerConfigsFlow: Flow<Map<String, ProviderConfig>> = configs

    override fun getConfigFlow(provider: String): Flow<ProviderConfig?> = configs.map { it[provider] }

    override suspend fun getConfig(provider: String): ProviderConfig? = configs.value[provider]

    override suspend fun saveConfig(provider: String, config: ProviderConfig) {
        if (failSave) throw SecureStorageException("Encryption failed")
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

/** History held in memory with the behaviour of the real store: saving an id replaces the old entry. */
class FakeHistoryRepository : HistoryRepository {
    val items = mutableListOf<HistorySummary>()

    /** The entries saved so far (same list as [items]). */
    val added: MutableList<HistorySummary> get() = items

    /** Ids passed to [deleteSummary], in call order. */
    val deleted = mutableListOf<String>()

    /** Calls of [setPinned], in call order, and the last value per id. */
    val pinCalls = mutableListOf<Pair<String, Boolean>>()
    val pinned = mutableMapOf<String, Boolean>()

    /** The arguments of the last [getSummaries] call. */
    var lastQuery: String? = null
    var lastFilter: HistoryFilter? = null

    override fun getSummaries(query: String, filter: HistoryFilter): Flow<PagingData<HistorySummary>> {
        lastQuery = query
        lastFilter = filter
        return emptyFlow()
    }

    override suspend fun setPinned(id: String, pinned: Boolean) {
        pinCalls += id to pinned
        this.pinned[id] = pinned
    }

    override suspend fun addSummary(summary: HistorySummary) {
        items.removeAll { it.id == summary.id }
        items.add(summary)
    }

    override suspend fun replaceSummary(summary: HistorySummary) = addSummary(summary)

    override suspend fun addSummaries(summaries: List<HistorySummary>) {
        items.addAll(summaries)
    }

    override suspend fun getAllSourceLinks(): List<String> = items.mapNotNull { it.sourceLink }
    override suspend fun getAllSummaries(): List<HistorySummary> = items.toList()
    override suspend fun getSummariesByIds(ids: List<String>): List<HistorySummary> = items.filter { it.id in ids }

    override suspend fun deleteSummary(id: String) {
        deleted += id
        items.removeAll { it.id == id }
    }
}

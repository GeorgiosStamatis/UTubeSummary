package io.github.utubesummary.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryType

interface HistoryRepository {
    fun getSummaries(query: String, type: SummaryType?): Flow<PagingData<HistorySummary>>
    suspend fun addSummary(summary: HistorySummary)
    suspend fun deleteSummary(id: String)
}

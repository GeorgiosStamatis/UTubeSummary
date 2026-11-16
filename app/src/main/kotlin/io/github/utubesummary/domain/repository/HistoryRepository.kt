package io.github.utubesummary.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryType

interface HistoryRepository {
    fun getSummaries(query: String, type: SummaryType?): Flow<PagingData<HistorySummary>>
    suspend fun addSummary(summary: HistorySummary)

    /** Saves all [summaries] at once, as they are (no merging with existing entries). */
    suspend fun addSummaries(summaries: List<HistorySummary>)

    /** The source link of every history entry that has one. */
    suspend fun getAllSourceLinks(): List<String>
    suspend fun deleteSummary(id: String)
}

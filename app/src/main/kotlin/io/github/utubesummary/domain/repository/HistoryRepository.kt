package io.github.utubesummary.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import io.github.utubesummary.model.HistoryFilter
import io.github.utubesummary.model.HistorySummary

interface HistoryRepository {
    fun getSummaries(query: String, filter: HistoryFilter): Flow<PagingData<HistorySummary>>

    /** Pins or unpins an item; pinned items are listed first. */
    suspend fun setPinned(id: String, pinned: Boolean)
    suspend fun addSummary(summary: HistorySummary)

    /** Writes [summary] as it is, replacing the stored entry with the same id (or adding it). */
    suspend fun replaceSummary(summary: HistorySummary)

    /** Saves all [summaries] at once, as they are (no merging with existing entries). */
    suspend fun addSummaries(summaries: List<HistorySummary>)

    /** The source link of every history entry that has one. */
    suspend fun getAllSourceLinks(): List<String>
    suspend fun deleteSummary(id: String)
}

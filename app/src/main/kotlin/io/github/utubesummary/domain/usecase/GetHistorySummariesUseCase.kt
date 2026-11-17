package io.github.utubesummary.domain.usecase

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.model.HistoryFilter
import io.github.utubesummary.model.HistorySummary
import javax.inject.Inject

class GetHistorySummariesUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(query: String, filter: HistoryFilter): Flow<PagingData<HistorySummary>> {
        return historyRepository.getSummaries(query, filter)
    }
}

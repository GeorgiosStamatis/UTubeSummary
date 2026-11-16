package io.github.utubesummary.domain.usecase

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryType
import javax.inject.Inject

class GetHistorySummariesUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(query: String, type: SummaryType?): Flow<PagingData<HistorySummary>> {
        return historyRepository.getSummaries(query, type)
    }
}

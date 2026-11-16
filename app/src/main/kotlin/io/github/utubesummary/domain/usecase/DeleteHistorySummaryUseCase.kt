package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.repository.HistoryRepository
import javax.inject.Inject

class DeleteHistorySummaryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(id: String) {
        historyRepository.deleteSummary(id)
    }
}

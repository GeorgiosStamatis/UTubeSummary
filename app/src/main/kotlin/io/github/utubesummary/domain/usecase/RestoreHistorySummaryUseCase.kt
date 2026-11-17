package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.model.HistorySummary
import javax.inject.Inject

class RestoreHistorySummaryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    /** Puts the deleted entry back exactly as it was (same id and date, no merging). */
    suspend operator fun invoke(summary: HistorySummary) {
        historyRepository.addSummaries(listOf(summary))
    }
}

package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.model.HistorySummary
import javax.inject.Inject

/** Loads one History entry by id; null if it no longer exists. */
class GetHistorySummaryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(id: String): HistorySummary? =
        historyRepository.getSummariesByIds(listOf(id)).firstOrNull()
}

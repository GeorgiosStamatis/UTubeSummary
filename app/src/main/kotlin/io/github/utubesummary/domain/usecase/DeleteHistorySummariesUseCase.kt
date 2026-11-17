package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.model.HistorySummary
import javax.inject.Inject

/** Deletes several History entries at once and returns them, so the deletion can be undone. */
class DeleteHistorySummariesUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(ids: Collection<String>): List<HistorySummary> {
        val entries = historyRepository.getSummariesByIds(ids.toList())
        entries.forEach { historyRepository.deleteSummary(it.id) }
        return entries
    }
}

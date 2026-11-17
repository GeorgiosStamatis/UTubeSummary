package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.withoutResult
import javax.inject.Inject

/** Removes one summary from a History entry; the entry is deleted with its last summary. */
class RemoveHistoryResultUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    /** Returns the entry as it is now, or null if it was deleted. */
    suspend operator fun invoke(summary: HistorySummary, result: HistoryLengthResult): HistorySummary? {
        val updated = summary.withoutResult(result)
        if (updated == null) {
            historyRepository.deleteSummary(summary.id)
        } else {
            historyRepository.replaceSummary(updated)
        }
        return updated
    }

    /** Puts the entry back as it was before the removal. */
    suspend fun undo(original: HistorySummary) {
        historyRepository.replaceSummary(original)
    }
}

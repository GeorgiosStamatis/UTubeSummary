package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.repository.HistoryRepository
import javax.inject.Inject

class SetHistoryPinnedUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    suspend operator fun invoke(ids: Collection<String>, pinned: Boolean) {
        ids.forEach { historyRepository.setPinned(it, pinned) }
    }

    suspend operator fun invoke(id: String, pinned: Boolean) {
        historyRepository.setPinned(id, pinned)
    }
}

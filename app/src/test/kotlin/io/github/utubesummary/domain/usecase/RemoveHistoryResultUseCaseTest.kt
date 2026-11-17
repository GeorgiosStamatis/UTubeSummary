package io.github.utubesummary.domain.usecase

import androidx.paging.PagingData
import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.model.HistoryFilter
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RemoveHistoryResultUseCaseTest {

    private class FakeRepository(var stored: HistorySummary?) : HistoryRepository {
        override fun getSummaries(query: String, filter: HistoryFilter): Flow<PagingData<HistorySummary>> = emptyFlow()
        override suspend fun setPinned(id: String, pinned: Boolean) {}
        override suspend fun addSummary(summary: HistorySummary) {}
        override suspend fun getSummariesByIds(ids: List<String>): List<HistorySummary> = emptyList()
        override suspend fun replaceSummary(summary: HistorySummary) { stored = summary }
        override suspend fun addSummaries(summaries: List<HistorySummary>) {}
        override suspend fun getAllSourceLinks(): List<String> = emptyList()
        override suspend fun deleteSummary(id: String) { stored = null }
    }

    private val first = HistoryLengthResult(SummaryLength.MEDIUM, "first", profileId = "a", createdOn = 100L)
    private val second = HistoryLengthResult(SummaryLength.MEDIUM, "second", createdOn = 200L)
    private val entry = HistorySummary(
        title = "t",
        type = SummaryType.ARTICLE,
        length = SummaryLength.MEDIUM,
        lengthResults = mapOf(SummaryLength.MEDIUM to second),
        alternatives = listOf(first),
    )

    @Test
    fun `removing one summary updates the entry and undo restores it`() = runTest {
        val repository = FakeRepository(entry)
        val useCase = RemoveHistoryResultUseCase(repository)

        val updated = useCase(entry, first)

        assertEquals(entry.copy(alternatives = emptyList()), updated)
        assertEquals(updated, repository.stored)

        useCase.undo(entry)
        assertEquals(entry, repository.stored)
    }

    @Test
    fun `removing the last summary deletes the entry`() = runTest {
        val single = entry.copy(alternatives = emptyList())
        val repository = FakeRepository(single)

        assertNull(RemoveHistoryResultUseCase(repository)(single, second))
        assertNull(repository.stored)
    }
}

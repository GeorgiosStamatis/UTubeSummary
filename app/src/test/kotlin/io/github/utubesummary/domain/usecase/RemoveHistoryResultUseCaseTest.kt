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
import io.github.utubesummary.testutil.FakeHistoryRepository
import org.junit.jupiter.api.Assertions.assertTrue

class RemoveHistoryResultUseCaseTest {

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
        val repository = FakeHistoryRepository().apply { items += entry }
        val useCase = RemoveHistoryResultUseCase(repository)

        val updated = useCase(entry, first)

        assertEquals(entry.copy(alternatives = emptyList()), updated)
        assertEquals(updated, repository.items.single())

        useCase.undo(entry)
        assertEquals(entry, repository.items.single())
    }

    @Test
    fun `removing the last summary deletes the entry`() = runTest {
        val single = entry.copy(alternatives = emptyList())
        val repository = FakeHistoryRepository().apply { items += single }

        assertNull(RemoveHistoryResultUseCase(repository)(single, second))
        assertTrue(repository.items.isEmpty())
    }
}

package io.github.utubesummary.vm

import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.domain.usecase.DeleteHistorySummariesUseCase
import io.github.utubesummary.domain.usecase.DeleteHistorySummaryUseCase
import io.github.utubesummary.domain.usecase.GetHistorySummariesUseCase
import io.github.utubesummary.domain.usecase.GetHistorySummaryUseCase
import io.github.utubesummary.domain.usecase.RemoveHistoryResultUseCase
import io.github.utubesummary.domain.usecase.RestoreHistorySummaryUseCase
import io.github.utubesummary.domain.usecase.SetHistoryPinnedUseCase
import io.github.utubesummary.model.HistoryFilter
import io.github.utubesummary.model.HistorySort
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeHistoryRepository : HistoryRepository {
        val items = mutableListOf<HistorySummary>()

        var lastFilter: HistoryFilter? = null
        val pinned = mutableMapOf<String, Boolean>()

        override fun getSummaries(
            query: String,
            filter: HistoryFilter,
        ): Flow<PagingData<HistorySummary>> {
            lastFilter = filter
            return emptyFlow()
        }

        override suspend fun setPinned(id: String, pinned: Boolean) {
            this.pinned[id] = pinned
        }

        override suspend fun getAllSummaries(): List<HistorySummary> = items.toList()

        override suspend fun getSummariesByIds(ids: List<String>): List<HistorySummary> =
            items.filter { it.id in ids }

        override suspend fun replaceSummary(summary: HistorySummary) {
            items.removeAll { it.id == summary.id }
            items.add(summary)
        }

        override suspend fun addSummary(summary: HistorySummary) {
            items.removeAll { it.id == summary.id }
            items.add(summary)
        }

        override suspend fun addSummaries(summaries: List<HistorySummary>) {
            items.addAll(summaries)
        }

        override suspend fun getAllSourceLinks(): List<String> = items.mapNotNull { it.sourceLink }

        override suspend fun deleteSummary(id: String) {
            items.removeAll { it.id == id }
        }
    }

    private lateinit var fakeRepository: FakeHistoryRepository
    private lateinit var viewModel: HistoryViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeHistoryRepository()
        viewModel = HistoryViewModel(
            getHistorySummariesUseCase = GetHistorySummariesUseCase(fakeRepository),
            deleteHistorySummaryUseCase = DeleteHistorySummaryUseCase(fakeRepository),
            restoreHistorySummaryUseCase = RestoreHistorySummaryUseCase(fakeRepository),
            setHistoryPinnedUseCase = SetHistoryPinnedUseCase(fakeRepository),
            removeHistoryResultUseCase = RemoveHistoryResultUseCase(fakeRepository),
            deleteHistorySummariesUseCase = DeleteHistorySummariesUseCase(fakeRepository),
            getHistorySummaryUseCase = GetHistorySummaryUseCase(fakeRepository),
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test onFilterChanged toggles filter on and off`() {
        assertNull(viewModel.filterType.value)

        // Select VIDEO
        viewModel.onFilterChanged(SummaryType.VIDEO)
        assertEquals(SummaryType.VIDEO, viewModel.filterType.value)

        // Select VIDEO again -> toggles to null
        viewModel.onFilterChanged(SummaryType.VIDEO)
        assertNull(viewModel.filterType.value)

        // Select ARTICLE
        viewModel.onFilterChanged(SummaryType.ARTICLE)
        assertEquals(SummaryType.ARTICLE, viewModel.filterType.value)
    }

    @Test
    fun `test onSearchTextChanged updates searchQuery`() {
        assertEquals("", viewModel.searchQuery.value)

        viewModel.onSearchTextChanged("compose")
        assertEquals("compose", viewModel.searchQuery.value)

        viewModel.onSearchTextChanged("kotlin multiplatform")
        assertEquals("kotlin multiplatform", viewModel.searchQuery.value)
    }

    @Test
    fun `test restoreSummary and deleteSummary delegates to repository`() =
        runTest(testDispatcher) {
            val summary1 = HistorySummary(
                id = "id-1",
                title = "Article 1",
                author = "Author 1",
                length = SummaryLength.SHORT,
                type = SummaryType.ARTICLE,
                lengthResults = mapOf(
                    SummaryLength.SHORT to HistoryLengthResult(
                        length = SummaryLength.SHORT,
                        summary = "Summary 1",
                        provider = "OPENAI"
                    )
                )
            )
            val summary2 = HistorySummary(
                id = "id-2",
                title = "Video 2",
                author = "Author 2",
                length = SummaryLength.LONG,
                type = SummaryType.VIDEO,
                lengthResults = mapOf(
                    SummaryLength.LONG to HistoryLengthResult(
                        length = SummaryLength.LONG,
                        summary = "Summary 2",
                        provider = "GEMINI"
                    )
                )
            )

            viewModel.restoreSummary(summary1)
            viewModel.restoreSummary(summary2)

            assertEquals(2, fakeRepository.items.size)
            assertTrue(fakeRepository.items.contains(summary1))
            assertTrue(fakeRepository.items.contains(summary2))

            viewModel.deleteSummary("id-1")
            assertEquals(1, fakeRepository.items.size)
            assertEquals("id-2", fakeRepository.items.first().id)
        }

    @Test
    fun `the profile filter toggles and pinning goes to the repository`() {
        viewModel.onProfileFilterChanged("quick_brief")
        assertEquals("quick_brief", viewModel.profileFilter.value)

        viewModel.onProfileFilterChanged("quick_brief")
        assertNull(viewModel.profileFilter.value)

        viewModel.onPinnedOnlyChanged(true)
        assertTrue(viewModel.pinnedOnly.value)

        viewModel.setPinned("abc", true)
        assertEquals(true, fakeRepository.pinned["abc"])
    }

    private fun entry(id: String) = HistorySummary(id = id, title = id, type = SummaryType.ARTICLE)

    @Test
    fun `selection toggles and bulk delete returns the deleted entries for undo`() = runTest(testDispatcher) {
        fakeRepository.items.addAll(listOf(entry("a"), entry("b"), entry("c")))

        viewModel.toggleSelection("a")
        viewModel.toggleSelection("c")
        viewModel.toggleSelection("b")
        viewModel.toggleSelection("b")
        assertEquals(setOf("a", "c"), viewModel.selectedIds.value)

        var deleted: List<HistorySummary> = emptyList()
        viewModel.deleteSelected { deleted = it }

        assertEquals(listOf("a", "c"), deleted.map { it.id })
        assertEquals(listOf("b"), fakeRepository.items.map { it.id })
        assertTrue(viewModel.selectedIds.value.isEmpty())

        viewModel.restoreSummaries(deleted)
        assertEquals(setOf("a", "b", "c"), fakeRepository.items.map { it.id }.toSet())
    }

    @Test
    fun `bulk pin applies to every selected entry and ends the selection`() = runTest(testDispatcher) {
        viewModel.toggleSelection("a")
        viewModel.toggleSelection("b")

        viewModel.setSelectedPinned(true)

        assertEquals(mapOf("a" to true, "b" to true), fakeRepository.pinned)
        assertTrue(viewModel.selectedIds.value.isEmpty())
    }

    @Test
    fun `the sort order can be changed and defaults to newest first`() {
        assertEquals(HistorySort.NEWEST, viewModel.sort.value)

        viewModel.onSortChanged(HistorySort.TITLE)

        assertEquals(HistorySort.TITLE, viewModel.sort.value)
    }

    @Test
    fun `an entry can be loaded again by its id and a deleted one gives null`() = runTest(testDispatcher) {
        fakeRepository.items.addAll(listOf(entry("a"), entry("b")))
        var loaded: HistorySummary? = entry("unset")
        var missing: HistorySummary? = entry("unset")

        viewModel.loadSummary("b") { loaded = it }
        viewModel.loadSummary("gone") { missing = it }

        assertEquals("b", loaded?.id)
        assertNull(missing)
    }
}

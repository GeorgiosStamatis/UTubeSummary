package io.github.utubesummary.data

import androidx.paging.PagingSource
import io.github.utubesummary.data.local.database.entity.HistoryEntity
import io.github.utubesummary.data.repository.HistoryRepositoryImpl
import io.github.utubesummary.data.repository.escapeLikePattern
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HistoryRepositoryImplTest {

    /** In-memory DAO whose reads yield, so unsynchronized read-modify-write cycles interleave. */
    private class FakeHistoryDao : HistoryDao {
        val rows = linkedMapOf<String, HistoryEntity>()

        override fun getSummaries(query: String, type: SummaryType?): PagingSource<Int, HistoryEntity> =
            error("not used")

        override suspend fun insert(summary: HistoryEntity) {
            rows[summary.id] = summary
        }

        override suspend fun insertAll(summaries: List<HistoryEntity>) {
            summaries.forEach { rows.putIfAbsent(it.id, it) }
        }

        override suspend fun getAllSourceLinks(): List<String> = rows.values.mapNotNull { it.sourceLink }

        override suspend fun getAllBySourceLink(link: String): List<HistoryEntity> {
            yield()
            return rows.values.filter { it.sourceLink == link }.sortedByDescending { it.createdOn }
        }

        override suspend fun getAllBySourceText(text: String): List<HistoryEntity> =
            rows.values.filter { it.sourceText == text }

        override suspend fun getAll(): List<HistoryEntity> = rows.values.toList()

        override suspend fun deleteById(id: String) {
            rows.remove(id)
        }

        override suspend fun deleteByIds(ids: List<String>) {
            ids.forEach { rows.remove(it) }
        }
    }

    private fun summary(
        length: SummaryLength,
        text: String,
        profileId: String? = null,
        link: String = "https://example.com/video",
    ) = HistorySummary(
        title = "Title",
        length = length,
        type = SummaryType.ARTICLE,
        sourceLink = link,
        lengthResults = mapOf(
            length to HistoryLengthResult(length = length, summary = text, profileId = profileId)
        ),
    )

    @Test
    fun `summaries of the same link are merged by length`() = runTest {
        val dao = FakeHistoryDao()
        val repository = HistoryRepositoryImpl(dao)

        repository.addSummary(summary(SummaryLength.SHORT, "short"))
        repository.addSummary(summary(SummaryLength.LONG, "long"))

        assertEquals(1, dao.rows.size)
        val lengths = dao.rows.values.single().lengthResults!!
        assertEquals(setOf(SummaryLength.SHORT, SummaryLength.LONG), lengths.keys)
    }

    @Test
    fun `a newer result for the same length replaces the older one and records its profile`() = runTest {
        val dao = FakeHistoryDao()
        val repository = HistoryRepositoryImpl(dao)

        repository.addSummary(summary(SummaryLength.MEDIUM, "default result"))
        repository.addSummary(summary(SummaryLength.MEDIUM, "brief result", profileId = "quick_brief"))

        val result = dao.rows.values.single().lengthResults!!.getValue(SummaryLength.MEDIUM)
        assertEquals("brief result", result.summary)
        assertEquals("quick_brief", result.profileId)
    }

    @Test
    fun `overlapping saves of the same link do not lose a result`() = runTest {
        val dao = FakeHistoryDao()
        val repository = HistoryRepositoryImpl(dao)

        listOf(
            async { repository.addSummary(summary(SummaryLength.SHORT, "short")) },
            async { repository.addSummary(summary(SummaryLength.LONG, "long")) },
        ).awaitAll()

        assertEquals(1, dao.rows.size)
        assertEquals(2, dao.rows.values.single().lengthResults!!.size)
    }

    @Test
    fun `addSummaries restores an entry exactly as it was`() = runTest {
        val dao = FakeHistoryDao()
        val repository = HistoryRepositoryImpl(dao)
        val deleted = summary(SummaryLength.SHORT, "short").copy(createdOn = 1_000L)

        repository.addSummaries(listOf(deleted))

        val restored = dao.rows.getValue(deleted.id)
        assertEquals(1_000L, restored.createdOn)
        assertTrue(restored.lengthResults!!.containsKey(SummaryLength.SHORT))
    }

    @Test
    fun `search wildcards are escaped so they match literally`() {
        assertEquals("100\\% sure", escapeLikePattern("100% sure"))
        assertEquals("snake\\_case", escapeLikePattern("snake_case"))
        assertEquals("a\\\\b", escapeLikePattern("a\\b"))
        assertEquals("plain", escapeLikePattern("plain"))
    }
}

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
import io.github.utubesummary.domain.provider.Clock

class HistoryRepositoryImplTest {

    /** In-memory DAO whose reads yield, so unsynchronized read-modify-write cycles interleave. */
    private class FakeHistoryDao : HistoryDao {
        val rows = linkedMapOf<String, HistoryEntity>()

        override fun getSummaries(
            query: String,
            type: SummaryType?,
            profileKey: String?,
            pinnedOnly: Boolean,
            sort: Int,
        ): PagingSource<Int, HistoryEntity> = error("not used")

        override suspend fun setPinned(id: String, pinned: Boolean) {
            rows[id]?.let { rows[id] = it.copy(pinned = pinned) }
        }

        override suspend fun insert(summary: HistoryEntity) {
            rows[summary.id] = summary
        }

        override suspend fun insertAll(summaries: List<HistoryEntity>) {
            summaries.forEach { rows.putIfAbsent(it.id, it) }
        }

        override suspend fun getByIds(ids: List<String>): List<HistoryEntity> = rows.values.filter { it.id in ids }

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
    fun `a merged entry is stamped with the time of the clock`() = runTest {
        val dao = FakeHistoryDao()
        var now = 1_000L
        val repository = HistoryRepositoryImpl(dao, clock = object : Clock {
            override fun nowMillis(): Long = now
        })

        repository.addSummary(summary(SummaryLength.SHORT, "short"))
        now = 2_000L
        repository.addSummary(summary(SummaryLength.LONG, "long"))

        assertEquals(2_000L, dao.rows.values.single().createdOn)
    }

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
    fun `a result of another profile becomes current and the old one is kept as an alternative`() = runTest {
        val dao = FakeHistoryDao()
        val repository = HistoryRepositoryImpl(dao)

        repository.addSummary(summary(SummaryLength.MEDIUM, "default result"))
        repository.addSummary(summary(SummaryLength.MEDIUM, "brief result", profileId = "quick_brief"))

        val row = dao.rows.values.single()
        val current = row.lengthResults!!.getValue(SummaryLength.MEDIUM)
        assertEquals("brief result", current.summary)
        assertEquals("quick_brief", current.profileId)
        assertEquals(listOf("default result"), row.alternatives!!.map { it.summary })
        assertEquals(" quick_brief default ", row.variantKeys)
    }

    @Test
    fun `the same profile and language replaces its result instead of adding a duplicate`() = runTest {
        val dao = FakeHistoryDao()
        val repository = HistoryRepositoryImpl(dao)

        repository.addSummary(summary(SummaryLength.MEDIUM, "first", profileId = "quick_brief"))
        repository.addSummary(summary(SummaryLength.MEDIUM, "second", profileId = "quick_brief"))

        val row = dao.rows.values.single()
        assertEquals("second", row.lengthResults!!.getValue(SummaryLength.MEDIUM).summary)
        assertTrue(row.alternatives.isNullOrEmpty())
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

    @Test
    fun `pinning survives a new summary of the same item`() = runTest {
        val dao = FakeHistoryDao()
        val repository = HistoryRepositoryImpl(dao)
        repository.addSummary(summary(SummaryLength.SHORT, "short"))
        val id = dao.rows.keys.single()

        repository.setPinned(id, true)
        repository.addSummary(summary(SummaryLength.LONG, "long"))

        assertTrue(dao.rows.values.single().pinned)
    }
}

package io.github.utubesummary.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.utubesummary.data.HistoryDao
import io.github.utubesummary.data.local.database.entity.HistoryEntity
import io.github.utubesummary.data.local.database.mapper.toDomain
import io.github.utubesummary.data.local.database.mapper.toEntity
import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.model.HistoryFilter
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.mergeHistoryResult
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 20

/** Makes `%`, `_` and `\` in a search text match literally (the DAO query uses `ESCAPE '\'`). */
internal fun escapeLikePattern(text: String): String =
    text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
) : HistoryRepository {
    @Volatile
    private var isDeduplicated = false
    private val deduplicationMutex = Mutex()

    // Serializes read-merge-write cycles so overlapping saves cannot overwrite each other.
    private val writeMutex = Mutex()

    override fun getSummaries(query: String, filter: HistoryFilter): Flow<PagingData<HistorySummary>> {
        // Profile ids are stored as " id1 id2 "; match whole ids, with LIKE wildcards escaped.
        val profileKey = filter.profileId?.let { " ${escapeLikePattern(it)} " }
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                historyDao.getSummaries(
                    escapeLikePattern(query), filter.type, profileKey, filter.pinnedOnly, filter.sort.code
                )
            }
        ).flow
            .onStart { ensureDeduplicated() }
            .map { pagingData ->
                pagingData.map { it.toDomain() }
            }
    }

    override suspend fun addSummaries(summaries: List<HistorySummary>) {
        historyDao.insertAll(summaries.map { it.toEntity() })
    }

    override suspend fun getAllSourceLinks(): List<String> = historyDao.getAllSourceLinks()

    override suspend fun addSummary(summary: HistorySummary): Unit = writeMutex.withLock {
        val existingRecords = findExistingRecords(summary)
        if (existingRecords.isNotEmpty()) {
            val primary = existingRecords.first().toDomain()
            val mergedLengthResults = mutableMapOf<SummaryLength, HistoryLengthResult>()
            for (record in existingRecords) {
                mergedLengthResults.putAll(record.toDomain().allLengthResults)
            }
            val newResult = summary.activeResult ?: HistoryLengthResult(
                length = summary.length,
                summary = summary.summary,
                provider = summary.provider,
                model = summary.model,
                overview = summary.overview,
                keyPoints = summary.keyPoints,
                tags = summary.tags,
            )
            // The new result becomes the current one for its length. A result of another profile or
            // language that it displaces is kept as an alternative (see mergeHistoryResult).
            val (currentResults, alternatives) = mergeHistoryResult(
                current = mergedLengthResults,
                alternatives = existingRecords.flatMap { it.toDomain().alternatives }.distinct(),
                new = newResult,
            )

            val updated = primary.copy(
                title = summary.title.ifBlank { primary.title },
                author = summary.author.ifBlank { primary.author },
                length = summary.length,
                createdOn = System.currentTimeMillis(),
                lengthResults = currentResults,
                alternatives = alternatives,
                pinned = existingRecords.any { it.pinned },
            )
            historyDao.insert(updated.toEntity())

            val extraIds = existingRecords.drop(1).map { it.id }
            if (extraIds.isNotEmpty()) {
                historyDao.deleteByIds(extraIds)
            }
        } else {
            val initialResults = summary.allLengthResults
            val newSummary = summary.copy(lengthResults = initialResults)
            historyDao.insert(newSummary.toEntity())
        }
    }

    override suspend fun getAllSummaries(): List<HistorySummary> =
        historyDao.getAll().map { it.toDomain() }

    override suspend fun getSummariesByIds(ids: List<String>): List<HistorySummary> =
        if (ids.isEmpty()) emptyList() else historyDao.getByIds(ids).map { it.toDomain() }

    override suspend fun replaceSummary(summary: HistorySummary) {
        historyDao.insert(summary.toEntity())
    }

    override suspend fun setPinned(id: String, pinned: Boolean) {
        historyDao.setPinned(id, pinned)
    }

    override suspend fun deleteSummary(id: String) {
        historyDao.deleteById(id)
    }

    private suspend fun findExistingRecords(summary: HistorySummary): List<HistoryEntity> {
        val link = summary.sourceLink?.trim()
        if (!link.isNullOrBlank()) {
            val records = historyDao.getAllBySourceLink(link)
            if (records.isNotEmpty()) return records
        }
        val text = summary.sourceText?.trim()
        if (!text.isNullOrBlank()) {
            val records = historyDao.getAllBySourceText(text)
            if (records.isNotEmpty()) return records
        }
        return emptyList()
    }

    private suspend fun ensureDeduplicated() {
        if (isDeduplicated) return
        deduplicationMutex.withLock {
            if (isDeduplicated) return
            val result = runCatching { writeMutex.withLock {
                val all = historyDao.getAll()
                val grouped = all.groupBy { entity ->
                    when {
                        !entity.sourceLink.isNullOrBlank() -> "link:" + entity.sourceLink.trim()
                        !entity.sourceText.isNullOrBlank() -> "text:" + entity.sourceText.trim()
                        else -> "id:" + entity.id
                    }
                }
                for ((_, group) in grouped) {
                    if (group.size > 1) {
                        val latest = group.maxByOrNull { it.createdOn } ?: group.first()
                        val mergedLengths = mutableMapOf<SummaryLength, HistoryLengthResult>()
                        for (item in group) {
                            mergedLengths.putAll(item.toDomain().allLengthResults)
                        }
                        val updated = latest.toDomain().copy(
                            lengthResults = mergedLengths,
                            alternatives = group.flatMap { it.toDomain().alternatives }.distinct(),
                            pinned = group.any { it.pinned },
                        )
                        historyDao.insert(updated.toEntity())
                        val toDelete = group.filter { it.id != latest.id }.map { it.id }
                        if (toDelete.isNotEmpty()) {
                            historyDao.deleteByIds(toDelete)
                        }
                    }
                }

                // Rows from before profile filtering have no variant keys yet: fill them in.
                historyDao.getAll().filter { it.variantKeys.isEmpty() }.forEach {
                    historyDao.insert(it.toDomain().toEntity())
                }
            } }
            // A failed pass is retried the next time the history is opened.
            if (result.isSuccess) isDeduplicated = true
        }
    }
}

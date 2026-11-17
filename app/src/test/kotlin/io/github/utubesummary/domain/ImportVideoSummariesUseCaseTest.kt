package io.github.utubesummary.domain

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import io.github.utubesummary.domain.importer.InvalidBackupException
import io.github.utubesummary.domain.provider.TextFileReader
import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.domain.usecase.ImportVideoSummariesUseCaseImpl
import io.github.utubesummary.model.HistoryFilter
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.ImportResult
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryType
import io.github.utubesummary.model.VideoSubtype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportVideoSummariesUseCaseTest {

    private class FakeHistory(existingLinks: List<String>) : HistoryRepository {
        val items = existingLinks.map {
            HistorySummary(title = "old", type = SummaryType.VIDEO, sourceLink = it)
        }.toMutableList()

        override fun getSummaries(query: String, filter: HistoryFilter): Flow<PagingData<HistorySummary>> =
            emptyFlow()

        override suspend fun setPinned(id: String, pinned: Boolean) {}

        override suspend fun replaceSummary(summary: HistorySummary) {
            items += summary
        }

        override suspend fun addSummary(summary: HistorySummary) {
            items += summary
        }

        override suspend fun addSummaries(summaries: List<HistorySummary>) {
            items += summaries
        }

        override suspend fun getAllSourceLinks(): List<String> = items.mapNotNull { it.sourceLink }
        override suspend fun deleteSummary(id: String) {}
    }

    private fun entry(id: String, title: String = "Video $id") =
        """{"video_id":"$id","video_url":"https://youtube.com/watch?v=$id&si=x","video_title":"$title","video_uploader":"U","video_summary":"[{\"Chapter\":\"text $id\"}]","update_time":1700000000}"""

    private fun useCase(history: FakeHistory, json: String) =
        ImportVideoSummariesUseCaseImpl(history, object : TextFileReader {
            override suspend fun readText(uriString: String, maxBytes: Long) = json
        })

    @Test
    fun `imports new videos and skips existing ones whatever the link form`() = runTest {
        val history = FakeHistory(
            listOf(
                "https://youtu.be/AAAAAAAAAAA",                       // other link form, same video
                "https://www.youtube.com/watch?v=BBBBBBBBBBB&t=30s",  // extra parameters
                "https://example.com/article",
            )
        )
        val json = "[" + listOf(entry("AAAAAAAAAAA"), entry("BBBBBBBBBBB"), entry("CCCCCCCCCCC")).joinToString(",") + "]"

        val result = useCase(history, json)("content://file").getOrThrow()

        assertEquals(ImportResult(imported = 1, skippedExisting = 2, invalid = 0), result)
        val added = history.items.last()
        assertEquals("Video CCCCCCCCCCC", added.title)
        assertEquals(SummaryType.VIDEO, added.type)
        assertEquals(VideoSubtype.YOUTUBE, added.subtype)
        assertEquals(SummaryLength.NONE, added.length)
        assertEquals("https://www.youtube.com/watch?v=CCCCCCCCCCC", added.sourceLink)
        assertEquals("text CCCCCCCCCCC", added.summary)
        assertEquals(1700000000_000L, added.createdOn)
        assertEquals(4, history.items.size)
    }

    @Test
    fun `a video repeated in the file is imported once and importing twice adds nothing`() = runTest {
        val history = FakeHistory(emptyList())
        val json = "[" + listOf(entry("AAAAAAAAAAA"), entry("AAAAAAAAAAA"), entry("BBBBBBBBBBB")).joinToString(",") + "]"

        assertEquals(ImportResult(2, 1, 0), useCase(history, json)("x").getOrThrow())
        assertEquals(ImportResult(0, 3, 0), useCase(history, json)("x").getOrThrow())
        assertEquals(2, history.items.size)
    }

    @Test
    fun `an invalid file fails without touching the history`() = runTest {
        val history = FakeHistory(emptyList())

        val result = useCase(history, "{ \"not\": \"an array\" }")("x")

        assertTrue(result.exceptionOrNull() is InvalidBackupException)
        assertTrue(history.items.isEmpty())
    }
}

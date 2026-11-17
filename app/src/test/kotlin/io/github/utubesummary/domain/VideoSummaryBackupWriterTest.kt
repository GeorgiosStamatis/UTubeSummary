package io.github.utubesummary.domain

import io.github.utubesummary.domain.importer.VideoSummaryBackupParser
import io.github.utubesummary.domain.importer.buildVideoSummaryBackup
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VideoSummaryBackupWriterTest {

    private fun video(id: String, title: String, text: String, createdOn: Long = 1_700_000_000_000L) = HistorySummary(
        title = title,
        author = "Author $id",
        createdOn = createdOn,
        type = SummaryType.VIDEO,
        length = SummaryLength.MEDIUM,
        sourceLink = "https://www.youtube.com/watch?v=$id",
        lengthResults = mapOf(SummaryLength.MEDIUM to HistoryLengthResult(SummaryLength.MEDIUM, text)),
    )

    @Test
    fun `an export can be imported again`() {
        val backup = buildVideoSummaryBackup(
            listOf(
                video("dQw4w9WgXcQ", "First \"quoted\" title", "Line one.\nLine two."),
                video("aaaaaaaaaaa", "Second", "Only text", createdOn = 1_600_000_000_000L),
            )
        )

        val parsed = VideoSummaryBackupParser.parse(backup.json)

        assertEquals(2, backup.count)
        assertEquals(0, parsed.invalid)
        assertEquals(listOf("dQw4w9WgXcQ", "aaaaaaaaaaa"), parsed.videos.map { it.videoId })
        assertEquals("First \"quoted\" title", parsed.videos[0].title)
        assertEquals("Author dQw4w9WgXcQ", parsed.videos[0].author)
        assertEquals("Line one.\nLine two.", parsed.videos[0].summary)
        assertEquals(1_600_000_000_000L, parsed.videos[1].createdOn)
    }

    @Test
    fun `only youtube videos with a summary are exported`() {
        val article = HistorySummary(
            title = "Article",
            type = SummaryType.ARTICLE,
            sourceLink = "https://example.com/post",
            lengthResults = mapOf(SummaryLength.MEDIUM to HistoryLengthResult(SummaryLength.MEDIUM, "text")),
        )
        val emptyVideo = video("bbbbbbbbbbb", "Empty", "   ")

        val backup = buildVideoSummaryBackup(listOf(article, emptyVideo, video("ccccccccccc", "Kept", "ok")))

        assertEquals(1, backup.count)
        assertEquals(listOf("ccccccccccc"), VideoSummaryBackupParser.parse(backup.json).videos.map { it.videoId })
    }

    @Test
    fun `nothing to export gives an empty backup`() {
        assertEquals(0, buildVideoSummaryBackup(emptyList()).count)
    }
}

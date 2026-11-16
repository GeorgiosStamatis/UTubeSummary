package io.github.utubesummary.domain

import io.github.utubesummary.domain.importer.InvalidBackupException
import io.github.utubesummary.domain.importer.VideoSummaryBackupParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VideoSummaryBackupParserTest {

    // Same shape as a real backup: video_summary is a JSON string holding [{"Chapter": "..."}]
    private val sample = """
    [
      {
        "video_id": "IZhGvZ4UlPs",
        "video_url": "https://youtube.com/watch?v=IZhGvZ4UlPs&si=3GIuKtub4W0KqfL1",
        "video_title": "How Much Did Claude Cook",
        "video_uploader": "AI LABS",
        "video_duration": 556,
        "video_views": null,
        "video_tags": "[\"claude\"]",
        "video_summary": "[{\"Chapter\":\"• first point\\n• second point\"}]",
        "update_time": 1764096629,
        "summary_language": "en"
      },
      {
        "video_id": "dQw4w9WgXcQ",
        "video_url": "https://youtu.be/dQw4w9WgXcQ",
        "video_title": "Two chapters",
        "video_uploader": null,
        "video_summary": "[{\"Chapter\":\"one\"},{\"Chapter\":\"two\"}]",
        "update_time": 1764096700
      },
      { "video_id": "", "video_url": "https://example.com", "video_title": "No id", "video_summary": "[{\"Chapter\":\"x\"}]", "update_time": 1 },
      { "video_id": "aaaaaaaaaaa", "video_title": "", "video_summary": "[{\"Chapter\":\"x\"}]", "update_time": 1 },
      { "video_id": "bbbbbbbbbbb", "video_title": "Empty summary", "video_summary": "[]", "update_time": 1 }
    ]
    """.trimIndent()

    @Test
    fun `parses the real format`() {
        val parsed = VideoSummaryBackupParser.parse(sample, fallbackTime = 5)

        assertEquals(2, parsed.videos.size)
        assertEquals(3, parsed.invalid)

        val first = parsed.videos[0]
        assertEquals("IZhGvZ4UlPs", first.videoId)
        assertEquals("How Much Did Claude Cook", first.title)
        assertEquals("AI LABS", first.author)
        assertEquals(1764096629_000L, first.createdOn)
        assertEquals("• first point\n• second point", first.summary)
        assertEquals("https://www.youtube.com/watch?v=IZhGvZ4UlPs", first.sourceLink)

        val second = parsed.videos[1]
        assertEquals("", second.author)
        assertEquals("one\n\ntwo", second.summary)
    }

    @Test
    fun `an id can come from the url`() {
        val text = """[{"video_url":"https://youtu.be/dQw4w9WgXcQ","video_title":"T","video_summary":"plain text summary","update_time":1}]"""
        val video = VideoSummaryBackupParser.parse(text).videos.single()
        assertEquals("dQw4w9WgXcQ", video.videoId)
        assertEquals("plain text summary", video.summary)
    }

    @Test
    fun `empty array is a valid empty backup`() {
        val parsed = VideoSummaryBackupParser.parse("[]")
        assertTrue(parsed.videos.isEmpty())
        assertEquals(0, parsed.invalid)
    }

    @Test
    fun `files that are not a backup are rejected`() {
        assertThrows(InvalidBackupException::class.java) { VideoSummaryBackupParser.parse("not json") }
        assertThrows(InvalidBackupException::class.java) { VideoSummaryBackupParser.parse("{\"a\": 1}") }
    }
}

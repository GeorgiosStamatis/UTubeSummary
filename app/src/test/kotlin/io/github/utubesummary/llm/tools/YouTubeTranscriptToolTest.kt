package io.github.utubesummary.llm.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class YouTubeTranscriptToolTest {

    private val id = "dQw4w9WgXcQ"

    @Test
    fun `video id is read from every common link form`() {
        listOf(
            "https://www.youtube.com/watch?v=$id",
            "https://www.youtube.com/watch?v=$id&t=42s",
            "https://m.youtube.com/watch?feature=share&v=$id",
            "https://youtu.be/$id",
            "https://youtu.be/$id?si=tracking",
            "https://www.youtube.com/shorts/$id",
            "https://www.youtube.com/embed/$id",
            "https://www.youtube.com/live/$id",
            "https://www.youtube.com/$id",
            "youtube.com/watch?v=$id",
        ).forEach { link ->
            assertEquals(id, YouTubeTranscriptTool.extractVideoId(link), link)
        }
    }

    @Test
    fun `non youtube hosts and malformed ids give null`() {
        assertNull(YouTubeTranscriptTool.extractVideoId("https://example.com/watch?v=$id"))
        assertNull(YouTubeTranscriptTool.extractVideoId("https://www.youtube.com/watch?v=tooShort"))
        assertNull(YouTubeTranscriptTool.extractVideoId("https://www.youtube.com/"))
    }
}

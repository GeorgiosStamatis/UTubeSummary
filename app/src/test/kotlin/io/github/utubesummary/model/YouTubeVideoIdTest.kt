package io.github.utubesummary.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class YouTubeVideoIdTest {
    private val id = "IZhGvZ4UlPs"

    @Test
    fun `all youtube link forms give the same id`() {
        listOf(
            "https://youtube.com/watch?v=$id&si=3GIuKtub4W0KqfL1",
            "https://www.youtube.com/watch?v=$id",
            "https://m.youtube.com/watch?feature=share&v=$id",
            "https://youtu.be/$id",
            "https://youtu.be/$id?si=abc",
            "https://www.youtube.com/shorts/$id",
            "https://www.youtube.com/embed/$id",
            "https://www.youtube.com/live/$id?feature=share",
            "youtube.com/watch?v=$id",
        ).forEach { assertEquals(id, youtubeVideoId(it), it) }
    }

    @Test
    fun `other links give no id`() {
        listOf(
            null, "", "   ", "https://example.com/watch?v=$id",
            "https://www.youtube.com/@somechannel", "https://www.youtube.com/watch?v=short",
            "https://www.bilibili.com/video/BV1xx", "not a url at all",
        ).forEach { assertNull(youtubeVideoId(it), it.toString()) }
    }
}

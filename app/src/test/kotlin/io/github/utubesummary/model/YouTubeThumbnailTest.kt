package io.github.utubesummary.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class YouTubeThumbnailTest {
    private val expected = "https://i.ytimg.com/vi/IZhGvZ4UlPs/mqdefault.jpg"

    @Test
    fun `every youtube link form gives the same thumbnail, including imported links`() {
        listOf(
            "https://www.youtube.com/watch?v=IZhGvZ4UlPs", // canonical link of imported items
            "https://youtube.com/watch?v=IZhGvZ4UlPs&si=3GIuKtub4W0KqfL1",
            "https://youtu.be/IZhGvZ4UlPs",
            "https://www.youtube.com/shorts/IZhGvZ4UlPs",
            "https://www.youtube.com/embed/IZhGvZ4UlPs",
        ).forEach { assertEquals(expected, youtubeThumbnailUrl(it), it) }
    }

    @Test
    fun `other links have no thumbnail`() {
        listOf(null, "", "https://www.bilibili.com/video/BV1xx", "https://example.com/article")
            .forEach { assertNull(youtubeThumbnailUrl(it), it.toString()) }
    }

    @Test
    fun `preferences default to showing thumbnails`() {
        assertEquals(true, UserPreferences().showThumbnails)
    }
}

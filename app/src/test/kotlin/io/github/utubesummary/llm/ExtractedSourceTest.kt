package io.github.utubesummary.llm

import io.github.utubesummary.model.ExtractedContent
import io.github.utubesummary.model.SummarySource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExtractedSourceTest {

    private val content = ExtractedContent(title = "T", author = "A", content = "text", languageCode = "en")

    @Test
    fun `a youtube video keeps its link and flag`() {
        val payload = payloadForExtracted(SummarySource.Extracted(SummarySource.Video("https://youtu.be/dQw4w9WgXcQ"), content))

        assertEquals(content, payload.content)
        assertEquals("https://youtu.be/dQw4w9WgXcQ", payload.sourceLink)
        assertTrue(payload.isYoutube)
        assertFalse(payload.isBiliBili)
    }

    @Test
    fun `a bilibili video keeps its link and flag`() {
        val payload = payloadForExtracted(SummarySource.Extracted(SummarySource.Video("https://www.bilibili.com/video/BV1xx411c7mD"), content))

        assertEquals("https://www.bilibili.com/video/BV1xx411c7mD", payload.sourceLink)
        assertTrue(payload.isBiliBili)
        assertFalse(payload.isYoutube)
    }

    @Test
    fun `an article and a document keep their link`() {
        val article = payloadForExtracted(SummarySource.Extracted(SummarySource.Article("https://example.com/post"), content))
        val document = payloadForExtracted(SummarySource.Extracted(SummarySource.Document("a.pdf", "content://docs/a"), content))

        assertEquals("https://example.com/post", article.sourceLink)
        assertEquals("content://docs/a", document.sourceLink)
        assertFalse(article.isYoutube || article.isBiliBili || document.isYoutube || document.isBiliBili)
    }

    @Test
    fun `other sources have no link`() {
        val payload = payloadForExtracted(SummarySource.Extracted(SummarySource.Text("words"), content))

        assertNull(payload.sourceLink)
        assertEquals(content, payload.content)
    }
}

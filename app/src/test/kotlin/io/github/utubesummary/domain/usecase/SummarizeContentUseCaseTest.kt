package io.github.utubesummary.domain.usecase

import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummarySource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SummarizeContentUseCaseTest {

    @Test
    fun `a document uri becomes a document source named after the file`() {
        val source = classifySummarySource("content://docs/42", autoExtractUrl = true, documentName = "report.pdf")

        assertEquals(SummarySource.Document("report.pdf", "content://docs/42"), source)
    }

    @Test
    fun `a document without a known name gets a generic one`() {
        val source = classifySummarySource("file:///x.docx", autoExtractUrl = true, documentName = null)

        assertEquals(SummarySource.Document("Document", "file:///x.docx"), source)
    }

    @Test
    fun `video links and article links are told apart`() {
        assertTrue(
            classifySummarySource("https://youtu.be/dQw4w9WgXcQ", true, null) is SummarySource.Video
        )
        assertTrue(
            classifySummarySource("https://www.bilibili.com/video/BV1xx411c7mD", true, null) is SummarySource.Video
        )
        assertTrue(
            classifySummarySource("https://example.com/post", true, null) is SummarySource.Article
        )
    }

    @Test
    fun `the first link in shared text is used when link extraction is on`() {
        val shared = "Watch this https://youtu.be/dQw4w9WgXcQ it is great"

        assertEquals(
            SummarySource.Video("https://youtu.be/dQw4w9WgXcQ"),
            classifySummarySource(shared, autoExtractUrl = true, documentName = null)
        )
        assertEquals(
            SummarySource.Text(shared),
            classifySummarySource(shared, autoExtractUrl = false, documentName = null)
        )
    }

    @Test
    fun `plain text and blank input`() {
        assertEquals(SummarySource.Text("hello world"), classifySummarySource("hello world", true, null))
        assertEquals(SummarySource.None, classifySummarySource("   ", true, null))
    }

    @Test
    fun `length is none without length options, else override, saved or medium`() {
        assertEquals(SummaryLength.NONE, resolveTargetLength(false, SummaryLength.LONG, "SHORT"))
        assertEquals(SummaryLength.LONG, resolveTargetLength(true, SummaryLength.LONG, "SHORT"))
        assertEquals(SummaryLength.SHORT, resolveTargetLength(true, null, "SHORT"))
        assertEquals(SummaryLength.MEDIUM, resolveTargetLength(true, null, "NONE"))
        assertEquals(SummaryLength.MEDIUM, resolveTargetLength(true, null, "garbage"))
    }
}

package io.github.utubesummary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IncomingShareTest {

    @Test
    fun `documents and images are file shares`() {
        assertTrue(isFileShare("application/pdf"))
        assertTrue(isFileShare("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertTrue(isFileShare("image/png"))
        assertTrue(isFileShare("image/jpeg"))
    }

    @Test
    fun `text and unknown types are not file shares`() {
        assertFalse(isFileShare("text/plain"))
        assertFalse(isFileShare("text/markdown"))
        assertFalse(isFileShare(""))
        assertFalse(isFileShare(null))
    }

    @Test
    fun `a text share gives the shared text`() {
        val content = sharedContentOf(
            mimeType = "text/plain",
            text = { "https://youtu.be/dQw4w9WgXcQ" },
            streamUri = { error("the file link must not be read for a text share") },
        )

        assertEquals("https://youtu.be/dQw4w9WgXcQ", content)
    }

    @Test
    fun `a file share gives the file link`() {
        val content = sharedContentOf(
            mimeType = "application/pdf",
            text = { error("the text must not be read for a file share") },
            streamUri = { "content://docs/report.pdf" },
        )

        assertEquals("content://docs/report.pdf", content)
    }

    @Test
    fun `a file share without a file link gives nothing, even if it carries text`() {
        assertNull(sharedContentOf("image/png", text = { "some text" }, streamUri = { null }))
    }

    @Test
    fun `a share without a type is read as text`() {
        assertEquals("hello", sharedContentOf(null, text = { "hello" }, streamUri = { "content://x" }))
        assertNull(sharedContentOf(null, text = { null }, streamUri = { "content://x" }))
    }

    @Test
    fun `a missing summary after a handoff starts summarizing again`() {
        assertTrue(shouldAutoTrigger(hasSummary = false, autoTrigger = false, handoffId = "id"))
    }

    @Test
    fun `an explicit auto start works without a handoff`() {
        assertTrue(shouldAutoTrigger(hasSummary = false, autoTrigger = true, handoffId = null))
    }

    @Test
    fun `a handed over summary is shown and not summarized again`() {
        assertFalse(shouldAutoTrigger(hasSummary = true, autoTrigger = true, handoffId = "id"))
        assertFalse(shouldAutoTrigger(hasSummary = true, autoTrigger = false, handoffId = null))
    }

    @Test
    fun `a plain share does not start by itself`() {
        assertFalse(shouldAutoTrigger(hasSummary = false, autoTrigger = false, handoffId = null))
    }
}

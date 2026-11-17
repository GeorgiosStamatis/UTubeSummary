package io.github.utubesummary.llm.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FileKindTest {

    @Test
    fun `documents and images are recognised by name`() {
        assertEquals(FileKind.PDF, fileKindOf("Report.PDF", null))
        assertEquals(FileKind.DOCX, fileKindOf("notes.docx", "application/octet-stream"))
        assertEquals(FileKind.IMAGE, fileKindOf("photo.jpeg", null))
        assertEquals(FileKind.IMAGE, fileKindOf("scan.png", null))
    }

    @Test
    fun `images are also recognised by mime type`() {
        assertEquals(FileKind.IMAGE, fileKindOf("IMG_0001", "image/webp"))
    }

    @Test
    fun `plain text and markdown files are text`() {
        listOf("a.txt", "README.md", "b.markdown", "c.TEXT").forEach {
            assertEquals(FileKind.TEXT, fileKindOf(it, null), it)
        }
        listOf("text/plain", "text/markdown", "text/x-markdown").forEach {
            assertEquals(FileKind.TEXT, fileKindOf("untitled", it), it)
        }
    }

    @Test
    fun `other files are not supported`() {
        assertNull(fileKindOf("archive.zip", "application/zip"))
        assertNull(fileKindOf("data.csv", "text/csv"))
        assertNull(fileKindOf("noextension", null))
    }
}

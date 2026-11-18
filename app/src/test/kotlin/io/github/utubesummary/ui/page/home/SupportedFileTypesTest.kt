package io.github.utubesummary.ui.page.home

import io.github.utubesummary.llm.tools.FileKind
import io.github.utubesummary.llm.tools.fileKindOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/** What the file picker offers must be something the extractor can read. */
class SupportedFileTypesTest {

    private val sampleName = mapOf(
        MimeTypes.PDF to "doc.pdf",
        MimeTypes.DOCX to "doc.docx",
        MimeTypes.PNG to "pic.png",
        MimeTypes.JPEG to "pic.jpeg",
        MimeTypes.JPG to "pic.jpg",
        MimeTypes.WEBP to "pic.webp",
        MimeTypes.TEXT to "notes.txt",
        MimeTypes.MARKDOWN to "notes.md",
        MimeTypes.MARKDOWN_LEGACY to "notes.markdown",
    )

    @Test
    fun `every picker type has a sample and the list has no duplicates`() {
        assertEquals(MimeTypes.allSupported.toSet(), sampleName.keys)
        assertEquals(MimeTypes.allSupported.size, MimeTypes.allSupported.toSet().size)
    }

    @Test
    fun `every type offered by the picker is read by some extractor`() {
        MimeTypes.allSupported.forEach { mime ->
            assertNotNull(fileKindOf(sampleName.getValue(mime), mime), "$mime is offered but not readable")
        }
    }

    @Test
    fun `text and markdown files are read as text by name or by type`() {
        assertEquals(FileKind.TEXT, fileKindOf("notes.md", null))
        assertEquals(FileKind.TEXT, fileKindOf("README", MimeTypes.MARKDOWN))
        assertEquals(FileKind.TEXT, fileKindOf("README", MimeTypes.MARKDOWN_LEGACY))
        assertEquals(FileKind.TEXT, fileKindOf("notes", MimeTypes.TEXT))
    }
}

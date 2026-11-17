package io.github.utubesummary.llm.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DocxTextBuilderTest {

    /** Feeds a tiny document: each string is an element name ("w:p"), or text if it has no prefix. */
    private fun build(vararg events: String): String {
        val builder = DocxTextBuilder()
        events.forEach { event ->
            when {
                event.startsWith("<") -> builder.startElement(event.removePrefix("<"))
                event.startsWith("/") -> builder.endElement(event.removePrefix("/"))
                else -> builder.text(event)
            }
        }
        return builder.build()
    }

    @Test
    fun `paragraphs become lines`() {
        assertEquals("One\nTwo", build("<w:p", "One", "/w:p", "<w:p", "Two", "/w:p"))
    }

    @Test
    fun `tabs and line breaks are kept`() {
        assertEquals("a\tb\nc", build("<w:p", "a", "<w:tab", "/w:tab", "b", "<w:br", "/w:br", "c", "/w:p"))
    }

    @Test
    fun `a table becomes one line per row with tabs between the cells`() {
        val text = build(
            "<w:tbl",
            "<w:tr", "<w:tc", "<w:p", "A1", "/w:p", "/w:tc", "<w:tc", "<w:p", "B1", "/w:p", "/w:tc", "/w:tr",
            "<w:tr", "<w:tc", "<w:p", "A2", "/w:p", "/w:tc", "<w:tc", "<w:p", "B2", "/w:p", "/w:tc", "/w:tr",
            "/w:tbl",
        )

        assertEquals("A1\tB1\nA2\tB2", text)
    }

    @Test
    fun `several paragraphs in one cell stay in that cell`() {
        val text = build(
            "<w:tr",
            "<w:tc", "<w:p", "first", "/w:p", "<w:p", "second", "/w:p", "/w:tc",
            "<w:tc", "<w:p", "x", "/w:p", "/w:tc",
            "/w:tr",
        )

        assertEquals("first second\tx", text)
    }

    @Test
    fun `text after a table is on its own line`() {
        val text = build(
            "<w:tr", "<w:tc", "<w:p", "cell", "/w:p", "/w:tc", "/w:tr",
            "<w:p", "after", "/w:p",
        )

        assertEquals("cell\nafter", text)
    }

    @Test
    fun `a nested table keeps its cells inside the outer cell`() {
        val text = build(
            "<w:tr",
            "<w:tc",
            "<w:tr", "<w:tc", "<w:p", "inner", "/w:p", "/w:tc", "/w:tr",
            "/w:tc",
            "<w:tc", "<w:p", "outer", "/w:p", "/w:tc",
            "/w:tr",
        )

        // The inner row ends with a line break, the outer row follows on the next line.
        assertEquals("inner\n\touter", text)
    }
}

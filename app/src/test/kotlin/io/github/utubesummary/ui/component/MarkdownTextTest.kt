package io.github.utubesummary.ui.component

import androidx.compose.ui.text.font.FontWeight
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarkdownTextTest {

    @Test
    fun `plain text is unchanged`() {
        val text = "First line.\nSecond line with a * b and 5 * 3."

        assertEquals(text, markdownToAnnotatedString(text).text)
    }

    @Test
    fun `bold markers are removed and styled`() {
        val result = markdownToAnnotatedString("This is **important** news")

        assertEquals("This is important news", result.text)
        val span = result.spanStyles.single()
        assertEquals(FontWeight.Bold, span.item.fontWeight)
        assertEquals("important", result.text.substring(span.start, span.end))
    }

    @Test
    fun `headings lose their hashes and are bold`() {
        val result = markdownToAnnotatedString("### Thesis\nBody")

        assertEquals("Thesis\nBody", result.text)
        assertEquals(FontWeight.Bold, result.spanStyles.first().item.fontWeight)
    }

    @Test
    fun `bullets use a bullet character`() {
        assertEquals("• one\n• two", markdownToAnnotatedString("- one\n* two").text)
    }

    @Test
    fun `table separator rows are dropped and cells are joined`() {
        val table = "| Criterion | Score |\n| --- | ---: |\n| Depth | 3/4 |"

        val text = markdownToAnnotatedString(table).text

        assertEquals("Criterion  ·  Score\nDepth  ·  3/4", text)
        assertTrue(!text.contains("---"))
    }
}

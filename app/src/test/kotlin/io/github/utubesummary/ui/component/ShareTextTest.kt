package io.github.utubesummary.ui.component

import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShareTextTest {

    private fun output(
        title: String = "A title",
        author: String = "",
        summary: String = "The body.",
        link: String? = null,
        overview: String? = null,
        keyPoints: List<String> = emptyList(),
        tags: List<String> = emptyList(),
    ) = SummaryOutput(
        title = title,
        author = author,
        summary = summary,
        sourceLink = link,
        isYoutubeLink = false,
        isBiliBiliLink = false,
        length = SummaryLength.MEDIUM,
        overview = overview,
        keyPoints = keyPoints,
        tags = tags,
    )

    @Test
    fun `a full summary becomes markdown with title, source, points and tags`() {
        val text = summaryShareText(
            output(
                author = "Ada",
                link = "https://example.com/v",
                overview = "Short take.",
                keyPoints = listOf("One", "Two"),
                tags = listOf("AI", "local models"),
            )
        )

        assertEquals(
            """
            # A title

            By Ada · Source: https://example.com/v

            > Short take.

            - One
            - Two

            The body.

            #AI #local_models
            """.trimIndent(),
            text
        )
    }

    @Test
    fun `empty parts are left out`() {
        assertEquals("# A title\n\nThe body.", summaryShareText(output()))
        assertEquals("The body.", summaryShareText(output(title = "")))
    }

    @Test
    fun `the file name is the title without characters a file cannot have`() {
        assertEquals("What is AI Part 1.md", suggestedMarkdownFileName("What is AI? / Part 1:"))
        assertEquals("a b c.md", suggestedMarkdownFileName("  a   b\tc  "))
    }

    @Test
    fun `an empty or unusable title gives a default name and long titles are cut`() {
        assertEquals("summary.md", suggestedMarkdownFileName(""))
        assertEquals("summary.md", suggestedMarkdownFileName("???"))
        assertEquals(80 + ".md".length, suggestedMarkdownFileName("x".repeat(200)).length)
    }
}

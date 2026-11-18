package io.github.utubesummary.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TitleAndAuthorTest {

    private fun resolve(
        structuredTitle: String? = null,
        structuredAuthor: String? = null,
        extractedTitle: String = "Unknown",
        extractedAuthor: String = "Unknown",
        sourceLink: String? = null,
    ) = resolveTitleAndAuthor(structuredTitle, structuredAuthor, extractedTitle, extractedAuthor, sourceLink)

    @Test
    fun `the model's title wins when it is usable`() {
        assertEquals("Great Title", resolve(structuredTitle = "Great Title", extractedTitle = "Page title").title)
    }

    @Test
    fun `a placeholder title from the model falls back to the extracted title`() {
        assertEquals("Real Page Title", resolve(structuredTitle = "Untitled", extractedTitle = "Real Page Title").title)
        assertEquals("Real Page Title", resolve(structuredTitle = "  ", extractedTitle = "Real Page Title").title)
    }

    @Test
    fun `text input never becomes the title`() {
        assertEquals("", resolve(extractedTitle = "Text Input").title)
        assertEquals("", resolve(extractedTitle = "text input").title)
    }

    @Test
    fun `a title is guessed from the link when nothing else is usable`() {
        assertEquals(
            "My Great Post",
            resolve(extractedTitle = "Text Input", sourceLink = "https://example.com/blog/my-great-post").title
        )
        assertEquals(
            "My Great Post",
            resolve(extractedTitle = "Just a moment...", sourceLink = "https://example.com/my-great-post").title
        )
    }

    @Test
    fun `an anti-bot title is only a last resort without a link`() {
        assertEquals("Medium", resolve(extractedTitle = "Medium").title)
    }

    @Test
    fun `the model's author wins and is trimmed`() {
        assertEquals("Jane Doe", resolve(structuredAuthor = "  Jane Doe ", extractedAuthor = "Someone").author)
    }

    @Test
    fun `unknown authors fall back to the extracted author`() {
        assertEquals("Jane Doe", resolve(structuredAuthor = "Unknown", extractedAuthor = "Jane Doe").author)
        assertEquals("Jane Doe", resolve(structuredAuthor = "n/a", extractedAuthor = "Jane Doe").author)
    }

    @Test
    fun `a long paragraph is not an author`() {
        val paragraph = "This is clearly a sentence that the model returned instead of a name of a person."
        assertEquals("", resolve(structuredAuthor = paragraph).author)
    }

    @Test
    fun `the author is guessed from the link and is empty when unknown`() {
        assertEquals("jane", resolve(sourceLink = "https://medium.com/@jane/some-post").author)
        assertEquals("", resolve().author)
        assertEquals("", resolve(extractedAuthor = "Article").author)
    }
}

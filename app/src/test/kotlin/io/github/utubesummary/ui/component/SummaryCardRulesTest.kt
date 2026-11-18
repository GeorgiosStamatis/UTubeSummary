package io.github.utubesummary.ui.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SummaryCardRulesTest {

    @Test
    fun `a normal name is a known author`() {
        assertTrue(isKnownAuthor("Jane Doe"))
        assertTrue(isKnownAuthor("  Jane Doe  "))
    }

    @Test
    fun `missing and placeholder authors are not shown`() {
        assertFalse(isKnownAuthor(null))
        assertFalse(isKnownAuthor(""))
        assertFalse(isKnownAuthor("   "))
        listOf("Unknown", "unknown author", "N/A", "none", "null", "Article", "Author", "Creator", "Speaker")
            .forEach { assertFalse(isKnownAuthor(it), "$it should not be shown") }
    }

    @Test
    fun `a link, a paragraph or several lines is not an author`() {
        assertFalse(isKnownAuthor("https://example.com/channel"))
        assertFalse(isKnownAuthor("HTTP://example.com"))
        assertFalse(isKnownAuthor("x".repeat(61)))
        assertFalse(isKnownAuthor("First line\nsecond line"))
        assertFalse(isKnownAuthor("First line\rsecond line"))
        assertTrue(isKnownAuthor("x".repeat(60)))
    }

    @Test
    fun `an overview is hidden when there is none`() {
        assertFalse(shouldShowOverview(null, "A summary body that is long enough."))
        assertFalse(shouldShowOverview("   ", "A summary body that is long enough."))
        assertFalse(shouldShowOverview("An overview", ""))
        assertFalse(shouldShowOverview("An overview", "   "))
    }

    @Test
    fun `an overview repeated in the summary is hidden`() {
        assertFalse(shouldShowOverview("The same text", "the same text"))
        assertFalse(shouldShowOverview("It begins like this", "It begins like this and then goes on for a long time."))
        assertFalse(shouldShowOverview("goes on", "It begins and then goes on for a long time, really."))
    }

    @Test
    fun `an overview needs a clearly longer summary`() {
        // 10 characters of overview: the summary must be at least 12.5 characters long.
        assertFalse(shouldShowOverview("abcdefghij", "klmnopqrstuv"))
        assertTrue(shouldShowOverview("abcdefghij", "klmnopqrstuvw"))
    }

    @Test
    fun `a different overview with a long summary is shown`() {
        assertTrue(shouldShowOverview("A short teaser.", "The full summary explains everything in much more detail."))
    }

    @Test
    fun `playback speed cycles through all rates and wraps around`() {
        var speed = PlaybackSpeed.HALF
        val seen = mutableListOf(speed)
        repeat(PlaybackSpeed.entries.size - 1) {
            speed = speed.next()
            seen += speed
        }

        assertEquals(PlaybackSpeed.entries.toList(), seen)
        assertEquals(PlaybackSpeed.HALF, speed.next())
        assertEquals(1.0f, PlaybackSpeed.NORMAL.rate)
    }
}

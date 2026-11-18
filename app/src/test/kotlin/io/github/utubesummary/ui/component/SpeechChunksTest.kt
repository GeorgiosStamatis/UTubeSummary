package io.github.utubesummary.ui.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SpeechChunksTest {

    @Test
    fun `short text is one chunk`() {
        assertEquals(listOf(SpeechChunk(0, "Hello world")), speechChunks("Hello world", 0, 100))
    }

    @Test
    fun `empty text and an offset at the end give nothing`() {
        assertEquals(emptyList<SpeechChunk>(), speechChunks("", 0, 10))
        assertEquals(emptyList<SpeechChunk>(), speechChunks("abc", 3, 10))
        assertEquals(emptyList<SpeechChunk>(), speechChunks("abc", 99, 10))
    }

    @Test
    fun `long text is split after spaces and nothing is lost`() {
        val text = "one two three four five six seven eight nine ten"

        val chunks = speechChunks(text, 0, 12)

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.text.length <= 12 })
        assertTrue(chunks.all { it.text.endsWith(" ") || it === chunks.last() })
        assertEquals(text, chunks.joinToString("") { it.text })
    }

    @Test
    fun `chunks start where the previous one ended`() {
        val text = "alpha beta gamma delta epsilon zeta"

        val chunks = speechChunks(text, 0, 10)

        var expected = 0
        for (chunk in chunks) {
            assertEquals(expected, chunk.start)
            expected += chunk.text.length
        }
        assertEquals(text.length, expected)
    }

    @Test
    fun `a word longer than the limit is cut at the limit`() {
        val chunks = speechChunks("abcdefghijklmnopqrstuvwxyz", 0, 10)

        assertEquals(listOf("abcdefghij", "klmnopqrst", "uvwxyz"), chunks.map { it.text })
    }

    @Test
    fun `a space exactly at the limit does not make the chunk too long`() {
        val chunks = speechChunks("0123456789 abcdefghij", 0, 10)

        assertTrue(chunks.all { it.text.length <= 10 })
        assertEquals("0123456789 abcdefghij", chunks.joinToString("") { it.text })
    }

    @Test
    fun `resuming starts at the given offset`() {
        val text = "one two three four five six"

        val chunks = speechChunks(text, 8, 100)

        assertEquals(listOf(SpeechChunk(8, "three four five six")), chunks)
    }

    @Test
    fun `the limit must be positive`() {
        assertThrows<IllegalArgumentException> { speechChunks("text", 0, 0) }
    }
}

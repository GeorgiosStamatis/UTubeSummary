package io.github.utubesummary.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextDecodingTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun `plain utf-8 is decoded`() {
        assertEquals("héllo ✓", decodeTextBytes("héllo ✓".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `a utf-8 byte order mark is removed`() {
        val withBom = bytes(0xEF, 0xBB, 0xBF) + "text".toByteArray(Charsets.UTF_8)

        assertEquals("text", decodeTextBytes(withBom))
    }

    @Test
    fun `utf-16 little and big endian files are decoded`() {
        val little = bytes(0xFF, 0xFE) + "Grüße".toByteArray(Charsets.UTF_16LE)
        val big = bytes(0xFE, 0xFF) + "Grüße".toByteArray(Charsets.UTF_16BE)

        assertEquals("Grüße", decodeTextBytes(little))
        assertEquals("Grüße", decodeTextBytes(big))
    }

    @Test
    fun `an empty file gives empty text`() {
        assertEquals("", decodeTextBytes(ByteArray(0)))
        assertEquals("", decodeTextBytes(bytes(0xEF, 0xBB, 0xBF)))
    }

    @Test
    fun `line endings are kept unless normalizing is asked for`() {
        val text = "a\r\nb\rc\nd".toByteArray()

        assertEquals("a\r\nb\rc\nd", decodeTextBytes(text))
        assertEquals("a\nb\nc\nd", decodeTextBytes(text, normalizeLineEndings = true))
    }
}

package io.github.utubesummary.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentFingerprintTest {

    @Test
    fun `the same text always gives the same fingerprint`() {
        assertEquals(contentFingerprint("hello world"), contentFingerprint("hello world"))
    }

    @Test
    fun `a fingerprint is 16 lower case hex characters`() {
        val fingerprint = contentFingerprint("hello world")

        assertEquals(16, fingerprint.length)
        assertTrue(fingerprint.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `different texts give different fingerprints`() {
        assertNotEquals(contentFingerprint("hello world"), contentFingerprint("hello world!"))
        assertNotEquals(contentFingerprint("a"), contentFingerprint(""))
    }

    @Test
    fun `the fingerprint is the start of the SHA-256 of the utf-8 text`() {
        // SHA-256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        assertEquals("ba7816bf8f01cfea", contentFingerprint("abc"))
    }

    @Test
    fun `the fingerprint does not contain the text`() {
        assertFalse(contentFingerprint("secret transcript").contains("secret"))
    }

    @Test
    fun `videos and web pages can be updated, text and documents cannot`() {
        fun entry(type: SummaryType, link: String?) = HistorySummary(title = "t", type = type, sourceLink = link)

        assertTrue(entry(SummaryType.VIDEO, "https://youtu.be/dQw4w9WgXcQ").canBeUpdated())
        assertTrue(entry(SummaryType.ARTICLE, "https://example.com/post").canBeUpdated())
        assertFalse(entry(SummaryType.TEXT, null).canBeUpdated())
        assertFalse(entry(SummaryType.DOCUMENT, "content://docs/a.pdf").canBeUpdated())
        assertFalse(entry(SummaryType.VIDEO, "  ").canBeUpdated())
        assertFalse(entry(SummaryType.ARTICLE, null).canBeUpdated())
    }
}

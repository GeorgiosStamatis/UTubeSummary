package io.github.utubesummary.data.provider

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import io.github.utubesummary.model.ExtractedContent

class InMemorySourceTextStoreTest {

    @Test
    fun `stores and returns text`() {
        val store = InMemorySourceTextStore()
        store.put("a", "text a")
        assertEquals("text a", store.get("a"))
        assertNull(store.get("missing"))
    }

    @Test
    fun `keeps only the most recently used entries`() {
        val store = InMemorySourceTextStore()
        store.put("a", "1")
        store.put("b", "2")
        store.put("c", "3")
        store.get("a") // "a" is now the most recently used
        store.put("d", "4") // evicts "b"

        assertNull(store.get("b"))
        assertEquals("1", store.get("a"))
        assertEquals("3", store.get("c"))
        assertEquals("4", store.get("d"))
    }

    @Test
    fun `stores the whole extraction and returns its text`() {
        val store = InMemorySourceTextStore()
        val content = ExtractedContent(title = "T", author = "A", content = "body", languageCode = "en")

        store.putContent("a", content)

        assertEquals(content, store.getContent("a"))
        assertEquals("body", store.get("a"))
        assertNull(store.getContent("missing"))
    }

    @Test
    fun `a text only entry has no title or author of its own`() {
        val store = InMemorySourceTextStore()

        store.put("a", "text")

        assertEquals("text", store.getContent("a")!!.content)
        assertNull(store.getContent("a")!!.languageCode)
    }
}

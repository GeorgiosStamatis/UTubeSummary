package io.github.utubesummary.data.provider

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

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
}

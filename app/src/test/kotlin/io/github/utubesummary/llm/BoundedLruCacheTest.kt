package io.github.utubesummary.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class BoundedLruCacheTest {

    @Test
    fun `the least recently used entry is dropped first`() {
        val cache = BoundedLruCache<String, String>(maxSize = 2)

        cache.getOrPut("a") { "A" }
        cache.getOrPut("b") { "B" }
        cache.getOrPut("a") { error("already cached") } // touch a: b is now the oldest
        cache.getOrPut("c") { "C" }

        assertEquals(listOf("a", "c"), cache.keys())
        assertEquals(2, cache.size())
    }

    @Test
    fun `an existing value is reused without creating a new one`() {
        val cache = BoundedLruCache<String, Any>(maxSize = 3)
        val first = cache.getOrPut("k") { Any() }

        assertSame(first, cache.getOrPut("k") { Any() })
    }

    @Test
    fun `the digest is stable and does not contain the secret`() {
        val digest = "sk-secret-key".sha256Hex()

        assertEquals(digest, "sk-secret-key".sha256Hex())
        assertNotEquals("sk-secret-key", digest)
        assertEquals(64, digest.length)
    }
}

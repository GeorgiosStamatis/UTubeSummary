package io.github.utubesummary.llm

import java.security.MessageDigest

/**
 * A small thread-safe cache that drops the least recently used entry once [maxSize] is exceeded.
 * Values are created while holding the lock, so [create] must be cheap and must not use the cache.
 */
class BoundedLruCache<K : Any, V : Any>(private val maxSize: Int) {
    private val entries = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean =
            size > maxSize
    }

    @Synchronized
    fun getOrPut(key: K, create: () -> V): V =
        entries[key] ?: create().also { entries[key] = it }

    @Synchronized
    fun keys(): List<K> = entries.keys.toList()

    @Synchronized
    fun size(): Int = entries.size
}

/** A stable stand-in for a secret, so cache keys never hold the secret itself. */
internal fun String.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256").digest(toByteArray())
        .joinToString("") { "%02x".format(it) }

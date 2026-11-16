package io.github.utubesummary.data.provider

import io.github.utubesummary.domain.provider.SourceTextStore
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_ENTRIES = 3

/** Small in-memory LRU: the last few sources, gone when the process ends. */
@Singleton
class InMemorySourceTextStore @Inject constructor() : SourceTextStore {

    private val entries = object : LinkedHashMap<String, String>(MAX_ENTRIES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean =
            size > MAX_ENTRIES
    }

    @Synchronized
    override fun put(key: String, text: String) {
        entries[key] = text
    }

    @Synchronized
    override fun get(key: String): String? = entries[key]
}

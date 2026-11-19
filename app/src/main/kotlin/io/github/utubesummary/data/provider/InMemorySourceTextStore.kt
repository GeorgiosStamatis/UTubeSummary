package io.github.utubesummary.data.provider

import io.github.utubesummary.domain.provider.SourceTextStore
import io.github.utubesummary.model.ExtractedContent
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_ENTRIES = 3

/** Small in-memory LRU: the last few sources, gone when the process ends. */
@Singleton
class InMemorySourceTextStore @Inject constructor() : SourceTextStore {

    private val entries = object : LinkedHashMap<String, ExtractedContent>(MAX_ENTRIES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ExtractedContent>): Boolean =
            size > MAX_ENTRIES
    }

    @Synchronized
    override fun put(key: String, text: String) {
        entries[key] = ExtractedContent(content = text)
    }

    @Synchronized
    override fun get(key: String): String? = entries[key]?.content

    @Synchronized
    override fun putContent(key: String, content: ExtractedContent) {
        entries[key] = content
    }

    @Synchronized
    override fun getContent(key: String): ExtractedContent? = entries[key]
}

package io.github.utubesummary

import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput
import java.util.UUID

/**
 * Hands a finished summary from the instant dialog to the main activity inside the same process.
 * Intent extras are limited to about 1 MB for the whole transaction, which long summaries can
 * exceed, so the intent only carries the id returned by [put].
 */
object SummaryHandoff {
    data class Entry(
        val summary: SummaryOutput,
        val lengthResults: Map<SummaryLength, SummaryOutput>,
    )

    private const val MAX_ENTRIES = 3
    private val entries = LinkedHashMap<String, Entry>()

    @Synchronized
    fun put(entry: Entry): String {
        val id = UUID.randomUUID().toString()
        entries[id] = entry
        while (entries.size > MAX_ENTRIES) {
            entries.remove(entries.keys.first())
        }
        return id
    }

    /** Returns the entry once; null when it was already taken or the process was recreated. */
    @Synchronized
    fun take(id: String): Entry? = entries.remove(id)
}

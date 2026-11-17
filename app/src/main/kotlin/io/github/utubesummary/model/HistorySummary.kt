package io.github.utubesummary.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class HistorySummary(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String = "",
    val createdOn: Long = System.currentTimeMillis(),
    val length: SummaryLength = SummaryLength.NONE,
    val type: SummaryType,
    val subtype: VideoSubtype? = null,
    val sourceLink: String? = null,
    val sourceText: String? = null,
    val lengthResults: Map<SummaryLength, HistoryLengthResult> = emptyMap(),
    /** Earlier results that were replaced as the current one for their length by another profile/language. */
    val alternatives: List<HistoryLengthResult> = emptyList(),
    val pinned: Boolean = false,
) {

    val activeResult: HistoryLengthResult?
        get() = lengthResults[length] ?: lengthResults.values.firstOrNull()

    val summary: String get() = activeResult?.summary.orEmpty()
    val overview: String? get() = activeResult?.overview
    val keyPoints: List<String> get() = activeResult?.keyPoints.orEmpty()
    val tags: List<String> get() = activeResult?.tags.orEmpty()
    val provider: String? get() = activeResult?.provider
    val model: String? get() = activeResult?.model

    val allLengthResults: Map<SummaryLength, HistoryLengthResult>
        get() = lengthResults

    /**
     * Every result of this item, oldest first, so a new summary is appended after the last one.
     * Entries from before timestamps existed all have `createdOn = 0`: they are ordered by length
     * (short, medium, long) with the one made last (the active result) at the end.
     */
    fun allResultsOldestFirst(): List<HistoryLengthResult> {
        val active = activeResult
        return (lengthResults.values + alternatives).sortedWith(
            compareBy<HistoryLengthResult> { it.createdOn }
                .thenBy { it === active }
                .thenBy { it.length.ordinal }
        )
    }

    val hasLength: Boolean
        get() = length != SummaryLength.NONE

    val isYoutubeLink: Boolean
        get() = (type == SummaryType.VIDEO && subtype == VideoSubtype.YOUTUBE) ||
                isYouTubeLink(sourceLink)

    val isBiliBiliLink: Boolean
        get() = (type == SummaryType.VIDEO && subtype == VideoSubtype.BILIBILI) ||
                isBiliBiliLink(sourceLink)
}
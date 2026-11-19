package io.github.utubesummary.model

sealed class SummarySource {
    data class Video(val url: String) : SummarySource()
    data class Article(val url: String) : SummarySource()
    data class Text(val content: String) : SummarySource()
    data class Document(val filename: String, val uri: String) : SummarySource()
    /**
     * [original] whose text was already extracted in an earlier summary of the same input, so it is
     * not downloaded or read again.
     */
    data class Extracted(val original: SummarySource, val content: ExtractedContent) : SummarySource()

    data object None : SummarySource()
}

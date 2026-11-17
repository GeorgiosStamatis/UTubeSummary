package io.github.utubesummary.model

/** Where a running summarization currently is. */
enum class SummarizationPhase {
    /** Reading the video transcript, article or document. */
    EXTRACTING,

    /** Waiting for the language model. */
    SUMMARIZING,
}

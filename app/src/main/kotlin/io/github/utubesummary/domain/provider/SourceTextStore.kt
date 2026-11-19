package io.github.utubesummary.domain.provider

import io.github.utubesummary.model.ExtractedContent

/**
 * Keeps what was extracted from the most recently summarized inputs (video transcript, article,
 * document text) in memory only: questions can be asked about it, and summarizing the same input
 * again does not extract it again. Never persisted.
 */
interface SourceTextStore {
    fun put(key: String, text: String)
    fun get(key: String): String?

    /** Stores the whole extraction (title, author, language and text) of the input [key]. */
    fun putContent(key: String, content: ExtractedContent)
    fun getContent(key: String): ExtractedContent?
}

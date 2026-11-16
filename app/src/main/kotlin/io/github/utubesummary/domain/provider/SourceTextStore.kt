package io.github.utubesummary.domain.provider

/**
 * Keeps the extracted text (video transcript, article, document text) of the most recently
 * summarized inputs in memory only, so questions can be asked about them. Never persisted.
 */
interface SourceTextStore {
    fun put(key: String, text: String)
    fun get(key: String): String?
}

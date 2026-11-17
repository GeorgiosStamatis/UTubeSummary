package io.github.utubesummary.domain.provider

/** Writes a text file the user chose as the destination. */
interface TextFileWriter {
    suspend fun writeText(uriString: String, text: String)
}

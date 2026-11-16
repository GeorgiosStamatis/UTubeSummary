package io.github.utubesummary.domain.provider

/** The file is bigger than the allowed size. */
class FileTooLargeException(val maxBytes: Long) : Exception("File is larger than $maxBytes bytes")

/** Reads a user-chosen text file. */
interface TextFileReader {
    /** @throws FileTooLargeException if the file is larger than [maxBytes]. */
    suspend fun readText(uriString: String, maxBytes: Long): String
}

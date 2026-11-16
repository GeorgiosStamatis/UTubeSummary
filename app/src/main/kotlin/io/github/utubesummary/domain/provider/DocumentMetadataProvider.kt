package io.github.utubesummary.domain.provider

interface DocumentMetadataProvider {
    suspend fun getFileName(uriString: String): String?
}

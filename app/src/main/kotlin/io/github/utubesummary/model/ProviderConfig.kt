package io.github.utubesummary.model

data class ProviderConfig(
    val apiKey: String = "",
    val baseUrl: String = "",
    val activeModel: String = "",
    val models: List<String> = emptyList(),
    /** A key is saved but can no longer be decrypted (secure storage was reset). */
    val keyUnreadable: Boolean = false,
)

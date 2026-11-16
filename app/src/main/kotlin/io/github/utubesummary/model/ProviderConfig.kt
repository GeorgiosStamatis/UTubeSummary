package io.github.utubesummary.model

data class ProviderConfig(
    val apiKey: String = "",
    val baseUrl: String = "",
    val activeModel: String = "",
    val models: List<String> = emptyList(),
)

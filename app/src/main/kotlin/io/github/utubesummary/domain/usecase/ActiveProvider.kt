package io.github.utubesummary.domain.usecase

import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ProviderConfig
import io.github.utubesummary.model.UserPreferences

/** The provider selected in Settings together with its saved configuration. */
data class ActiveProvider(
    val provider: AIProvider,
    val config: ProviderConfig?,
    val apiKey: String,
) {
    val baseUrl: String? get() = config?.baseUrl?.takeIf { it.isNotBlank() }
    val model: String? get() = config?.activeModel?.takeIf { it.isNotBlank() }

    /** For error details: no secrets. */
    val description: String
        get() = "Provider: ${provider.name}, model: ${model ?: "-"}, URL: ${baseUrl ?: "default"}"
}

/**
 * @throws SummaryException.NoKeyException if no provider is selected or a required key is missing
 * @throws SummaryException.KeyUnreadableException if the saved key can't be decrypted any more
 */
fun resolveActiveProvider(
    prefs: UserPreferences,
    providerConfigs: Map<String, ProviderConfig>,
): ActiveProvider {
    val provider = prefs.activeProvider?.let { name -> AIProvider.entries.find { it.name == name } }
        ?: throw SummaryException.NoKeyException()
    val config = providerConfigs[provider.name]
    if (config?.keyUnreadable == true) throw SummaryException.KeyUnreadableException()
    val apiKey = config?.apiKey.orEmpty()
    if (provider.isRequiredApiKey && apiKey.isBlank()) throw SummaryException.NoKeyException()
    return ActiveProvider(provider, config, apiKey)
}

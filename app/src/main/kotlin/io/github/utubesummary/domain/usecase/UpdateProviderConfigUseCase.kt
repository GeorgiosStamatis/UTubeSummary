package io.github.utubesummary.domain.usecase

import kotlinx.coroutines.flow.first
import io.github.utubesummary.domain.repository.AIProviderConfigRepository
import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ProviderConfig
import javax.inject.Inject

private val privateIpv4 = Regex(
    "^(127|10)(\\.\\d{1,3}){3}$|^192\\.168(\\.\\d{1,3}){2}$|^172\\.(1[6-9]|2\\d|3[01])(\\.\\d{1,3}){2}$"
)

fun normalizeProviderBaseUrl(url: String): String {
    val trimmed = url.trim()
    return when {
        trimmed.isBlank() -> ""
        trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith(
            "https://",
            ignoreCase = true
        ) -> trimmed

        // Local / LAN servers (e.g. LM Studio, Ollama) rarely have TLS.
        isLocalHost(trimmed) -> "http://$trimmed"
        else -> "https://$trimmed"
    }
}

private fun isLocalHost(url: String): Boolean {
    val host = url.substringBefore('/').substringBefore(':').lowercase()
    return host == "localhost" || host.endsWith(".local") || privateIpv4.matches(host)
}

class UpdateProviderConfigUseCase @Inject constructor(
    private val aiProviderConfigRepository: AIProviderConfigRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    fun normalizeBaseUrl(url: String): String = normalizeProviderBaseUrl(url)

    suspend fun updateApiKey(provider: String? = null, apiKey: String) {
        val targetProvider = provider
            ?: userPreferencesRepository.preferencesFlow.first().activeProvider
            ?: return
        aiProviderConfigRepository.updateApiKey(targetProvider, apiKey.trim())
    }

    suspend fun updateBaseUrl(provider: String? = null, baseUrl: String) {
        val targetProvider = provider
            ?: userPreferencesRepository.preferencesFlow.first().activeProvider
            ?: return
        aiProviderConfigRepository.updateBaseUrl(targetProvider, normalizeBaseUrl(baseUrl))
    }

    suspend fun setProviderConfig(
        provider: String,
        baseUrl: String,
        apiKey: String,
        providerOrder: List<String>? = null,
    ) {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        val currentConfig = aiProviderConfigRepository.getConfig(provider) ?: ProviderConfig()
        aiProviderConfigRepository.saveConfig(
            provider,
            currentConfig.copy(apiKey = apiKey.trim(), baseUrl = normalizedBaseUrl)
        )
        userPreferencesRepository.setActiveProvider(provider)
        if (providerOrder != null) {
            userPreferencesRepository.setProviderOrder(providerOrder)
        }
    }

    suspend fun updateModelForProvider(provider: String? = null, model: String) {
        val targetProvider = provider
            ?: userPreferencesRepository.preferencesFlow.first().activeProvider
            ?: return
        val currentConfig = aiProviderConfigRepository.getConfig(targetProvider) ?: ProviderConfig()
        val models =
            if (currentConfig.models.isNotEmpty() && !currentConfig.models.contains(model)) {
                currentConfig.models + model
            } else {
                currentConfig.models
            }
        aiProviderConfigRepository.saveConfig(
            targetProvider,
            currentConfig.copy(activeModel = model, models = models)
        )
    }

    suspend fun setProviderModels(
        provider: String,
        models: List<String>,
        selectedModel: String? = null,
    ) {
        val currentConfig = aiProviderConfigRepository.getConfig(provider) ?: ProviderConfig()
        val targetModel =
            selectedModel ?: currentConfig.activeModel.takeIf { it in models }
            ?: models.firstOrNull()
            ?: ""
        aiProviderConfigRepository.saveConfig(
            provider,
            currentConfig.copy(models = models, activeModel = targetModel)
        )
        val currentActiveProvider = userPreferencesRepository.preferencesFlow.first().activeProvider
        if (currentActiveProvider == null) {
            userPreferencesRepository.setActiveProvider(provider)
        }
    }

    suspend fun resetProviderModelsToDefault(provider: String) {
        val aiProvider = AIProvider.entries.find { it.name == provider } ?: return
        val defaultModels = aiProvider.defaultModelIds
        val currentConfig = aiProviderConfigRepository.getConfig(provider) ?: ProviderConfig()
        val defaultModel = defaultModels.firstOrNull() ?: ""
        aiProviderConfigRepository.saveConfig(
            provider,
            currentConfig.copy(models = defaultModels, activeModel = defaultModel)
        )
    }
}

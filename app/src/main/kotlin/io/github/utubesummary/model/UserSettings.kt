package io.github.utubesummary.model

data class UserSettings(
    val preferences: UserPreferences,
    val providerConfigs: Map<String, ProviderConfig>,
)

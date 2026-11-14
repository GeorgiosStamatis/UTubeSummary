package io.github.utubesummary.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import io.github.utubesummary.domain.repository.AIProviderConfigRepository
import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.model.UserSettings
import javax.inject.Inject

class GetUserSettingsUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiProviderConfigRepository: AIProviderConfigRepository,
) {
    operator fun invoke(): Flow<UserSettings> = combine(
        userPreferencesRepository.preferencesFlow,
        aiProviderConfigRepository.providerConfigsFlow,
    ) { preferences, providerConfigs ->
        UserSettings(
            preferences = preferences,
            providerConfigs = providerConfigs,
        )
    }
}

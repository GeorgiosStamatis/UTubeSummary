package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SetOnboardingStatusUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(isOnboarded: Boolean) {
        userPreferencesRepository.setIsOnboarded(isOnboarded)
    }
}

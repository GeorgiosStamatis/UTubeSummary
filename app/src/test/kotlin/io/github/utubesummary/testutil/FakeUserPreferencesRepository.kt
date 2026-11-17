package io.github.utubesummary.testutil

import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.model.ProfileEntry
import io.github.utubesummary.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory preferences for tests; every setter writes into [prefs]. */
class FakeUserPreferencesRepository : UserPreferencesRepository {
    val prefs = MutableStateFlow(UserPreferences())

    override val preferencesFlow: Flow<UserPreferences> = prefs

    private fun update(transform: (UserPreferences) -> UserPreferences) {
        prefs.value = transform(prefs.value)
    }

    override suspend fun setUseOriginalLanguage(value: Boolean) = update { it.copy(useOriginalLanguage = value) }
    override suspend fun setSummaryLanguage(value: String) = update { it.copy(summaryLanguage = value) }
    override suspend fun setDynamicColor(value: Boolean) = update { it.copy(dynamicColor = value) }
    override suspend fun setShowThumbnails(value: Boolean) = update { it.copy(showThumbnails = value) }
    override suspend fun setTheme(value: Int) = update { it.copy(theme = value) }
    override suspend fun setActiveProvider(value: String?) = update { it.copy(activeProvider = value) }
    override suspend fun setProviderOrder(value: List<String>) = update { it.copy(providerOrder = value) }
    override suspend fun setIsOnboarded(value: Boolean) = update { it.copy(isOnboarded = value) }
    override suspend fun setShowLength(value: Boolean) = update { it.copy(showLength = value) }
    override suspend fun setSummaryLength(value: String) = update { it.copy(summaryLength = value) }
    override suspend fun setAutoExtractUrl(value: Boolean) = update { it.copy(autoExtractUrl = value) }
    override suspend fun setBilibiliSessData(data: String, expires: Long) =
        update { it.copy(bilibiliSessData = data, bilibiliSessDataExpires = expires) }

    override suspend fun clearBilibiliSessData() =
        update { it.copy(bilibiliSessData = "", bilibiliSessDataExpires = 0L) }

    override suspend fun setIsAppendMode(value: Boolean) = update { it.copy(isAppendMode = value) }
    override suspend fun setCustomBasePrompt(value: String) = update { it.copy(customBasePrompt = value) }
    override suspend fun setAdditionalSystemPrompt(value: String) =
        update { it.copy(additionalSystemPrompt = value) }

    override suspend fun setProfileEntries(value: List<ProfileEntry>) = update { it.copy(profileEntries = value) }
    override suspend fun setSelectedProfileId(value: String) = update { it.copy(selectedProfileId = value) }
}

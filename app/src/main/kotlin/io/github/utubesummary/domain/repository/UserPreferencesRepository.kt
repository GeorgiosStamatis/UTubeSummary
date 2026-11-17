package io.github.utubesummary.domain.repository

import kotlinx.coroutines.flow.Flow
import io.github.utubesummary.model.ProfileEntry
import io.github.utubesummary.model.UserPreferences

interface UserPreferencesRepository {
    val preferencesFlow: Flow<UserPreferences>

    suspend fun setUseOriginalLanguage(value: Boolean)
    suspend fun setSummaryLanguage(value: String)
    suspend fun setDynamicColor(value: Boolean)
    suspend fun setShowThumbnails(value: Boolean)
    suspend fun setTheme(value: Int)
    suspend fun setActiveProvider(value: String?)
    suspend fun setProviderOrder(value: List<String>)
    suspend fun setIsOnboarded(value: Boolean)
    suspend fun setShowLength(value: Boolean)
    suspend fun setSummaryLength(value: String)
    suspend fun setAutoExtractUrl(value: Boolean)
    suspend fun setBilibiliSessData(data: String, expires: Long)
    suspend fun clearBilibiliSessData()
    suspend fun setIsAppendMode(value: Boolean)
    suspend fun setCustomBasePrompt(value: String)
    suspend fun setAdditionalSystemPrompt(value: String)
    suspend fun setProfileEntries(value: List<ProfileEntry>)
    suspend fun setSelectedProfileId(value: String)
}

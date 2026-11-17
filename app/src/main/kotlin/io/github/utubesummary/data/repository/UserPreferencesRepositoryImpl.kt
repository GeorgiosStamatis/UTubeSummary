package io.github.utubesummary.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import io.github.utubesummary.data.local.datastore.userPreferencesDataStore
import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.model.ProfileEntry
import io.github.utubesummary.model.UserPreferences
import io.github.utubesummary.util.DecodedSecret
import io.github.utubesummary.util.SecurityUtil
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserPreferencesRepo"

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesRepository {

    override val preferencesFlow: Flow<UserPreferences>
        get() = context.userPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(UserPreferences())
                } else {
                    throw exception
                }
            }
            .map { it.withDecryptedCookie() }

    /**
     * The BiliBili cookie is stored encrypted; callers always see the plain value.
     * An old plain-text cookie is encrypted on first read, and one that can no longer be
     * decrypted is dropped, which is the same as being logged out.
     */
    private suspend fun UserPreferences.withDecryptedCookie(): UserPreferences {
        if (bilibiliSessData.isEmpty()) return this
        return when (val decoded = SecurityUtil.codec.decode(bilibiliSessData, legacyIsPlain = true)) {
            is DecodedSecret.Value -> {
                if (decoded.needsMigration) {
                    runCatching { setBilibiliSessData(decoded.text, bilibiliSessDataExpires) }
                        .onFailure { Log.w(TAG, "Could not encrypt the stored BiliBili cookie", it) }
                }
                copy(bilibiliSessData = decoded.text)
            }

            DecodedSecret.Unreadable -> {
                Log.w(TAG, "Stored BiliBili cookie can't be decrypted; clearing it")
                clearBilibiliSessData()
                copy(bilibiliSessData = "", bilibiliSessDataExpires = 0L)
            }
        }
    }

    private suspend fun updatePreferences(transform: suspend (UserPreferences) -> UserPreferences) {
        context.userPreferencesDataStore.updateData { transform(it) }
    }

    override suspend fun setUseOriginalLanguage(value: Boolean) =
        updatePreferences { it.copy(useOriginalLanguage = value) }

    override suspend fun setDynamicColor(value: Boolean) =
        updatePreferences { it.copy(dynamicColor = value) }

    override suspend fun setShowThumbnails(value: Boolean) =
        updatePreferences { it.copy(showThumbnails = value) }

    override suspend fun setTheme(value: Int) = updatePreferences { it.copy(theme = value) }

    override suspend fun setActiveProvider(value: String?) =
        updatePreferences { it.copy(activeProvider = value) }

    override suspend fun setProviderOrder(value: List<String>) =
        updatePreferences { it.copy(providerOrder = value) }

    override suspend fun setIsOnboarded(value: Boolean) =
        updatePreferences { it.copy(isOnboarded = value) }

    override suspend fun setShowLength(value: Boolean) =
        updatePreferences { it.copy(showLength = value) }

    override suspend fun setSummaryLength(value: String) =
        updatePreferences { it.copy(summaryLength = value) }

    override suspend fun setAutoExtractUrl(value: Boolean) =
        updatePreferences { it.copy(autoExtractUrl = value) }

    override suspend fun setBilibiliSessData(data: String, expires: Long) {
        // Throws SecureStorageException if it can't be encrypted: never store it as plain text.
        val encrypted = SecurityUtil.codec.encode(data)
        updatePreferences { it.copy(bilibiliSessData = encrypted, bilibiliSessDataExpires = expires) }
    }

    override suspend fun clearBilibiliSessData() =
        updatePreferences { it.copy(bilibiliSessData = "", bilibiliSessDataExpires = 0L) }

    override suspend fun setIsAppendMode(value: Boolean) =
        updatePreferences { it.copy(isAppendMode = value) }

    override suspend fun setCustomBasePrompt(value: String) =
        updatePreferences { it.copy(customBasePrompt = value) }

    override suspend fun setAdditionalSystemPrompt(value: String) =
        updatePreferences { it.copy(additionalSystemPrompt = value) }

    override suspend fun setProfileEntries(value: List<ProfileEntry>) =
        updatePreferences { it.copy(profileEntries = value) }

    override suspend fun setSelectedProfileId(value: String) =
        updatePreferences { it.copy(selectedProfileId = value) }
}

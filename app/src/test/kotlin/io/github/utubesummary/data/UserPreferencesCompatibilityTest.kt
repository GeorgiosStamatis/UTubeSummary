package io.github.utubesummary.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import io.github.utubesummary.model.ProfileEntry
import io.github.utubesummary.model.UserPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Settings files written by older builds must still be read correctly. [LegacyPreferences] is
 * the layout before `showThumbnails` existed (numbers follow the declaration order).
 */
@OptIn(ExperimentalSerializationApi::class)
class UserPreferencesCompatibilityTest {

    @Serializable
    private data class LegacyPreferences(
        val isOnboarded: Boolean = false,
        val useOriginalLanguage: Boolean = true,
        val dynamicColor: Boolean = true,
        val theme: Int = 0,
        val activeProvider: String? = null,
        val showLength: Boolean = true,
        val summaryLength: String = "MEDIUM",
        val autoExtractUrl: Boolean = true,
        val bilibiliSessData: String = "",
        val bilibiliSessDataExpires: Long = 0L,
        val isAppendMode: Boolean = true,
        val customBasePrompt: String = "",
        val additionalSystemPrompt: String = "",
        val providerOrder: List<String> = emptyList(),
    )

    @Test
    fun `a settings file from an older build is read field by field`() {
        val old = LegacyPreferences(
            isOnboarded = true,
            useOriginalLanguage = false,
            dynamicColor = false,
            theme = 2,
            activeProvider = "OPEN_ROUTER",
            showLength = false,
            summaryLength = "LONG",
            autoExtractUrl = false,
            bilibiliSessData = "enc1:abc",
            bilibiliSessDataExpires = 1234L,
            isAppendMode = false,
            customBasePrompt = "base",
            additionalSystemPrompt = "extra",
            providerOrder = listOf("OPENAI", "CLAUDE"),
        )

        val decoded = ProtoBuf.decodeFromByteArray(
            UserPreferences.serializer(),
            ProtoBuf.encodeToByteArray(LegacyPreferences.serializer(), old)
        )

        assertEquals(
            UserPreferences(
                isOnboarded = true,
                useOriginalLanguage = false,
                dynamicColor = false,
                theme = 2,
                activeProvider = "OPEN_ROUTER",
                showLength = false,
                summaryLength = "LONG",
                autoExtractUrl = false,
                bilibiliSessData = "enc1:abc",
                bilibiliSessDataExpires = 1234L,
                isAppendMode = false,
                customBasePrompt = "base",
                additionalSystemPrompt = "extra",
                providerOrder = listOf("OPENAI", "CLAUDE"),
                showThumbnails = true, // not in the old file: the default
            ),
            decoded
        )
    }

    @Test
    fun `preferences round trip`() {
        val prefs = UserPreferences(isOnboarded = true, showThumbnails = false, theme = 1)
        val bytes = ProtoBuf.encodeToByteArray(UserPreferences.serializer(), prefs)
        assertEquals(prefs, ProtoBuf.decodeFromByteArray(UserPreferences.serializer(), bytes))
    }

    @Test
    fun `profile entries and selected profile survive a round trip`() {
        val prefs = UserPreferences(
            profileEntries = listOf(ProfileEntry("custom-1", "Mine", "prompt [Language instructions]")),
            selectedProfileId = "custom-1",
        )

        val decoded = ProtoBuf.decodeFromByteArray(
            UserPreferences.serializer(),
            ProtoBuf.encodeToByteArray(UserPreferences.serializer(), prefs)
        )

        assertEquals(prefs, decoded)
    }
}

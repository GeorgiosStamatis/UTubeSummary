package io.github.utubesummary.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Stored as ProtoBuf. Every field has a fixed [ProtoNumber]: without it numbers follow the
 * declaration order, so inserting or removing a field in the middle would make existing files
 * decode into the wrong fields. NEVER renumber a field; new fields go at the end with the next
 * number.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UserPreferences(
    // state
    @ProtoNumber(1) val isOnboarded: Boolean = false,
    // settings
    @ProtoNumber(2) val useOriginalLanguage: Boolean = true,
    @ProtoNumber(3) val dynamicColor: Boolean = true,
    @ProtoNumber(4) val theme: Int = 0,
    @ProtoNumber(5) val activeProvider: String? = null,
    @ProtoNumber(6) val showLength: Boolean = true,
    @ProtoNumber(7) val summaryLength: String = SummaryLength.MEDIUM.name,
    @ProtoNumber(8) val autoExtractUrl: Boolean = true,
    @ProtoNumber(9) val bilibiliSessData: String = "",
    @ProtoNumber(10) val bilibiliSessDataExpires: Long = 0L,
    @ProtoNumber(11) val isAppendMode: Boolean = true,
    @ProtoNumber(12) val customBasePrompt: String = "",
    @ProtoNumber(13) val additionalSystemPrompt: String = "",
    @ProtoNumber(14) val providerOrder: List<String> = emptyList(),
    @ProtoNumber(15) val showThumbnails: Boolean = true,
    @ProtoNumber(16) val profileEntries: List<ProfileEntry> = emptyList(),
    @ProtoNumber(17) val selectedProfileId: String = DEFAULT_PROFILE_ID,
    // "" = never chosen (falls back to useOriginalLanguage), see SummaryLanguage.kt
    @ProtoNumber(18) val summaryLanguage: String = "",
)

/** A user-created profile, or a user-edited override of a built-in profile with the same [id]. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProfileEntry(
    @ProtoNumber(1) val id: String,
    @ProtoNumber(2) val name: String = "",
    @ProtoNumber(3) val basePrompt: String = "",
)

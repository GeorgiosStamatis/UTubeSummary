package io.github.utubesummary.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** A profile as it is shared between devices: a small JSON text. */
@Serializable
data class SharedProfile(
    /** Marks the JSON as a UTubeSummary profile; only [SHARED_PROFILE_VERSION] is understood. */
    @SerialName("utubesummary_profile") val version: Int = 0,
    val name: String,
    val basePrompt: String,
    val structuredOutput: Boolean = false,
)

const val SHARED_PROFILE_VERSION = 1

private val shareJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun encodeSharedProfile(name: String, basePrompt: String, structuredOutput: Boolean): String =
    shareJson.encodeToString(
        SharedProfile(SHARED_PROFILE_VERSION, name.trim(), basePrompt, structuredOutput)
    )

/**
 * Reads a shared profile from [text], which may have other text around the JSON (for example from a
 * chat message). Returns null for anything that is not a profile with a name and a prompt.
 */
fun decodeSharedProfile(text: String): SharedProfile? {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    val profile = runCatching {
        shareJson.decodeFromString<SharedProfile>(text.substring(start, end + 1))
    }.getOrNull() ?: return null
    return profile.takeIf {
        it.version == SHARED_PROFILE_VERSION && it.name.isNotBlank() && it.basePrompt.isNotBlank()
    }?.copy(name = profile.name.trim())
}

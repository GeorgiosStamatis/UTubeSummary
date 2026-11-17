package io.github.utubesummary.domain.usecase

import io.github.utubesummary.llm.builtInProfilePrompts
import io.github.utubesummary.llm.defaultSystemPromptPlaceholder
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import io.github.utubesummary.model.SummaryProfile
import io.github.utubesummary.model.UserPreferences

/**
 * Merges the built-in profiles with the user's stored entries: edited built-ins override the
 * shipped prompt/name, remaining entries are user-created profiles. The Default profile always
 * comes first and is edited through the legacy base prompt setting.
 */
fun resolveProfiles(prefs: UserPreferences): List<SummaryProfile> {
    val entries = prefs.profileEntries.associateBy { it.id }
    val default = SummaryProfile(
        id = DEFAULT_PROFILE_ID,
        name = null,
        basePrompt = if (!prefs.isAppendMode && prefs.customBasePrompt.isNotBlank()) {
            prefs.customBasePrompt
        } else {
            defaultSystemPromptPlaceholder
        },
        isBuiltIn = true,
    )
    val builtIns = builtInProfilePrompts.map { (id, prompt) ->
        val entry = entries[id]
        SummaryProfile(
            id = id,
            name = entry?.name?.takeIf { it.isNotBlank() },
            basePrompt = entry?.basePrompt?.takeIf { it.isNotBlank() } ?: prompt,
            isBuiltIn = true,
            isModified = entry != null,
        )
    }
    val custom = prefs.profileEntries
        .filter { it.id != DEFAULT_PROFILE_ID && it.id !in builtInProfilePrompts }
        .map { SummaryProfile(it.id, it.name, it.basePrompt, isBuiltIn = false) }
    return listOf(default) + builtIns + custom
}

/** Returns the profile with [profileId], falling back to the Default profile. */
fun resolveProfile(prefs: UserPreferences, profileId: String?): SummaryProfile {
    val profiles = resolveProfiles(prefs)
    return profiles.firstOrNull { it.id == (profileId ?: prefs.selectedProfileId) } ?: profiles.first()
}

package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.llm.builtInProfilePrompts
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import io.github.utubesummary.model.ProfileEntry
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class UpdateSummaryProfilesUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) {
    suspend fun select(profileId: String) {
        repository.setSelectedProfileId(profileId)
    }

    /** Creates a profile when [id] is null, otherwise updates (or overrides, for built-ins) [id]. */
    suspend fun save(id: String?, name: String, basePrompt: String, structuredOutput: Boolean): String {
        val profileId = id ?: UUID.randomUUID().toString()
        require(profileId != DEFAULT_PROFILE_ID) { "The Default profile is edited in advanced setup" }
        val entries = repository.preferencesFlow.first().profileEntries
        // The prompt is stored as typed: trimming it would fight the editor while the user types.
        val entry = ProfileEntry(profileId, name.trim(), basePrompt, structuredOutput)
        repository.setProfileEntries(entries.filterNot { it.id == profileId } + entry)
        return profileId
    }

    /** Deletes a custom profile or resets a built-in one to its shipped prompt. */
    suspend fun deleteOrReset(profileId: String) {
        val prefs = repository.preferencesFlow.first()
        repository.setProfileEntries(prefs.profileEntries.filterNot { it.id == profileId })
        val removed = profileId !in builtInProfilePrompts
        if (removed && prefs.selectedProfileId == profileId) {
            repository.setSelectedProfileId(DEFAULT_PROFILE_ID)
        }
    }
}

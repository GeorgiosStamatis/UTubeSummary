package io.github.utubesummary.domain.usecase

import io.github.utubesummary.llm.CRITICAL_ANALYST_PROFILE_ID
import io.github.utubesummary.llm.criticalAnalystPrompt
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import io.github.utubesummary.testutil.FakeUserPreferencesRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateSummaryProfilesUseCaseTest {

    private val repository = FakeUserPreferencesRepository()
    private val useCase = UpdateSummaryProfilesUseCase(repository)

    private fun profiles() = resolveProfiles(repository.prefs.value)

    @Test
    fun `a new profile gets an id and shows up after the built-ins`() = runTest {
        val id = useCase.save(null, "  My profile ", "Summarize like a pirate")

        val created = profiles().last()
        assertEquals(id, created.id)
        assertEquals("My profile", created.name)
        assertEquals("Summarize like a pirate", created.basePrompt)
        assertFalse(created.isBuiltIn)
    }

    @Test
    fun `editing a built-in with an empty name keeps its localized name and the prompt as typed`() = runTest {
        useCase.save(CRITICAL_ANALYST_PROFILE_ID, "", "be skeptical ")

        val profile = profiles().first { it.id == CRITICAL_ANALYST_PROFILE_ID }
        assertNull(profile.name)
        assertEquals("be skeptical ", profile.basePrompt)
        assertTrue(profile.isModified)
    }

    @Test
    fun `resetting a built-in restores the shipped prompt and keeps the selection`() = runTest {
        useCase.save(CRITICAL_ANALYST_PROFILE_ID, "", "changed")
        useCase.select(CRITICAL_ANALYST_PROFILE_ID)

        useCase.deleteOrReset(CRITICAL_ANALYST_PROFILE_ID)

        val profile = profiles().first { it.id == CRITICAL_ANALYST_PROFILE_ID }
        assertEquals(criticalAnalystPrompt, profile.basePrompt)
        assertFalse(profile.isModified)
        assertEquals(CRITICAL_ANALYST_PROFILE_ID, repository.prefs.value.selectedProfileId)
    }

    @Test
    fun `deleting the selected custom profile falls back to Default`() = runTest {
        val id = useCase.save(null, "Mine", "prompt")
        useCase.select(id)

        useCase.deleteOrReset(id)

        assertTrue(profiles().none { it.id == id })
        assertEquals(DEFAULT_PROFILE_ID, repository.prefs.value.selectedProfileId)
    }

    @Test
    fun `the Default profile cannot be saved over`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase.save(DEFAULT_PROFILE_ID, "x", "y") }
        }
    }
}

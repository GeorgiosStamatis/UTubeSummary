package io.github.utubesummary.domain.usecase

import io.github.utubesummary.llm.CRITICAL_ANALYST_PROFILE_ID
import io.github.utubesummary.llm.criticalAnalystPrompt
import io.github.utubesummary.llm.defaultSystemPromptPlaceholder
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import io.github.utubesummary.model.ProfileEntry
import io.github.utubesummary.model.UserPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResolveProfilesTest {

    @Test
    fun `default profile comes first followed by built-ins`() {
        val profiles = resolveProfiles(UserPreferences())

        assertEquals(DEFAULT_PROFILE_ID, profiles.first().id)
        assertEquals(defaultSystemPromptPlaceholder, profiles.first().basePrompt)
        assertTrue(profiles.all { it.isBuiltIn })
        assertTrue(profiles.any { it.id == CRITICAL_ANALYST_PROFILE_ID })
    }

    @Test
    fun `default profile uses the legacy custom base prompt when append mode is off`() {
        val prefs = UserPreferences(isAppendMode = false, customBasePrompt = "my base")

        assertEquals("my base", resolveProfiles(prefs).first().basePrompt)
    }

    @Test
    fun `edited built-in overrides the shipped prompt and can be reset`() {
        val edited = UserPreferences(
            profileEntries = listOf(ProfileEntry(CRITICAL_ANALYST_PROFILE_ID, "Skeptic", "be skeptical")),
        )
        val profile = resolveProfile(edited, CRITICAL_ANALYST_PROFILE_ID)

        assertEquals("Skeptic", profile.name)
        assertEquals("be skeptical", profile.basePrompt)
        assertTrue(profile.isModified)

        val reset = resolveProfile(UserPreferences(), CRITICAL_ANALYST_PROFILE_ID)
        assertEquals(criticalAnalystPrompt, reset.basePrompt)
        assertFalse(reset.isModified)
    }

    @Test
    fun `custom profiles are appended and non built-in`() {
        val prefs = UserPreferences(profileEntries = listOf(ProfileEntry("abc", "Mine", "prompt")))
        val custom = resolveProfiles(prefs).last()

        assertEquals("abc", custom.id)
        assertFalse(custom.isBuiltIn)
    }

    @Test
    fun `unknown selected profile falls back to default`() {
        val prefs = UserPreferences(selectedProfileId = "deleted")

        assertEquals(DEFAULT_PROFILE_ID, resolveProfile(prefs, null).id)
    }

    @Test
    fun `built-in profiles are listed in order with distinct non blank prompts`() {
        val profiles = resolveProfiles(UserPreferences())

        assertEquals(
            listOf(
                DEFAULT_PROFILE_ID,
                "technical_analysis",
                "critical_analyst",
                "curious_learner",
                "technical_content_evaluator",
                "quick_brief",
            ),
            profiles.map { it.id },
        )
        assertTrue(profiles.all { it.basePrompt.isNotBlank() })
        assertEquals(profiles.size, profiles.map { it.basePrompt }.toSet().size)
    }

    @Test
    fun `only the default profile uses structured output`() {
        val profiles = resolveProfiles(UserPreferences())

        assertTrue(usesStructuredOutput(profiles.first { it.id == DEFAULT_PROFILE_ID }))
        assertTrue(profiles.filter { it.id != DEFAULT_PROFILE_ID }.none { usesStructuredOutput(it) })
    }

    @Test
    fun `an edited built-in with an empty name keeps its localized name and its prompt as typed`() {
        val prefs = UserPreferences(
            profileEntries = listOf(ProfileEntry(CRITICAL_ANALYST_PROFILE_ID, "", "be skeptical \n")),
        )

        val profile = resolveProfile(prefs, CRITICAL_ANALYST_PROFILE_ID)

        assertEquals(null, profile.name)
        assertEquals("be skeptical \n", profile.basePrompt)
        assertTrue(profile.isModified)
    }

    @Test
    fun `the structured output flag of a profile is carried and decides the card`() {
        val prefs = UserPreferences(
            profileEntries = listOf(
                ProfileEntry(CRITICAL_ANALYST_PROFILE_ID, "", "edited", structuredOutput = true),
                ProfileEntry("custom", "Mine", "prompt"),
            ),
        )
        val profiles = resolveProfiles(prefs)

        assertTrue(usesStructuredOutput(profiles.first { it.id == CRITICAL_ANALYST_PROFILE_ID }))
        assertFalse(usesStructuredOutput(profiles.first { it.id == "custom" }))
        assertTrue(usesStructuredOutput(profiles.first { it.id == DEFAULT_PROFILE_ID }))
    }
}

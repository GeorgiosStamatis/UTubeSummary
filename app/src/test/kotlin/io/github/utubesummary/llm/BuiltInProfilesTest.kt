package io.github.utubesummary.llm

import io.github.utubesummary.model.CRITICAL_ANALYST_PROFILE_ID
import io.github.utubesummary.model.CURIOUS_LEARNER_PROFILE_ID
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import io.github.utubesummary.model.QUICK_BRIEF_PROFILE_ID
import io.github.utubesummary.model.TECHNICAL_ANALYSIS_PROFILE_ID
import io.github.utubesummary.model.TECHNICAL_CONTENT_EVALUATOR_PROFILE_ID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BuiltInProfilesTest {

    @Test
    fun `the five presets are shipped in a fixed order`() {
        assertEquals(
            listOf(
                TECHNICAL_ANALYSIS_PROFILE_ID,
                CRITICAL_ANALYST_PROFILE_ID,
                CURIOUS_LEARNER_PROFILE_ID,
                TECHNICAL_CONTENT_EVALUATOR_PROFILE_ID,
                QUICK_BRIEF_PROFILE_ID,
            ),
            builtInProfilePrompts.keys.toList()
        )
    }

    @Test
    fun `no preset uses the id of the default profile`() {
        assertFalse(DEFAULT_PROFILE_ID in builtInProfilePrompts)
    }

    @Test
    fun `every preset has its own substantial prompt`() {
        builtInProfilePrompts.forEach { (id, prompt) ->
            assertTrue(prompt.trim().length > 200, "$id prompt is too short")
        }
        assertEquals(builtInProfilePrompts.size, builtInProfilePrompts.values.toSet().size)
    }

    @Test
    fun `presets leave the language and length wording to the prompt builder`() {
        builtInProfilePrompts.forEach { (id, prompt) ->
            assertFalse("[Language instructions]" in prompt, "$id must not use the language token")
            assertFalse("[Length instructions]" in prompt, "$id must not use the length token")
        }
    }
}

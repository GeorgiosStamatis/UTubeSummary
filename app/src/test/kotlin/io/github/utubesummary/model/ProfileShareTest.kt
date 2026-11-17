package io.github.utubesummary.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProfileShareTest {

    @Test
    fun `a profile survives sharing`() {
        val text = encodeSharedProfile(" My profile ", "Line one\nLine \"two\" [Language instructions]", true)

        val decoded = decodeSharedProfile(text)

        assertEquals(
            SharedProfile(SHARED_PROFILE_VERSION, "My profile", "Line one\nLine \"two\" [Language instructions]", true),
            decoded
        )
    }

    @Test
    fun `text around the json is ignored`() {
        val json = encodeSharedProfile("Brief", "Be short", false)

        assertEquals("Brief", decodeSharedProfile("Try this one:\n$json\nThanks!")!!.name)
    }

    @Test
    fun `other json and incomplete profiles are rejected`() {
        assertNull(decodeSharedProfile("""{"name":"x","basePrompt":"y"}"""))
        assertNull(decodeSharedProfile("""{"utubesummary_profile":2,"name":"x","basePrompt":"y"}"""))
        assertNull(decodeSharedProfile("""{"utubesummary_profile":1,"name":" ","basePrompt":"y"}"""))
        assertNull(decodeSharedProfile("""{"utubesummary_profile":1,"name":"x","basePrompt":""}"""))
        assertNull(decodeSharedProfile("not json at all"))
        assertNull(decodeSharedProfile("{broken"))
    }

    @Test
    fun `the structured output flag defaults to off`() {
        val decoded = decodeSharedProfile("""{"utubesummary_profile":1,"name":"x","basePrompt":"y"}""")

        assertEquals(false, decoded!!.structuredOutput)
    }
}

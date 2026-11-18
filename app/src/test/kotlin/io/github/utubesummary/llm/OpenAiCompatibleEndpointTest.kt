package io.github.utubesummary.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OpenAiCompatibleEndpointTest {

    @Test
    fun `a bare host is called at v1 chat completions`() {
        val endpoint = openAiCompatibleEndpoint("http://192.168.1.10:1234")!!

        assertEquals("http://192.168.1.10:1234", endpoint.baseUrl)
        assertEquals("v1/chat/completions", endpoint.chatCompletionsPath)
    }

    @Test
    fun `a url with a path keeps it and adds chat completions`() {
        val endpoint = openAiCompatibleEndpoint("http://pc:1234/v1")!!

        assertEquals("http://pc:1234/v1", endpoint.baseUrl)
        assertEquals("chat/completions", endpoint.chatCompletionsPath)
    }

    @Test
    fun `any path counts, not only v1`() {
        assertEquals("chat/completions", openAiCompatibleEndpoint("https://example.com/api/openai")!!.chatCompletionsPath)
    }

    @Test
    fun `trailing slashes and spaces are ignored`() {
        val bare = openAiCompatibleEndpoint("  http://pc:1234/  ")!!
        val withPath = openAiCompatibleEndpoint("http://pc:1234/v1/")!!

        assertEquals("http://pc:1234", bare.baseUrl)
        assertEquals("v1/chat/completions", bare.chatCompletionsPath)
        assertEquals("http://pc:1234/v1", withPath.baseUrl)
        assertEquals("chat/completions", withPath.chatCompletionsPath)
    }

    @Test
    fun `a blank or missing address gives no endpoint`() {
        assertNull(openAiCompatibleEndpoint(null))
        assertNull(openAiCompatibleEndpoint(""))
        assertNull(openAiCompatibleEndpoint("   "))
        assertNull(openAiCompatibleEndpoint("/"))
    }
}

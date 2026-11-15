package io.github.utubesummary.data.provider

import io.github.utubesummary.llm.AIProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

class ProviderConnectionRequestTest {

    private fun build(provider: AIProvider, url: String = "", key: String = "k") =
        buildConnectionRequest(provider, url, key, Locale.ENGLISH)

    @Test
    fun `openai style providers use models endpoint with bearer token`() {
        val openAi = build(AIProvider.OPENAI)
        assertEquals("https://api.openai.com/v1/models", openAi.url)
        assertEquals("Bearer k", openAi.headers["Authorization"])

        // Bare host gets /v1, a URL with a path keeps it.
        assertEquals("http://pc:1234/v1/models", build(AIProvider.LM_STUDIO, "http://pc:1234/").url)
        assertEquals("http://pc:1234/v1/models", build(AIProvider.LM_STUDIO, "http://pc:1234/v1").url)
        assertEquals("https://x.io/api/models", build(AIProvider.CUSTOM, "https://x.io/api").url)
    }

    @Test
    fun `optional token is not sent when blank`() {
        val request = build(AIProvider.LM_STUDIO, "http://pc:1234", key = "")
        assertTrue(request.headers.isEmpty())
    }

    @Test
    fun `claude and gemini use their own auth headers`() {
        val claude = build(AIProvider.CLAUDE)
        assertEquals("https://api.anthropic.com/v1/models", claude.url)
        assertEquals("k", claude.headers["x-api-key"])
        assertEquals("2023-06-01", claude.headers["anthropic-version"])

        val gemini = build(AIProvider.GEMINI)
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models", gemini.url)
        assertEquals("k", gemini.headers["x-goog-api-key"])
    }

    @Test
    fun `ollama lists tags and locale picks regional defaults`() {
        assertEquals("http://pc:11434/api/tags", build(AIProvider.OLLAMA, "http://pc:11434/").url)

        val zh = buildConnectionRequest(
            AIProvider.KIMI, "", "k", Locale.forLanguageTag("zh-Hans-CN")
        )
        assertEquals("https://api.moonshot.cn/v1/models", zh.url)
        assertEquals("https://api.moonshot.ai/v1/models", build(AIProvider.KIMI).url)
    }
}

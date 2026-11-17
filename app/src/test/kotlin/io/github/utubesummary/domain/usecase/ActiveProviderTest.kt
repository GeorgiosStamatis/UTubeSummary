package io.github.utubesummary.domain.usecase

import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ProviderConfig
import io.github.utubesummary.model.UserPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ActiveProviderTest {

    @Test
    fun `no selected provider means no key`() {
        assertThrows(SummaryException.NoKeyException::class.java) {
            resolveActiveProvider(UserPreferences(), emptyMap())
        }
    }

    @Test
    fun `a provider that needs a key without one is reported`() {
        val prefs = UserPreferences(activeProvider = AIProvider.OPENAI.name)

        assertThrows(SummaryException.NoKeyException::class.java) {
            resolveActiveProvider(prefs, mapOf(AIProvider.OPENAI.name to ProviderConfig(apiKey = " ")))
        }
    }

    @Test
    fun `an unreadable saved key is reported as such`() {
        val prefs = UserPreferences(activeProvider = AIProvider.OPENAI.name)
        val configs = mapOf(AIProvider.OPENAI.name to ProviderConfig(keyUnreadable = true))

        assertThrows(SummaryException.KeyUnreadableException::class.java) {
            resolveActiveProvider(prefs, configs)
        }
    }

    @Test
    fun `a provider without a required key works with the saved model and url`() {
        val prefs = UserPreferences(activeProvider = AIProvider.OLLAMA.name)
        val configs = mapOf(
            AIProvider.OLLAMA.name to ProviderConfig(baseUrl = "http://192.168.1.10:11434", activeModel = "llama")
        )

        val active = resolveActiveProvider(prefs, configs)

        assertEquals(AIProvider.OLLAMA, active.provider)
        assertEquals("llama", active.model)
        assertEquals("http://192.168.1.10:11434", active.baseUrl)
        assertEquals("", active.apiKey)
    }

    @Test
    fun `blank url and model are treated as unset and the description hides the key`() {
        val prefs = UserPreferences(activeProvider = AIProvider.OPENAI.name)
        val configs = mapOf(AIProvider.OPENAI.name to ProviderConfig(apiKey = "sk-secret"))

        val active = resolveActiveProvider(prefs, configs)

        assertNull(active.model)
        assertNull(active.baseUrl)
        assertEquals(false, active.description.contains("sk-secret"))
    }
}

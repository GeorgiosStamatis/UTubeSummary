package io.github.utubesummary.domain.usecase

import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ProviderConfig
import io.github.utubesummary.testutil.FakeProviderConfigRepository
import io.github.utubesummary.testutil.FakeUserPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProviderConfigUseCaseTest {

    private val configs = FakeProviderConfigRepository()
    private val prefs = FakeUserPreferencesRepository()
    private val useCase = UpdateProviderConfigUseCase(configs, prefs)

    @Test
    fun `blank urls stay blank and given schemes are kept`() {
        assertEquals("", normalizeProviderBaseUrl("   "))
        assertEquals("http://host:1234", normalizeProviderBaseUrl("http://host:1234"))
        assertEquals("HTTPS://Host", normalizeProviderBaseUrl("HTTPS://Host"))
    }

    @Test
    fun `local and LAN addresses default to http`() {
        assertEquals("http://localhost:1234", normalizeProviderBaseUrl("localhost:1234"))
        assertEquals("http://192.168.1.10:1234", normalizeProviderBaseUrl("192.168.1.10:1234"))
        assertEquals("http://10.0.0.5/v1", normalizeProviderBaseUrl("10.0.0.5/v1"))
        assertEquals("http://172.16.0.1", normalizeProviderBaseUrl("172.16.0.1"))
        assertEquals("http://127.0.0.1:11434", normalizeProviderBaseUrl(" 127.0.0.1:11434 "))
        assertEquals("http://mypc.local:11434", normalizeProviderBaseUrl("mypc.local:11434"))
    }

    @Test
    fun `public hosts and addresses outside the private ranges default to https`() {
        assertEquals("https://api.example.com", normalizeProviderBaseUrl("api.example.com"))
        assertEquals("https://172.32.0.1", normalizeProviderBaseUrl("172.32.0.1"))
        assertEquals("https://8.8.8.8", normalizeProviderBaseUrl("8.8.8.8"))
    }

    @Test
    fun `the official endpoint is stored as empty with or without the version path`() = runTest {
        useCase.setProviderConfig(AIProvider.OPENAI.name, "api.openai.com/v1", " sk-key ")

        val saved = configs.getConfig(AIProvider.OPENAI.name)!!
        assertEquals("", saved.baseUrl)
        assertEquals("sk-key", saved.apiKey)
        assertEquals(AIProvider.OPENAI.name, prefs.prefs.value.activeProvider)
    }

    @Test
    fun `a proxy url loses a trailing slash and the version path Koog adds itself`() = runTest {
        useCase.setProviderConfig(AIProvider.OPENAI.name, "https://proxy.example.com/v1/", "k")

        assertEquals("https://proxy.example.com", configs.getConfig(AIProvider.OPENAI.name)!!.baseUrl)
    }

    @Test
    fun `a custom provider keeps its path and a LAN address gets http`() = runTest {
        useCase.setProviderConfig(AIProvider.CUSTOM.name, "192.168.1.5:1234/v1", "")

        assertEquals("http://192.168.1.5:1234/v1", configs.getConfig(AIProvider.CUSTOM.name)!!.baseUrl)
    }

    @Test
    fun `saving a provider can also store the provider order`() = runTest {
        useCase.setProviderConfig("CLAUDE", "", "k", providerOrder = listOf("CLAUDE", "OPENAI"))

        assertEquals(listOf("CLAUDE", "OPENAI"), prefs.prefs.value.providerOrder)
    }

    @Test
    fun `the key is saved for the active provider and trimmed`() = runTest {
        prefs.setActiveProvider("GEMINI")

        useCase.updateApiKey(apiKey = "  abc  ")

        assertEquals("abc", configs.getConfig("GEMINI")!!.apiKey)
    }

    @Test
    fun `without a provider and without an active provider nothing is saved`() = runTest {
        useCase.updateApiKey(apiKey = "abc")
        useCase.updateBaseUrl(baseUrl = "x.example.com")
        useCase.updateModelForProvider(model = "m")

        assertEquals(emptyMap<String, ProviderConfig>(), configs.configs.value)
    }

    @Test
    fun `a model missing from a non-empty list is added to it`() = runTest {
        configs.saveConfig("OPENAI", ProviderConfig(models = listOf("a", "b"), activeModel = "a"))

        useCase.updateModelForProvider("OPENAI", "c")

        val saved = configs.getConfig("OPENAI")!!
        assertEquals("c", saved.activeModel)
        assertEquals(listOf("a", "b", "c"), saved.models)
    }

    @Test
    fun `an empty model list stays empty so the built-in defaults apply`() = runTest {
        useCase.updateModelForProvider("OPENAI", "custom-model")

        val saved = configs.getConfig("OPENAI")!!
        assertEquals("custom-model", saved.activeModel)
        assertEquals(emptyList<String>(), saved.models)
    }

    @Test
    fun `models keep the active one when it is still listed`() = runTest {
        configs.saveConfig("OPENAI", ProviderConfig(activeModel = "b"))

        useCase.setProviderModels("OPENAI", listOf("a", "b"))

        assertEquals("b", configs.getConfig("OPENAI")!!.activeModel)
    }

    @Test
    fun `models fall back to a chosen model, then to the first one`() = runTest {
        configs.saveConfig("OPENAI", ProviderConfig(activeModel = "gone"))

        useCase.setProviderModels("OPENAI", listOf("a", "b"))
        assertEquals("a", configs.getConfig("OPENAI")!!.activeModel)

        useCase.setProviderModels("OPENAI", listOf("a", "b"), selectedModel = "b")
        assertEquals("b", configs.getConfig("OPENAI")!!.activeModel)
    }

    @Test
    fun `setting models makes the provider active only when none is`() = runTest {
        useCase.setProviderModels("OPENAI", listOf("a"))
        assertEquals("OPENAI", prefs.prefs.value.activeProvider)

        useCase.setProviderModels("CLAUDE", listOf("c"))
        assertEquals("OPENAI", prefs.prefs.value.activeProvider)
    }

    @Test
    fun `fetched models replace the list and an empty answer changes nothing`() = runTest {
        configs.saveConfig("OLLAMA", ProviderConfig(activeModel = "llama3", models = listOf("llama3")))

        useCase.applyFetchedModels("OLLAMA", emptyList())
        assertEquals(listOf("llama3"), configs.getConfig("OLLAMA")!!.models)

        useCase.applyFetchedModels("OLLAMA", listOf("llama3", "qwen"))
        assertEquals(listOf("llama3", "qwen"), configs.getConfig("OLLAMA")!!.models)
    }

    @Test
    fun `a selected model the server does not list is kept at the top`() = runTest {
        configs.saveConfig("OPENAI", ProviderConfig(activeModel = "my-alias"))

        useCase.applyFetchedModels("OPENAI", listOf("gpt-a", "gpt-b"))

        val saved = configs.getConfig("OPENAI")!!
        assertEquals(listOf("my-alias", "gpt-a", "gpt-b"), saved.models)
        assertEquals("my-alias", saved.activeModel)
    }

    @Test
    fun `resetting the models restores the built-in list and its first model`() = runTest {
        configs.saveConfig("OPENAI", ProviderConfig(apiKey = "k", models = listOf("x"), activeModel = "x"))

        useCase.resetProviderModelsToDefault("OPENAI")

        val saved = configs.getConfig("OPENAI")!!
        assertEquals(AIProvider.OPENAI.defaultModelIds, saved.models)
        assertEquals(AIProvider.OPENAI.defaultModelIds.first(), saved.activeModel)
        assertEquals("k", saved.apiKey)
    }

    @Test
    fun `resetting an unknown provider changes nothing`() = runTest {
        useCase.resetProviderModelsToDefault("NOT_A_PROVIDER")

        assertEquals(emptyMap<String, ProviderConfig>(), configs.configs.value)
    }
}

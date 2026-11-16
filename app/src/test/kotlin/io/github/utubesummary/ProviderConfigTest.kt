package io.github.utubesummary

import io.github.utubesummary.data.AIProviderConfigEntity
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ProviderConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AIProviderConfigTest {

    @Test
    fun `test AIProviderConfigEntity to and from ProviderConfig`() {
        val models = listOf("gpt-4o", "gpt-4o-mini", "o3-mini")
        val config = ProviderConfig(
            apiKey = "",
            baseUrl = "https://api.openai.com/v1",
            activeModel = "gpt-4o",
            models = models
        )

        val entity = AIProviderConfigEntity.fromProviderConfig("OPENAI", config)
        assertEquals("OPENAI", entity.provider)
        assertEquals(models, entity.models)
        assertEquals("gpt-4o", entity.activeModel)
        assertEquals("https://api.openai.com/v1", entity.baseUrl)

        val convertedConfig = entity.toProviderConfig()
        assertEquals(config.apiKey, convertedConfig.apiKey)
        assertEquals(config.baseUrl, convertedConfig.baseUrl)
        assertEquals(config.activeModel, convertedConfig.activeModel)
        assertEquals(config.models, convertedConfig.models)
    }

    @Test
    fun `test AIProvider default models fallback`() {
        val provider = AIProvider.OPENAI
        val defaultModels = provider.defaultModelIds
        assertTrue(defaultModels.isNotEmpty())
        assertTrue(defaultModels.contains("gpt-4o-mini") || defaultModels.any { it.contains("gpt") })

        // When provider config has empty models, getEffectiveModels should return defaults
        val emptyConfig =
            ProviderConfig(apiKey = "key", baseUrl = "", activeModel = "", models = emptyList())
        assertEquals(defaultModels, provider.getEffectiveModels(emptyConfig))
        assertEquals(defaultModels.first(), provider.getEffectiveModel(emptyConfig))

        // When null config
        assertEquals(defaultModels, provider.getEffectiveModels(null))
        assertEquals(defaultModels.first(), provider.getEffectiveModel(null))
    }

    @Test
    fun `test AIProvider custom models and reordered list`() {
        val provider = AIProvider.OPENAI
        val customModels = listOf("my-custom-model-2", "my-custom-model-1", "gpt-4o")
        val customConfig = ProviderConfig(
            apiKey = "key",
            baseUrl = "",
            activeModel = "my-custom-model-2",
            models = customModels
        )

        val effectiveModels = provider.getEffectiveModels(customConfig)
        assertEquals(customModels, effectiveModels)
        assertEquals("my-custom-model-2", provider.getEffectiveModel(customConfig))
    }

    @Test
    fun `test AIProvider getEffectiveProviders with custom and empty order`() {
        // Empty order returns default AIProvider.entries
        val defaultProviders = AIProvider.getEffectiveProviders(emptyList())
        assertEquals(AIProvider.entries, defaultProviders)

        // Custom order places specified providers first and appends remaining
        val customOrder = listOf("CLAUDE", "DEEPSEEK", "GEMINI")
        val effectiveProviders = AIProvider.getEffectiveProviders(customOrder)
        assertEquals(AIProvider.CLAUDE, effectiveProviders[0])
        assertEquals(AIProvider.DEEPSEEK, effectiveProviders[1])
        assertEquals(AIProvider.GEMINI, effectiveProviders[2])
        assertEquals(AIProvider.entries.size, effectiveProviders.size)
        assertTrue(effectiveProviders.containsAll(AIProvider.entries))
    }

    @Test
    fun `test Kimi MiniMax and Zhipu providers configuration and default models`() {
        val mistral = AIProvider.MISTRAL
        assertEquals("Mistral", mistral.id.display)

        val kimi = AIProvider.KIMI
        assertEquals("Moonshot", kimi.id.display)
        assertTrue(kimi.defaultModelIds.contains("kimi-k3"))
        assertFalse(kimi.defaultModelIds.contains("moonshot-v1-8k"))
        assertFalse(kimi.defaultModelIds.contains("kimi-k2.5"))
        assertEquals(
            listOf("kimi-k3", "kimi-k2.7-code", "kimi-k2.7-code-highspeed", "kimi-k2.6"),
            kimi.defaultModelIds
        )
        assertEquals("kimi-k3", kimi.getEffectiveModel(null))

        val minimax = AIProvider.MINIMAX
        assertEquals("MiniMax", minimax.id.display)
        assertTrue(minimax.defaultModelIds.contains("MiniMax-M3"))
        assertFalse(minimax.defaultModelIds.contains("MiniMax-M2.5"))
        assertFalse(minimax.defaultModelIds.contains("MiniMax-M2.1"))
        assertEquals(
            listOf("MiniMax-M3", "MiniMax-M2.7", "MiniMax-M2.7-highspeed"),
            minimax.defaultModelIds
        )
        assertEquals("MiniMax-M3", minimax.getEffectiveModel(null))

        val zhipu = AIProvider.ZHIPU
        assertEquals("Zhipu", zhipu.id.display)
        assertTrue(zhipu.defaultModelIds.contains("glm-5.3"))
        assertTrue(zhipu.defaultModelIds.contains("glm-5.3-flash"))
        assertFalse(zhipu.defaultModelIds.contains("glm-5-turbo"))
        assertEquals("glm-5.3", zhipu.getEffectiveModel(null))

        // Verify that all models for OpenAI-compatible providers support OpenAIEndpoint.Completions
        listOf(AIProvider.KIMI, AIProvider.MINIMAX, AIProvider.ZHIPU).forEach { provider ->
            provider.models.forEach { model ->
                assertTrue(
                    model.supports(ai.koog.prompt.llm.LLMCapability.OpenAIEndpoint.Completions),
                    "Model ${model.id} in ${provider.name} must support OpenAIEndpoint.Completions"
                )
            }
            val customModel =
                io.github.utubesummary.llm.CustomLLModel(provider, "custom-test-model")
                    .toLLModel()
            assertTrue(
                customModel.supports(ai.koog.prompt.llm.LLMCapability.OpenAIEndpoint.Completions),
                "Custom model in ${provider.name} must support OpenAIEndpoint.Completions"
            )
        }
    }

    @Test
    fun `test LM Studio and Custom providers need a URL and take an optional token`() {
        listOf(AIProvider.LM_STUDIO, AIProvider.CUSTOM).forEach { provider ->
            assertTrue(provider.isMandatoryBaseUrl)
            assertFalse(provider.isRequiredApiKey)
            assertTrue(provider.showsApiKeyField)
            assertTrue(provider.defaultModelIds.isEmpty())
            assertEquals("", provider.getEffectiveModel(null))

            val customModel =
                io.github.utubesummary.llm.CustomLLModel(provider, "my-model").toLLModel()
            assertTrue(
                customModel.supports(ai.koog.prompt.llm.LLMCapability.OpenAIEndpoint.Completions)
            )
        }
        assertEquals("LM Studio", AIProvider.LM_STUDIO.id.display)
        assertEquals("Custom Provider", AIProvider.CUSTOM.id.display)
        // Ollama keeps its URL-only form, hosted providers keep a required key.
        assertFalse(AIProvider.OLLAMA.showsApiKeyField)
        assertTrue(AIProvider.OPENAI.showsApiKeyField)
    }

    @Test
    fun `test resolveModel without a model name fails for providers without defaults`() {
        listOf(AIProvider.LM_STUDIO, AIProvider.CUSTOM).forEach { provider ->
            org.junit.jupiter.api.Assertions.assertThrows(
                io.github.utubesummary.exception.SummaryException.ModelNotFoundException::class.java
            ) {
                io.github.utubesummary.llm.AgentConfigFactory.resolveModel(provider, null)
            }
            assertEquals(
                "my-model",
                io.github.utubesummary.llm.AgentConfigFactory.resolveModel(provider, "my-model").id
            )
        }
    }

    @Test
    fun `built-in providers have an official default URL and self-hosted ones do not`() {
        val en = java.util.Locale.ENGLISH
        val zh = java.util.Locale.forLanguageTag("zh-Hans")

        assertEquals("https://api.openai.com", AIProvider.OPENAI.defaultBaseUrl(en))
        assertEquals("https://openrouter.ai", AIProvider.OPEN_ROUTER.defaultBaseUrl(en))
        assertEquals("https://api.moonshot.ai/v1", AIProvider.KIMI.defaultBaseUrl(en))
        assertEquals("https://api.moonshot.cn/v1", AIProvider.KIMI.defaultBaseUrl(zh))
        assertTrue(AIProvider.KIMI.officialBaseUrls.contains("https://api.moonshot.cn/v1"))

        listOf(AIProvider.OLLAMA, AIProvider.LM_STUDIO, AIProvider.CUSTOM).forEach {
            assertEquals(null, it.defaultBaseUrl(en))
            assertTrue(it.officialBaseUrls.isEmpty())
        }
        // Every provider is either mandatory-URL or has a default.
        AIProvider.entries.forEach {
            assertEquals(it.isMandatoryBaseUrl, it.defaultBaseUrl(en) == null, it.name)
        }
    }

    @Test
    fun `normalizeRootUrl strips the version path Koog adds itself`() {
        assertEquals("https://openrouter.ai", AIProvider.OPEN_ROUTER.normalizeRootUrl("https://openrouter.ai/api/v1/"))
        assertEquals("https://openrouter.ai", AIProvider.OPEN_ROUTER.normalizeRootUrl("https://openrouter.ai/api"))
        assertEquals("https://api.openai.com", AIProvider.OPENAI.normalizeRootUrl("https://api.openai.com/v1"))
        assertEquals("https://api.anthropic.com", AIProvider.CLAUDE.normalizeRootUrl("https://api.anthropic.com/v1"))
        assertEquals("https://g.example.com", AIProvider.GEMINI.normalizeRootUrl("https://g.example.com/v1beta"))
        // Providers whose URL works as typed are left alone.
        assertEquals("https://api.deepseek.com/v1", AIProvider.DEEPSEEK.normalizeRootUrl("https://api.deepseek.com/v1"))
        assertEquals("http://pc:1234/v1", AIProvider.LM_STUDIO.normalizeRootUrl("http://pc:1234/v1/"))
    }
}

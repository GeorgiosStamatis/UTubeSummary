package io.github.utubesummary.llm

import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.dashscope.DashscopeModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import io.github.utubesummary.R
import io.github.utubesummary.model.ProviderConfig
import java.util.Locale

internal fun Locale.isSimplifiedChinese(): Boolean = language == "zh" && script == "Hans"

private val openAICompatibleCapabilities = listOf(
    LLMCapability.Completion,
    LLMCapability.Schema.JSON.Basic,
    LLMCapability.Schema.JSON.Standard,
    LLMCapability.OpenAIEndpoint.Completions,
)

enum class AIProvider(
    val id: LLMProvider,
    val isMandatoryBaseUrl: Boolean,
    val isRequiredApiKey: Boolean,
    val icon: Int,
    val isMonochromeIcon: Boolean = false,
    val models: List<LLModel> = emptyList(),
    val isApiKeyOptional: Boolean = false,
    val baseUrlPlaceholder: String? = null,
) {
    OPENAI(
        LLMProvider.OpenAI,
        false,
        true,
        R.drawable.chatgpt,
        true,
        OpenAIModels.models
            .filter {
                it.supports(LLMCapability.Completion)
                        && !it.supports(LLMCapability.Audio)
            }
            .distinct()
            .sortedBy { it.id }
    ),
    GEMINI(
        LLMProvider.Google,
        false,
        true,
        R.drawable.gemini,
        false,
        GoogleModels.models.filter { it.supports(LLMCapability.Completion) }
            .sortedBy { it.id }
    ),
    CLAUDE(
        LLMProvider.Anthropic,
        false,
        true,
        R.drawable.claude,
        false,
        AnthropicModels.models.filter { it.supports(LLMCapability.Completion) }
            .sortedWith(compareBy<LLModel> { model ->
                val versionRegex = Regex("(\\d[\\d.-]*\\d|\\d)")
                val match = versionRegex.find(model.id)
                match?.value?.replace('-', '.')?.toFloatOrNull() ?: Float.MAX_VALUE
            }.thenBy { it.id })
    ),
    DEEPSEEK(
        LLMProvider.DeepSeek,
        false,
        true,
        R.drawable.deepseek,
        false,
        DeepSeekModels.models.filter { it.supports(LLMCapability.Completion) }
            .sortedBy { it.id }
    ),
    MISTRAL(
        LLMProvider(LLMProvider.MistralAI.id, "Mistral"),
        false,
        true,
        R.drawable.mistral,
        false,
        MistralAIModels.models.filter { it.supports(LLMCapability.Completion) }
            .sortedBy { it.id }
    ),
    QWEN(
        LLMProvider.Alibaba,
        false,
        true,
        R.drawable.qwen,
        false,
        DashscopeModels.models.filter { it.supports(LLMCapability.Completion) }
    ),
    OLLAMA(
        LLMProvider.Ollama,
        true,
        false,
        R.drawable.ollama,
        true,
        // just name a few most popular models
        listOf(
            OllamaModels.Meta.LLAMA_3_2,
            OllamaModels.Meta.LLAMA_4,
            OllamaModels.Alibaba.QWEN_2_5_05B,
            OllamaModels.Alibaba.QWEN_3_06B,
            LLModel(
                provider = LLMProvider.Google,
                id = "gemma3n",
                capabilities = listOf(LLMCapability.Completion),
                contextLength = 32_768,
            )
        )
    ),
    LM_STUDIO(
        LLMProvider("lmstudio", "LM Studio"),
        true,
        false,
        R.drawable.lmstudio,
        true,
        emptyList(),
        isApiKeyOptional = true,
        baseUrlPlaceholder = "http://192.168.1.10:1234/v1",
    ),
    OPEN_ROUTER(
        LLMProvider.OpenRouter,
        false,
        true,
        R.drawable.openrouter,
        false,
        OpenRouterModels.models.filter { it.supports(LLMCapability.Completion) }
            .sortedBy { it.id }
    ),
    KIMI(
        LLMProvider("moonshot", "Moonshot"),
        false,
        true,
        R.drawable.kimi,
        false,
        listOf(
            LLModel(
                provider = LLMProvider("moonshot", "Moonshot"),
                id = "kimi-k3",
                capabilities = openAICompatibleCapabilities,
                contextLength = 1_000_000,
            ),
            LLModel(
                provider = LLMProvider("moonshot", "Moonshot"),
                id = "kimi-k2.7-code",
                capabilities = openAICompatibleCapabilities,
                contextLength = 256_000,
            ),
            LLModel(
                provider = LLMProvider("moonshot", "Moonshot"),
                id = "kimi-k2.7-code-highspeed",
                capabilities = openAICompatibleCapabilities,
                contextLength = 256_000,
            ),
            LLModel(
                provider = LLMProvider("moonshot", "Moonshot"),
                id = "kimi-k2.6",
                capabilities = openAICompatibleCapabilities,
                contextLength = 256_000,
            ),
        )
    ),
    MINIMAX(
        LLMProvider("minimax", "MiniMax"),
        false,
        true,
        R.drawable.minimax,
        false,
        listOf(
            LLModel(
                provider = LLMProvider("minimax", "MiniMax"),
                id = "MiniMax-M3",
                capabilities = openAICompatibleCapabilities,
                contextLength = 1_000_000,
            ),
            LLModel(
                provider = LLMProvider("minimax", "MiniMax"),
                id = "MiniMax-M2.7",
                capabilities = openAICompatibleCapabilities,
                contextLength = 204_800,
            ),
            LLModel(
                provider = LLMProvider("minimax", "MiniMax"),
                id = "MiniMax-M2.7-highspeed",
                capabilities = openAICompatibleCapabilities,
                contextLength = 204_800,
            ),
        )
    ),
    ZHIPU(
        LLMProvider("zhipu", "Zhipu"),
        false,
        true,
        R.drawable.zhipu,
        false,
        listOf(
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-5.3",
                capabilities = openAICompatibleCapabilities,
                contextLength = 1_000_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-5.3-flash",
                capabilities = openAICompatibleCapabilities,
                contextLength = 1_000_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-5.2",
                capabilities = openAICompatibleCapabilities,
                contextLength = 1_000_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-5.1",
                capabilities = openAICompatibleCapabilities,
                contextLength = 200_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-5",
                capabilities = openAICompatibleCapabilities,
                contextLength = 200_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-4.7",
                capabilities = openAICompatibleCapabilities,
                contextLength = 200_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-4.7-flash",
                capabilities = openAICompatibleCapabilities,
                contextLength = 200_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-4.7-flashx",
                capabilities = openAICompatibleCapabilities,
                contextLength = 200_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-4.6",
                capabilities = openAICompatibleCapabilities,
                contextLength = 200_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-4.5",
                capabilities = openAICompatibleCapabilities,
                contextLength = 128_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-4.5-air",
                capabilities = openAICompatibleCapabilities,
                contextLength = 128_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-4.5-flash",
                capabilities = openAICompatibleCapabilities,
                contextLength = 128_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-4.6v",
                capabilities = openAICompatibleCapabilities,
                contextLength = 128_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-4.6v-flash",
                capabilities = openAICompatibleCapabilities,
                contextLength = 128_000,
            ),
            LLModel(
                provider = LLMProvider("zhipu", "Zhipu"),
                id = "glm-4.5v",
                capabilities = openAICompatibleCapabilities,
                contextLength = 64_000,
            ),
        )
    ),
    NVIDIA(
        LLMProvider("nvidia", "NVIDIA"),
        false,
        true,
        R.drawable.nvidia,
        false,
        // A few well-known chat models; the full catalog (build.nvidia.com/models) is loaded from the
        // server with Test, and any other model id can be typed in Manage Models.
        listOf(
            "meta/llama-3.3-70b-instruct" to 128_000,
            "meta/llama-3.1-405b-instruct" to 128_000,
            "meta/llama-3.1-70b-instruct" to 128_000,
            "nvidia/llama-3.3-nemotron-super-49b-v1" to 128_000,
        ).map { (id, context) ->
            LLModel(
                provider = LLMProvider("nvidia", "NVIDIA"),
                id = id,
                capabilities = openAICompatibleCapabilities,
                contextLength = context.toLong(),
            )
        }
    ),
    CUSTOM(
        LLMProvider("custom", "Custom Provider"),
        true,
        false,
        R.drawable.custom_provider,
        true,
        emptyList(),
        isApiKeyOptional = true,
        baseUrlPlaceholder = "https://example.com/v1",
    );

    /** Whether the settings dialog shows an API key / token field for this provider. */
    val showsApiKeyField: Boolean
        get() = isRequiredApiKey || isApiKeyOptional

    /**
     * The provider's official endpoint, or null when the user must supply one (self-hosted).
     * Some defaults differ for Simplified Chinese users.
     */
    fun defaultBaseUrl(locale: Locale): String? {
        val chinese = locale.isSimplifiedChinese()
        return when (this) {
            OPENAI -> "https://api.openai.com"
            GEMINI -> "https://generativelanguage.googleapis.com"
            CLAUDE -> "https://api.anthropic.com"
            DEEPSEEK -> "https://api.deepseek.com"
            MISTRAL -> "https://api.mistral.ai"
            OPEN_ROUTER -> "https://openrouter.ai"
            QWEN -> if (chinese) "https://dashscope.aliyuncs.com" else "https://dashscope-intl.aliyuncs.com"
            KIMI -> if (chinese) "https://api.moonshot.cn/v1" else "https://api.moonshot.ai/v1"
            MINIMAX -> if (chinese) "https://api.minimaxi.com/v1" else "https://api.minimax.io/v1"
            NVIDIA -> "https://integrate.api.nvidia.com/v1"
            ZHIPU -> if (chinese) "https://open.bigmodel.cn/api/paas/v4" else "https://api.z.ai/api/paas/v4"
            OLLAMA, LM_STUDIO, CUSTOM -> null
        }
    }

    /**
     * Reduces [url] to the root Koog expects. Koog adds the version path itself for these
     * providers (OpenAI `v1/...`, Anthropic `v1/...`, Gemini `v1beta/...`, OpenRouter
     * `api/v1/...`), so a pasted `https://openrouter.ai/api/v1` would otherwise end up as
     * `.../api/v1/api/v1/chat/completions` (HTTP 404). Other providers keep their URL as typed.
     */
    fun normalizeRootUrl(url: String): String {
        val trimmed = url.trim().trimEnd('/')
        val suffixes = when (this) {
            OPENAI, CLAUDE -> listOf("/v1")
            GEMINI -> listOf("/v1beta", "/v1")
            OPEN_ROUTER -> listOf("/api/v1", "/api")
            else -> emptyList()
        }
        val suffix = suffixes.firstOrNull { trimmed.endsWith(it, ignoreCase = true) }
        return if (suffix != null) trimmed.dropLast(suffix.length) else trimmed
    }

    /** Every official endpoint this provider can default to, across locales. */
    val officialBaseUrls: Set<String>
        get() = listOf(Locale.ENGLISH, Locale.forLanguageTag("zh-Hans"))
            .mapNotNull { defaultBaseUrl(it)?.trimEnd('/') }
            .toSet()

    val defaultModelIds: List<String>
        get() = models.map { it.id }

    fun getEffectiveModels(providerConfig: ProviderConfig?): List<String> =
        providerConfig?.models?.takeIf { it.isNotEmpty() } ?: defaultModelIds

    fun getEffectiveModel(providerConfig: ProviderConfig?): String {
        val effectiveModels = getEffectiveModels(providerConfig)
        return providerConfig?.activeModel?.takeIf { it.isNotBlank() && it in effectiveModels }
            ?: effectiveModels.firstOrNull()
            ?: defaultModelIds.firstOrNull()
            ?: ""
    }

    companion object {
        fun getEffectiveProviders(order: List<String>): List<AIProvider> {
            if (order.isEmpty()) return entries
            val ordered = order.mapNotNull { name ->
                runCatching { valueOf(name) }.getOrNull()
            }
            val missing = entries.filter { it !in ordered }
            return ordered + missing
        }
    }
}

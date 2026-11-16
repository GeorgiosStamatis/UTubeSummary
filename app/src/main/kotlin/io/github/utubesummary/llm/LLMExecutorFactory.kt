package io.github.utubesummary.llm

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.dashscope.DashscopeClientSettings
import ai.koog.prompt.executor.clients.dashscope.DashscopeLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekClientSettings
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.google.GoogleClientSettings
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.mistralai.MistralAIClientSettings
import ai.koog.prompt.executor.clients.mistralai.MistralAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterClientSettings
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import io.ktor.client.HttpClient
import java.net.URI
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val MAX_EXECUTOR_CACHE_SIZE = 10

data class ExecutorCacheKey(
    val provider: AIProvider,
    val apiKey: String,
    val baseUrl: String?,
    val appLanguage: Locale,
)

class LLMExecutorFactory(
    httpClient: HttpClient,
) {
    private val koogHttpClientFactory = KtorKoogHttpClient.Factory(httpClient)
    private val geminiKoogHttpClientFactory = KtorKoogHttpClient.Factory(
        HttpClient(GeminiSanitizingHttpClientEngine(httpClient.engine))
    )

    private val executorCache = ConcurrentHashMap<ExecutorCacheKey, PromptExecutor>()

    fun getOrCreateExecutor(
        provider: AIProvider,
        apiKey: String,
        baseUrl: String?,
        appLanguage: Locale,
    ): PromptExecutor {
        val cacheKey = ExecutorCacheKey(provider, apiKey, baseUrl, appLanguage)
        return executorCache.computeIfAbsent(cacheKey) {
            if (executorCache.size > MAX_EXECUTOR_CACHE_SIZE) {
                executorCache.clear()
            }
            createExecutor(provider, apiKey, baseUrl, appLanguage)
        }
    }

    private fun createExecutor(
        provider: AIProvider,
        apiKey: String,
        baseUrl: String?,
        appLanguage: Locale,
    ): PromptExecutor {
        return when (provider) {
            AIProvider.OPENAI -> createOpenAIExecutor(apiKey, baseUrl)
            AIProvider.GEMINI -> createGeminiExecutor(apiKey, baseUrl)
            AIProvider.CLAUDE -> createClaudeExecutor(apiKey, baseUrl)
            AIProvider.DEEPSEEK -> createDeepSeekExecutor(apiKey, baseUrl)
            AIProvider.MISTRAL -> createMistralExecutor(apiKey, baseUrl)
            AIProvider.QWEN -> createQwenExecutor(apiKey, baseUrl, appLanguage)
            AIProvider.OLLAMA -> createOllamaExecutor(baseUrl)
            AIProvider.OPEN_ROUTER -> createOpenRouterExecutor(apiKey, baseUrl)
            AIProvider.KIMI -> createKimiExecutor(apiKey, baseUrl, appLanguage)
            AIProvider.MINIMAX -> createMiniMaxExecutor(apiKey, baseUrl, appLanguage)
            AIProvider.ZHIPU -> createZhipuExecutor(apiKey, baseUrl, appLanguage)
            AIProvider.LM_STUDIO, AIProvider.CUSTOM ->
                createOpenAICompatibleExecutor(provider, apiKey, baseUrl)
        }
    }

    private fun rootUrlOrNull(provider: AIProvider, baseUrl: String?): String? =
        baseUrl?.let { provider.normalizeRootUrl(it) }?.takeIf { it.isNotBlank() }

    private fun createOpenAIExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = rootUrlOrNull(AIProvider.OPENAI, baseUrl)
            ?.let {
                OpenAILLMClient(
                    apiKey,
                    settings = OpenAIClientSettings(baseUrl = it),
                    httpClientFactory = koogHttpClientFactory
                )
            }
            ?: OpenAILLMClient(apiKey, httpClientFactory = koogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createGeminiExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = rootUrlOrNull(AIProvider.GEMINI, baseUrl)
            ?.let {
                GoogleLLMClient(
                    apiKey,
                    settings = GoogleClientSettings(baseUrl = it),
                    httpClientFactory = geminiKoogHttpClientFactory
                )
            }
            ?: GoogleLLMClient(apiKey, httpClientFactory = geminiKoogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createClaudeExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = rootUrlOrNull(AIProvider.CLAUDE, baseUrl)
            ?.let {
                AnthropicLLMClient(
                    apiKey,
                    settings = AnthropicClientSettings(baseUrl = it),
                    httpClientFactory = koogHttpClientFactory
                )
            }
            ?: AnthropicLLMClient(apiKey, httpClientFactory = koogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createDeepSeekExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let {
                DeepSeekLLMClient(
                    apiKey,
                    settings = DeepSeekClientSettings(baseUrl = it),
                    httpClientFactory = koogHttpClientFactory
                )
            }
            ?: DeepSeekLLMClient(apiKey, httpClientFactory = koogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createMistralExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let {
                MistralAILLMClient(
                    apiKey,
                    settings = MistralAIClientSettings(baseUrl = it),
                    httpClientFactory = koogHttpClientFactory
                )
            }
            ?: MistralAILLMClient(apiKey, httpClientFactory = koogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createQwenExecutor(
        apiKey: String,
        baseUrl: String?,
        appLanguage: Locale,
    ): PromptExecutor {
        // Simplified Chinese always uses the mainland endpoint.
        val finalBaseUrl = if (appLanguage.isSimplifiedChinese()) {
            AIProvider.QWEN.defaultBaseUrl(appLanguage)
        } else {
            baseUrl
        }
        val client = finalBaseUrl?.takeIf { it.isNotBlank() }
            ?.let {
                DashscopeLLMClient(
                    apiKey,
                    settings = DashscopeClientSettings(baseUrl = it),
                    httpClientFactory = koogHttpClientFactory
                )
            }
            ?: DashscopeLLMClient(apiKey, httpClientFactory = koogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createOllamaExecutor(baseUrl: String?): PromptExecutor {
        val client = baseUrl?.takeIf { it.isNotBlank() }
            ?.let { OllamaClient(baseUrl = it, httpClientFactory = koogHttpClientFactory) }
            ?: throw IllegalArgumentException("Base URL is required for Ollama")

        return MultiLLMPromptExecutor(client)
    }

    private fun createOpenRouterExecutor(apiKey: String, baseUrl: String?): PromptExecutor {
        val client = rootUrlOrNull(AIProvider.OPEN_ROUTER, baseUrl)
            ?.let {
                OpenRouterLLMClient(
                    apiKey,
                    settings = OpenRouterClientSettings(baseUrl = it),
                    httpClientFactory = koogHttpClientFactory
                )
            }
            ?: OpenRouterLLMClient(apiKey, httpClientFactory = koogHttpClientFactory)

        return MultiLLMPromptExecutor(client)
    }

    private fun createKimiExecutor(
        apiKey: String,
        baseUrl: String?,
        appLanguage: Locale,
    ): PromptExecutor {
        val defaultBaseUrl = AIProvider.KIMI.defaultBaseUrl(appLanguage)!!
        val finalBaseUrl = (baseUrl?.takeIf { it.isNotBlank() } ?: defaultBaseUrl).trimEnd('/')

        val client = OpenAILLMClient(
            apiKey,
            settings = OpenAIClientSettings(
                baseUrl = finalBaseUrl,
                chatCompletionsPath = "chat/completions",
            ),
            httpClientFactory = koogHttpClientFactory
        )
        return MultiLLMPromptExecutor(AIProvider.KIMI.id to client)
    }

    private fun createMiniMaxExecutor(
        apiKey: String,
        baseUrl: String?,
        appLanguage: Locale,
    ): PromptExecutor {
        val defaultBaseUrl = AIProvider.MINIMAX.defaultBaseUrl(appLanguage)!!
        val finalBaseUrl = (baseUrl?.takeIf { it.isNotBlank() } ?: defaultBaseUrl).trimEnd('/')

        val client = OpenAILLMClient(
            apiKey,
            settings = OpenAIClientSettings(
                baseUrl = finalBaseUrl,
                chatCompletionsPath = "chat/completions",
            ),
            httpClientFactory = koogHttpClientFactory
        )
        return MultiLLMPromptExecutor(AIProvider.MINIMAX.id to client)
    }

    private fun createZhipuExecutor(
        apiKey: String,
        baseUrl: String?,
        appLanguage: Locale,
    ): PromptExecutor {
        val defaultBaseUrl = AIProvider.ZHIPU.defaultBaseUrl(appLanguage)!!
        val finalBaseUrl = (baseUrl?.takeIf { it.isNotBlank() } ?: defaultBaseUrl).trimEnd('/')

        val client = OpenAILLMClient(
            apiKey,
            settings = OpenAIClientSettings(
                baseUrl = finalBaseUrl,
                chatCompletionsPath = "chat/completions",
            ),
            httpClientFactory = koogHttpClientFactory
        )
        return MultiLLMPromptExecutor(AIProvider.ZHIPU.id to client)
    }

    /**
     * Executor for self-hosted / third-party OpenAI-compatible servers (LM Studio, custom).
     *
     * The URL may be given with or without a path prefix: a bare host (`http://pc:1234`) uses
     * `/v1/chat/completions`, while a URL that already has a path (`http://pc:1234/v1`) uses
     * `<path>/chat/completions`. The API key is optional; local servers usually ignore it.
     */
    private fun createOpenAICompatibleExecutor(
        provider: AIProvider,
        apiKey: String,
        baseUrl: String?,
    ): PromptExecutor {
        val url = baseUrl?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Base URL is required for ${provider.id.display}")
        val hasPath = runCatching { URI(url).path }.getOrNull().isNullOrEmpty().not()

        val client = OpenAILLMClient(
            apiKey.ifBlank { "not-needed" },
            settings = OpenAIClientSettings(
                baseUrl = url,
                chatCompletionsPath = if (hasPath) "chat/completions" else "v1/chat/completions",
            ),
            httpClientFactory = koogHttpClientFactory
        )
        return MultiLLMPromptExecutor(provider.id to client)
    }
}

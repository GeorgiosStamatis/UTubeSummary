package io.github.utubesummary.data.provider

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import io.github.utubesummary.domain.provider.ProviderConnectionChecker
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ProviderTestResult
import java.net.URI
import java.util.Locale
import javax.inject.Inject

private const val TIMEOUT_MS = 15_000L

internal data class ConnectionRequest(val url: String, val headers: Map<String, String>)

/**
 * Builds a cheap, token-free request (list models) that exercises both the URL and the key.
 * Defaults mirror the ones used by [io.github.utubesummary.llm.LLMExecutorFactory].
 */
internal fun buildConnectionRequest(
    provider: AIProvider,
    baseUrl: String,
    apiKey: String,
    locale: Locale,
): ConnectionRequest {
    val isSimplifiedChinese = locale.language == "zh" && locale.script == "Hans"
    val custom = baseUrl.trim().trimEnd('/')
    fun base(default: String) = custom.ifBlank { default }

    // "<host>" -> "<host>/<versioned>/models", "<host>/<path>" -> "<host>/<path>/models"
    fun modelsUrl(root: String, versionedPath: String): String {
        val hasPath = runCatching { URI(root).path }.getOrNull().isNullOrEmpty().not()
        return if (hasPath) "$root/models" else "$root/$versionedPath/models"
    }

    val bearer = if (apiKey.isNotBlank()) mapOf("Authorization" to "Bearer $apiKey") else emptyMap()

    return when (provider) {
        AIProvider.OPENAI -> ConnectionRequest(
            modelsUrl(base("https://api.openai.com"), "v1"), bearer
        )

        AIProvider.DEEPSEEK -> ConnectionRequest(
            modelsUrl(base("https://api.deepseek.com"), "v1"), bearer
        )

        AIProvider.MISTRAL -> ConnectionRequest(
            modelsUrl(base("https://api.mistral.ai"), "v1"), bearer
        )

        AIProvider.OPEN_ROUTER -> ConnectionRequest(
            modelsUrl(base("https://openrouter.ai/api"), "v1"), bearer
        )

        AIProvider.QWEN -> {
            val root = if (isSimplifiedChinese) {
                "https://dashscope.aliyuncs.com"
            } else {
                base("https://dashscope-intl.aliyuncs.com")
            }
            ConnectionRequest("$root/compatible-mode/v1/models", bearer)
        }

        AIProvider.KIMI -> ConnectionRequest(
            modelsUrl(
                base(if (isSimplifiedChinese) "https://api.moonshot.cn/v1" else "https://api.moonshot.ai/v1"),
                "v1"
            ), bearer
        )

        AIProvider.MINIMAX -> ConnectionRequest(
            modelsUrl(
                base(if (isSimplifiedChinese) "https://api.minimaxi.com/v1" else "https://api.minimax.io/v1"),
                "v1"
            ), bearer
        )

        AIProvider.ZHIPU -> ConnectionRequest(
            modelsUrl(
                base(if (isSimplifiedChinese) "https://open.bigmodel.cn/api/paas/v4" else "https://api.z.ai/api/paas/v4"),
                "v4"
            ), bearer
        )

        AIProvider.LM_STUDIO, AIProvider.CUSTOM -> ConnectionRequest(
            modelsUrl(custom, "v1"), bearer
        )

        AIProvider.CLAUDE -> ConnectionRequest(
            modelsUrl(base("https://api.anthropic.com"), "v1") + "?limit=1000",
            mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01")
        )

        AIProvider.GEMINI -> ConnectionRequest(
            modelsUrl(base("https://generativelanguage.googleapis.com"), "v1beta") + "?pageSize=1000",
            mapOf("x-goog-api-key" to apiKey)
        )

        AIProvider.OLLAMA -> ConnectionRequest("$custom/api/tags", emptyMap())
    }
}

class KtorProviderConnectionChecker @Inject constructor(
    private val httpClient: HttpClient,
) : ProviderConnectionChecker {

    override suspend fun check(
        provider: AIProvider,
        baseUrl: String,
        apiKey: String,
        locale: Locale,
    ): ProviderTestResult = withContext(Dispatchers.IO) {
        try {
            val request = buildConnectionRequest(provider, baseUrl, apiKey, locale)
            val (status, body) = withTimeoutOrNull(TIMEOUT_MS) {
                val response = httpClient.get(request.url) {
                    request.headers.forEach { (name, value) -> header(name, value) }
                }
                response.status.value to
                        if (response.status.value in 200..299) response.bodyAsText() else ""
            } ?: return@withContext ProviderTestResult.Unreachable

            when (status) {
                in 200..299 -> ProviderTestResult.Success(
                    keyChecked = apiKey.isNotBlank(),
                    models = parseModelIds(provider, body),
                )
                401, 403 -> ProviderTestResult.AuthFailed(status)
                404 -> ProviderTestResult.EndpointNotFound
                else -> ProviderTestResult.HttpError(status)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ProviderTestResult.Unreachable
        }
    }
}

private val OPENAI_NON_CHAT_MODEL = Regex(
    "embed|whisper|tts|dall-e|moderation|audio|realtime|transcribe|image|davinci|babbage|sora",
    RegexOption.IGNORE_CASE
)

/** Extracts a sorted, de-duplicated list of chat-capable model ids from a "list models" body. */
internal fun parseModelIds(provider: AIProvider, body: String): List<String> {
    val root: JsonElement = runCatching { Json.parseToJsonElement(body) }.getOrNull()
        ?: return emptyList()

    fun JsonElement.items(key: String): List<JsonObject> =
        (this as? JsonObject)?.get(key)?.let { it as? JsonArray }
            ?.mapNotNull { it as? JsonObject }.orEmpty()

    fun JsonObject.text(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull

    val ids = when (provider) {
        AIProvider.OLLAMA -> root.items("models").mapNotNull { it.text("name") }

        AIProvider.GEMINI -> root.items("models")
            .filter { model ->
                val methods = (model["supportedGenerationMethods"] as? JsonArray)
                    ?.map { it.jsonPrimitive.content }
                methods == null || "generateContent" in methods
            }
            .mapNotNull { it.text("name")?.removePrefix("models/") }

        AIProvider.OPENAI -> root.items("data").mapNotNull { it.text("id") }
            .filterNot { OPENAI_NON_CHAT_MODEL.containsMatchIn(it) }

        else -> root.items("data").mapNotNull { it.text("id") }
    }
    return ids.filter { it.isNotBlank() }.distinct().sorted()
}

package io.github.utubesummary.llm

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel

private val openAIEndpointProviders = setOf(
    AIProvider.OPENAI,
    AIProvider.KIMI,
    AIProvider.MINIMAX,
    AIProvider.ZHIPU,
    AIProvider.LM_STUDIO,
    AIProvider.CUSTOM,
)

data class CustomLLModel(
    val provider: AIProvider,
    val name: String,
) {

    fun toLLModel(): LLModel = LLModel(
        provider = provider.id,
        id = name,
        // The real size is unknown for custom models; assume a modern 128k window.
        contextLength = 128_000,
        capabilities = listOfNotNull(
            LLMCapability.Completion,
            LLMCapability.Schema.JSON.Basic,
            LLMCapability.Schema.JSON.Standard,
            if (provider in openAIEndpointProviders) {
                LLMCapability.OpenAIEndpoint.Completions
            } else null,
            if (provider == AIProvider.OPENAI) LLMCapability.OpenAIEndpoint.Responses else null,
        ),
    )
}

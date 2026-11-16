package io.github.utubesummary.domain.provider

import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ProviderTestResult
import java.util.Locale

/** Checks that an AI provider is reachable and accepts the given credentials. */
interface ProviderConnectionChecker {
    suspend fun check(
        provider: AIProvider,
        baseUrl: String,
        apiKey: String,
        locale: Locale,
    ): ProviderTestResult
}

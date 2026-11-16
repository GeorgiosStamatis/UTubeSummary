package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.provider.AppLocaleProvider
import io.github.utubesummary.domain.provider.ProviderConnectionChecker
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.ProviderTestResult
import javax.inject.Inject

class TestProviderConnectionUseCase @Inject constructor(
    private val checker: ProviderConnectionChecker,
    private val appLocaleProvider: AppLocaleProvider,
) {
    suspend operator fun invoke(
        provider: AIProvider,
        baseUrl: String,
        apiKey: String,
    ): ProviderTestResult = checker.check(
        provider = provider,
        baseUrl = normalizeProviderBaseUrl(baseUrl),
        apiKey = apiKey.trim(),
        locale = appLocaleProvider.getCurrentLocale(),
    )
}

package io.github.utubesummary.model

/** Outcome of a "Test connection" check against an AI provider. */
sealed interface ProviderTestResult {
    /**
     * The server answered and accepted the request. [keyChecked] is true when a key was sent;
     * [models] are the model ids the server reported (empty if none or not parseable).
     */
    data class Success(
        val keyChecked: Boolean,
        val models: List<String> = emptyList(),
    ) : ProviderTestResult

    /** HTTP 401/403: the server rejected the credentials. */
    data class AuthFailed(val statusCode: Int) : ProviderTestResult

    /** HTTP 404: the server answered but the models endpoint was not found (wrong URL?). */
    data object EndpointNotFound : ProviderTestResult

    /** Any other HTTP error. */
    data class HttpError(val statusCode: Int) : ProviderTestResult

    /** No answer: bad host, no network, TLS error, timeout... */
    data object Unreachable : ProviderTestResult
}

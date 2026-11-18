package io.github.utubesummary.llm.tools

import io.github.utubesummary.exception.SummaryException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The identity the app presents to YouTube's internal player API ("innertube"). YouTube can stop
 * accepting an old version without notice, so everything that identifies the client lives here.
 */
internal data class InnertubeClient(
    val name: String,
    val version: String,
    /** `User-Agent` of the real app this client imitates; null sends none (what the app always did). */
    val userAgent: String? = null,
    /** Numeric id YouTube uses for this client in `X-YouTube-Client-Name`; null sends no client headers. */
    val clientNameId: Int? = null,
    /** Extra fields of the `client` object (device, OS, language, region). */
    val extraContext: Map<String, JsonElement> = emptyMap(),
) {
    /** Short text for logs and error details, e.g. "ANDROID 20.10.38". */
    val label: String get() = "$name $version"
}

/**
 * The client the app has always used. The value is the one of the Python library
 * `youtube-transcript-api` that this code follows (ANDROID client, version 20.10.38); last checked
 * working in November 2026. If YouTube starts refusing it, this is the line to update.
 */
internal val ANDROID_LEGACY = InnertubeClient(name = "ANDROID", version = "20.10.38")

/**
 * The same client with everything the real YouTube Android app sends: its `User-Agent`, the client
 * headers, device/OS fields and language/region. Tried after [ANDROID_LEGACY] fails; the values are
 * those of the YouTube Android app of that version.
 */
internal val ANDROID_FULL = InnertubeClient(
    name = "ANDROID",
    version = "20.10.38",
    userAgent = "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip",
    clientNameId = 3,
    extraContext = mapOf(
        "androidSdkVersion" to JsonPrimitive(30),
        "osName" to JsonPrimitive("Android"),
        "osVersion" to JsonPrimitive("11"),
        "hl" to JsonPrimitive("en"),
        "gl" to JsonPrimitive("US"),
    ),
)

/**
 * The YouTube iOS app. A different client family: if YouTube changes what it accepts from Android
 * clients, this one may still work. Verified working in the probe of November 2026.
 */
internal val IOS = InnertubeClient(
    name = "IOS",
    version = "20.10.4",
    userAgent = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
    clientNameId = 5,
    extraContext = mapOf(
        "deviceMake" to JsonPrimitive("Apple"),
        "deviceModel" to JsonPrimitive("iPhone16,2"),
        "osName" to JsonPrimitive("iPhone"),
        "osVersion" to JsonPrimitive("18.3.2.22D82"),
        "hl" to JsonPrimitive("en"),
        "gl" to JsonPrimitive("US"),
    ),
)

/** One way of asking YouTube: as [client], with or without the public API key from the watch page. */
internal data class ClientAttempt(val client: InnertubeClient, val useApiKey: Boolean) {
    /** Text for logs and error details, e.g. "ANDROID 20.10.38 +key". */
    val label: String get() = "${client.label} ${if (useApiKey) "+key" else "no key"}"
}

/**
 * The order in which YouTube is asked. The first is exactly what the app has always sent; the
 * others only run when it fails. The probe of November 2026 showed that the Android and iOS clients
 * work with and without the key, and that the TV and web clients do not work as specified, so those
 * are not listed.
 */
internal val YOUTUBE_ATTEMPTS = listOf(
    ClientAttempt(ANDROID_LEGACY, useApiKey = true),
    ClientAttempt(ANDROID_FULL, useApiKey = false),
    ClientAttempt(IOS, useApiKey = false),
)

/** The HTTP headers that go with a request (player API or subtitle download) made as [client]. */
internal fun requestHeaders(client: InnertubeClient): Map<String, String> = buildMap {
    client.userAgent?.let { put("User-Agent", it) }
    client.clientNameId?.let { put("X-YouTube-Client-Name", it.toString()) }
    if (client.clientNameId != null) put("X-YouTube-Client-Version", client.version)
}

/** The JSON body of a `/youtubei/v1/player` request for [videoId] as seen by [client]. */
internal fun playerRequestBody(client: InnertubeClient, videoId: String): JsonObject = buildJsonObject {
    put("context", buildJsonObject {
        put("client", buildJsonObject {
            put("clientName", client.name)
            put("clientVersion", client.version)
            client.extraContext.forEach { (key, value) -> put(key, value) }
        })
    })
    put("videoId", videoId)
}

/** What asking YouTube for the player data led to. */
internal sealed interface PlayerOutcome {
    /** The video can be played; [response] holds its details and caption tracks. */
    data class Playable(val response: JsonObject) : PlayerOutcome

    /**
     * YouTube refuses the video: private, age or login restricted, removed. [retryable] is true for a
     * "sign in" answer, which another client may not get.
     */
    data class Restricted(val reason: String, val retryable: Boolean = false) : PlayerOutcome

    /** YouTube answered a request with an HTTP error. */
    data class HttpError(val step: String, val status: Int) : PlayerOutcome

    /** The page that carries the public API key had no key (consent page, changed layout). */
    data class NoApiKey(val step: String) : PlayerOutcome

    /** The answer was not the JSON that was expected. */
    data class BadBody(val step: String) : PlayerOutcome
}

/** [Restricted] when the answer says the video cannot be played, otherwise [PlayerOutcome.Playable]. */
internal fun classifyPlayerResponse(response: JsonObject): PlayerOutcome =
    playabilityProblem(response)?.let { reason ->
        PlayerOutcome.Restricted(reason, retryable = playabilityStatus(response) == "LOGIN_REQUIRED")
    } ?: PlayerOutcome.Playable(response)

/** What went wrong with an outcome that is not playable, for logs and error details. */
internal fun PlayerOutcome.failureDetail(): String = when (this) {
    is PlayerOutcome.Restricted -> reason
    is PlayerOutcome.HttpError -> "$step: HTTP $status"
    is PlayerOutcome.NoApiKey -> "$step: no API key found"
    is PlayerOutcome.BadBody -> "$step: unexpected answer"
    is PlayerOutcome.Playable -> "unexpected playable outcome"
}

/** The error to show for a [PlayerOutcome] that is not playable; only a real restriction says "unavailable". */
internal fun PlayerOutcome.toFailure(client: InnertubeClient): SummaryException = when (this) {
    is PlayerOutcome.Restricted -> SummaryException.VideoUnavailableException(reason)
    else -> extractionFailed(client, failureDetail())
}

/** "YouTube changed something" error; [detail] and the client are shown behind "Show details". */
internal fun extractionFailed(client: InnertubeClient, detail: String): SummaryException.ExtractionFailedException =
    SummaryException.ExtractionFailedException("${client.label} · $detail")

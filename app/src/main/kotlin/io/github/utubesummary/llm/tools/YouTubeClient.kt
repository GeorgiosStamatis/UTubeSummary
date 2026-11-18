package io.github.utubesummary.llm.tools

import io.github.utubesummary.exception.SummaryException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The identity the app presents to YouTube's internal player API ("innertube"). YouTube can stop
 * accepting an old version without notice, so everything that identifies the client lives here.
 */
internal data class InnertubeClient(val name: String, val version: String) {
    /** Short text for logs and error details, e.g. "ANDROID 20.10.38". */
    val label: String get() = "$name $version"
}

/**
 * The client the app has always used. The value is the one of the Python library
 * `youtube-transcript-api` that this code follows (ANDROID client, version 20.10.38); last checked
 * working in November 2026. If YouTube starts refusing it, this is the line to update.
 */
internal val ANDROID_LEGACY = InnertubeClient(name = "ANDROID", version = "20.10.38")

/** The JSON body of a `/youtubei/v1/player` request for [videoId] as seen by [client]. */
internal fun playerRequestBody(client: InnertubeClient, videoId: String): JsonObject = buildJsonObject {
    put("context", buildJsonObject {
        put("client", buildJsonObject {
            put("clientName", client.name)
            put("clientVersion", client.version)
        })
    })
    put("videoId", videoId)
}

/** What asking YouTube for the player data led to. */
internal sealed interface PlayerOutcome {
    /** The video can be played; [response] holds its details and caption tracks. */
    data class Playable(val response: JsonObject) : PlayerOutcome

    /** YouTube refuses the video itself: private, age or login restricted, removed. */
    data class Restricted(val reason: String) : PlayerOutcome

    /** YouTube answered a request with an HTTP error. */
    data class HttpError(val step: String, val status: Int) : PlayerOutcome

    /** The page that carries the public API key had no key (consent page, changed layout). */
    data class NoApiKey(val step: String) : PlayerOutcome

    /** The answer was not the JSON that was expected. */
    data class BadBody(val step: String) : PlayerOutcome
}

/** [Restricted] when the answer says the video cannot be played, otherwise [PlayerOutcome.Playable]. */
internal fun classifyPlayerResponse(response: JsonObject): PlayerOutcome =
    playabilityProblem(response)?.let { PlayerOutcome.Restricted(it) } ?: PlayerOutcome.Playable(response)

/** The error to show for a [PlayerOutcome] that is not playable; only a real restriction says "unavailable". */
internal fun PlayerOutcome.toFailure(client: InnertubeClient): SummaryException = when (this) {
    is PlayerOutcome.Restricted -> SummaryException.VideoUnavailableException(reason)
    is PlayerOutcome.HttpError -> extractionFailed(client, "$step: HTTP $status")
    is PlayerOutcome.NoApiKey -> extractionFailed(client, "$step: no API key found")
    is PlayerOutcome.BadBody -> extractionFailed(client, "$step: unexpected answer")
    is PlayerOutcome.Playable -> extractionFailed(client, "unexpected playable outcome")
}

/** "YouTube changed something" error; [detail] and the client are shown behind "Show details". */
internal fun extractionFailed(client: InnertubeClient, detail: String): SummaryException.ExtractionFailedException =
    SummaryException.ExtractionFailedException("${client.label} · $detail")
